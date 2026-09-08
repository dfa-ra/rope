package httpapi

import (
	"bytes"
	"context"
	"crypto/ed25519"
	"crypto/rand"
	"encoding/base64"
	"encoding/binary"
	"encoding/hex"
	"encoding/json"
	"io"
	"log"
	"net/http"
	"net/http/httptest"
	"os"
	"path/filepath"
	"strconv"
	"strings"
	"testing"
	"time"

	"github.com/coder/websocket"
	"github.com/coder/websocket/wsjson"
	"github.com/dfa-ra/rope/server/go/internal/authz"
	"github.com/dfa-ra/rope/server/go/internal/config"
	"github.com/dfa-ra/rope/server/go/internal/db"
	"github.com/dfa-ra/rope/server/go/internal/envelope"
	"github.com/google/uuid"
)

type testDevice struct {
	pub  ed25519.PublicKey
	priv ed25519.PrivateKey
	id   string
	blob []byte
}

func newDevice(t *testing.T) testDevice {
	t.Helper()
	pub, priv, err := ed25519.GenerateKey(rand.Reader)
	if err != nil {
		t.Fatal(err)
	}
	blob := make([]byte, 70)
	copy(blob[0:4], []byte("ROPP"))
	binary.LittleEndian.PutUint16(blob[4:6], 1)
	copy(blob[6:38], pub)
	if _, err := rand.Read(blob[38:70]); err != nil {
		t.Fatal(err)
	}
	return testDevice{pub: pub, priv: priv, id: hex.EncodeToString(pub), blob: blob}
}

func testServer(t *testing.T) (*Server, *httptest.Server, string) {
	t.Helper()
	return testServerCfg(t, nil)
}

func testServerCfg(t *testing.T, tweak func(*config.Config)) (*Server, *httptest.Server, string) {
	t.Helper()
	dir := t.TempDir()
	store, err := db.Open(filepath.Join(dir, "data.db"))
	if err != nil {
		t.Fatal(err)
	}
	t.Cleanup(func() { _ = store.Close() })
	cfg := config.Default()
	cfg.DataDir = dir
	cfg.AllowHTTP = true
	cfg.ServerID = "test-server"
	cfg.SetupToken = "setup-secret"
	cfg.MailboxTTLSeconds = 60
	if tweak != nil {
		tweak(&cfg)
	}
	if err := store.EnsureMeta(cfg.ServerID, config.ServerVersion, config.ProtocolVersion); err != nil {
		t.Fatal(err)
	}
	s := New(cfg, store, log.New(io.Discard, "", 0))
	hs := httptest.NewServer(s.Router())
	t.Cleanup(hs.Close)
	return s, hs, cfg.SetupToken
}

func authReq(t *testing.T, method, url, path string, body []byte, d testDevice) *http.Request {
	t.Helper()
	ts := time.Now().Unix()
	msg := authz.AuthMessage(method, path, ts, body)
	sig := ed25519.Sign(d.priv, []byte(msg))
	header := "Rope " + d.id + "." + itoa(ts) + "." + base64.RawURLEncoding.EncodeToString(sig)
	req, err := http.NewRequest(method, url, bytes.NewReader(body))
	if err != nil {
		t.Fatal(err)
	}
	req.Header.Set("Authorization", header)
	req.Header.Set("Content-Type", "application/json")
	return req
}

func itoa(v int64) string {
	return strconv.FormatInt(v, 10)
}

func bootstrap(t *testing.T, hs *httptest.Server, token string, d testDevice, name string) {
	t.Helper()
	body, _ := json.Marshal(map[string]any{
		"token":           token,
		"display_name":    name,
		"public_identity": d.blob,
		"device_id":       d.id,
	})
	resp, err := http.Post(hs.URL+"/v1/bootstrap", "application/json", bytes.NewReader(body))
	if err != nil {
		t.Fatal(err)
	}
	defer resp.Body.Close()
	if resp.StatusCode != 200 {
		b, _ := io.ReadAll(resp.Body)
		t.Fatalf("bootstrap %d: %s", resp.StatusCode, b)
	}
}

