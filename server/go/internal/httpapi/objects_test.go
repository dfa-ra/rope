package httpapi

import (
	"bytes"
	"crypto/sha256"
	"encoding/hex"
	"encoding/json"
	"io"
	"net/http"
	"testing"

	"github.com/dfa-ra/rope/server/go/internal/config"
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
	} {
		if _, ok := out[key]; !ok {
			t.Fatalf("missing admin field %s in %v", key, out)
		}
	}
}
