package httpapi

import (
	"bytes"
	"crypto/sha256"
	"encoding/hex"
	"encoding/json"
	"io"
	"net/http"
	"net/http/httptest"
	"os"
	"path/filepath"
	"strings"
	"testing"
	"time"

	"github.com/dfa-ra/rope/server/go/internal/config"
	"github.com/google/uuid"
)

func sha256Hex(b []byte) string {
	sum := sha256.Sum256(b)
	return hex.EncodeToString(sum[:])
}

func TestObjectUploadDownloadAndHashMismatch(t *testing.T) {
	_, hs, setup := testServer(t)
	owner := newDevice(t)
	bootstrap(t, hs, setup, owner, "owner")

	body := bytes.Repeat([]byte{0xAB}, 4096)
	req := authReq(t, http.MethodPost, hs.URL+"/v1/objects", "/v1/objects", body, owner)
	req.Header.Set("X-Rope-SHA256", "deadbeef")
	resp, err := http.DefaultClient.Do(req)
	if err != nil {
		t.Fatal(err)
	}
	io.Copy(io.Discard, resp.Body)
	resp.Body.Close()
	if resp.StatusCode != http.StatusBadRequest {
		t.Fatalf("hash mismatch want 400 got %d", resp.StatusCode)
	}

	sum := sha256Hex(body)
	req = authReq(t, http.MethodPost, hs.URL+"/v1/objects", "/v1/objects", body, owner)
	req.Header.Set("X-Rope-SHA256", sum)
	resp, err = http.DefaultClient.Do(req)
	if err != nil {
		t.Fatal(err)
	}
	if resp.StatusCode != http.StatusOK {
		b, _ := io.ReadAll(resp.Body)
		t.Fatalf("upload want 200 got %d body=%s", resp.StatusCode, b)
	}
	var out struct {
		ObjectID string `json:"object_id"`
		SHA256   string `json:"sha256"`
		Size     int64  `json:"size"`
	}
	if err := json.NewDecoder(resp.Body).Decode(&out); err != nil {
		t.Fatal(err)
	}
	resp.Body.Close()
	if out.ObjectID == "" || out.Size != int64(len(body)) || out.SHA256 != sum {
		t.Fatalf("unexpected upload response %+v", out)
	}

	req = authReq(t, http.MethodGet, hs.URL+"/v1/objects/"+out.ObjectID, "/v1/objects/"+out.ObjectID, nil, owner)
	resp, err = http.DefaultClient.Do(req)
	if err != nil {
		t.Fatal(err)
	}
	got, _ := io.ReadAll(resp.Body)
	resp.Body.Close()
	if resp.StatusCode != http.StatusOK {
		t.Fatalf("download want 200 got %d", resp.StatusCode)
	}
	if resp.Header.Get("X-Rope-SHA256") != sum {
		t.Fatalf("download hash %s want %s", resp.Header.Get("X-Rope-SHA256"), sum)
	}
	if resp.Header.Get("Cache-Control") != "no-store" {
		t.Fatalf("download Cache-Control %q", resp.Header.Get("Cache-Control"))
	}
	if !bytes.Equal(got, body) {
		t.Fatal("downloaded body mismatch")
	}
}

func TestObjectQuota(t *testing.T) {
	_, hs, setup := testServerCfg(t, func(c *config.Config) {
		c.ObjectQuotaBytes = 100
	})
	owner := newDevice(t)
	bootstrap(t, hs, setup, owner, "owner")
	body := bytes.Repeat([]byte{1}, 80)
	sum := sha256Hex(body)
	req := authReq(t, http.MethodPost, hs.URL+"/v1/objects", "/v1/objects", body, owner)
	req.Header.Set("X-Rope-SHA256", sum)
	resp, err := http.DefaultClient.Do(req)
	if err != nil {
		t.Fatal(err)
	}
	io.Copy(io.Discard, resp.Body)
	resp.Body.Close()
	if resp.StatusCode != http.StatusOK {
		t.Fatalf("first upload %d", resp.StatusCode)
	}
	req = authReq(t, http.MethodPost, hs.URL+"/v1/objects", "/v1/objects", body, owner)
	req.Header.Set("X-Rope-SHA256", sum)
	resp, err = http.DefaultClient.Do(req)
	if err != nil {
		t.Fatal(err)
	}
	io.Copy(io.Discard, resp.Body)
	resp.Body.Close()
	if resp.StatusCode != http.StatusInsufficientStorage {
		t.Fatalf("quota want 507 got %d", resp.StatusCode)
	}
}

func getObject(t *testing.T, hs *httptest.Server, d testDevice, id string) (int, string) {
	t.Helper()
	path := "/v1/objects/" + id
	req := authReq(t, http.MethodGet, hs.URL+path, path, nil, d)
	resp, err := http.DefaultClient.Do(req)
	if err != nil {
		t.Fatal(err)
	}
	defer resp.Body.Close()
	b, _ := io.ReadAll(resp.Body)
	return resp.StatusCode, string(b)
}