func TestHealthAndInfo(t *testing.T) {
	_, hs, _ := testServer(t)
	resp, err := http.Get(hs.URL + "/health")
	if err != nil {
		t.Fatal(err)
	}
	var health map[string]any
	if err := json.NewDecoder(resp.Body).Decode(&health); err != nil {
		t.Fatal(err)
	}
	resp.Body.Close()
	if resp.StatusCode != 200 {
		t.Fatal(resp.StatusCode)
	}
	if health["ok"] != true {
		t.Fatalf("health %+v", health)
	}
	if health["turn_running"] != false {
		t.Fatalf("debug server must not claim coturn is listening: %+v", health)
	}
	resp, err = http.Get(hs.URL + "/v1/info")
	if err != nil {
		t.Fatal(err)
	}
	defer resp.Body.Close()
	var info map[string]any
	if err := json.NewDecoder(resp.Body).Decode(&info); err != nil {
		t.Fatal(err)
	}
	if info["server_id"] != "test-server" {
		t.Fatalf("%v", info)
	}
	if info["fingerprint"] != config.HTTPDevFingerprint() {
		t.Fatalf("fp %v", info["fingerprint"])
	}
	if _, ok := info["ice_servers"]; ok {
		t.Fatal("debug server without TURN must not advertise ice_servers")
	}
	if _, ok := info["ice_ttl_seconds"]; ok {
		t.Fatal("no ice_servers so ice_ttl_seconds must be omitted")
	}
}

func TestInfoAdvertisesIceWhenConfigured(t *testing.T) {
	_, hs, _ := testServerCfg(t, func(cfg *config.Config) {
		cfg.PublicHost = "198.51.100.20"
		cfg.TurnSecret = "hmac-from-install"
		cfg.TurnsPort = 443
	})
	resp, err := http.Get(hs.URL + "/v1/info")
	if err != nil {
		t.Fatal(err)
	}
	defer resp.Body.Close()
	var info struct {
		ServerID      string             `json:"server_id"`
		PublicIP      string           `json:"public_ip"`
		IceTTLSeconds int              `json:"ice_ttl_seconds"`
		IceServers    []config.IceServer `json:"ice_servers"`
	}
	if err := json.NewDecoder(resp.Body).Decode(&info); err != nil {
		t.Fatal(err)
	}
	if len(info.IceServers) != 2 {
		t.Fatalf("%+v", info.IceServers)
	}
	if info.IceTTLSeconds != config.Default().IceTTLSeconds() {
		t.Fatalf("ice_ttl_seconds %d want %d", info.IceTTLSeconds, config.Default().IceTTLSeconds())
	}
	if info.IceServers[0].URLs[0] != "stun:198.51.100.20:3478" {
		t.Fatalf("stun %v", info.IceServers[0].URLs)
	}
	turn := info.IceServers[1]
	if turn.Username == "" || turn.Credential == "" {
		t.Fatal("missing time-limited TURN creds")
	}
	if turn.Credential != config.TurnCredential("hmac-from-install", turn.Username) {
		t.Fatal("HMAC mismatch")
	}
	joined := strings.Join(turn.URLs, " ")
	if !strings.Contains(joined, "turns:198.51.100.20:443?transport=tcp") {
		t.Fatalf("missing turns: %s", joined)
	}
	if !strings.Contains(joined, "turn:198.51.100.20:3478") {
		t.Fatalf("missing turn: %s", joined)
	}
	if !strings.HasSuffix(turn.Username, ":rope") {
		t.Fatalf("HMAC user %s", turn.Username)
	}
	if turn.Hostname != "" {
		t.Fatalf("IP-only host must omit hostname: %q", turn.Hostname)
	}
	if info.PublicIP != "198.51.100.20" {
		t.Fatalf("public_ip %q", info.PublicIP)
	}
	resp, err = http.Get(hs.URL + "/health")
	if err != nil {
		t.Fatal(err)
	}
	var health map[string]any
	if err := json.NewDecoder(resp.Body).Decode(&health); err != nil {
		t.Fatal(err)
	}
	resp.Body.Close()
	if health["ok"] != true {
		t.Fatalf("health %+v", health)
	}
	if _, ok := health["turn_running"]; !ok {
		t.Fatal("health must report turn_running (coturn listening), not only secret presence")
	}
}

