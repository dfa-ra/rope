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
	"strconv"

	"github.com/coder/websocket"
	"github.com/coder/websocket/wsjson"
	"github.com/google/uuid"
)

type Server struct {
	Cfg    config.Config
	Store  *db.Store
	Log    *log.Logger
	Hub    *Hub
	Limit  *ratelimit.Limiter
	FP     string
	setup  string
}

func New(cfg config.Config, store *db.Store, logger *log.Logger) *Server {
	if logger == nil {
		logger = log.Default()
	}
	s := &Server{
		Cfg:   cfg,
		Store: store,
		Log:   logger,
		Hub:   NewHub(),
		Limit: ratelimit.New(60, time.Minute),
		setup: cfg.SetupToken,
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
	w.WriteHeader(status)
	_ = json.NewEncoder(w).Encode(v)
}

func (s *Server) health(w http.ResponseWriter, _ *http.Request) {
	writeJSON(w, 200, map[string]any{"ok": true})
}

func (s *Server) version(w http.ResponseWriter, _ *http.Request) {
	writeJSON(w, 200, map[string]any{"server": config.ServerVersion, "protocol": config.ProtocolVersion})
}

func (s *Server) info(w http.ResponseWriter, _ *http.Request) {
	writeJSON(w, 200, map[string]any{
		"server_id":         s.Cfg.ServerID,
		"protocol_version":  config.ProtocolVersion,
		"fingerprint":       s.FP,
	})
}

type bootstrapReq struct {
	Token          string `json:"token"`
	DisplayName    string `json:"display_name"`
	PublicIdentity []byte `json:"public_identity"`
	DeviceID       string `json:"device_id"`
}

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
	taken, err := s.Store.LoginTaken(req.DisplayName)
	if err != nil {
		writeJSON(w, 500, map[string]string{"error": "db"})
		return
	}
	if taken {
		writeJSON(w, 409, map[string]string{"error": "login taken"})
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
			writeJSON(w, 403, map[string]string{"error": "invalid setup token"})
			return
		}
		role = "owner"
		s.setup = "" // one-time in-memory; config file still has it but DB owner exists
	} else {
		if _, err := s.Store.ConsumeToken(req.Token); err != nil {
			writeJSON(w, 403, map[string]string{"error": err.Error()})
			return
		}
	}
	if _, err := s.Store.Device(req.DeviceID); err == nil {
		writeJSON(w, 409, map[string]string{"error": "device exists"})
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

func (s *Server) directory(w http.ResponseWriter, _ *http.Request, _ authed, _ []byte) {
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
	outM := []mJSON{}
	for _, m := range members {
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
	if req.TTLSeconds <= 0 {
		req.TTLSeconds = 3600
	}
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
	writeJSON(w, 200, map[string]any{
		"server_id":         s.Cfg.ServerID,
		"version":           config.ServerVersion,
		"protocol_version":  config.ProtocolVersion,
		"member_count":      mc,
		"device_count":      dc,
		"mailbox_count":     box,
		"object_count":      oc,
		"object_bytes":      obytes,
		"group_count":       gc,
		"listen":            s.Cfg.Listen,
		"max_object_bytes":  s.objectLimit(),
		"online_devices":    len(s.Hub.Online()),
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
	if err := s.Store.RevokeMember(req.MemberID); err != nil {
		writeJSON(w, 500, map[string]string{"error": "db"})
		return
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
	if err := s.Store.RevokeDevice(req.DeviceID); err != nil {
		writeJSON(w, 500, map[string]string{"error": "db"})
		return
	}
	s.Hub.Drop(req.DeviceID)
	writeJSON(w, 200, map[string]any{"ok": true})
}

type wsIn struct {
	Type      string   `json:"type"`
	Envelope  []byte   `json:"envelope"`
	Envelopes [][]byte `json:"envelopes"`
	MessageID string   `json:"message_id"`
	GroupID   string   `json:"group_id"`
	CallID    string   `json:"call_id"`
	To        string   `json:"to"`
	Event     string   `json:"event"`
	Payload   string   `json:"payload"`
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

func (s *Server) ws(w http.ResponseWriter, r *http.Request) {
	q := r.URL.Query()
	deviceID := strings.ToLower(q.Get("device_id"))
	ts := q.Get("ts")
	sig := q.Get("sig")
	if deviceID == "" || ts == "" || sig == "" {
		http.Error(w, "missing auth", 401)
		return
	}
	tsi, err := strconv.ParseInt(ts, 10, 64)
	if err != nil {
		http.Error(w, "bad ts", 401)
		return
	}
	if err := authz.CheckTimestamp(tsi, time.Now()); err != nil {
		http.Error(w, "skew", 401)
		return
	}
	dev, err := s.Store.Device(deviceID)
	if err != nil || dev.Revoked {
		http.Error(w, "unknown device", 401)
		return
	}
	mem, err := s.Store.Member(dev.MemberID)
	if err != nil || mem.Revoked {
		http.Error(w, "revoked", 401)
		return
	}
	sigb, err := authz.DecodeB64URL(sig)
	if err != nil {
		http.Error(w, "bad sig", 401)
		return
	}
	if err := authz.Verify(dev.SignPublic, authz.WSMessage(tsi), sigb); err != nil {
		http.Error(w, "bad sig", 401)
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

func (s *Server) handleCall(ctx context.Context, from *clientConn, in wsIn) {
	if in.CallID == "" || in.To == "" {
		_ = from.write(ctx, wsOut{Type: "error", Code: "protocol", Message: "call fields"})
		return
	}
	if dest, ok := s.Hub.Get(strings.ToLower(in.To)); ok {
		_ = dest.write(ctx, wsOut{
			Type:    "call",
			CallID:  in.CallID,
			From:    from.id,
			Event:   in.Event,
			Payload: in.Payload,
		})
		return
	}
	_ = from.write(ctx, wsOut{Type: "error", Code: "not_found", Message: "peer offline"})
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
	if x := r.Header.Get("X-Forwarded-For"); x != "" {
		return strings.Split(x, ",")[0]
	}
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
	delete(h.clients, id)
	h.mu.Unlock()
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
