package httpapi

import (
	"encoding/json"
	"io"
	"net/http"
	"strings"
	"testing"
)

func TestValidDisplayNameRejectsControlBeforeTrim(t *testing.T) {
	if _, ok := validDisplayName("ada\n"); ok {
		t.Fatal("newline must fail before trim")
	}
	if _, ok := validDisplayName("\rada"); ok {
		t.Fatal("cr")
	}
	if _, ok := validDisplayName("ada\x00"); ok {
		t.Fatal("nul")
	}
	name, ok := validDisplayName(" ada ")
	if !ok || name != "ada" {
		t.Fatalf("trim got %q %v", name, ok)
	}
	if _, ok := validDisplayName("a"); ok {
		t.Fatal("too short")
	}
	if _, ok := validDisplayName(strings.Repeat("я", 25)); ok {
		t.Fatal("25 runes")
	}
}

func TestPatchMeRenamesAndRejectsTakenOrControl(t *testing.T) {
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

	body, _ := json.Marshal(map[string]string{"display_name": "deck"})
	req = authReq(t, http.MethodPatch, hs.URL+"/v1/me", "/v1/me", body, owner)
	resp, err = http.DefaultClient.Do(req)
	if err != nil {
		t.Fatal(err)
	}
	if resp.StatusCode != 200 {
		b, _ := io.ReadAll(resp.Body)
		t.Fatalf("rename %d %s", resp.StatusCode, b)
	}
	var got struct {
		DisplayName string `json:"display_name"`
	}
	if err := json.NewDecoder(resp.Body).Decode(&got); err != nil {
		t.Fatal(err)
	}
	resp.Body.Close()
	if got.DisplayName != "deck" {
		t.Fatalf("renamed %+v", got)
	}

	same, _ := json.Marshal(map[string]string{"display_name": " deck "})
	req = authReq(t, http.MethodPatch, hs.URL+"/v1/me", "/v1/me", same, owner)
	resp, err = http.DefaultClient.Do(req)
	if err != nil {
		t.Fatal(err)
	}
	io.Copy(io.Discard, resp.Body)
	resp.Body.Close()
	if resp.StatusCode != 200 {
		t.Fatalf("idempotent want 200 got %d", resp.StatusCode)
	}

	taken, _ := json.Marshal(map[string]string{"display_name": "guest"})
	req = authReq(t, http.MethodPatch, hs.URL+"/v1/me", "/v1/me", taken, owner)
	resp, err = http.DefaultClient.Do(req)
	if err != nil {
		t.Fatal(err)
	}
	io.Copy(io.Discard, resp.Body)
	resp.Body.Close()
	if resp.StatusCode != 409 {
		t.Fatalf("taken want 409 got %d", resp.StatusCode)
	}

	crlf, _ := json.Marshal(map[string]string{"display_name": "deck\nadmin"})
	req = authReq(t, http.MethodPatch, hs.URL+"/v1/me", "/v1/me", crlf, owner)
	resp, err = http.DefaultClient.Do(req)
	if err != nil {
		t.Fatal(err)
	}
	io.Copy(io.Discard, resp.Body)
	resp.Body.Close()
	if resp.StatusCode != 400 {
		t.Fatalf("crlf want 400 got %d", resp.StatusCode)
	}

	dirReq := authReq(t, http.MethodGet, hs.URL+"/v1/directory", "/v1/directory", nil, owner)
	dirResp, err := http.DefaultClient.Do(dirReq)
	if err != nil {
		t.Fatal(err)
	}
	var dir struct {
		Members []struct {
			MemberID    string `json:"member_id"`
			DisplayName string `json:"display_name"`
		} `json:"members"`
	}
	if err := json.NewDecoder(dirResp.Body).Decode(&dir); err != nil {
		t.Fatal(err)
	}
	dirResp.Body.Close()
	guestID := ""
	for _, m := range dir.Members {
		if m.DisplayName == "guest" {
			guestID = m.MemberID
		}
	}
	if guestID == "" {
		t.Fatal("missing guest member")
	}
	rev, _ := json.Marshal(map[string]string{"member_id": guestID})
	req = authReq(t, http.MethodPost, hs.URL+"/v1/admin/revoke-member", "/v1/admin/revoke-member", rev, owner)
	resp, err = http.DefaultClient.Do(req)
	if err != nil {
		t.Fatal(err)
	}
	io.Copy(io.Discard, resp.Body)
	resp.Body.Close()
	if resp.StatusCode != 200 {
		t.Fatalf("revoke %d", resp.StatusCode)
	}
	reuse, _ := json.Marshal(map[string]string{"display_name": "guest"})
	req = authReq(t, http.MethodPatch, hs.URL+"/v1/me", "/v1/me", reuse, owner)
	resp, err = http.DefaultClient.Do(req)
	if err != nil {
		t.Fatal(err)
	}
	if resp.StatusCode != 200 {
		b, _ := io.ReadAll(resp.Body)
		t.Fatalf("reuse revoked name want 200 got %d %s", resp.StatusCode, b)
	}
	resp.Body.Close()
}
