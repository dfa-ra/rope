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
	if resp.Header.Get("Cache-Control") != "no-store" {
		t.Fatalf("health Cache-Control %q", resp.Header.Get("Cache-Control"))
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
	if resp.Header.Get("Cache-Control") != "no-store" {
		t.Fatalf("info Cache-Control %q", resp.Header.Get("Cache-Control"))
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
	_, hs, token := testServerCfg(t, func(cfg *config.Config) {
		cfg.PublicHost = "198.51.100.20"
		cfg.TurnSecret = "hmac-from-install"
		cfg.TurnsPort = 443
	})
	resp, err := http.Get(hs.URL + "/v1/info")
	if err != nil {
		t.Fatal(err)
	}
	var anon map[string]any
	if err := json.NewDecoder(resp.Body).Decode(&anon); err != nil {
		t.Fatal(err)
	}
	resp.Body.Close()
	if _, ok := anon["ice_servers"]; ok {
		t.Fatal("unauthenticated GET /v1/info must not advertise TURN creds")
	}
	if _, ok := anon["ice_ttl_seconds"]; ok {
		t.Fatal("unauthenticated GET /v1/info must omit ice_ttl_seconds")
	}
	if _, ok := anon["public_ip"]; ok {
		t.Fatal("unauthenticated GET /v1/info must not advertise public_ip")
	}
	if anon["server_id"] != "test-server" || anon["fingerprint"] == nil {
		t.Fatalf("join still needs server_id and fingerprint: %+v", anon)
	}

	owner := newDevice(t)
	bootstrap(t, hs, token, owner, "owner")
	req := authReq(t, http.MethodGet, hs.URL+"/v1/info", "/v1/info", nil, owner)
	resp, err = http.DefaultClient.Do(req)
	if err != nil {
		t.Fatal(err)
	}
	defer resp.Body.Close()
	if resp.StatusCode != 200 {
		t.Fatalf("authed info %d", resp.StatusCode)
	}
	var info struct {
		ServerID      string             `json:"server_id"`
		PublicIP      string             `json:"public_ip"`
		IceTTLSeconds int                `json:"ice_ttl_seconds"`
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
	_, hs, token := testServerCfg(t, func(cfg *config.Config) {
		cfg.PublicHost = "198.51.100.20"
		cfg.TurnSecret = "hmac-from-install"
		cfg.TurnsPort = 5349
	})
	owner := newDevice(t)
	bootstrap(t, hs, token, owner, "owner")
	req := authReq(t, http.MethodGet, hs.URL+"/v1/info", "/v1/info", nil, owner)
	resp, err := http.DefaultClient.Do(req)
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
	_, hs, token := testServerCfg(t, func(cfg *config.Config) {
		cfg.PublicHost = "vps.example"
		cfg.PublicIP = "203.0.113.9"
		cfg.TurnSecret = "hmac-from-install"
		cfg.TurnsPort = 443
	})
	owner := newDevice(t)
	bootstrap(t, hs, token, owner, "owner")
	req := authReq(t, http.MethodGet, hs.URL+"/v1/info", "/v1/info", nil, owner)
	resp, err := http.DefaultClient.Do(req)
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

	_, hsIP, tokenIP := testServerCfg(t, func(cfg *config.Config) {
		cfg.PublicHost = "203.0.113.9"
		cfg.TLSHostname = "rope.example"
		cfg.TurnSecret = "hmac-from-install"
		cfg.TurnsPort = 443
	})
	ipOwner := newDevice(t)
	bootstrap(t, hsIP, tokenIP, ipOwner, "owner")
	reqIP := authReq(t, http.MethodGet, hsIP.URL+"/v1/info", "/v1/info", nil, ipOwner)
	resp, err = http.DefaultClient.Do(reqIP)
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
	_, hs, token := testServerCfg(t, func(cfg *config.Config) {
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

	owner := newDevice(t)
	bootstrap(t, hs, token, owner, "owner")
	req := authReq(t, http.MethodGet, hs.URL+"/v1/info", "/v1/info", nil, owner)
	resp, err = http.DefaultClient.Do(req)
	if err != nil {
		t.Fatal(err)
	}
	defer resp.Body.Close()
	var authed map[string]any
	if err := json.NewDecoder(resp.Body).Decode(&authed); err != nil {
		t.Fatal(err)
	}
	if _, ok := authed["ice_servers"]; ok {
		t.Fatalf("private-only host must not advertise ice_servers when authed: %+v", authed)
	}
}

func TestClientIPIgnoresXForwardedFor(t *testing.T) {
	r := httptest.NewRequest(http.MethodPost, "/v1/bootstrap", nil)
	r.RemoteAddr = "203.0.113.9:4321"
	r.Header.Set("X-Forwarded-For", "1.2.3.4, 10.0.0.1")
	if got := clientIP(r); got != "203.0.113.9" {
		t.Fatalf("clientIP=%q", got)
	}
}

func TestHealthHidesTurnAddressOffLoopback(t *testing.T) {
	turn := config.TurnReport{
		Running:        true,
		AllocateOK:     true,
		TurnsListening: true,
		RelayedIP:      "203.0.113.9",
		TurnPort:       3478,
		TurnsPort:      443,
		Error:          "should stay local",
	}
	pub := healthJSON(true, false, turn)
	if pub["ok"] != true || pub["turn_running"] != true || pub["turn_allocate_ok"] != true {
		t.Fatalf("public health %+v", pub)
	}
	for _, k := range []string{"turn_relayed_ip", "turn_port", "turns_port", "turns_listening", "turn_error"} {
		if _, ok := pub[k]; ok {
			t.Fatalf("public health leaked %s: %+v", k, pub)
		}
	}
	loc := healthJSON(true, true, turn)
	if loc["turn_relayed_ip"] != "203.0.113.9" {
		t.Fatalf("loopback health missing relayed ip: %+v", loc)
	}
	if loc["turn_port"] != 3478 || loc["turns_port"] != 443 {
		t.Fatalf("loopback health missing ports: %+v", loc)
	}
	if loc["turn_error"] != "should stay local" {
		t.Fatalf("loopback health missing error: %+v", loc)
	}
	if !addrIsLoopback("127.0.0.1:9") || !addrIsLoopback("[::1]:9") {
		t.Fatal("loopback RemoteAddr must be local")
	}
	if addrIsLoopback("203.0.113.9:4321") {
		t.Fatal("public RemoteAddr must not be local")
	}
	r := httptest.NewRequest(http.MethodGet, "/health", nil)
	r.RemoteAddr = "203.0.113.9:4321"
	r.Header.Set("X-Forwarded-For", "127.0.0.1")
	if addrIsLoopback(r.RemoteAddr) {
		t.Fatal("X-Forwarded-For must not make health loopback")
	}
}

func TestInfoBadAuthDoesNotLeakIce(t *testing.T) {
	_, hs, _ := testServerCfg(t, func(cfg *config.Config) {
		cfg.PublicHost = "198.51.100.20"
		cfg.TurnSecret = "hmac-from-install"
		cfg.TurnsPort = 443
	})
	req, err := http.NewRequest(http.MethodGet, hs.URL+"/v1/info", nil)
	if err != nil {
		t.Fatal(err)
	}
	req.Header.Set("Authorization", "Rope deadbeef.1.aaaa")
	resp, err := http.DefaultClient.Do(req)
	if err != nil {
		t.Fatal(err)
	}
	defer resp.Body.Close()
	if resp.StatusCode != 401 {
		t.Fatalf("got %d", resp.StatusCode)
	}
	var body map[string]any
	if err := json.NewDecoder(resp.Body).Decode(&body); err != nil {
		t.Fatal(err)
	}
	if _, ok := body["ice_servers"]; ok {
		t.Fatalf("401 info leaked ice: %+v", body)
	}
	if _, ok := body["public_ip"]; ok {
		t.Fatalf("401 info leaked public_ip: %+v", body)
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

func TestInviteTTLSecondsClamp(t *testing.T) {
	if got := inviteTTLSeconds(0); got != defaultInviteTTLSeconds {
		t.Fatalf("zero %d", got)
	}
	if got := inviteTTLSeconds(-1); got != defaultInviteTTLSeconds {
		t.Fatalf("neg %d", got)
	}
	if got := inviteTTLSeconds(3600); got != 3600 {
		t.Fatalf("hour %d", got)
	}
	if got := inviteTTLSeconds(1); got != 1 {
		t.Fatalf("one %d", got)
	}
	if got := inviteTTLSeconds(maxInviteTTLSeconds); got != maxInviteTTLSeconds {
		t.Fatalf("max %d", got)
	}
	if got := inviteTTLSeconds(maxInviteTTLSeconds + 1); got != maxInviteTTLSeconds {
		t.Fatalf("over %d", got)
	}
	if got := inviteTTLSeconds(1_000_000_000); got != maxInviteTTLSeconds {
		t.Fatalf("huge %d", got)
	}
}

func TestInviteTTLIsCapped(t *testing.T) {
	_, hs, setup := testServer(t)
	owner := newDevice(t)
	bootstrap(t, hs, setup, owner, "owner")
	before := time.Now()
	req := authReq(t, http.MethodPost, hs.URL+"/v1/invites", "/v1/invites", []byte(`{"ttl_seconds":1000000000}`), owner)
	resp, err := http.DefaultClient.Do(req)
	if err != nil {
		t.Fatal(err)
	}
	defer resp.Body.Close()
	if resp.StatusCode != 200 {
		b, _ := io.ReadAll(resp.Body)
		t.Fatalf("invite %d %s", resp.StatusCode, b)
	}
	var inv struct {
		ExpiresAt string `json:"expires_at"`
	}
	if err := json.NewDecoder(resp.Body).Decode(&inv); err != nil {
		t.Fatal(err)
	}
	exp, err := time.Parse(time.RFC3339, inv.ExpiresAt)
	if err != nil {
		t.Fatal(err)
	}
	maxDur := time.Duration(maxInviteTTLSeconds) * time.Second
	if exp.Before(before.Add(maxDur - 5*time.Second)) || exp.After(time.Now().Add(maxDur+5*time.Second)) {
		t.Fatalf("expires_at %s not clamped to 24h", inv.ExpiresAt)
	}
	if exp.After(before.Add(48 * time.Hour)) {
		t.Fatalf("expires_at %s still multi-day", inv.ExpiresAt)
	}
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

	ok := newDevice(t)
	bootstrap(t, hs, inv.Token, ok, "boris")
}

func TestBootstrapGarbageTokenDoesNotEnumerateLogin(t *testing.T) {
	_, hs, setup := testServer(t)
	owner := newDevice(t)
	bootstrap(t, hs, setup, owner, "anna")
	taken := newDevice(t)
	free := newDevice(t)
	bodyTaken, _ := json.Marshal(map[string]any{
		"token": "nope", "display_name": "anna", "public_identity": taken.blob, "device_id": taken.id,
	})
	bodyFree, _ := json.Marshal(map[string]any{
		"token": "nope", "display_name": "zoya", "public_identity": free.blob, "device_id": free.id,
	})
	respTaken, err := http.Post(hs.URL+"/v1/bootstrap", "application/json", bytes.NewReader(bodyTaken))
	if err != nil {
		t.Fatal(err)
	}
	respFree, err := http.Post(hs.URL+"/v1/bootstrap", "application/json", bytes.NewReader(bodyFree))
	if err != nil {
		t.Fatal(err)
	}
	if respTaken.StatusCode != 403 || respFree.StatusCode != 403 {
		t.Fatalf("garbage token must be 403 for taken and free login, got %d and %d",
			respTaken.StatusCode, respFree.StatusCode)
	}
	if got := bootstrapErr(t, respTaken); got != bootstrapInvalidToken {
		t.Fatalf("taken-name garbage body %q", got)
	}
	if got := bootstrapErr(t, respFree); got != bootstrapInvalidToken {
		t.Fatalf("free-name garbage body %q", got)
	}
}

func TestBootstrapTokenErrorsAreOpaque(t *testing.T) {
	_, hs, setup := testServer(t)
	fresh := newDevice(t)
	body, _ := json.Marshal(map[string]any{
		"token": "nope", "display_name": "nope", "public_identity": fresh.blob, "device_id": fresh.id,
	})
	resp, err := http.Post(hs.URL+"/v1/bootstrap", "application/json", bytes.NewReader(body))
	if err != nil {
		t.Fatal(err)
	}
	if resp.StatusCode != 403 || bootstrapErr(t, resp) != bootstrapInvalidToken {
		t.Fatalf("uninitialized setup miss must be opaque 403, got %d", resp.StatusCode)
	}

	owner := newDevice(t)
	bootstrap(t, hs, setup, owner, "anna")
	req := authReq(t, http.MethodPost, hs.URL+"/v1/invites", "/v1/invites", []byte(`{"ttl_seconds":60}`), owner)
	invResp, err := http.DefaultClient.Do(req)
	if err != nil {
		t.Fatal(err)
	}
	var inv struct {
		Token string `json:"token"`
	}
	if err := json.NewDecoder(invResp.Body).Decode(&inv); err != nil {
		t.Fatal(err)
	}
	invResp.Body.Close()

	guest := newDevice(t)
	bootstrap(t, hs, inv.Token, guest, "guest")
	reuse := newDevice(t)
	reuseBody, _ := json.Marshal(map[string]any{
		"token": inv.Token, "display_name": "guest2", "public_identity": reuse.blob, "device_id": reuse.id,
	})
	reuseResp, err := http.Post(hs.URL+"/v1/bootstrap", "application/json", bytes.NewReader(reuseBody))
	if err != nil {
		t.Fatal(err)
	}
	if reuseResp.StatusCode != 403 || bootstrapErr(t, reuseResp) != bootstrapInvalidToken {
		t.Fatalf("used invite must be opaque 403, got %d", reuseResp.StatusCode)
	}

	ttlReq := authReq(t, http.MethodPost, hs.URL+"/v1/invites", "/v1/invites", []byte(`{"ttl_seconds":1}`), owner)
	ttlResp, err := http.DefaultClient.Do(ttlReq)
	if err != nil {
		t.Fatal(err)
	}
	if err := json.NewDecoder(ttlResp.Body).Decode(&inv); err != nil {
		t.Fatal(err)
	}
	ttlResp.Body.Close()
	time.Sleep(2 * time.Second)
	late := newDevice(t)
	lateBody, _ := json.Marshal(map[string]any{
		"token": inv.Token, "display_name": "late", "public_identity": late.blob, "device_id": late.id,
	})
	lateResp, err := http.Post(hs.URL+"/v1/bootstrap", "application/json", bytes.NewReader(lateBody))
	if err != nil {
		t.Fatal(err)
	}
	if lateResp.StatusCode != 403 || bootstrapErr(t, lateResp) != bootstrapInvalidToken {
		t.Fatalf("expired invite must be opaque 403, got %d", lateResp.StatusCode)
	}
}

func bootstrapErr(t *testing.T, resp *http.Response) string {
	t.Helper()
	defer resp.Body.Close()
	raw, err := io.ReadAll(resp.Body)
	if err != nil {
		t.Fatal(err)
	}
	var out struct {
		Error string `json:"error"`
	}
	if err := json.Unmarshal(raw, &out); err != nil {
		t.Fatalf("body %s: %v", raw, err)
	}
	return out.Error
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

func wsQuery(hs *httptest.Server, deviceID, ts, sig string) string {
	return hs.URL + "/v1/ws?device_id=" + deviceID + "&ts=" + ts + "&sig=" + sig
}

func getWSAuth(t *testing.T, url string) (int, string) {
	t.Helper()
	resp, err := http.Get(url)
	if err != nil {
		t.Fatal(err)
	}
	defer resp.Body.Close()
	b, _ := io.ReadAll(resp.Body)
	return resp.StatusCode, string(b)
}

func assertWSUnauthorized(t *testing.T, code int, body, label string) {
	t.Helper()
	if code != 401 {
		t.Fatalf("%s want 401 got %d body=%q", label, code, body)
	}
	if !strings.Contains(body, "unauthorized") {
		t.Fatalf("%s want unauthorized got %q", label, body)
	}
	for _, leak := range []string{"missing auth", "skew", "unknown device", "revoked", "bad sig"} {
		if strings.Contains(body, leak) {
			t.Fatalf("%s leaked %q in %q", label, leak, body)
		}
	}
}

func TestWsUnauthorizedIsUniform(t *testing.T) {
	_, hs, setup := testServer(t)
	owner := newDevice(t)
	guest := newDevice(t)
	bootstrap(t, hs, setup, owner, "owner")

	code, body := getWSAuth(t, hs.URL+"/v1/ws")
	assertWSUnauthorized(t, code, body, "no creds")

	dummy := base64.RawURLEncoding.EncodeToString(bytes.Repeat([]byte{1}, 64))
	now := itoa(time.Now().Unix())
	code, body = getWSAuth(t, wsQuery(hs, strings.Repeat("ab", 32), now, dummy))
	assertWSUnauthorized(t, code, body, "unknown device")

	bad := base64.RawURLEncoding.EncodeToString(bytes.Repeat([]byte{2}, 64))
	code, body = getWSAuth(t, wsQuery(hs, owner.id, now, bad))
	assertWSUnauthorized(t, code, body, "bad sig")

	old := itoa(time.Now().Add(-20 * time.Minute).Unix())
	sig := base64.RawURLEncoding.EncodeToString(ed25519.Sign(owner.priv, []byte(authz.WSMessage(time.Now().Add(-20*time.Minute).Unix()))))
	code, body = getWSAuth(t, wsQuery(hs, owner.id, old, sig))
	assertWSUnauthorized(t, code, body, "skew")

	req := authReq(t, http.MethodPost, hs.URL+"/v1/invites", "/v1/invites", []byte(`{"ttl_seconds":60}`), owner)
	resp, err := http.DefaultClient.Do(req)
	if err != nil {
		t.Fatal(err)
	}
	var inv struct {
		Token string `json:"token"`
	}
	_ = json.NewDecoder(resp.Body).Decode(&inv)
	resp.Body.Close()
	bootstrap(t, hs, inv.Token, guest, "guest")

	dirReq := authReq(t, http.MethodGet, hs.URL+"/v1/directory", "/v1/directory", nil, owner)
	dirResp, err := http.DefaultClient.Do(dirReq)
	if err != nil {
		t.Fatal(err)
	}
	var dir struct {
		Members []struct {
			MemberID    string `json:"member_id"`
			DisplayName string `json:"display_name"`
		} `json:"members"`
	}
	if err := json.NewDecoder(dirResp.Body).Decode(&dir); err != nil {
		t.Fatal(err)
	}
	dirResp.Body.Close()
	var guestMember string
	for _, m := range dir.Members {
		if m.DisplayName == "guest" {
			guestMember = m.MemberID
		}
	}
	if guestMember == "" {
		t.Fatal("guest member missing")
	}
	revBody, _ := json.Marshal(map[string]string{"member_id": guestMember})
	revReq := authReq(t, http.MethodPost, hs.URL+"/v1/admin/revoke-member", "/v1/admin/revoke-member", revBody, owner)
	revResp, err := http.DefaultClient.Do(revReq)
	if err != nil {
		t.Fatal(err)
	}
	io.Copy(io.Discard, revResp.Body)
	revResp.Body.Close()
	if revResp.StatusCode != 200 {
		t.Fatalf("revoke guest %d", revResp.StatusCode)
	}

	ts := time.Now().Unix()
	okSig := base64.RawURLEncoding.EncodeToString(ed25519.Sign(guest.priv, []byte(authz.WSMessage(ts))))
	code, body = getWSAuth(t, wsQuery(hs, guest.id, itoa(ts), okSig))
	assertWSUnauthorized(t, code, body, "revoked member")
}

func dialWS(t *testing.T, ctx context.Context, hs *httptest.Server, d testDevice) *websocket.Conn {
	t.Helper()
	return dialWSMode(t, ctx, hs, d, "query")
}

func dialWSMode(t *testing.T, ctx context.Context, hs *httptest.Server, d testDevice, mode string) *websocket.Conn {
	t.Helper()
	ts := time.Now().Unix()
	sig := ed25519.Sign(d.priv, []byte(authz.WSMessage(ts)))
	b64 := base64.RawURLEncoding.EncodeToString(sig)
	base := strings.Replace(hs.URL, "http", "ws", 1)
	opts := &websocket.DialOptions{}
	u := base + "/v1/ws"
	switch mode {
	case "query":
		u += "?device_id=" + d.id + "&ts=" + itoa(ts) + "&sig=" + b64
	case "header":
		opts.HTTPHeader = http.Header{authz.WsAuthHeader: []string{d.id + "." + itoa(ts) + "." + b64}}
	case "both":
		u += "?device_id=" + d.id + "&ts=" + itoa(ts) + "&sig=" + b64
		opts.HTTPHeader = http.Header{authz.WsAuthHeader: []string{d.id + "." + itoa(ts) + "." + b64}}
	default:
		t.Fatalf("mode %s", mode)
	}
	c, _, err := websocket.Dial(ctx, u, opts)
	if err != nil {
		t.Fatal(err)
	}
	return c
}

func TestWsAuthHeaderOnlyAndBoth(t *testing.T) {
	_, hs, setup := testServer(t)
	d := newDevice(t)
	bootstrap(t, hs, setup, d, "owner")
	ctx := context.Background()
	for _, mode := range []string{"query", "header", "both"} {
		c := dialWSMode(t, ctx, hs, d, mode)
		drainHello(t, ctx, c)
		_ = c.Close(websocket.StatusNormalClosure, "")
	}
}

func TestWsAuthRejectsGarbageHeader(t *testing.T) {
	_, hs, setup := testServer(t)
	d := newDevice(t)
	bootstrap(t, hs, setup, d, "owner")
	ts := time.Now().Unix()
	sig := ed25519.Sign(d.priv, []byte(authz.WSMessage(ts)))
	b64 := base64.RawURLEncoding.EncodeToString(sig)
	u := strings.Replace(hs.URL, "http", "ws", 1) + "/v1/ws?device_id=" + d.id + "&ts=" + itoa(ts) + "&sig=" + b64
	_, _, err := websocket.Dial(context.Background(), u, &websocket.DialOptions{
		HTTPHeader: http.Header{authz.WsAuthHeader: []string{"nope"}},
	})
	if err == nil {
		t.Fatal("garbage header must not fall back to query")
	}
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

func TestRecovererWritesJSON(t *testing.T) {
	s := &Server{Log: log.New(io.Discard, "", 0)}
	h := s.recoverer(http.HandlerFunc(func(http.ResponseWriter, *http.Request) {
		panic("test-panic")
	}))
	rec := httptest.NewRecorder()
	h.ServeHTTP(rec, httptest.NewRequest(http.MethodGet, "/panic", nil))
	if rec.Code != 500 {
		t.Fatalf("code %d", rec.Code)
	}
	ct := rec.Header().Get("Content-Type")
	if !strings.Contains(ct, "application/json") {
		t.Fatalf("content-type %q", ct)
	}
	body := rec.Body.String()
	if !strings.Contains(body, `"error":"internal"`) {
		t.Fatalf("body %q", body)
	}
	if strings.Contains(body, "test-panic") {
		t.Fatalf("leaked panic %q", body)
	}
}

func TestMain(m *testing.M) {
	os.Exit(m.Run())
}