func TestInfoAdvertises5349Not443(t *testing.T) {
	_, hs, _ := testServerCfg(t, func(cfg *config.Config) {
		cfg.PublicHost = "198.51.100.20"
		cfg.TurnSecret = "hmac-from-install"
		cfg.TurnsPort = 5349
	})
	resp, err := http.Get(hs.URL + "/v1/info")
	if err != nil {
		t.Fatal(err)
	}
	defer resp.Body.Close()
	var info struct {
		IceServers []config.IceServer `json:"ice_servers"`
	}
	if err := json.NewDecoder(resp.Body).Decode(&info); err != nil {
		t.Fatal(err)
	}
	joined := strings.Join(info.IceServers[1].URLs, " ")
	if strings.Contains(joined, ":443") {
		t.Fatalf("443 advertised: %s", joined)
	}
	if !strings.Contains(joined, "turns:198.51.100.20:5349?transport=tcp") {
		t.Fatalf("missing 5349: %s", joined)
	}
}

func TestInfoHostnameAndPublicIP(t *testing.T) {
	_, hs, _ := testServerCfg(t, func(cfg *config.Config) {
		cfg.PublicHost = "vps.example"
		cfg.PublicIP = "203.0.113.9"
		cfg.TurnSecret = "hmac-from-install"
		cfg.TurnsPort = 443
	})
	resp, err := http.Get(hs.URL + "/v1/info")
	if err != nil {
		t.Fatal(err)
	}
	defer resp.Body.Close()
	var info struct {
		PublicIP      string             `json:"public_ip"`
		IceTTLSeconds int                `json:"ice_ttl_seconds"`
		IceServers    []config.IceServer `json:"ice_servers"`
	}
	if err := json.NewDecoder(resp.Body).Decode(&info); err != nil {
		t.Fatal(err)
	}
	if info.PublicIP != "203.0.113.9" {
		t.Fatalf("public_ip %q", info.PublicIP)
	}
	if info.IceTTLSeconds != config.Default().IceTTLSeconds() {
		t.Fatalf("ice_ttl_seconds %d", info.IceTTLSeconds)
	}
	if len(info.IceServers) != 2 {
		t.Fatalf("%+v", info.IceServers)
	}
	if info.IceServers[0].Hostname != "vps.example" || info.IceServers[1].Hostname != "vps.example" {
		t.Fatalf("hostname %+v", info.IceServers)
	}
	if !strings.Contains(strings.Join(info.IceServers[1].URLs, " "), "turns:vps.example:443") {
		t.Fatalf("urls %+v", info.IceServers[1].URLs)
	}

	_, hsIP, _ := testServerCfg(t, func(cfg *config.Config) {
		cfg.PublicHost = "203.0.113.9"
		cfg.TLSHostname = "rope.example"
		cfg.TurnSecret = "hmac-from-install"
		cfg.TurnsPort = 443
	})
	resp, err = http.Get(hsIP.URL + "/v1/info")
	if err != nil {
		t.Fatal(err)
	}
	defer resp.Body.Close()
	if err := json.NewDecoder(resp.Body).Decode(&info); err != nil {
		t.Fatal(err)
	}
	if info.PublicIP != "203.0.113.9" {
		t.Fatalf("ip-host public_ip %q", info.PublicIP)
	}
	if info.IceServers[1].Hostname != "rope.example" {
		t.Fatalf("SNI when urls are IP: %+v", info.IceServers[1])
	}
	if !strings.Contains(info.IceServers[1].URLs[0], "203.0.113.9") {
		t.Fatalf("urls stay on IP: %v", info.IceServers[1].URLs)
	}
}

