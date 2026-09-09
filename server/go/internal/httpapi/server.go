package httpapi

import (
	"context"
	"crypto/sha256"
	"crypto/tls"
	"encoding/hex"
	"encoding/json"
	"errors"
	"io"
	"log"
	"net"
	"net/http"
	"strings"
	"sync"
	"time"

	"github.com/dfa-ra/rope/server/go/internal/authz"
	"github.com/dfa-ra/rope/server/go/internal/config"
	"github.com/dfa-ra/rope/server/go/internal/db"
	"github.com/dfa-ra/rope/server/go/internal/envelope"
	"github.com/dfa-ra/rope/server/go/internal/login"
	"github.com/dfa-ra/rope/server/go/internal/ratelimit"
	"github.com/go-chi/chi/v5"

	"github.com/coder/websocket"
	"github.com/coder/websocket/wsjson"
	"github.com/google/uuid"
)

type Server struct {
	Cfg          config.Config
	Store        *db.Store
	Log          *log.Logger
	Hub          *Hub
	Limit        *ratelimit.Limiter
	CallAudio    *ratelimit.Limiter
	CallRing     *ratelimit.Limiter
	CallRelay    *ratelimit.Limiter
	FP           string
	setup        string
	pendingMu    sync.Mutex
	pendingCalls map[string]pendingCall
}

func New(cfg config.Config, store *db.Store, logger *log.Logger) *Server {
	if logger == nil {
		logger = log.Default()
	}
	s := &Server{
		Cfg:          cfg,
		Store:        store,
		Log:          logger,
		Hub:          NewHub(),
		Limit:        ratelimit.New(60, time.Minute),
		CallAudio:    ratelimit.New(callAudioPerSec, time.Second),
		CallRing:     ratelimit.New(callRingBurst, callRingWindow),
		CallRelay:    ratelimit.New(callRingBurst, callRingWindow),
		setup:        cfg.SetupToken,
		pendingCalls: map[string]pendingCall{},
	}
	if cfg.FingerprintOverride != "" {
		s.FP = cfg.FingerprintOverride
	} else if cfg.AllowHTTP {
		s.FP = config.HTTPDevFingerprint()
	}
	return s
}

func (s *Server) Router() http.Handler {
	r := chi.NewRouter()
	r.Use(s.recoverer)
	r.Get("/health", s.health)
	r.Get("/version", s.version)
	r.Get("/v1/info", s.info)
	r.Post("/v1/bootstrap", s.bootstrap)
	r.Get("/v1/directory", s.withAuth(s.directory))
	r.Post("/v1/invites", s.withAuth(s.createInvite))
	r.Get("/v1/admin/status", s.withAuth(s.adminStatus))
	r.Post("/v1/admin/revoke-member", s.withAuth(s.revokeMember))
	r.Post("/v1/admin/revoke-device", s.withAuth(s.revokeDevice))
	r.Post("/v1/objects", s.withAuthLimit(int64(s.objectLimit())+1, s.uploadObject))
	r.Get("/v1/objects/{id}", s.withAuth(s.downloadObject))
	r.Post("/v1/groups", s.withAuth(s.createGroup))
	r.Get("/v1/groups", s.withAuth(s.listGroups))
	r.Patch("/v1/groups/{id}", s.withAuth(s.renameGroup))
	r.Post("/v1/groups/{id}/members", s.withAuth(s.groupAdd))
	r.Post("/v1/groups/{id}/remove", s.withAuth(s.groupRemove))
	r.Get("/v1/ws", s.ws)
	return r
}

func (s *Server) objectLimit() int {
	if s.Cfg.MaxObjectBytes > 0 {
		return s.Cfg.MaxObjectBytes
	}
	return 25 * 1024 * 1024
}

func (s *Server) ListenAndServe() error {
	h := &http.Server{
		Addr:              s.Cfg.Listen,
		Handler:           s.Router(),
		ReadHeaderTimeout: 10 * time.Second,
	}
	s.logTurn()
	if s.Cfg.AllowHTTP {
		s.Log.Printf("rope-server %s listening http://%s (debug)", config.ServerVersion, s.Cfg.Listen)
		return h.ListenAndServe()
	}
	cert, err := tls.LoadX509KeyPair(s.Cfg.TLSCert, s.Cfg.TLSKey)
	if err != nil {
		return err
	}
	if s.FP == "" && len(cert.Certificate) > 0 {
		sum := sha256.Sum256(cert.Certificate[0])
		s.FP = hex.EncodeToString(sum[:])
	}
	h.TLSConfig = &tls.Config{MinVersion: tls.VersionTLS12, Certificates: []tls.Certificate{cert}}
	s.Log.Printf("rope-server %s listening https://%s fp=%s", config.ServerVersion, s.Cfg.Listen, s.FP)
	ln, err := net.Listen("tcp", s.Cfg.Listen)
	if err != nil {
		return err
	}
	return h.Serve(tls.NewListener(ln, h.TLSConfig))
}

