package httpapi

import (
	"encoding/json"
	"io"
	"net/http"
	"strings"
	"testing"
)

func TestValidGroupDescRejectsControlBeforeTrim(t *testing.T) {
	got, ok := validGroupDesc("  crew notes  ")
	if !ok || got != "crew notes" {
		t.Fatalf("trim got %q %v", got, ok)
	}
	if _, ok := validGroupDesc("crew\nnotes"); ok {
		t.Fatal("newline must fail before trim")
	}
	if _, ok := validGroupDesc("\rcrew"); ok {
		t.Fatal("cr")
	}
	if _, ok := validGroupDesc("crew\x00"); ok {
		t.Fatal("nul")
	}
	empty, ok := validGroupDesc("")
	if !ok || empty != "" {
		t.Fatalf("empty got %q %v", empty, ok)
	}
	if _, ok := validGroupDesc(strings.Repeat("я", 121)); ok {
		t.Fatal("121 runes")
	}
	got, ok = validGroupDesc(strings.Repeat("я", 120))
	if !ok || utf8len(got) != 120 {
		t.Fatalf("120 runes got %d %v", utf8len(got), ok)
	}
}

func utf8len(s string) int { return len([]rune(s)) }

func TestPatchGroupDescOrganizerOnly(t *testing.T) {
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
	var created struct {
		GroupID     string `json:"group_id"`
		Description string `json:"description"`
	}
	if err := json.NewDecoder(resp.Body).Decode(&created); err != nil {
		t.Fatal(err)
	}
	resp.Body.Close()
	if created.GroupID == "" || created.Description != "" {
		t.Fatalf("create %+v", created)
	}

	body, _ := json.Marshal(map[string]string{"device_id": guest.id})
	addPath := "/v1/groups/" + created.GroupID + "/members"
	req = authReq(t, http.MethodPost, hs.URL+addPath, addPath, body, owner)
	resp, err = http.DefaultClient.Do(req)
	if err != nil {
		t.Fatal(err)
	}
	io.Copy(io.Discard, resp.Body)
	resp.Body.Close()
	if resp.StatusCode != 200 {
		t.Fatalf("add %d", resp.StatusCode)
	}

	patchPath := "/v1/groups/" + created.GroupID + "/description"
	desc, _ := json.Marshal(map[string]string{"description": "deck notes"})
	req = authReq(t, http.MethodPatch, hs.URL+patchPath, patchPath, desc, guest)
	resp, err = http.DefaultClient.Do(req)
	if err != nil {
		t.Fatal(err)
	}
	io.Copy(io.Discard, resp.Body)
	resp.Body.Close()
	if resp.StatusCode != 403 {
		t.Fatalf("guest patch want 403 got %d", resp.StatusCode)
	}

	crlf, _ := json.Marshal(map[string]string{"description": "deck\nnotes"})
	req = authReq(t, http.MethodPatch, hs.URL+patchPath, patchPath, crlf, owner)
	resp, err = http.DefaultClient.Do(req)
	if err != nil {
		t.Fatal(err)
	}
	io.Copy(io.Discard, resp.Body)
	resp.Body.Close()
	if resp.StatusCode != 400 {
		t.Fatalf("crlf want 400 got %d", resp.StatusCode)
	}

	req = authReq(t, http.MethodPatch, hs.URL+patchPath, patchPath, desc, owner)
	resp, err = http.DefaultClient.Do(req)
	if err != nil {
		t.Fatal(err)
	}
	if resp.StatusCode != 200 {
		b, _ := io.ReadAll(resp.Body)
		t.Fatalf("owner patch %d %s", resp.StatusCode, b)
	}
	var got struct {
		Description string `json:"description"`
	}
	if err := json.NewDecoder(resp.Body).Decode(&got); err != nil {
		t.Fatal(err)
	}
	resp.Body.Close()
	if got.Description != "deck notes" {
		t.Fatalf("patched %+v", got)
	}

	req = authReq(t, http.MethodGet, hs.URL+"/v1/groups", "/v1/groups", nil, owner)
	resp, err = http.DefaultClient.Do(req)
	if err != nil {
		t.Fatal(err)
	}
	var listed struct {
		Groups []struct {
			Description string `json:"description"`
		} `json:"groups"`
	}
	if err := json.NewDecoder(resp.Body).Decode(&listed); err != nil {
		t.Fatal(err)
	}
	resp.Body.Close()
	if len(listed.Groups) != 1 || listed.Groups[0].Description != "deck notes" {
		t.Fatalf("list %+v", listed.Groups)
	}

	clear, _ := json.Marshal(map[string]string{"description": "  "})
	req = authReq(t, http.MethodPatch, hs.URL+patchPath, patchPath, clear, owner)
	resp, err = http.DefaultClient.Do(req)
	if err != nil {
		t.Fatal(err)
	}
	if resp.StatusCode != 200 {
		b, _ := io.ReadAll(resp.Body)
		t.Fatalf("clear %d %s", resp.StatusCode, b)
	}
	if err := json.NewDecoder(resp.Body).Decode(&got); err != nil {
		t.Fatal(err)
	}
	resp.Body.Close()
	if got.Description != "" {
		t.Fatalf("cleared %+v", got)
	}

	req = authReq(t, http.MethodGet, hs.URL+"/v1/groups", "/v1/groups", nil, owner)
	resp, err = http.DefaultClient.Do(req)
	if err != nil {
		t.Fatal(err)
	}
	if err := json.NewDecoder(resp.Body).Decode(&listed); err != nil {
		t.Fatal(err)
	}
	resp.Body.Close()
	if len(listed.Groups) != 1 || listed.Groups[0].Description != "" {
		t.Fatalf("list cleared %+v", listed.Groups)
	}
}

func TestGroupDescPathIDRejectsControl(t *testing.T) {
	if _, ok := groupDescPathID("not-a-uuid"); ok {
		t.Fatal("uuid")
	}
	if _, ok := groupDescPathID(" 00000000-0000-0000-0000-000000000001"); ok {
		t.Fatal("leading space")
	}
	if _, ok := groupDescPathID("00000000-0000-0000-0000-000000000001\n"); ok {
		t.Fatal("newline")
	}
}