func TestInfoOmitsIceWhenHostIsPrivate(t *testing.T) {
	_, hs, _ := testServerCfg(t, func(cfg *config.Config) {
		cfg.PublicHost = "10.0.0.4"
		cfg.TurnSecret = "hmac-from-install"
		cfg.TurnsPort = 443
	})
	resp, err := http.Get(hs.URL + "/v1/info")
	if err != nil {
		t.Fatal(err)
	}
	defer resp.Body.Close()
	var info map[string]any
	if err := json.NewDecoder(resp.Body).Decode(&info); err != nil {
		t.Fatal(err)
	}
	if _, ok := info["ice_servers"]; ok {
		t.Fatalf("private-only host must not advertise ice_servers: %+v", info)
	}
	if _, ok := info["ice_ttl_seconds"]; ok {
		t.Fatal("no ice_servers so no ice_ttl_seconds")
	}
	if _, ok := info["public_ip"]; ok {
		t.Fatalf("private public_ip leaked: %+v", info)
	}
}

func TestInviteSingleUseAndExpiry(t *testing.T) {
	_, hs, setup := testServer(t)
	owner := newDevice(t)
	bootstrap(t, hs, setup, owner, "owner")

	body := []byte(`{"ttl_seconds":3600}`)
	req := authReq(t, http.MethodPost, hs.URL+"/v1/invites", "/v1/invites", body, owner)
	resp, err := http.DefaultClient.Do(req)
	if err != nil {
		t.Fatal(err)
	}
	var inv struct {
		Token string `json:"token"`
	}
	if err := json.NewDecoder(resp.Body).Decode(&inv); err != nil {
		t.Fatal(err)
	}
	resp.Body.Close()
	if inv.Token == "" {
		t.Fatal("missing token")
	}
	guest := newDevice(t)
	bootstrap(t, hs, inv.Token, guest, "guest")
	guest2 := newDevice(t)
	body2, _ := json.Marshal(map[string]any{
		"token": inv.Token, "display_name": "guest2", "public_identity": guest2.blob, "device_id": guest2.id,
	})
	resp, err = http.Post(hs.URL+"/v1/bootstrap", "application/json", bytes.NewReader(body2))
	if err != nil {
		t.Fatal(err)
	}
	if resp.StatusCode != 403 {
		t.Fatalf("reuse wanted 403 got %d", resp.StatusCode)
	}
	resp.Body.Close()

	body = []byte(`{"ttl_seconds":1}`)
	req = authReq(t, http.MethodPost, hs.URL+"/v1/invites", "/v1/invites", body, owner)
	resp, err = http.DefaultClient.Do(req)
	if err != nil {
		t.Fatal(err)
	}
	if err := json.NewDecoder(resp.Body).Decode(&inv); err != nil {
		t.Fatal(err)
	}
	resp.Body.Close()
	time.Sleep(2 * time.Second)
	late := newDevice(t)
	body2, _ = json.Marshal(map[string]any{
		"token": inv.Token, "display_name": "late", "public_identity": late.blob, "device_id": late.id,
	})
	resp, err = http.Post(hs.URL+"/v1/bootstrap", "application/json", bytes.NewReader(body2))
	if err != nil {
		t.Fatal(err)
	}
	if resp.StatusCode != 403 {
		t.Fatalf("expired wanted 403 got %d", resp.StatusCode)
	}
	resp.Body.Close()
}

func TestWrongSetupTokenRejected(t *testing.T) {
	_, hs, _ := testServer(t)
	d := newDevice(t)
	body, _ := json.Marshal(map[string]any{
		"token": "nope", "display_name": "nope", "public_identity": d.blob, "device_id": d.id,
	})
	resp, err := http.Post(hs.URL+"/v1/bootstrap", "application/json", bytes.NewReader(body))
	if err != nil {
		t.Fatal(err)
	}
	if resp.StatusCode != 403 {
		t.Fatalf("got %d", resp.StatusCode)
	}
	resp.Body.Close()
}

