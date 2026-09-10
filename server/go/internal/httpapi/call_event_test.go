package httpapi

import (
	"context"
	"encoding/json"
	"net/http"
	"testing"

	"github.com/coder/websocket"
	"github.com/coder/websocket/wsjson"
)

func TestValidCallEventRejectsControlBeforeTrim(t *testing.T) {
	got, ok := validCallEvent("ring")
	if !ok || got != "ring" {
		t.Fatalf("plain got %q %v", got, ok)
	}
	got, ok = validCallEvent("  ring  ")
	if !ok || got != "ring" {
		t.Fatalf("spaces still trim got %q %v", got, ok)
	}
	if _, ok := validCallEvent("ring\n"); ok {
		t.Fatal("newline must fail before trim")
	}
	if _, ok := validCallEvent("\nring"); ok {
		t.Fatal("leading newline")
	}
	if _, ok := validCallEvent("ring\r"); ok {
		t.Fatal("cr")
	}
	if _, ok := validCallEvent("ring\x00"); ok {
		t.Fatal("nul")
	}
	if _, ok := validCallEvent(""); ok {
		t.Fatal("empty")
	}
	if _, ok := validCallEvent("   "); ok {
		t.Fatal("blank")
	}
}

func TestCallEventControlDoesNotRingPeer(t *testing.T) {
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
	bobWS := dialWS(t, ctx, hs, bob)
	defer bobWS.Close(websocket.StatusNormalClosure, "")
	drainHello(t, ctx, aliceWS)
	drainHello(t, ctx, bobWS)

	if err := wsjson.Write(ctx, aliceWS, map[string]any{
		"type": "call", "call_id": "c-nl", "to": bob.id, "event": "ring\n",
	}); err != nil {
		t.Fatal(err)
	}
	got := readSkipPresence(t, ctx, aliceWS)
	if got.Type != "error" || got.Code != "protocol" {
		t.Fatalf("crlf event want protocol got %+v", got)
	}

	if err := wsjson.Write(ctx, aliceWS, map[string]any{
		"type": "call", "call_id": "c-ok", "to": bob.id, "event": "ring",
	}); err != nil {
		t.Fatal(err)
	}
	ring := readSkipPresence(t, ctx, bobWS)
	if ring.Type != "call" || ring.Event != "ring" || ring.CallID != "c-ok" {
		t.Fatalf("clean event want ring c-ok (not leaked crlf) got %+v", ring)
	}
}
