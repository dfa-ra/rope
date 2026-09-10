package httpapi

import (
	"encoding/json"
	"io"
	"net/http"
	"testing"
)

func TestValidDeviceIDRejectsControlBeforeTrim(t *testing.T) {
	got, ok := validDeviceID("deadbeef")
	if !ok || got != "deadbeef" {
		t.Fatalf("plain got %q %v", got, ok)
	}
	got, ok = validDeviceID("  deadbeef  ")
	if !ok || got != "deadbeef" {
		t.Fatalf("spaces still trim got %q %v", got, ok)
	}
	if _, ok := validDeviceID("deadbeef\n"); ok {
		t.Fatal("newline must fail before trim")
	}
	if _, ok := validDeviceID("\ndeadbeef"); ok {
		t.Fatal("leading newline")
	}
	if _, ok := validDeviceID("deadbeef\r"); ok {
		t.Fatal("cr")
	}
	if _, ok := validDeviceID("deadbeef\x00"); ok {
		t.Fatal("nul")
	}
	if _, ok := validDeviceID(""); ok {
		t.Fatal("empty")
	}
	if _, ok := validDeviceID("   "); ok {
		t.Fatal("blank")
	}
}

func TestGroupAddRemoveRejectsDeviceIDControl(t *testing.T) {
	_, hs, setup := testServer(t)
	owner := newDevice(t)
	guest := newDevice(t)
	bootstrap(t, hs, setup, owner, "owner")
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

	req = authReq(t, http.MethodPost, hs.URL+"/v1/groups", "/v1/groups", []byte(`{"name":"crew"}`), owner)
	resp, err = http.DefaultClient.Do(req)
	if err != nil {
		t.Fatal(err)
	}
	if resp.StatusCode != 200 {
		b, _ := io.ReadAll(resp.Body)
		t.Fatalf("create %d %s", resp.StatusCode, b)
	}
	var created groupJSON
	if err := json.NewDecoder(resp.Body).Decode(&created); err != nil {
		t.Fatal(err)
	}
	resp.Body.Close()

	crlf, _ := json.Marshal(map[string]string{"device_id": "\n" + guest.id})
	req = authReq(t, http.MethodPost, hs.URL+"/v1/groups/"+created.GroupID+"/members", "/v1/groups/"+created.GroupID+"/members", crlf, owner)
	resp, err = http.DefaultClient.Do(req)
	if err != nil {
		t.Fatal(err)
	}
	io.Copy(io.Discard, resp.Body)
	resp.Body.Close()
	if resp.StatusCode != 400 {
		t.Fatalf("crlf add want 400 got %d", resp.StatusCode)
	}

	padded, _ := json.Marshal(map[string]string{"device_id": " " + guest.id + " "})
	req = authReq(t, http.MethodPost, hs.URL+"/v1/groups/"+created.GroupID+"/members", "/v1/groups/"+created.GroupID+"/members", padded, owner)
	resp, err = http.DefaultClient.Do(req)
	if err != nil {
		t.Fatal(err)
	}
	if resp.StatusCode != 200 {
		b, _ := io.ReadAll(resp.Body)
		t.Fatalf("padded add %d %s", resp.StatusCode, b)
	}
	if err := json.NewDecoder(resp.Body).Decode(&created); err != nil {
		t.Fatal(err)
	}
	resp.Body.Close()
	if created.Epoch != 2 || len(created.Members) != 2 {
		t.Fatalf("after padded add %+v", created)
	}

	crlfLeave, _ := json.Marshal(map[string]string{"device_id": guest.id + "\n"})
	req = authReq(t, http.MethodPost, hs.URL+"/v1/groups/"+created.GroupID+"/remove", "/v1/groups/"+created.GroupID+"/remove", crlfLeave, guest)
	resp, err = http.DefaultClient.Do(req)
	if err != nil {
		t.Fatal(err)
	}
	io.Copy(io.Discard, resp.Body)
	resp.Body.Close()
	if resp.StatusCode != 400 {
		t.Fatalf("crlf leave want 400 got %d", resp.StatusCode)
	}

	leave, _ := json.Marshal(map[string]string{"device_id": " " + guest.id + " "})
	req = authReq(t, http.MethodPost, hs.URL+"/v1/groups/"+created.GroupID+"/remove", "/v1/groups/"+created.GroupID+"/remove", leave, guest)
	resp, err = http.DefaultClient.Do(req)
	if err != nil {
		t.Fatal(err)
	}
	if resp.StatusCode != 200 {
		b, _ := io.ReadAll(resp.Body)
		t.Fatalf("padded leave %d %s", resp.StatusCode, b)
	}
	if err := json.NewDecoder(resp.Body).Decode(&created); err != nil {
		t.Fatal(err)
	}
	resp.Body.Close()
	if created.Epoch != 3 || len(created.Members) != 1 {
		t.Fatalf("after padded leave %+v", created)
	}
}