func (s *Server) recoverer(next http.Handler) http.Handler {
	return http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		defer func() {
			if rec := recover(); rec != nil {
				s.Log.Printf("panic: %v", rec)
				http.Error(w, `{"error":"internal"}`, http.StatusInternalServerError)
			}
		}()
		next.ServeHTTP(w, r)
	})
}

func writeJSON(w http.ResponseWriter, status int, v any) {
	w.Header().Set("Content-Type", "application/json")
	w.Header().Set("Cache-Control", "no-store")
	w.WriteHeader(status)
	_ = json.NewEncoder(w).Encode(v)
}

func (s *Server) logTurn() {
	if !s.Cfg.IceEnabled() {
		s.Log.Printf("ICE disabled (need public_host and turn_secret in config.json)")
		return
	}
	ice := s.Cfg.IceServers(time.Now())
	if len(ice) == 0 {
		s.Log.Printf("ICE not advertised: public_host=%s is not a usable public TURN/STUN host", s.Cfg.PublicHost)
	} else {
		s.Log.Printf("ICE advertised turn=%d turns=%d ttl=%s urls=%d", s.Cfg.EffectiveTurnPort(), s.Cfg.EffectiveTurnsPort(), s.Cfg.IceTTL(), len(s.Cfg.IceURLs()))
	}
	rep := s.Cfg.ProbeTurn(800 * time.Millisecond)
	s.Log.Printf("TURN listening=%v allocate=%v relayed=%s turns_listening=%v listen=%s external_ip=%s advertised=%d err=%q",
		rep.Running, rep.AllocateOK, rep.RelayedIP, rep.TurnsListening, rep.Listen, rep.ExternalIP, len(rep.Advertised), rep.Error)
	if !rep.Running || !rep.AllocateOK {
		s.Log.Printf("TURN not healthy — ICE is not ready (turn_running=%v allocate_ok=%v err=%q)", rep.Running, rep.AllocateOK, rep.Error)
	}
}

func (s *Server) health(w http.ResponseWriter, r *http.Request) {
	var turn config.TurnReport
	ice := s.Cfg.IceEnabled()
	loopback := addrIsLoopback(r.RemoteAddr)
	if ice {
		if loopback {
			turn = s.Cfg.ProbeTurn(800 * time.Millisecond)
		} else {
			turn = s.Cfg.CachedProbeTurn(800 * time.Millisecond)
		}
	}
	writeJSON(w, 200, healthJSON(ice, loopback, turn))
}

// healthJSON is the unauthenticated /health body.
// TURN ports, relayed IP, and error strings stay on loopback so install.sh
// can self-test; the public internet only gets booleans.
func healthJSON(ice, loopback bool, turn config.TurnReport) map[string]any {
	out := map[string]any{"ok": true, "turn_running": false, "turn_allocate_ok": false}
	if !ice {
		return out
	}
	out["turn_running"] = turn.Running
	out["turn_allocate_ok"] = turn.AllocateOK
	if !loopback {
		return out
	}
	out["turns_listening"] = turn.TurnsListening
	out["turn_port"] = turn.TurnPort
	out["turns_port"] = turn.TurnsPort
	if turn.RelayedIP != "" {
		out["turn_relayed_ip"] = turn.RelayedIP
	}
	if turn.Error != "" {
		out["turn_error"] = turn.Error
	}
	return out
}

func addrIsLoopback(remoteAddr string) bool {
	ip := net.ParseIP(clientIP(&http.Request{RemoteAddr: remoteAddr}))
	return ip != nil && ip.IsLoopback()
}

func (s *Server) version(w http.ResponseWriter, _ *http.Request) {
	writeJSON(w, 200, map[string]any{"server": config.ServerVersion, "protocol": config.ProtocolVersion})
}

func (s *Server) info(w http.ResponseWriter, r *http.Request) {
	out := map[string]any{
		"server_id":        s.Cfg.ServerID,
		"protocol_version": config.ProtocolVersion,
		"fingerprint":      s.FP,
	}
	if r.Header.Get("Authorization") == "" {
		writeJSON(w, 200, out)
		return
	}
	if _, err := s.authenticate(r, nil); err != nil {
		writeJSON(w, 401, map[string]string{"error": "unauthorized"})
		return
	}
	if ip := s.Cfg.PublicIPv4(); ip != "" {
		out["public_ip"] = ip
	}
	if ice := s.Cfg.IceServers(time.Now()); len(ice) > 0 {
		out["ice_servers"] = ice
		out["ice_ttl_seconds"] = s.Cfg.IceTTLSeconds()
	}
	writeJSON(w, 200, out)
}

