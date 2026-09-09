package httpapi

import (
	"encoding/json"
	"io"
	"net/http"
	"testing"

	"github.com/dfa-ra/rope/server/go/internal/db"
	"github.com/google/uuid"
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

func TestLastOwnerCannotRevokeOwnDevice(t *testing.T) {
	_, hs, setup := testServer(t)
	owner := newDevice(t)
	bootstrap(t, hs, setup, owner, "owner")

	body := []byte(`{"device_id":"` + owner.id + `"}`)
	req := authReq(t, http.MethodPost, hs.URL+"/v1/admin/revoke-device", "/v1/admin/revoke-device", body, owner)
	resp, err := http.DefaultClient.Do(req)
	if err != nil {
		t.Fatal(err)
	}
	b, _ := io.ReadAll(resp.Body)
	resp.Body.Close()
	if resp.StatusCode != 409 {
		t.Fatalf("last owner revoke-device wanted 409 got %d %s", resp.StatusCode, b)
	}

	dirReq := authReq(t, http.MethodGet, hs.URL+"/v1/directory", "/v1/directory", nil, owner)
	resp, err = http.DefaultClient.Do(dirReq)
	if err != nil {
		t.Fatal(err)
	}
	resp.Body.Close()
	if resp.StatusCode != 200 {
		t.Fatalf("last owner still wanted 200 directory got %d", resp.StatusCode)
	}
}

func TestOwnerCanRevokeGuestDevice(t *testing.T) {
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

	forbid := []byte(`{"device_id":"` + owner.id + `"}`)
	bad := authReq(t, http.MethodPost, hs.URL+"/v1/admin/revoke-device", "/v1/admin/revoke-device", forbid, guest)
	resp, err = http.DefaultClient.Do(bad)
	if err != nil {
		t.Fatal(err)
	}
	b, _ := io.ReadAll(resp.Body)
	resp.Body.Close()
	if resp.StatusCode != 403 {
		t.Fatalf("guest revoke-device wanted 403 got %d %s", resp.StatusCode, b)
	}

	okBody := []byte(`{"device_id":"` + guest.id + `"}`)
	okReq := authReq(t, http.MethodPost, hs.URL+"/v1/admin/revoke-device", "/v1/admin/revoke-device", okBody, owner)
	resp, err = http.DefaultClient.Do(okReq)
	if err != nil {
		t.Fatal(err)
	}
	b, _ = io.ReadAll(resp.Body)
	resp.Body.Close()
	if resp.StatusCode != 200 {
		t.Fatalf("owner revoke guest device wanted 200 got %d %s", resp.StatusCode, b)
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

	unknown := []byte(`{"device_id":"aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa"}`)
	miss := authReq(t, http.MethodPost, hs.URL+"/v1/admin/revoke-device", "/v1/admin/revoke-device", unknown, owner)
	resp, err = http.DefaultClient.Do(miss)
	if err != nil {
		t.Fatal(err)
	}
	b, _ = io.ReadAll(resp.Body)
	resp.Body.Close()
	if resp.StatusCode != 404 {
		t.Fatalf("unknown device wanted 404 got %d %s", resp.StatusCode, b)
	}
}

func attachDevice(t *testing.T, s *Server, memberID string, d testDevice) {
	t.Helper()
	if err := s.Store.InsertDevice(db.Device{
		ID:             d.id,
		MemberID:       memberID,
		PublicIdentity: d.blob,
		SignPublic:     d.pub,
	}); err != nil {
		t.Fatal(err)
	}
}

func TestLastOwnerCanRevokeSpareDevice(t *testing.T) {
	s, hs, setup := testServer(t)
	owner := newDevice(t)
	bootstrap(t, hs, setup, owner, "owner")

	dirReq := authReq(t, http.MethodGet, hs.URL+"/v1/directory", "/v1/directory", nil, owner)
	dirResp, err := http.DefaultClient.Do(dirReq)
	if err != nil {
		t.Fatal(err)
	}
	var dir struct {
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
		t.Fatal("owner member id missing")
	}

	spare := newDevice(t)
	attachDevice(t, s, ownerMember, spare)

	okBody := []byte(`{"device_id":"` + spare.id + `"}`)
	okReq := authReq(t, http.MethodPost, hs.URL+"/v1/admin/revoke-device", "/v1/admin/revoke-device", okBody, owner)
	resp, err := http.DefaultClient.Do(okReq)
	if err != nil {
		t.Fatal(err)
	}
	b, _ := io.ReadAll(resp.Body)
	resp.Body.Close()
	if resp.StatusCode != 200 {
		t.Fatalf("spare owner device wanted 200 got %d %s", resp.StatusCode, b)
	}

	last := []byte(`{"device_id":"` + owner.id + `"}`)
	req := authReq(t, http.MethodPost, hs.URL+"/v1/admin/revoke-device", "/v1/admin/revoke-device", last, owner)
	resp, err = http.DefaultClient.Do(req)
	if err != nil {
		t.Fatal(err)
	}
	b, _ = io.ReadAll(resp.Body)
	resp.Body.Close()
	if resp.StatusCode != 409 {
		t.Fatalf("last owner device wanted 409 got %d %s", resp.StatusCode, b)
	}
}

func TestCoOwnerRevokeDeviceKeysOffDevicesNotMembers(t *testing.T) {
	s, hs, setup := testServer(t)
	owner := newDevice(t)
	bootstrap(t, hs, setup, owner, "owner")

	peer := newDevice(t)
	peerMember := uuid.NewString()
	if err := s.Store.InsertMember(peerMember, "coowner", "owner"); err != nil {
		t.Fatal(err)
	}
	attachDevice(t, s, peerMember, peer)

	n, err := s.Store.OwnerCount()
	if err != nil || n != 2 {
		t.Fatalf("owner members %d %v", n, err)
	}
	devices, err := s.Store.OwnerDeviceCount()
	if err != nil || devices != 2 {
		t.Fatalf("owner devices %d %v", devices, err)
	}

	okBody := []byte(`{"device_id":"` + peer.id + `"}`)
	okReq := authReq(t, http.MethodPost, hs.URL+"/v1/admin/revoke-device", "/v1/admin/revoke-device", okBody, owner)
	resp, err := http.DefaultClient.Do(okReq)
	if err != nil {
		t.Fatal(err)
	}
	b, _ := io.ReadAll(resp.Body)
	resp.Body.Close()
	if resp.StatusCode != 200 {
		t.Fatalf("co-owner device wanted 200 got %d %s", resp.StatusCode, b)
	}

	n, err = s.Store.OwnerCount()
	if err != nil || n != 2 {
		t.Fatalf("OwnerCount still 2 after device revoke, got %d %v", n, err)
	}
	devices, err = s.Store.OwnerDeviceCount()
	if err != nil || devices != 1 {
		t.Fatalf("owner devices after revoke %d %v", devices, err)
	}

	last := []byte(`{"device_id":"` + owner.id + `"}`)
	req := authReq(t, http.MethodPost, hs.URL+"/v1/admin/revoke-device", "/v1/admin/revoke-device", last, owner)
	resp, err = http.DefaultClient.Do(req)
	if err != nil {
		t.Fatal(err)
	}
	b, _ = io.ReadAll(resp.Body)
	resp.Body.Close()
	if resp.StatusCode != 409 {
		t.Fatalf("last remaining owner device wanted 409 got %d %s", resp.StatusCode, b)
	}

	dirReq := authReq(t, http.MethodGet, hs.URL+"/v1/directory", "/v1/directory", nil, owner)
	resp, err = http.DefaultClient.Do(dirReq)
	if err != nil {
		t.Fatal(err)
	}
	resp.Body.Close()
	if resp.StatusCode != 200 {
		t.Fatalf("surviving owner directory wanted 200 got %d", resp.StatusCode)
	}
}
