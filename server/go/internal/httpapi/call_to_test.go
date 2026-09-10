package httpapi

import (
	"context"
	"encoding/json"
	"net/http"
	"testing"

	"github.com/coder/websocket"
	"github.com/coder/websocket/wsjson"
)

func TestValidCallToRejectsControlBeforeTrim(t *testing.T) {
	id := "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa"
	got, ok := validCallTo(id)
	if !ok || got != id {
		t.Fatalf("hex got %q %v", got, ok)
	}
	if _, ok := validCallTo("\n" + id); ok {
		t.Fatal("leading newline must fail before trim")
	}
	if _, ok := validCallTo(id + "\r"); ok {
		t.Fatal("cr")
	}
	if _, ok := validCallTo(id + "\x00"); ok {
		t.Fatal("nul")
	}
	if _, ok := validCallTo(" " + id); ok {
		t.Fatal("leading space")
	}
	mixed := "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA"
	got, ok = validCallTo(mixed)
	if !ok || got != id {
		t.Fatalf("case fold got %q %v", got, ok)
	}
}

func TestCallToControlDoesNotRingPeer(t *testing.T) {
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
		"type": "call", "call_id": "c-nl", "to": "\n" + bob.id, "event": "ring",
	}); err != nil {
		t.Fatal(err)
	}
	got := readSkipPresence(t, ctx, aliceWS)
	if got.Type != "error" || got.Code != "protocol" {
		t.Fatalf("crlf to want protocol got %+v", got)
	}

	if err := wsjson.Write(ctx, aliceWS, map[string]any{
		"type": "call", "call_id": "c-ok", "to": bob.id, "event": "ring",
	}); err != nil {
		t.Fatal(err)
	}
	ring := readSkipPresence(t, ctx, bobWS)
	if ring.Type != "call" || ring.Event != "ring" || ring.CallID != "c-ok" {
		t.Fatalf("clean to want ring c-ok (not leaked crlf) got %+v", ring)
	}
}