type bootstrapReq struct {
	Token          string `json:"token"`
	DisplayName    string `json:"display_name"`
	PublicIdentity []byte `json:"public_identity"`
	DeviceID       string `json:"device_id"`
}

// Unauthenticated bootstrap must not distinguish setup vs invite vs used vs
// expired — that is an extra oracle on POST /v1/bootstrap.
const bootstrapInvalidToken = "invalid token"

func (s *Server) bootstrap(w http.ResponseWriter, r *http.Request) {
	if !s.Limit.Allow("bootstrap:" + clientIP(r)) {
		writeJSON(w, 429, map[string]string{"error": "rate_limited"})
		return
	}
	var req bootstrapReq
	if err := json.NewDecoder(io.LimitReader(r.Body, 1<<20)).Decode(&req); err != nil {
		writeJSON(w, 400, map[string]string{"error": "bad json"})
		return
	}
	req.DisplayName = login.Normalize(req.DisplayName)
	if len(req.PublicIdentity) < 70 || req.DeviceID == "" || req.Token == "" {
		writeJSON(w, 400, map[string]string{"error": "missing fields"})
		return
	}
	if !login.Valid(req.DisplayName) {
		writeJSON(w, 400, map[string]string{"error": "login must be 2-24 letters, digits, _ . -"})
		return
	}
	if len(req.PublicIdentity) < 38 || hex.EncodeToString(req.PublicIdentity[6:38]) != strings.ToLower(req.DeviceID) {
		writeJSON(w, 400, map[string]string{"error": "device_id mismatch"})
		return
	}
	hasOwner, err := s.Store.HasOwner()
	if err != nil {
		writeJSON(w, 500, map[string]string{"error": "db"})
		return
	}
	role := "member"
	if !hasOwner {
		if req.Token != s.setup {
			writeJSON(w, 403, map[string]string{"error": bootstrapInvalidToken})
			return
		}
		role = "owner"
	} else if err := s.Store.PeekInvite(req.Token); err != nil {
		writeJSON(w, 403, map[string]string{"error": bootstrapInvalidToken})
		return
	}
	taken, err := s.Store.LoginTaken(req.DisplayName)
	if err != nil {
		writeJSON(w, 500, map[string]string{"error": "db"})
		return
	}
	if taken {
		writeJSON(w, 409, map[string]string{"error": "login taken"})
		return
	}
	if _, err := s.Store.Device(req.DeviceID); err == nil {
		writeJSON(w, 409, map[string]string{"error": "device exists"})
		return
	}
	if !hasOwner {
		s.setup = "" // one-time in-memory; config file still has it but DB owner exists
	} else if _, err := s.Store.ConsumeToken(req.Token); err != nil {
		writeJSON(w, 403, map[string]string{"error": bootstrapInvalidToken})
		return
	}
	memberID := uuid.NewString()
	if err := s.Store.InsertMember(memberID, req.DisplayName, role); err != nil {
		writeJSON(w, 500, map[string]string{"error": "db"})
		return
	}
	signPub := append([]byte{}, req.PublicIdentity[6:38]...)
	if err := s.Store.InsertDevice(db.Device{
		ID:             strings.ToLower(req.DeviceID),
		MemberID:       memberID,
		PublicIdentity: req.PublicIdentity,
		SignPublic:     signPub,
	}); err != nil {
		writeJSON(w, 500, map[string]string{"error": "db"})
		return
	}
	s.Log.Printf("bootstrap role=%s device=%s", role, req.DeviceID[:8])
	writeJSON(w, 200, map[string]any{
		"member_id": memberID,
		"device_id": strings.ToLower(req.DeviceID),
		"role":      role,
		"server_id": s.Cfg.ServerID,
	})
}

type authed struct {
	Device db.Device
	Member db.Member
}

func (s *Server) authenticate(r *http.Request, body []byte) (authed, error) {
	parts, err := authz.ParseHeader(r.Header.Get("Authorization"))
	if err != nil {
		return authed{}, err
	}
	if err := authz.CheckTimestamp(parts.Timestamp, time.Now()); err != nil {
		return authed{}, err
	}
	dev, err := s.Store.Device(parts.DeviceID)
	if err != nil || dev.Revoked {
		return authed{}, errors.New("unknown device")
	}
	mem, err := s.Store.Member(dev.MemberID)
	if err != nil || mem.Revoked {
		return authed{}, errors.New("revoked member")
	}
	msg := authz.AuthMessage(r.Method, r.URL.Path, parts.Timestamp, body)
	if err := authz.Verify(dev.SignPublic, msg, parts.Signature); err != nil {
		return authed{}, err
	}
	s.Store.TouchDevice(dev.ID)
	return authed{Device: dev, Member: mem}, nil
}