func assertObjectNotFound(t *testing.T, code int, body, label string) {
	t.Helper()
	if code != 404 {
		t.Fatalf("%s want 404 got %d body=%q", label, code, body)
	}
	if !strings.Contains(body, `"error":"not found"`) {
		t.Fatalf("%s want not found got %q", label, body)
	}
	for _, leak := range []string{"expired", "missing blob"} {
		if strings.Contains(body, leak) {
			t.Fatalf("%s leaked %q in %q", label, leak, body)
		}
	}
}

func TestObjectDownloadErrorsAreUniform(t *testing.T) {
	s, hs, setup := testServer(t)
	owner := newDevice(t)
	bootstrap(t, hs, setup, owner, "owner")

	code, body := getObject(t, hs, owner, uuid.NewString())
	assertObjectNotFound(t, code, body, "unknown uuid")

	code, body = getObject(t, hs, owner, "not-a-uuid")
	assertObjectNotFound(t, code, body, "garbage id")

	if err := os.MkdirAll(s.Cfg.ObjectsDir(), 0o750); err != nil {
		t.Fatal(err)
	}

	expiredID := uuid.NewString()
	if err := s.Store.InsertObject(expiredID, owner.id, "dead", 1, -time.Hour); err != nil {
		t.Fatal(err)
	}
	if err := os.WriteFile(filepath.Join(s.Cfg.ObjectsDir(), expiredID), []byte("x"), 0o640); err != nil {
		t.Fatal(err)
	}
	code, body = getObject(t, hs, owner, expiredID)
	assertObjectNotFound(t, code, body, "expired")

	missingID := uuid.NewString()
	if err := s.Store.InsertObject(missingID, owner.id, "dead", 1, time.Hour); err != nil {
		t.Fatal(err)
	}
	code, body = getObject(t, hs, owner, missingID)
	assertObjectNotFound(t, code, body, "missing blob")
}

func TestAdminStage2Fields(t *testing.T) {
	_, hs, setup := testServer(t)
	owner := newDevice(t)
	bootstrap(t, hs, setup, owner, "owner")
	req := authReq(t, http.MethodGet, hs.URL+"/v1/admin/status", "/v1/admin/status", nil, owner)
	resp, err := http.DefaultClient.Do(req)
	if err != nil {
		t.Fatal(err)
	}
	defer resp.Body.Close()
	if resp.StatusCode != http.StatusOK {
		b, _ := io.ReadAll(resp.Body)
		t.Fatalf("admin %d %s", resp.StatusCode, b)
	}
	var out map[string]any
	if err := json.NewDecoder(resp.Body).Decode(&out); err != nil {
		t.Fatal(err)
	}
	for _, key := range []string{
		"server_id", "version", "device_count", "object_count", "object_bytes",
		"group_count", "max_object_bytes", "online_devices", "mailbox_count",
		"turn_running", "turn_allocate_ok", "turns_listening", "turn_error", "ice_enabled",
	} {
		if _, ok := out[key]; !ok {
			t.Fatalf("missing admin field %s in %v", key, out)
		}
	}
	if out["ice_enabled"] != false {
		t.Fatalf("no TURN config so ice_enabled must be false: %v", out["ice_enabled"])
	}
	if out["turn_running"] != false {
		t.Fatalf("turn_running means coturn listen, not secret: %v", out["turn_running"])
	}
}

func TestAdminTurnRunningIsListenNotSecret(t *testing.T) {
	_, hs, setup := testServerCfg(t, func(cfg *config.Config) {
		cfg.PublicHost = "198.51.100.20"
		cfg.TurnSecret = "hmac-from-install"
		cfg.TurnsPort = 443
	})
	owner := newDevice(t)
	bootstrap(t, hs, setup, owner, "owner")
	orig := config.DialTCP
	t.Cleanup(func() { config.DialTCP = orig })
	config.DialTCP = func(string, time.Duration) error { return errAdminTurnDown }
	req := authReq(t, http.MethodGet, hs.URL+"/v1/admin/status", "/v1/admin/status", nil, owner)
	resp, err := http.DefaultClient.Do(req)
	if err != nil {
		t.Fatal(err)
	}
	defer resp.Body.Close()
	var out map[string]any
	if err := json.NewDecoder(resp.Body).Decode(&out); err != nil {
		t.Fatal(err)
	}
	if out["ice_enabled"] != true {
		t.Fatal("secret+host must set ice_enabled")
	}
	if out["turn_running"] != false {
		t.Fatalf("nothing listening so turn_running must be false: %v", out)
	}
	errStr, _ := out["turn_error"].(string)
	if errStr == "" {
		t.Fatal("expected turn_error when coturn is down")
	}
}

type adminTurnDown string

func (e adminTurnDown) Error() string { return string(e) }

var errAdminTurnDown = adminTurnDown("down")
