package httpapi

import (
	"context"
	"encoding/json"
	"net/http"
	"strings"
	"testing"
	"time"

	"github.com/coder/websocket"
	"github.com/coder/websocket/wsjson"
	"github.com/dfa-ra/rope/server/go/internal/ratelimit"
)

func TestGroupSendRejectsNonMember(t *testing.T) {
	_, hs, setup := testServer(t)
	alice := newDevice(t)
	bob := newDevice(t)
	carol := newDevice(t)
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
	req = authReq(t, http.MethodPost, hs.URL+"/v1/invites", "/v1/invites", []byte(`{"ttl_seconds":60}`), alice)
	resp, err = http.DefaultClient.Do(req)
	if err != nil {
		t.Fatal(err)
	}
	_ = json.NewDecoder(resp.Body).Decode(&inv)
	resp.Body.Close()
	bootstrap(t, hs, inv.Token, carol, "carol")

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
	bobWS := dialWS(t, ctx, hs, bob)
	defer bobWS.Close(websocket.StatusNormalClosure, "")
	carolWS := dialWS(t, ctx, hs, carol)
	defer carolWS.Close(websocket.StatusNormalClosure, "")
	drainHello(t, ctx, aliceWS)
	drainHello(t, ctx, bobWS)
	drainHello(t, ctx, carolWS)

	env := buildEnvelope(t, alice, bob, "group-hi")
	if err := wsjson.Write(ctx, aliceWS, map[string]any{
		"type": "group_send", "group_id": g.GroupID, "envelopes": [][]byte{env},
	}); err != nil {
		t.Fatal(err)
	}
	queued := readSkipPresence(t, ctx, aliceWS)
	if queued.Type != "queued" {
		t.Fatalf("member send want queued got %+v", queued)
	}
	got := readSkipPresence(t, ctx, bobWS)
	if got.Type != "deliver" {
		t.Fatalf("bob want deliver got %+v", got)
	}

	env2 := buildEnvelope(t, carol, bob, "intruder")
	if err := wsjson.Write(ctx, carolWS, map[string]any{
		"type": "group_send", "group_id": g.GroupID, "envelopes": [][]byte{env2},
	}); err != nil {
		t.Fatal(err)
	}
	rej := readSkipPresence(t, ctx, carolWS)
	if rej.Type != "error" || rej.Code != "auth" {
		t.Fatalf("non-member want auth error got %+v", rej)
	}
}

func TestCallRelayLiveAndOffline(t *testing.T) {
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
		"type": "call", "call_id": "c1", "to": bob.id, "event": "ring", "payload": "sdp",
	}); err != nil {
		t.Fatal(err)
	}
	got := readSkipPresence(t, ctx, bobWS)
	if got.Type != "call" || got.Event != "ring" || got.From != alice.id || got.CallID != "c1" {
		t.Fatalf("call relay %+v", got)
	}

	_ = bobWS.Close(websocket.StatusNormalClosure, "")
	time.Sleep(200 * time.Millisecond)
	if err := wsjson.Write(ctx, aliceWS, map[string]any{
		"type": "call", "call_id": "c2", "to": bob.id, "event": "ring",
	}); err != nil {
		t.Fatal(err)
	}
	off := readSkipPresence(t, ctx, aliceWS)
	if off.Type != "queued" || off.CallID != "c2" {
		t.Fatalf("offline call want queued got %+v", off)
	}
}

func TestCallRelayResolvesMemberIdAndObjectPayload(t *testing.T) {
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

	devs, err := s.Store.ListDevices()
	if err != nil {
		t.Fatal(err)
	}
	var bobMember string
	for _, d := range devs {
		if d.ID == bob.id {
			bobMember = d.MemberID
		}
	}
	if bobMember == "" {
		t.Fatal("bob member_id missing")
	}

	ctx := context.Background()
	aliceWS := dialWS(t, ctx, hs, alice)
	defer aliceWS.Close(websocket.StatusNormalClosure, "")
	bobWS := dialWS(t, ctx, hs, bob)
	defer bobWS.Close(websocket.StatusNormalClosure, "")
	drainHello(t, ctx, aliceWS)
	drainHello(t, ctx, bobWS)

	if err := wsjson.Write(ctx, aliceWS, map[string]any{
		"type": "call", "call_id": "c-mem", "to": bobMember, "event": "ring",
	}); err != nil {
		t.Fatal(err)
	}
	got := readSkipPresence(t, ctx, bobWS)
	if got.Type != "call" || got.Event != "ring" || got.From != alice.id || got.CallID != "c-mem" {
		t.Fatalf("member_id call relay %+v", got)
	}

	if err := wsjson.Write(ctx, aliceWS, map[string]any{
		"type": "call", "call_id": "c-obj", "to": bob.id, "event": "offer",
		"payload": map[string]any{"kind": "offer", "sdp": "v=0"},
	}); err != nil {
		t.Fatal(err)
	}
	obj := readSkipPresence(t, ctx, bobWS)
	if obj.Type != "call" || obj.Event != "offer" || !strings.Contains(obj.Payload, "v=0") {
		t.Fatalf("object payload call %+v", obj)
	}
}