func (s *Server) withAuth(fn func(http.ResponseWriter, *http.Request, authed, []byte)) http.HandlerFunc {
	return s.withAuthLimit(1<<20, fn)
}

func (s *Server) withAuthLimit(limit int64, fn func(http.ResponseWriter, *http.Request, authed, []byte)) http.HandlerFunc {
	return func(w http.ResponseWriter, r *http.Request) {
		body, _ := io.ReadAll(io.LimitReader(r.Body, limit))
		a, err := s.authenticate(r, body)
		if err != nil {
			writeJSON(w, 401, map[string]string{"error": "unauthorized"})
			return
		}
		fn(w, r, a, body)
	}
}

func (s *Server) directory(w http.ResponseWriter, _ *http.Request, a authed, _ []byte) {
	members, err := s.Store.ListMembers()
	if err != nil {
		writeJSON(w, 500, map[string]string{"error": "db"})
		return
	}
	devices, err := s.Store.ListDevices()
	if err != nil {
		writeJSON(w, 500, map[string]string{"error": "db"})
		return
	}
	type mJSON struct {
		MemberID    string `json:"member_id"`
		DisplayName string `json:"display_name"`
		Role        string `json:"role"`
		Revoked     bool   `json:"revoked"`
	}
	type dJSON struct {
		DeviceID       string `json:"device_id"`
		MemberID       string `json:"member_id"`
		PublicIdentity []byte `json:"public_identity"`
		LastSeen       string `json:"last_seen"`
		Online         bool   `json:"online"`
		Revoked        bool   `json:"revoked"`
	}
	owner := a.Member.Role == "owner"
	outM := []mJSON{}
	for _, m := range members {
		if m.Revoked && !owner {
			continue
		}
		outM = append(outM, mJSON{m.ID, m.DisplayName, m.Role, m.Revoked})
	}
	outD := []dJSON{}
	for _, d := range devices {
		if d.Revoked {
			continue
		}
		_, online := s.Hub.Get(d.ID)
		outD = append(outD, dJSON{d.ID, d.MemberID, d.PublicIdentity, d.LastSeen, online, d.Revoked})
	}
	writeJSON(w, 200, map[string]any{"members": outM, "devices": outD})
}

const (
	defaultInviteTTLSeconds = 3600
	maxInviteTTLSeconds     = 24 * 3600
)

func inviteTTLSeconds(v int) int {
	if v <= 0 {
		return defaultInviteTTLSeconds
	}
	if v > maxInviteTTLSeconds {
		return maxInviteTTLSeconds
	}
	return v
}

func (s *Server) createInvite(w http.ResponseWriter, _ *http.Request, a authed, body []byte) {
	if a.Member.Role != "owner" {
		writeJSON(w, 403, map[string]string{"error": "owner only"})
		return
	}
	if !s.Limit.Allow("invite:" + a.Device.ID) {
		writeJSON(w, 429, map[string]string{"error": "rate_limited"})
		return
	}
	var req struct {
		TTLSeconds int `json:"ttl_seconds"`
	}
	_ = json.Unmarshal(body, &req)
	req.TTLSeconds = inviteTTLSeconds(req.TTLSeconds)
	token := uuid.NewString() + uuid.NewString()
	id := uuid.NewString()
	exp := time.Now().Add(time.Duration(req.TTLSeconds) * time.Second)
	if err := s.Store.InsertInvite(id, token, a.Member.ID, exp); err != nil {
		writeJSON(w, 500, map[string]string{"error": "db"})
		return
	}
	writeJSON(w, 200, map[string]any{
		"invite_id":  id,
		"token":      token,
		"expires_at": exp.UTC().Format(time.RFC3339),
	})
}

