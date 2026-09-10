package httpapi

import (
	"encoding/json"
	"io"
	"net/http"
	"testing"
)

func TestValidGroupIDRejectsControlBeforeTrim(t *testing.T) {
	id := "11111111-1111-1111-1111-111111111111"
	got, ok := validGroupID(id)
	if !ok || got != id {
		t.Fatalf("uuid got %q %v", got, ok)
	}
	if _, ok := validGroupID(id + "\n"); ok {
		t.Fatal("newline must fail before trim")
	}
	if _, ok := validGroupID("\r" + id); ok {
		t.Fatal("cr")
	}
	if _, ok := validGroupID(id + "\x00"); ok {
		t.Fatal("nul")
	}
	if _, ok := validGroupID(" " + id); ok {
		t.Fatal("leading space")
	}
	if _, ok := validGroupID("not-a-uuid"); ok {
		t.Fatal("junk")
	}
}

func TestDeleteGroupOrganizerOnly(t *testing.T) {
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
		GroupID string `json:"group_id"`
	}
	if err := json.NewDecoder(resp.Body).Decode(&created); err != nil {
		t.Fatal(err)
	}
	resp.Body.Close()
	if created.GroupID == "" {
		t.Fatal("missing group_id")
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

	delPath := "/v1/groups/" + created.GroupID
	req = authReq(t, http.MethodDelete, hs.URL+delPath, delPath, nil, guest)
	resp, err = http.DefaultClient.Do(req)
	if err != nil {
		t.Fatal(err)
	}
	io.Copy(io.Discard, resp.Body)
	resp.Body.Close()
	if resp.StatusCode != 403 {
		t.Fatalf("guest delete want 403 got %d", resp.StatusCode)
	}

	crlfPath := "/v1/groups/" + created.GroupID + "\n"
	req = authReq(t, http.MethodDelete, hs.URL+"/v1/groups/"+created.GroupID+"%0a", crlfPath, nil, owner)
	resp, err = http.DefaultClient.Do(req)
	if err != nil {
		t.Fatal(err)
	}
	io.Copy(io.Discard, resp.Body)
	resp.Body.Close()
	if resp.StatusCode != 400 {
		t.Fatalf("crlf want 400 got %d", resp.StatusCode)
	}

	req = authReq(t, http.MethodDelete, hs.URL+delPath, delPath, nil, owner)
	resp, err = http.DefaultClient.Do(req)
	if err != nil {
		t.Fatal(err)
	}
	if resp.StatusCode != 200 {
		b, _ := io.ReadAll(resp.Body)
		t.Fatalf("owner delete %d %s", resp.StatusCode, b)
	}
	var okBody struct {
		OK      bool   `json:"ok"`
		GroupID string `json:"group_id"`
	}
	if err := json.NewDecoder(resp.Body).Decode(&okBody); err != nil {
		t.Fatal(err)
	}
	resp.Body.Close()
	if !okBody.OK || okBody.GroupID != created.GroupID {
		t.Fatalf("delete body %+v", okBody)
	}

	req = authReq(t, http.MethodGet, hs.URL+"/v1/groups", "/v1/groups", nil, owner)
	resp, err = http.DefaultClient.Do(req)
	if err != nil {
		t.Fatal(err)
	}
	var listed struct {
		Groups []struct {
			GroupID string `json:"group_id"`
		} `json:"groups"`
	}
	if err := json.NewDecoder(resp.Body).Decode(&listed); err != nil {
		t.Fatal(err)
	}
	resp.Body.Close()
	if len(listed.Groups) != 0 {
		t.Fatalf("list after delete %+v", listed.Groups)
	}

	req = authReq(t, http.MethodDelete, hs.URL+delPath, delPath, nil, owner)
	resp, err = http.DefaultClient.Do(req)
	if err != nil {
		t.Fatal(err)
	}
	io.Copy(io.Discard, resp.Body)
	resp.Body.Close()
	if resp.StatusCode != 403 {
		t.Fatalf("repeat delete want 403 got %d", resp.StatusCode)
	}
}