func TestCallRelayAndAudioForward(t *testing.T) {
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
		"type": "call", "call_id": "c-rel", "to": bob.id, "event": "relay",
	}); err != nil {
		t.Fatal(err)
	}
	rel := readSkipPresence(t, ctx, bobWS)
	if rel.Type != "call" || rel.Event != "relay" || rel.From != alice.id || rel.CallID != "c-rel" {
		t.Fatalf("relay forward %+v", rel)
	}

	cipher := strings.Repeat("A", 64)
	if err := wsjson.Write(ctx, aliceWS, map[string]any{
		"type": "call", "call_id": "c-rel", "to": bob.id, "event": "audio", "payload": cipher,
	}); err != nil {
		t.Fatal(err)
	}
	aud := readSkipPresence(t, ctx, bobWS)
	if aud.Type != "call" || aud.Event != "audio" || aud.From != alice.id || aud.Payload != cipher {
		t.Fatalf("audio forward %+v", aud)
	}
}

func TestCallPayloadTooLarge(t *testing.T) {
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

	okPayload := strings.Repeat("x", maxCallPayloadBytes)
	if err := wsjson.Write(ctx, aliceWS, map[string]any{
		"type": "call", "call_id": "c-ok", "to": bob.id, "event": "audio", "payload": okPayload,
	}); err != nil {
		t.Fatal(err)
	}
	ok := readSkipPresence(t, ctx, bobWS)
	if ok.Type != "call" || ok.Event != "audio" || len(ok.Payload) != maxCallPayloadBytes {
		t.Fatalf("max payload should forward %+v", ok)
	}

	if err := wsjson.Write(ctx, aliceWS, map[string]any{
		"type": "call", "call_id": "c-big", "to": bob.id, "event": "audio",
		"payload": strings.Repeat("y", maxCallPayloadBytes+1),
	}); err != nil {
		t.Fatal(err)
	}
	got := readSkipPresence(t, ctx, aliceWS)
	if got.Type != "error" || got.Code != "too_large" {
		t.Fatalf("oversized want too_large got %+v", got)
	}
}

func TestCallAudioRateLimited(t *testing.T) {
	s, hs, setup := testServer(t)
	s.CallAudio = ratelimit.New(3, time.Minute)
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

	for i := 0; i < 3; i++ {
		if err := wsjson.Write(ctx, aliceWS, map[string]any{
			"type": "call", "call_id": "c-rl", "to": bob.id, "event": "audio", "payload": "AA==",
		}); err != nil {
			t.Fatal(err)
		}
		got := readSkipPresence(t, ctx, bobWS)
		if got.Type != "call" || got.Event != "audio" {
			t.Fatalf("audio %d want forward got %+v", i, got)
		}
	}
	if err := wsjson.Write(ctx, aliceWS, map[string]any{
		"type": "call", "call_id": "c-rl", "to": bob.id, "event": "AUDIO", "payload": "AA==",
	}); err != nil {
		t.Fatal(err)
	}
	lim := readSkipPresence(t, ctx, aliceWS)
	if lim.Type != "error" || lim.Code != "rate_limited" {
		t.Fatalf("want rate_limited got %+v", lim)
	}

	if err := wsjson.Write(ctx, aliceWS, map[string]any{
		"type": "call", "call_id": "c-rl", "to": bob.id, "event": "relay",
	}); err != nil {
		t.Fatal(err)
	}
	rel := readSkipPresence(t, ctx, bobWS)
	if rel.Type != "call" || rel.Event != "relay" {
		t.Fatalf("signaling must stay unthrottled %+v", rel)
	}
}

func TestCallPendingRingDeliveredOnReconnect(t *testing.T) {
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
	drainHello(t, ctx, aliceWS)
	drainHello(t, ctx, bobWS)

	_ = bobWS.Close(websocket.StatusNormalClosure, "")
	time.Sleep(200 * time.Millisecond)
	if err := wsjson.Write(ctx, aliceWS, map[string]any{
		"type": "call", "call_id": "c-pend", "to": bob.id, "event": "ring", "payload": "sdp",
	}); err != nil {
		t.Fatal(err)
	}
	queued := readSkipPresence(t, ctx, aliceWS)
	if queued.Type != "queued" || queued.CallID != "c-pend" {
		t.Fatalf("offline ring want queued got %+v", queued)
	}

	bobWS = dialWS(t, ctx, hs, bob)
	defer bobWS.Close(websocket.StatusNormalClosure, "")
	drainHello(t, ctx, bobWS)
	got := readSkipPresence(t, ctx, bobWS)
	if got.Type != "call" || got.Event != "ring" || got.From != alice.id || got.CallID != "c-pend" {
		t.Fatalf("reconnect want pending ring got %+v", got)
	}
	if got.Payload != "sdp" {
		t.Fatalf("pending ring payload %+v", got)
	}
}

func TestCallAudioPeerOffline(t *testing.T) {
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

	if err := wsjson.Write(ctx, aliceWS, map[string]any{
		"type": "call", "call_id": "c-off", "to": bob.id, "event": "audio", "payload": "AA==",
	}); err != nil {
		t.Fatal(err)
	}
	off := readSkipPresence(t, ctx, aliceWS)
	if off.Type != "error" || off.Code != "not_found" {
		t.Fatalf("offline audio want not_found got %+v", off)
	}
}