func (s *Server) adminStatus(w http.ResponseWriter, _ *http.Request, a authed, _ []byte) {
	if a.Member.Role != "owner" {
		writeJSON(w, 403, map[string]string{"error": "owner only"})
		return
	}
	s.gcExpiredObjects()
	mc, _ := s.Store.MemberCount()
	dc, _ := s.Store.DeviceCount()
	box, _ := s.Store.MailboxCount()
	oc, _ := s.Store.ObjectCount()
	obytes, _ := s.Store.ObjectBytesSum()
	gc, _ := s.Store.GroupCount()
	turn := s.Cfg.ProbeTurn(800 * time.Millisecond)
	writeJSON(w, 200, map[string]any{
		"server_id":        s.Cfg.ServerID,
		"version":          config.ServerVersion,
		"protocol_version": config.ProtocolVersion,
		"member_count":     mc,
		"device_count":     dc,
		"mailbox_count":    box,
		"object_count":     oc,
		"object_bytes":     obytes,
		"group_count":      gc,
		"listen":           s.Cfg.Listen,
		"max_object_bytes": s.objectLimit(),
		"online_devices":   len(s.Hub.Online()),
		"public_host":      s.Cfg.PublicHost,
		"public_ip":        s.Cfg.PublicIPv4(),
		"turn_port":        turn.TurnPort,
		"turns_port":       turn.TurnsPort,
		"ice_enabled":      s.Cfg.IceEnabled(),
		"turn_running":     turn.Running,
		"turn_allocate_ok": turn.AllocateOK,
		"turns_listening":  turn.TurnsListening,
		"turn_listen":      turn.Listen,
		"turn_external_ip": turn.ExternalIP,
		"turn_relayed_ip":  turn.RelayedIP,
		"turn_error":       turn.Error,
		"ice_urls":         turn.Advertised,
	})
}

func (s *Server) revokeMember(w http.ResponseWriter, _ *http.Request, a authed, body []byte) {
	if a.Member.Role != "owner" {
		writeJSON(w, 403, map[string]string{"error": "owner only"})
		return
	}
	var req struct {
		MemberID string `json:"member_id"`
	}
	if err := json.Unmarshal(body, &req); err != nil || req.MemberID == "" {
		writeJSON(w, 400, map[string]string{"error": "bad json"})
		return
	}
	devices, err := s.Store.ListDevices()
	if err != nil {
		writeJSON(w, 500, map[string]string{"error": "db"})
		return
	}
	if err := s.Store.RevokeMemberGuarded(req.MemberID); err != nil {
		if errors.Is(err, db.ErrNotFound) {
			writeJSON(w, 404, map[string]string{"error": "not found"})
			return
		}
		if errors.Is(err, db.ErrLastOwner) {
			writeJSON(w, 409, map[string]string{"error": "last owner"})
			return
		}
		writeJSON(w, 500, map[string]string{"error": "db"})
		return
	}
	for _, d := range devices {
		if d.MemberID == req.MemberID {
			s.Hub.Drop(d.ID)
		}
	}
	writeJSON(w, 200, map[string]any{"ok": true})
}

func (s *Server) revokeDevice(w http.ResponseWriter, _ *http.Request, a authed, body []byte) {
	if a.Member.Role != "owner" {
		writeJSON(w, 403, map[string]string{"error": "owner only"})
		return
	}
	var req struct {
		DeviceID string `json:"device_id"`
	}
	if err := json.Unmarshal(body, &req); err != nil || req.DeviceID == "" {
		writeJSON(w, 400, map[string]string{"error": "bad json"})
		return
	}
	deviceID := strings.ToLower(req.DeviceID)
	err := s.Store.RevokeDeviceGuarded(deviceID)
	if errors.Is(err, db.ErrNotFound) {
		writeJSON(w, 404, map[string]string{"error": "not found"})
		return
	}
	if errors.Is(err, db.ErrLastOwner) {
		writeJSON(w, 409, map[string]string{"error": "last owner"})
		return
	}
	if err != nil {
		writeJSON(w, 500, map[string]string{"error": "db"})
		return
	}
	s.Hub.Drop(deviceID)
	writeJSON(w, 200, map[string]any{"ok": true})
}

type wsIn struct {
	Type      string          `json:"type"`
	Envelope  []byte          `json:"envelope"`
	Envelopes [][]byte        `json:"envelopes"`
	MessageID string          `json:"message_id"`
	GroupID   string          `json:"group_id"`
	CallID    string          `json:"call_id"`
	To        string          `json:"to"`
	Event     string          `json:"event"`
	Payload   json.RawMessage `json:"payload"`
}

type wsOut struct {
	Type      string   `json:"type"`
	Envelope  []byte   `json:"envelope,omitempty"`
	MessageID string   `json:"message_id,omitempty"`
	Devices   []string `json:"devices,omitempty"`
	Code      string   `json:"code,omitempty"`
	Message   string   `json:"message,omitempty"`
	CallID    string   `json:"call_id,omitempty"`
	From      string   `json:"from,omitempty"`
	Event     string   `json:"event,omitempty"`
	Payload   string   `json:"payload,omitempty"`
}

func wsUnauthorized(w http.ResponseWriter) {
	http.Error(w, "unauthorized", http.StatusUnauthorized)
}