func buildEnvelope(t *testing.T, from, to testDevice, plaintext string) []byte {
	t.Helper()
	id := uuid.New()
	ts := uint64(time.Now().UnixMilli())
	ct := []byte("ENCRYPTED:" + hex.EncodeToString([]byte(plaintext)))
	buf := make([]byte, 0, 200)
	buf = append(buf, []byte("ROPE")...)
	ver := make([]byte, 2)
	binary.LittleEndian.PutUint16(ver, 1)
	buf = append(buf, ver...)
	buf = append(buf, 1, 0)
	buf = append(buf, id[:]...)
	tsb := make([]byte, 8)
	binary.LittleEndian.PutUint64(tsb, ts)
	buf = append(buf, tsb...)
	buf = append(buf, from.pub...)
	buf = append(buf, to.pub...)
	nonce := make([]byte, 24)
	_, _ = rand.Read(nonce)
	buf = append(buf, nonce...)
	ln := make([]byte, 4)
	binary.LittleEndian.PutUint32(ln, uint32(len(ct)))
	buf = append(buf, ln...)
	buf = append(buf, ct...)
	sig := ed25519.Sign(from.priv, buf)
	buf = append(buf, sig...)
	if _, err := envelope.Parse(buf); err != nil {
		t.Fatal(err)
	}
	return buf
}

func TestMailboxDeliverAndDeleteWithoutPlaintext(t *testing.T) {
	s, hs, setup := testServer(t)
	alice := newDevice(t)
	bob := newDevice(t)
	bootstrap(t, hs, setup, alice, "alice")
	req := authReq(t, http.MethodPost, hs.URL+"/v1/invites", "/v1/invites", []byte(`{"ttl_seconds":60}`), alice)
	resp, err := http.DefaultClient.Do(req)
	if err != nil {
		t.Fatal(err)
	}
	var inv struct {
		Token string `json:"token"`
	}
	_ = json.NewDecoder(resp.Body).Decode(&inv)
	resp.Body.Close()
	bootstrap(t, hs, inv.Token, bob, "bob")

	secret := "super-secret-plaintext-never-on-server"
	env := buildEnvelope(t, alice, bob, secret)

	ctx := context.Background()
	aliceWS := dialWS(t, ctx, hs, alice)
	defer aliceWS.Close(websocket.StatusNormalClosure, "")
	bobWS := dialWS(t, ctx, hs, bob)
	defer bobWS.Close(websocket.StatusNormalClosure, "")

	drainHello(t, ctx, bobWS)
	drainHello(t, ctx, aliceWS)

	if err := wsjson.Write(ctx, aliceWS, map[string]any{"type": "send", "envelope": env}); err != nil {
		t.Fatal(err)
	}
	queued := readSkipPresence(t, ctx, aliceWS)
	if queued.Type != "queued" {
		t.Fatalf("queued got %+v", queued)
	}
	delivered := readSkipPresence(t, ctx, bobWS)
	if delivered.Type != "deliver" {
		t.Fatalf("deliver got %+v", delivered)
	}
	if bytes.Contains(delivered.Envelope, []byte(secret)) {
		t.Fatal("plaintext leaked over the wire envelope")
	}
	n, err := s.Store.ScanPlaintext(secret)
	if err != nil {
		t.Fatal(err)
	}
	if n != 0 {
		t.Fatal("plaintext present in mailbox db")
	}
	if err := wsjson.Write(ctx, bobWS, map[string]any{"type": "ack", "message_id": delivered.MessageID}); err != nil {
		t.Fatal(err)
	}
	done := readSkipPresence(t, ctx, aliceWS)
	if done.Type != "delivered" {
		t.Fatalf("delivered got %+v", done)
	}
	count, err := s.Store.MailboxCount()
	if err != nil {
		t.Fatal(err)
	}
	if count != 0 {
		t.Fatalf("mailbox not empty: %d", count)
	}
}

