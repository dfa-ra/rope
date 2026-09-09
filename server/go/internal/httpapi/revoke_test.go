package httpapi

import (
	"encoding/json"
	"io"
	"net/http"
	"sync"
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

	again := authReq(t, http.MethodPost, hs.URL+"/v1/admin/revoke-device", "/v1/admin/revoke-device", okBody, owner)
	resp, err = http.DefaultClient.Do(again)
	if err != nil {
		t.Fatal(err)
	}
	b, _ = io.ReadAll(resp.Body)
	resp.Body.Close()
	if resp.StatusCode != 404 {
		t.Fatalf("already-revoked guest device wanted 404 got %d %s", resp.StatusCode, b)
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

func postRevokeDevice(t *testing.T, base, deviceID string, actor testDevice) int {
	t.Helper()
	body := []byte(`{"device_id":"` + deviceID + `"}`)
	req := authReq(t, http.MethodPost, base+"/v1/admin/revoke-device", "/v1/admin/revoke-device", body, actor)
	resp, err := http.DefaultClient.Do(req)
	if err != nil {
		t.Fatal(err)
	}
	b, _ := io.ReadAll(resp.Body)
	resp.Body.Close()
	if resp.StatusCode != 200 && resp.StatusCode != 409 && resp.StatusCode != 404 {
		t.Fatalf("revoke-device %s: %d %s", deviceID, resp.StatusCode, b)
	}
	return resp.StatusCode
}

func postRevokeMember(t *testing.T, base, memberID string, actor testDevice) int {
	t.Helper()
	body := []byte(`{"member_id":"` + memberID + `"}`)
	req := authReq(t, http.MethodPost, base+"/v1/admin/revoke-member", "/v1/admin/revoke-member", body, actor)
	resp, err := http.DefaultClient.Do(req)
	if err != nil {
		t.Fatal(err)
	}
	b, _ := io.ReadAll(resp.Body)
	resp.Body.Close()
	if resp.StatusCode != 200 && resp.StatusCode != 409 && resp.StatusCode != 404 && resp.StatusCode != 401 {
		t.Fatalf("revoke-member %s: %d %s", memberID, resp.StatusCode, b)
	}
	return resp.StatusCode
}

func TestConcurrentTwoOwnerRevokeDevice(t *testing.T) {
	s, hs, setup := testServer(t)
	owner := newDevice(t)
	bootstrap(t, hs, setup, owner, "owner")
	peer := newDevice(t)
	peerMember := uuid.NewString()
	if err := s.Store.InsertMember(peerMember, "coowner", "owner"); err != nil {
		t.Fatal(err)
	}
	attachDevice(t, s, peerMember, peer)

	// Widen the old check-then-act window: both handlers observe OwnerDeviceCount
	// before either UPDATE. RevokeDeviceGuarded does not call OwnerDeviceCount, so
	// this is a no-op on the atomic path and forces the pre-fix handler to overlap.
	var gate sync.WaitGroup
	gate.Add(2)
	db.AfterOwnerDeviceCount = func() {
		gate.Done()
		gate.Wait()
	}
	t.Cleanup(func() { db.AfterOwnerDeviceCount = nil })

	// Each owner revokes the other's device. Signing both requests as the
	// same device is not a two-actor race: if that device is revoked first,
	// the second HTTP call is 401 at authenticate, not 409.
	type pair struct {
		target string
		actor  testDevice
	}
	pairs := []pair{
		{peer.id, owner},
		{owner.id, peer},
	}
	reqs := make([]*http.Request, 0, 2)
	for _, p := range pairs {
		body := []byte(`{"device_id":"` + p.target + `"}`)
		reqs = append(reqs, authReq(t, http.MethodPost, hs.URL+"/v1/admin/revoke-device", "/v1/admin/revoke-device", body, p.actor))
	}

	type result struct {
		code int
		err  error
	}
	codes := make(chan result, 2)
	var start sync.WaitGroup
	start.Add(2)
	for _, req := range reqs {
		req := req
		go func() {
			start.Done()
			start.Wait()
			resp, err := http.DefaultClient.Do(req)
			if err != nil {
				codes <- result{err: err}
				return
			}
			_, _ = io.ReadAll(resp.Body)
			resp.Body.Close()
			codes <- result{code: resp.StatusCode}
		}()
	}
	got := []result{<-codes, <-codes}
	db.AfterOwnerDeviceCount = nil

	// Safety: never both 200. The loser is 409 when both passed
	// authenticate, or 401 if the winner revoked the other actor first.
	ok, loser := 0, 0
	for _, r := range got {
		if r.err != nil {
			t.Fatal(r.err)
		}
		switch r.code {
		case 200:
			ok++
		case 409, 401:
			loser++
		default:
			t.Fatalf("concurrent revoke-device statuses %v %v", got[0], got[1])
		}
	}
	if ok == 2 {
		t.Fatalf("both 200 — last-owner device invariant broken: %d and %d", got[0].code, got[1].code)
	}
	if ok != 1 || loser != 1 {
		t.Fatalf("concurrent two-owner revoke-device wanted one 200 and one 401/409, got %d and %d", got[0].code, got[1].code)
	}
	n, err := s.Store.OwnerDeviceCount()
	if err != nil || n != 1 {
		t.Fatalf("remaining owner devices %d %v (want 1)", n, err)
	}

	dirReq := authReq(t, http.MethodGet, hs.URL+"/v1/directory", "/v1/directory", nil, owner)
	resp, err := http.DefaultClient.Do(dirReq)
	if err != nil {
		t.Fatal(err)
	}
	resp.Body.Close()
	if resp.StatusCode != 200 && resp.StatusCode != 401 {
		t.Fatalf("surviving owner directory wanted 200 or 401 if this device lost, got %d", resp.StatusCode)
	}
	if resp.StatusCode == 401 {
		late := authReq(t, http.MethodGet, hs.URL+"/v1/directory", "/v1/directory", nil, peer)
		resp, err = http.DefaultClient.Do(late)
		if err != nil {
			t.Fatal(err)
		}
		resp.Body.Close()
		if resp.StatusCode != 200 {
			t.Fatalf("surviving co-owner directory wanted 200 got %d", resp.StatusCode)
		}
	}
}

func TestRevokeAlreadyRevokedSpareDevice404(t *testing.T) {
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

	if code := postRevokeDevice(t, hs.URL, spare.id, owner); code != 200 {
		t.Fatalf("spare revoke wanted 200 got %d", code)
	}
	if code := postRevokeDevice(t, hs.URL, spare.id, owner); code != 404 {
		t.Fatalf("already-revoked spare wanted 404 got %d", code)
	}
	if code := postRevokeDevice(t, hs.URL, owner.id, owner); code != 409 {
		t.Fatalf("last live owner device wanted 409 got %d", code)
	}
}

func TestRevokeAlreadyRevokedMember404(t *testing.T) {
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
		t.Fatal("guest member id missing")
	}

	if code := postRevokeMember(t, hs.URL, guestMember, owner); code != 200 {
		t.Fatalf("guest revoke wanted 200 got %d", code)
	}
	if code := postRevokeMember(t, hs.URL, guestMember, owner); code != 404 {
		t.Fatalf("already-revoked guest wanted 404 got %d", code)
	}
	if code := postRevokeMember(t, hs.URL, "missing-member", owner); code != 404 {
		t.Fatalf("unknown member wanted 404 got %d", code)
	}
}

func TestConcurrentTwoOwnerRevokeMember(t *testing.T) {
	s, hs, setup := testServer(t)
	owner := newDevice(t)
	bootstrap(t, hs, setup, owner, "owner")
	peer := newDevice(t)
	peerMember := uuid.NewString()
	if err := s.Store.InsertMember(peerMember, "coowner", "owner"); err != nil {
		t.Fatal(err)
	}
	attachDevice(t, s, peerMember, peer)

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

	// Widen the old check-then-act window: both handlers observe
	// OwnerDeviceCountExceptMember before either UPDATE. RevokeMemberGuarded
	// does not call OwnerDeviceCountExceptMember, so this is a no-op on the
	// atomic path and forces the pre-fix handler to overlap.
	var gate sync.WaitGroup
	gate.Add(2)
	db.AfterOwnerDeviceCount = func() {
		gate.Done()
		gate.Wait()
	}
	t.Cleanup(func() { db.AfterOwnerDeviceCount = nil })

	// Each owner revokes the other. Signing both requests as the same
	// device is not a two-actor race: if that device's member is revoked
	// first, the second HTTP call is 401 at authenticate, not 409.
	type pair struct {
		target string
		actor  testDevice
	}
	pairs := []pair{
		{peerMember, owner},
		{ownerMember, peer},
	}
	reqs := make([]*http.Request, 0, 2)
	for _, p := range pairs {
		body := []byte(`{"member_id":"` + p.target + `"}`)
		reqs = append(reqs, authReq(t, http.MethodPost, hs.URL+"/v1/admin/revoke-member", "/v1/admin/revoke-member", body, p.actor))
	}

	type result struct {
		code int
		err  error
	}
	codes := make(chan result, 2)
	var start sync.WaitGroup
	start.Add(2)
	for _, req := range reqs {
		req := req
		go func() {
			start.Done()
			start.Wait()
			resp, err := http.DefaultClient.Do(req)
			if err != nil {
				codes <- result{err: err}
				return
			}
			_, _ = io.ReadAll(resp.Body)
			resp.Body.Close()
			codes <- result{code: resp.StatusCode}
		}()
	}
	got := []result{<-codes, <-codes}
	db.AfterOwnerDeviceCount = nil

	// Safety: never both 200 (that bricks the instance). The loser is 409
	// when both passed authenticate, or 401 if the winner revoked the
	// other actor before that request authenticated.
	ok, loser := 0, 0
	for _, r := range got {
		if r.err != nil {
			t.Fatal(r.err)
		}
		switch r.code {
		case 200:
			ok++
		case 409, 401:
			loser++
		default:
			t.Fatalf("concurrent revoke-member statuses %v %v", got[0], got[1])
		}
	}
	if ok == 2 {
		t.Fatalf("both 200 — last-owner device invariant broken: %d and %d", got[0].code, got[1].code)
	}
	if ok != 1 || loser != 1 {
		t.Fatalf("concurrent two-owner revoke-member wanted one 200 and one 401/409, got %d and %d", got[0].code, got[1].code)
	}
	n, err := s.Store.OwnerDeviceCount()
	if err != nil || n != 1 {
		t.Fatalf("remaining owner devices %d %v (want 1)", n, err)
	}

	dirReq = authReq(t, http.MethodGet, hs.URL+"/v1/directory", "/v1/directory", nil, owner)
	resp, err := http.DefaultClient.Do(dirReq)
	if err != nil {
		t.Fatal(err)
	}
	resp.Body.Close()
	if resp.StatusCode != 200 && resp.StatusCode != 401 {
		t.Fatalf("surviving owner directory wanted 200 or 401 if this member lost, got %d", resp.StatusCode)
	}
	if resp.StatusCode == 401 {
		late := authReq(t, http.MethodGet, hs.URL+"/v1/directory", "/v1/directory", nil, peer)
		resp, err = http.DefaultClient.Do(late)
		if err != nil {
			t.Fatal(err)
		}
		resp.Body.Close()
		if resp.StatusCode != 200 {
			t.Fatalf("surviving co-owner directory wanted 200 got %d", resp.StatusCode)
		}
	}
}

func TestRevokeMemberBlockedWhenCoOwnerDevicesAlreadyGone(t *testing.T) {
	s, hs, setup := testServer(t)
	owner := newDevice(t)
	bootstrap(t, hs, setup, owner, "owner")
	peer := newDevice(t)
	peerMember := uuid.NewString()
	if err := s.Store.InsertMember(peerMember, "coowner", "owner"); err != nil {
		t.Fatal(err)
	}
	attachDevice(t, s, peerMember, peer)

	if code := postRevokeDevice(t, hs.URL, peer.id, owner); code != 200 {
		t.Fatalf("strip co-owner device wanted 200 got %d", code)
	}
	n, err := s.Store.OwnerCount()
	if err != nil || n != 2 {
		t.Fatalf("OwnerCount leftover members %d %v", n, err)
	}

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

	body := []byte(`{"member_id":"` + ownerMember + `"}`)
	req := authReq(t, http.MethodPost, hs.URL+"/v1/admin/revoke-member", "/v1/admin/revoke-member", body, owner)
	resp, err := http.DefaultClient.Do(req)
	if err != nil {
		t.Fatal(err)
	}
	b, _ := io.ReadAll(resp.Body)
	resp.Body.Close()
	if resp.StatusCode != 409 {
		t.Fatalf("revoke-member of last live owner wanted 409 got %d %s", resp.StatusCode, b)
	}

	leftover := []byte(`{"member_id":"` + peerMember + `"}`)
	okReq := authReq(t, http.MethodPost, hs.URL+"/v1/admin/revoke-member", "/v1/admin/revoke-member", leftover, owner)
	resp, err = http.DefaultClient.Do(okReq)
	if err != nil {
		t.Fatal(err)
	}
	b, _ = io.ReadAll(resp.Body)
	resp.Body.Close()
	if resp.StatusCode != 200 {
		t.Fatalf("revoke leftover device-less co-owner wanted 200 got %d %s", resp.StatusCode, b)
	}

	dirReq = authReq(t, http.MethodGet, hs.URL+"/v1/directory", "/v1/directory", nil, owner)
	resp, err = http.DefaultClient.Do(dirReq)
	if err != nil {
		t.Fatal(err)
	}
	resp.Body.Close()
	if resp.StatusCode != 200 {
		t.Fatalf("surviving owner directory wanted 200 got %d", resp.StatusCode)
	}
}