func (s *Server) ws(w http.ResponseWriter, r *http.Request) {
	q := r.URL.Query()
	parts, err := authz.WsCreds(r.Header.Get(authz.WsAuthHeader), q.Get("device_id"), q.Get("ts"), q.Get("sig"))
	if err != nil {
		wsUnauthorized(w)
		return
	}
	if err := authz.CheckTimestamp(parts.Timestamp, time.Now()); err != nil {
		wsUnauthorized(w)
		return
	}
	deviceID := parts.DeviceID
	dev, err := s.Store.Device(deviceID)
	if err != nil || dev.Revoked {
		wsUnauthorized(w)
		return
	}
	mem, err := s.Store.Member(dev.MemberID)
	if err != nil || mem.Revoked {
		wsUnauthorized(w)
		return
	}
	if err := authz.Verify(dev.SignPublic, authz.WSMessage(parts.Timestamp), parts.Signature); err != nil {
		wsUnauthorized(w)
		return
	}
	c, err := websocket.Accept(w, r, &websocket.AcceptOptions{InsecureSkipVerify: true})
	if err != nil {
		return
	}
	conn := &clientConn{id: deviceID, c: c, s: s}
	s.Hub.Add(deviceID, conn)
	s.Store.TouchDevice(deviceID)
	defer func() {
		s.Hub.DropIf(deviceID, conn)
		s.broadcastPresence()
		_ = c.Close(websocket.StatusNormalClosure, "")
	}()
	ctx := r.Context()
	pending, _ := s.Store.PendingMailbox(deviceID)
	for _, row := range pending {
		_ = conn.write(ctx, wsOut{Type: "deliver", Envelope: row.Blob, MessageID: row.MessageID})
	}
	_ = conn.write(ctx, wsOut{Type: "mailbox_done"})
	s.broadcastPresence()
	s.flushPendingCalls(ctx, conn)

	for {
		var in wsIn
		if err := wsjson.Read(ctx, c, &in); err != nil {
			return
		}
		switch in.Type {
		case "send":
			s.handleSend(ctx, conn, in.Envelope)
		case "ack":
			s.handleAck(ctx, conn, in.MessageID)
		case "group_send":
			s.handleGroupSend(ctx, conn, in.GroupID, in.Envelopes)
		case "call":
			s.handleCall(ctx, conn, in)
		default:
			_ = wsjson.Write(ctx, c, wsOut{Type: "error", Code: "protocol", Message: "unknown type"})
		}
	}
}

func (s *Server) handleSend(ctx context.Context, from *clientConn, raw []byte) {
	if len(raw) > s.Cfg.MaxEnvelopeBytes+128 {
		_ = from.write(ctx, wsOut{Type: "error", Code: "too_large", Message: "envelope too large"})
		return
	}
	if !s.Limit.Allow("ws-send:" + from.id) {
		_ = from.write(ctx, wsOut{Type: "error", Code: "rate_limited", Message: "slow down"})
		return
	}
	parsed, err := envelope.Parse(raw)
	if err != nil {
		_ = from.write(ctx, wsOut{Type: "error", Code: "protocol", Message: err.Error()})
		return
	}
	if parsed.Meta.Version != envelope.ProtocolVersion {
		_ = from.write(ctx, wsOut{Type: "error", Code: "protocol", Message: "incompatible version"})
		return
	}
	if parsed.Meta.SenderID != from.id {
		_ = from.write(ctx, wsOut{Type: "error", Code: "auth", Message: "sender mismatch"})
		return
	}
	sender, err := s.Store.Device(parsed.Meta.SenderID)
	if err != nil || sender.Revoked {
		_ = from.write(ctx, wsOut{Type: "error", Code: "auth", Message: "unknown sender"})
		return
	}
	if err := envelope.VerifySender(parsed, sender.SignPublic); err != nil {
		_ = from.write(ctx, wsOut{Type: "error", Code: "auth", Message: "bad signature"})
		return
	}
	recip, err := s.Store.Device(parsed.Meta.RecipientID)
	if err != nil || recip.Revoked {
		_ = from.write(ctx, wsOut{Type: "error", Code: "not_found", Message: "unknown recipient"})
		return
	}
	if err := s.Store.PutMailbox(parsed.Meta.MessageID, recip.ID, sender.ID, raw, s.Cfg.MailboxTTL()); err != nil {
		_ = from.write(ctx, wsOut{Type: "error", Code: "protocol", Message: "mailbox"})
		return
	}
	_ = from.write(ctx, wsOut{Type: "queued", MessageID: parsed.Meta.MessageID})
	if dest, ok := s.Hub.Get(recip.ID); ok {
		_ = dest.write(ctx, wsOut{Type: "deliver", Envelope: raw, MessageID: parsed.Meta.MessageID})
	}
}