func TestOfflineMailboxThenFlush(t *testing.T) {
	s, hs, setup := testServer(t)
	alice := newDevice(t)
	bob := newDevice(t)
	bootstrap(t, hs, setup, alice, "alice")
	req := authReq(t, http.MethodPost, hs.URL+"/v1/invites", "/v1/invites", []byte(`{"ttl_seconds":60}`), alice)
	resp, err := http.DefaultClient.Do(req)
	if err != nil {
		t.Fatal(err)
	}
	var inv struct {
		Token string `json:"token"`
	}
	_ = json.NewDecoder(resp.Body).Decode(&inv)
	resp.Body.Close()
	bootstrap(t, hs, inv.Token, bob, "bob")

	ctx := context.Background()
	aliceWS := dialWS(t, ctx, hs, alice)
	defer aliceWS.Close(websocket.StatusNormalClosure, "")
	drainHello(t, ctx, aliceWS)

	env := buildEnvelope(t, alice, bob, "offline-hi")
	if err := wsjson.Write(ctx, aliceWS, map[string]any{"type": "send", "envelope": env}); err != nil {
		t.Fatal(err)
	}
	queued := readSkipPresence(t, ctx, aliceWS)
	if queued.Type != "queued" {
		t.Fatalf("%+v", queued)
	}
	n, _ := s.Store.MailboxCount()
	if n != 1 {
		t.Fatalf("expected mailbox 1 got %d", n)
	}

	bobWS := dialWS(t, ctx, hs, bob)
	defer bobWS.Close(websocket.StatusNormalClosure, "")
	first := readSkipPresence(t, ctx, bobWS)
	if first.Type != "deliver" {
		t.Fatalf("expected deliver on reconnect, got %+v", first)
	}
}

func TestUniqueLoginRequired(t *testing.T) {
	_, hs, setup := testServer(t)
	owner := newDevice(t)
	body, _ := json.Marshal(map[string]any{
		"token": setup, "display_name": "", "public_identity": owner.blob, "device_id": owner.id,
	})
	resp, err := http.Post(hs.URL+"/v1/bootstrap", "application/json", bytes.NewReader(body))
	if err != nil {
		t.Fatal(err)
	}
	if resp.StatusCode != 400 {
		t.Fatalf("empty login wanted 400 got %d", resp.StatusCode)
	}
	resp.Body.Close()
	bootstrap(t, hs, setup, owner, "Anna")
	req := authReq(t, http.MethodPost, hs.URL+"/v1/invites", "/v1/invites", []byte(`{"ttl_seconds":60}`), owner)
	resp, err = http.DefaultClient.Do(req)
	if err != nil {
		t.Fatal(err)
	}
	var inv struct {
		Token string `json:"token"`
	}
	_ = json.NewDecoder(resp.Body).Decode(&inv)
	resp.Body.Close()
	dup := newDevice(t)
	body, _ = json.Marshal(map[string]any{
		"token": inv.Token, "display_name": "anna", "public_identity": dup.blob, "device_id": dup.id,
	})
	resp, err = http.Post(hs.URL+"/v1/bootstrap", "application/json", bytes.NewReader(body))
	if err != nil {
		t.Fatal(err)
	}
	if resp.StatusCode != 409 {
		t.Fatalf("duplicate login wanted 409 got %d", resp.StatusCode)
	}
	resp.Body.Close()
}

