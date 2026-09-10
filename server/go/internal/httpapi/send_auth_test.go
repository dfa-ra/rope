package httpapi

import (
	"context"
	"encoding/json"
	"net/http"
	"testing"

	"github.com/coder/websocket"
	"github.com/coder/websocket/wsjson"
)

func TestSendRejectsForeignSender(t *testing.T) {
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
	bobWS := dialWS(t, ctx, hs, bob)
	defer bobWS.Close(websocket.StatusNormalClosure, "")
	drainHello(t, ctx, aliceWS)
	drainHello(t, ctx, bobWS)

	stolen := buildEnvelope(t, bob, alice, "not-from-alice")
	if err := wsjson.Write(ctx, aliceWS, map[string]any{"type": "send", "envelope": stolen}); err != nil {
		t.Fatal(err)
	}
	got := readSkipPresence(t, ctx, aliceWS)
	if got.Type != "error" || got.Code != "auth" {
		t.Fatalf("foreign sender want auth error got %+v", got)
	}
	if got.Message != "sender mismatch" {
		t.Fatalf("foreign sender message %q", got.Message)
	}
	n, err := s.Store.MailboxCount()
	if err != nil {
		t.Fatal(err)
	}
	if n != 0 {
		t.Fatalf("stolen envelope queued: %d", n)
	}

	ok := buildEnvelope(t, alice, bob, "from-alice")
	if err := wsjson.Write(ctx, aliceWS, map[string]any{"type": "send", "envelope": ok}); err != nil {
		t.Fatal(err)
	}
	queued := readSkipPresence(t, ctx, aliceWS)
	if queued.Type != "queued" {
		t.Fatalf("own send want queued got %+v", queued)
	}
	delivered := readSkipPresence(t, ctx, bobWS)
	if delivered.Type != "deliver" {
		t.Fatalf("bob want deliver got %+v", delivered)
	}
}

func TestGroupSendRejectsForeignSender(t *testing.T) {
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

	req = authReq(t, http.MethodPost, hs.URL+"/v1/groups", "/v1/groups", []byte(`{"name":"crew"}`), alice)
	resp, err = http.DefaultClient.Do(req)
	if err != nil {
		t.Fatal(err)
	}
	var g groupJSON
	_ = json.NewDecoder(resp.Body).Decode(&g)
	resp.Body.Close()
	body, _ := json.Marshal(map[string]string{"device_id": bob.id})
	req = authReq(t, http.MethodPost, hs.URL+"/v1/groups/"+g.GroupID+"/members", "/v1/groups/"+g.GroupID+"/members", body, alice)
	resp, err = http.DefaultClient.Do(req)
	if err != nil {
		t.Fatal(err)
	}
	resp.Body.Close()

	ctx := context.Background()
	aliceWS := dialWS(t, ctx, hs, alice)
	defer aliceWS.Close(websocket.StatusNormalClosure, "")
	drainHello(t, ctx, aliceWS)

	stolen := buildEnvelope(t, bob, alice, "group-spoof")
	if err := wsjson.Write(ctx, aliceWS, map[string]any{
		"type": "group_send", "group_id": g.GroupID, "envelopes": [][]byte{stolen},
	}); err != nil {
		t.Fatal(err)
	}
	got := readSkipPresence(t, ctx, aliceWS)
	if got.Type != "error" || got.Code != "auth" {
		t.Fatalf("group foreign sender want auth error got %+v", got)
	}
}