func (s *Server) handleGroupSend(ctx context.Context, from *clientConn, groupID string, envs [][]byte) {
	ok, err := s.Store.IsGroupMember(groupID, from.id)
	if err != nil || !ok {
		_ = from.write(ctx, wsOut{Type: "error", Code: "auth", Message: "not a group member"})
		return
	}
	members, _ := s.Store.GroupMembers(groupID)
	allowed := map[string]bool{}
	for _, m := range members {
		allowed[m] = true
	}
	for _, raw := range envs {
		parsed, err := envelope.Parse(raw)
		if err != nil || parsed.Meta.SenderID != from.id || !allowed[parsed.Meta.RecipientID] {
			_ = from.write(ctx, wsOut{Type: "error", Code: "auth", Message: "group envelope rejected"})
			return
		}
		s.handleSend(ctx, from, raw)
	}
}

const (
	maxCallPayloadBytes = 16384
	callAudioPerSec     = 40
	callRingBurst       = 6
	callRingWindow      = 30 * time.Second
	pendingCallTTL      = 60 * time.Second
	maxPendingCalls     = 256
)

type pendingCall struct {
	callID  string
	from    string
	to      string
	event   string
	payload []byte
	expires time.Time
}

func (s *Server) handleCall(ctx context.Context, from *clientConn, in wsIn) {
	if in.CallID == "" || strings.TrimSpace(in.To) == "" {
		_ = from.write(ctx, wsOut{Type: "error", Code: "protocol", Message: "call fields"})
		return
	}
	event, ok := validCallEvent(in.Event)
	if !ok {
		_ = from.write(ctx, wsOut{Type: "error", Code: "protocol", Message: "call fields"})
		return
	}
	payload := decodeCallPayload(in.Payload)
	if len(payload) > maxCallPayloadBytes {
		_ = from.write(ctx, wsOut{Type: "error", Code: "too_large", Message: "call payload too large"})
		return
	}
	if strings.EqualFold(event, "audio") && !s.CallAudio.Allow("ws-call-audio:"+from.id) {
		_ = from.write(ctx, wsOut{Type: "error", Code: "rate_limited", Message: "slow down"})
		return
	}
	if strings.EqualFold(event, "ring") && s.CallRing != nil && !s.CallRing.Allow("ws-call-ring:"+from.id) {
		_ = from.write(ctx, wsOut{Type: "error", Code: "rate_limited", Message: "slow down"})
		return
	}
	if strings.EqualFold(event, "relay") && s.CallRelay != nil && !s.CallRelay.Allow("ws-call-relay:"+from.id) {
		_ = from.write(ctx, wsOut{Type: "error", Code: "rate_limited", Message: "slow down"})
		return
	}
	target := s.resolveCallTarget(in.To)
	if dest, ok := s.Hub.Get(target); ok {
		if isCallTerminal(event) {
			s.dropPendingCall(in.CallID)
		}
		_ = dest.write(ctx, wsOut{
			Type:    "call",
			CallID:  in.CallID,
			From:    from.id,
			Event:   event,
			Payload: payload,
		})
		return
	}
	if strings.EqualFold(event, "audio") {
		_ = from.write(ctx, wsOut{Type: "error", Code: "not_found", Message: "peer offline"})
		return
	}
	if !s.storePendingCall(pendingCall{
		callID:  in.CallID,
		from:    from.id,
		to:      target,
		event:   event,
		payload: []byte(payload),
		expires: time.Now().Add(pendingCallTTL),
	}) {
		_ = from.write(ctx, wsOut{Type: "error", Code: "rate_limited", Message: "call pending full"})
		return
	}
	_ = from.write(ctx, wsOut{Type: "queued", CallID: in.CallID})
}

func isCallTerminal(event string) bool {
	return strings.EqualFold(event, "hangup") || strings.EqualFold(event, "reject")
}

func (s *Server) storePendingCall(p pendingCall) bool {
	now := time.Now()
	s.pendingMu.Lock()
	defer s.pendingMu.Unlock()
	s.purgeExpiredPendingLocked(now)
	if _, exists := s.pendingCalls[p.callID]; !exists && len(s.pendingCalls) >= maxPendingCalls {
		return false
	}
	s.pendingCalls[p.callID] = p
	return true
}

func (s *Server) dropPendingCall(callID string) {
	s.pendingMu.Lock()
	defer s.pendingMu.Unlock()
	s.purgeExpiredPendingLocked(time.Now())
	delete(s.pendingCalls, callID)
}

