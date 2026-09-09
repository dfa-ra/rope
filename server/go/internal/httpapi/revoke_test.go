package httpapi

import (
	"encoding/json"
	"io"
	"net/http"
	"testing"
)

func TestOwnerRevokeMemberDropsFromDirectory(t *testing.T) {
	_, hs, setup := testServer(t)
	owner := newDevice(t)
	bootstrap(t, hs, setup, owner, "owner")

	body := []byte(`{"ttl_seconds":3600}`)
	req := authReq(t, http.MethodPost, hs.URL+"/v1/invites", "/v1/invites", body, owner)
	resp, err := http.DefaultClient.Do(req)
	if err != nil {
		t.Fatal(err)
	}
	var inv struct {
		Token string `json:"token"`
	}
	if err := json.NewDecoder(resp.Body).Decode(&inv); err != nil {
		t.Fatal(err)
	}
	resp.Body.Close()
	guest := newDevice(t)
	bootstrap(t, hs, inv.Token, guest, "guest")

	dirReq := authReq(t, http.MethodGet, hs.URL+"/v1/directory", "/v1/directory", nil, owner)
	dirResp, err := http.DefaultClient.Do(dirReq)
	if err != nil {
		t.Fatal(err)
	}
	var dir struct {
		Members []struct {
			MemberID string `json:"member_id"`
			Role     string `json:"role"`
		} `json:"members"`
		Devices []struct {
			DeviceID string `json:"device_id"`
			MemberID string `json:"member_id"`
		} `json:"devices"`
	}
	if err := json.NewDecoder(dirResp.Body).Decode(&dir); err != nil {
		t.Fatal(err)
	}
	dirResp.Body.Close()
	var guestMember string
	for _, d := range dir.Devices {
		if d.DeviceID == guest.id {
			guestMember = d.MemberID
		}
	}
	if guestMember == "" {
		t.Fatal("guest missing from directory")
	}

	forbid := []byte(`{"member_id":"` + guestMember + `"}`)
	bad := authReq(t, http.MethodPost, hs.URL+"/v1/admin/revoke-member", "/v1/admin/revoke-member", forbid, guest)
	resp, err = http.DefaultClient.Do(bad)
	if err != nil {
		t.Fatal(err)
	}
	b, _ := io.ReadAll(resp.Body)
	resp.Body.Close()
	if resp.StatusCode != 403 {
		t.Fatalf("guest revoke wanted 403 got %d %s", resp.StatusCode, b)
	}

	okBody := []byte(`{"member_id":"` + guestMember + `"}`)
	okReq := authReq(t, http.MethodPost, hs.URL+"/v1/admin/revoke-member", "/v1/admin/revoke-member", okBody, owner)
	resp, err = http.DefaultClient.Do(okReq)
	if err != nil {
		t.Fatal(err)
	}
	b, _ = io.ReadAll(resp.Body)
	resp.Body.Close()
	if resp.StatusCode != 200 {
		t.Fatalf("owner revoke wanted 200 got %d %s", resp.StatusCode, b)
	}

	dirReq = authReq(t, http.MethodGet, hs.URL+"/v1/directory", "/v1/directory", nil, owner)
	dirResp, err = http.DefaultClient.Do(dirReq)
	if err != nil {
		t.Fatal(err)
	}
	dir = struct {
		Members []struct {
			MemberID string `json:"member_id"`
			Role     string `json:"role"`
		} `json:"members"`
		Devices []struct {
			DeviceID string `json:"device_id"`
			MemberID string `json:"member_id"`
		} `json:"devices"`
	}{}
	if err := json.NewDecoder(dirResp.Body).Decode(&dir); err != nil {
		t.Fatal(err)
	}
	dirResp.Body.Close()
	for _, d := range dir.Devices {
		if d.DeviceID == guest.id {
			t.Fatal("revoked guest still in directory devices")
		}
	}

	late := authReq(t, http.MethodGet, hs.URL+"/v1/directory", "/v1/directory", nil, guest)
	resp, err = http.DefaultClient.Do(late)
	if err != nil {
		t.Fatal(err)
	}
	resp.Body.Close()
	if resp.StatusCode != 401 {
		t.Fatalf("revoked guest directory wanted 401 got %d", resp.StatusCode)
	}
}

func TestLastOwnerCannotRevokeSelf(t *testing.T) {
	_, hs, setup := testServer(t)
	owner := newDevice(t)
	bootstrap(t, hs, setup, owner, "owner")

	dirReq := authReq(t, http.MethodGet, hs.URL+"/v1/directory", "/v1/directory", nil, owner)
	dirResp, err := http.DefaultClient.Do(dirReq)
	if err != nil {
		t.Fatal(err)
	}
	var dir struct {
		Members []struct {
			MemberID string `json:"member_id"`
			Role     string `json:"role"`
		} `json:"members"`
		Devices []struct {
			DeviceID string `json:"device_id"`
			MemberID string `json:"member_id"`
		} `json:"devices"`
	}
	if err := json.NewDecoder(dirResp.Body).Decode(&dir); err != nil {
		t.Fatal(err)
	}
	dirResp.Body.Close()
	var ownerMember string
	for _, d := range dir.Devices {
		if d.DeviceID == owner.id {
			ownerMember = d.MemberID
		}
	}
	if ownerMember == "" {
		for _, m := range dir.Members {
			if m.Role == "owner" {
				ownerMember = m.MemberID
			}
		}
	}
	if ownerMember == "" {
		t.Fatal("owner member id missing")
	}
	body := []byte(`{"member_id":"` + ownerMember + `"}`)
	req := authReq(t, http.MethodPost, hs.URL+"/v1/admin/revoke-member", "/v1/admin/revoke-member", body, owner)
	resp, err := http.DefaultClient.Do(req)
	if err != nil {
		t.Fatal(err)
	}
	b, _ := io.ReadAll(resp.Body)
	resp.Body.Close()
	if resp.StatusCode != 409 {
		t.Fatalf("last owner wanted 409 got %d %s", resp.StatusCode, b)
	}
}