func TestPresenceBroadcast(t *testing.T) {
	_, hs, setup := testServer(t)
	alice := newDevice(t)
	bob := newDevice(t)
	bootstrap(t, hs, setup, alice, "alice")
	req := authReq(t, http.MethodPost, hs.URL+"/v1/invites", "/v1/invites", []byte(`{"ttl_seconds":60}`), alice)
	resp, err := http.DefaultClient.Do(req)
	if err != nil {
		t.Fatal(err)
	}
	var inv struct {
		Token string `json:"token"`
	}
	_ = json.NewDecoder(resp.Body).Decode(&inv)
	resp.Body.Close()
	bootstrap(t, hs, inv.Token, bob, "bob")

	ctx := context.Background()
	aliceWS := dialWS(t, ctx, hs, alice)
	defer aliceWS.Close(websocket.StatusNormalClosure, "")
	drainHello(t, ctx, aliceWS)

	bobWS := dialWS(t, ctx, hs, bob)
	var sawBob bool
	deadline := time.Now().Add(3 * time.Second)
	for time.Now().Before(deadline) && !sawBob {
		var msg wsOut
		rctx, cancel := context.WithTimeout(ctx, time.Second)
		err := wsjson.Read(rctx, aliceWS, &msg)
		cancel()
		if err != nil {
			continue
		}
		if msg.Type == "presence" {
			for _, id := range msg.Devices {
				if id == bob.id {
					sawBob = true
				}
			}
		}
	}
	if !sawBob {
		t.Fatal("alice did not see bob come online")
	}
	_ = bobWS.Close(websocket.StatusNormalClosure, "")
	sawOffline := false
	deadline = time.Now().Add(3 * time.Second)
	for time.Now().Before(deadline) && !sawOffline {
		var msg wsOut
		rctx, cancel := context.WithTimeout(ctx, time.Second)
		err := wsjson.Read(rctx, aliceWS, &msg)
		cancel()
		if err != nil {
			continue
		}
		if msg.Type == "presence" {
			sawOffline = true
			for _, id := range msg.Devices {
				if id == bob.id {
					sawOffline = false
				}
			}
		}
	}
	if !sawOffline {
		t.Fatal("alice did not see bob go offline")
	}
}

func TestRejectUnknownAuth(t *testing.T) {
	_, hs, _ := testServer(t)
	d := newDevice(t)
	req := authReq(t, http.MethodGet, hs.URL+"/v1/directory", "/v1/directory", nil, d)
	resp, err := http.DefaultClient.Do(req)
	if err != nil {
		t.Fatal(err)
	}
	if resp.StatusCode != 401 {
		t.Fatalf("got %d", resp.StatusCode)
	}
	resp.Body.Close()
}

func dialWS(t *testing.T, ctx context.Context, hs *httptest.Server, d testDevice) *websocket.Conn {
	t.Helper()
	ts := time.Now().Unix()
	sig := ed25519.Sign(d.priv, []byte(authz.WSMessage(ts)))
	u := strings.Replace(hs.URL, "http", "ws", 1) + "/v1/ws?device_id=" + d.id + "&ts=" + itoa(ts) + "&sig=" + base64.RawURLEncoding.EncodeToString(sig)
	c, _, err := websocket.Dial(ctx, u, nil)
	if err != nil {
		t.Fatal(err)
	}
	return c
}

func readSkipPresence(t *testing.T, ctx context.Context, c *websocket.Conn) wsOut {
	t.Helper()
	ctx, cancel := context.WithTimeout(ctx, 3*time.Second)
	defer cancel()
	for {
		var msg wsOut
		if err := wsjson.Read(ctx, c, &msg); err != nil {
			t.Fatal(err)
		}
		if msg.Type != "presence" {
			return msg
		}
	}
}

func drainHello(t *testing.T, ctx context.Context, c *websocket.Conn) {
	t.Helper()
	ctx, cancel := context.WithTimeout(ctx, 3*time.Second)
	defer cancel()
	gotDone := false
	gotPresence := false
	for !gotDone || !gotPresence {
		var msg wsOut
		if err := wsjson.Read(ctx, c, &msg); err != nil {
			t.Fatal(err)
		}
		switch msg.Type {
		case "mailbox_done":
			gotDone = true
		case "presence":
			gotPresence = true
		case "deliver":
			// leftover mailbox traffic is fine during hello
		default:
			t.Fatalf("unexpected hello frame %+v", msg)
		}
	}
}

func TestMain(m *testing.M) {
	os.Exit(m.Run())
}
