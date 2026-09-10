package httpapi

import (
	"context"
	"crypto/ed25519"
	"encoding/base64"
	"encoding/json"
	"net/http"
	"net/http/httptest"
	"strings"
	"testing"
	"time"

	"github.com/coder/websocket"
	"github.com/coder/websocket/wsjson"
	"github.com/dfa-ra/rope/server/go/internal/authz"
)

// Isolated transport so two sockets for the same device are not reused
// through http.DefaultClient's connection pool.
func dialWSIsolated(t *testing.T, ctx context.Context, hs *httptest.Server, d testDevice) *websocket.Conn {
	t.Helper()
	ts := time.Now().Unix()
	sig := ed25519.Sign(d.priv, []byte(authz.WSMessage(ts)))
	b64 := base64.RawURLEncoding.EncodeToString(sig)
	u := strings.Replace(hs.URL, "http", "ws", 1) + "/v1/ws?device_id=" + d.id + "&ts=" + itoa(ts) + "&sig=" + b64
	c, _, err := websocket.Dial(ctx, u, &websocket.DialOptions{
		HTTPClient: &http.Client{Transport: &http.Transport{DisableKeepAlives: true}},
	})
	if err != nil {
		t.Fatal(err)
	}
	return c
}

func TestHubReplaceClosesStaleSocket(t *testing.T) {
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
	stale := dialWSIsolated(t, ctx, hs, alice)
	defer stale.Close(websocket.StatusNormalClosure, "")
	drainHello(t, ctx, stale)

	live := dialWSIsolated(t, ctx, hs, alice)
	defer live.Close(websocket.StatusNormalClosure, "")
	drainHello(t, ctx, live)

	if n := len(s.Hub.Online()); n != 1 {
		t.Fatalf("online want 1 got %d %v", n, s.Hub.Online())
	}
	if id := s.Hub.Online()[0]; id != alice.id {
		t.Fatalf("online id %s want %s", id, alice.id)
	}

	readCtx, cancel := context.WithTimeout(ctx, 2*time.Second)
	defer cancel()
	staleClosed := false
	for {
		var msg wsOut
		if err := wsjson.Read(readCtx, stale, &msg); err != nil {
			staleClosed = true
			break
		}
	}
	if !staleClosed {
		t.Fatal("stale socket still readable after Hub.Add replace")
	}

	bobWS := dialWSIsolated(t, ctx, hs, bob)
	defer bobWS.Close(websocket.StatusNormalClosure, "")
	drainHello(t, ctx, bobWS)

	env := buildEnvelope(t, alice, bob, "after-replace")
	if err := wsjson.Write(ctx, live, map[string]any{"type": "send", "envelope": env}); err != nil {
		t.Fatal(err)
	}
	queued := readSkipPresence(t, ctx, live)
	if queued.Type != "queued" {
		t.Fatalf("live send want queued got %+v", queued)
	}
	got := readSkipPresence(t, ctx, bobWS)
	if got.Type != "deliver" {
		t.Fatalf("bob want deliver got %+v", got)
	}
}
