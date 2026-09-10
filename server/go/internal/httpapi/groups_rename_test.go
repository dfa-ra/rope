package httpapi

import (
	"encoding/json"
	"io"
	"net/http"
	"strings"
	"testing"
	"unicode/utf8"
)

func TestGroupRenameRejectsCrlfBeforeTrim(t *testing.T) {
	if _, ok := validGroupName("crew\n"); ok {
		t.Fatal("newline must fail before trim")
	}
	if _, ok := validGroupName("crew\r"); ok {
		t.Fatal("cr")
	}
	if n, ok := validGroupName(" crew "); !ok || n != "crew" {
		t.Fatalf("plain trim %q %v", n, ok)
	}
}

func TestValidGroupName(t *testing.T) {
	name, ok := validGroupName(" crew ")
	if !ok || name != "crew" {
		t.Fatalf("trim got %q %v", name, ok)
	}
	if _, ok := validGroupName("crew\n"); ok {
		t.Fatal("newline")
	}
	if _, ok := validGroupName("crew\x00"); ok {
		t.Fatal("nul")
	}
	if _, ok := validGroupName(""); ok {
		t.Fatal("empty")
	}
	if _, ok := validGroupName(strings.Repeat("я", 41)); ok {
		t.Fatal("41 runes")
	}
	name, ok = validGroupName(strings.Repeat("🙂", 40))
	if !ok || utf8.RuneCountInString(name) != 40 {
		t.Fatalf("40 emoji %q %v", name, ok)
	}
}

func TestGroupRenameOrganizerAndRejectControl(t *testing.T) {
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

	body, _ := json.Marshal(map[string]string{"device_id": guest.id})
	req = authReq(t, http.MethodPost, hs.URL+"/v1/groups/"+created.GroupID+"/members", "/v1/groups/"+created.GroupID+"/members", body, owner)
	resp, err = http.DefaultClient.Do(req)
	if err != nil {
		t.Fatal(err)
	}
	io.Copy(io.Discard, resp.Body)
	resp.Body.Close()
	if resp.StatusCode != 200 {
		t.Fatalf("add %d", resp.StatusCode)
	}

	rename, _ := json.Marshal(map[string]string{"name": "deck"})
	req = authReq(t, http.MethodPatch, hs.URL+"/v1/groups/"+created.GroupID, "/v1/groups/"+created.GroupID, rename, guest)
	resp, err = http.DefaultClient.Do(req)
	if err != nil {
		t.Fatal(err)
	}
	io.Copy(io.Discard, resp.Body)
	resp.Body.Close()
	if resp.StatusCode != 403 {
		t.Fatalf("guest rename want 403 got %d", resp.StatusCode)
	}

	req = authReq(t, http.MethodPatch, hs.URL+"/v1/groups/"+created.GroupID, "/v1/groups/"+created.GroupID, rename, owner)
	resp, err = http.DefaultClient.Do(req)
	if err != nil {
		t.Fatal(err)
	}
	if resp.StatusCode != 200 {
		b, _ := io.ReadAll(resp.Body)
		t.Fatalf("owner rename %d %s", resp.StatusCode, b)
	}
	if err := json.NewDecoder(resp.Body).Decode(&created); err != nil {
		t.Fatal(err)
	}
	resp.Body.Close()
	if created.Name != "deck" {
		t.Fatalf("renamed %+v", created)
	}

	crlf, _ := json.Marshal(map[string]string{"name": "deck\nadmin"})
	req = authReq(t, http.MethodPatch, hs.URL+"/v1/groups/"+created.GroupID, "/v1/groups/"+created.GroupID, crlf, owner)
	resp, err = http.DefaultClient.Do(req)
	if err != nil {
		t.Fatal(err)
	}
	io.Copy(io.Discard, resp.Body)
	resp.Body.Close()
	if resp.StatusCode != 400 {
		t.Fatalf("crlf rename want 400 got %d", resp.StatusCode)
	}
}

func TestGroupCreateRejectsControlAndLength(t *testing.T) {
	_, hs, setup := testServer(t)
	owner := newDevice(t)
	bootstrap(t, hs, setup, owner, "owner")

	req := authReq(t, http.MethodPost, hs.URL+"/v1/groups", "/v1/groups", []byte(`{"name":"crew\n"}`), owner)
	resp, err := http.DefaultClient.Do(req)
	if err != nil {
		t.Fatal(err)
	}
	io.Copy(io.Discard, resp.Body)
	resp.Body.Close()
	if resp.StatusCode != 400 {
		t.Fatalf("create crlf want 400 got %d", resp.StatusCode)
	}

	req = authReq(t, http.MethodPost, hs.URL+"/v1/groups", "/v1/groups", []byte(`{"name":"`+strings.Repeat("я", 41)+`"}`), owner)
	resp, err = http.DefaultClient.Do(req)
	if err != nil {
		t.Fatal(err)
	}
	io.Copy(io.Discard, resp.Body)
	resp.Body.Close()
	if resp.StatusCode != 400 {
		t.Fatalf("create 41 runes want 400 got %d", resp.StatusCode)
	}

	req = authReq(t, http.MethodPost, hs.URL+"/v1/groups", "/v1/groups", []byte(`{"name":"`+strings.Repeat("я", 40)+`"}`), owner)
	resp, err = http.DefaultClient.Do(req)
	if err != nil {
		t.Fatal(err)
	}
	if resp.StatusCode != 200 {
		b, _ := io.ReadAll(resp.Body)
		t.Fatalf("create 40 runes %d %s", resp.StatusCode, b)
	}
	resp.Body.Close()
}