func (s *Server) takePendingCallsFor(deviceID string) []pendingCall {
	now := time.Now()
	deviceID = strings.ToLower(deviceID)
	var memberID string
	if d, err := s.Store.Device(deviceID); err == nil {
		memberID = strings.ToLower(d.MemberID)
	}
	s.pendingMu.Lock()
	defer s.pendingMu.Unlock()
	s.purgeExpiredPendingLocked(now)
	var out []pendingCall
	for id, p := range s.pendingCalls {
		to := strings.ToLower(p.to)
		if to == deviceID || (memberID != "" && to == memberID) {
			out = append(out, p)
			delete(s.pendingCalls, id)
		}
	}
	return out
}

func (s *Server) purgeExpiredPendingLocked(now time.Time) {
	for id, p := range s.pendingCalls {
		if now.After(p.expires) {
			delete(s.pendingCalls, id)
		}
	}
}

func (s *Server) flushPendingCalls(ctx context.Context, dest *clientConn) {
	for _, p := range s.takePendingCallsFor(dest.id) {
		_ = dest.write(ctx, wsOut{
			Type:    "call",
			CallID:  p.callID,
			From:    p.from,
			Event:   p.event,
			Payload: string(p.payload),
		})
	}
}

// resolveCallTarget maps a WSS call `to` field to a live hub device.
// Chat routes by envelope recipient (device hex). Calls used to miss when the
// client sent a member_id or a stale device of a member who is online elsewhere.
func (s *Server) resolveCallTarget(to string) string {
	id := strings.ToLower(strings.TrimSpace(to))
	if id == "" {
		return ""
	}
	if _, ok := s.Hub.Get(id); ok {
		return id
	}
	devs, err := s.Store.ListDevices()
	if err != nil {
		return id
	}
	for _, d := range devs {
		if d.Revoked {
			continue
		}
		if strings.ToLower(d.MemberID) != id {
			continue
		}
		if _, ok := s.Hub.Get(d.ID); ok {
			return d.ID
		}
	}
	return id
}

func decodeCallPayload(raw json.RawMessage) string {
	if len(raw) == 0 || string(raw) == "null" {
		return ""
	}
	var s string
	if err := json.Unmarshal(raw, &s); err == nil {
		return s
	}
	return string(raw)
}

func (s *Server) handleAck(ctx context.Context, from *clientConn, messageID string) {
	row, err := s.Store.AckMailbox(messageID, from.id)
	if err != nil {
		_ = from.write(ctx, wsOut{Type: "error", Code: "not_found", Message: "no mailbox row"})
		return
	}
	if dest, ok := s.Hub.Get(row.SenderID); ok {
		_ = dest.write(ctx, wsOut{Type: "delivered", MessageID: messageID})
	}
}

func clientIP(r *http.Request) string {
	host, _, err := net.SplitHostPort(r.RemoteAddr)
	if err != nil {
		return r.RemoteAddr
	}
	return host
}

type clientConn struct {
	id string
	c  *websocket.Conn
	s  *Server
	mu sync.Mutex
}

func (c *clientConn) write(ctx context.Context, msg wsOut) error {
	c.mu.Lock()
	defer c.mu.Unlock()
	return wsjson.Write(ctx, c.c, msg)
}

type Hub struct {
	mu      sync.RWMutex
	clients map[string]*clientConn
}

func NewHub() *Hub { return &Hub{clients: map[string]*clientConn{}} }

func (h *Hub) Add(id string, c *clientConn) {
	h.mu.Lock()
	h.clients[id] = c
	h.mu.Unlock()
}

func (h *Hub) Drop(id string) {
	h.mu.Lock()
	c := h.clients[id]
	delete(h.clients, id)
	h.mu.Unlock()
	if c != nil && c.c != nil {
		_ = c.c.Close(websocket.StatusPolicyViolation, "revoked")
	}
}

func (h *Hub) DropIf(id string, c *clientConn) {
	h.mu.Lock()
	if h.clients[id] == c {
		delete(h.clients, id)
	}
	h.mu.Unlock()
}

func (s *Server) broadcastPresence() {
	ctx, cancel := context.WithTimeout(context.Background(), 2*time.Second)
	defer cancel()
	devices := s.Hub.Online()
	for _, id := range devices {
		if dest, ok := s.Hub.Get(id); ok {
			_ = dest.write(ctx, wsOut{Type: "presence", Devices: devices})
		}
	}
}

func (h *Hub) Get(id string) (*clientConn, bool) {
	h.mu.RLock()
	c, ok := h.clients[id]
	h.mu.RUnlock()
	return c, ok
}

func (h *Hub) Online() []string {
	h.mu.RLock()
	defer h.mu.RUnlock()
	out := make([]string, 0, len(h.clients))
	for id := range h.clients {
		out = append(out, id)
	}
	return out
}
