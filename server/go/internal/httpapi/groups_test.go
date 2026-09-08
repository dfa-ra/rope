package httpapi

import (
	"encoding/json"
	"io"
	"net/http"
	"testing"
)

type groupJSON struct {
	GroupID string   `json:"group_id"`
	Name    string   `json:"name"`
	Epoch   uint32   `json:"epoch"`
	Members []string `json:"members"`
}

func TestGroupCreateAddRemoveEpoch(t *testing.T) {
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
	if created.Epoch != 1 || len(created.Members) != 1 || created.Members[0] != owner.id {
		t.Fatalf("create %+v", created)
	}

	body, _ := json.Marshal(map[string]string{"device_id": guest.id})
	req = authReq(t, http.MethodPost, hs.URL+"/v1/groups/"+created.GroupID+"/members", "/v1/groups/"+created.GroupID+"/members", body, owner)
	resp, err = http.DefaultClient.Do(req)
	if err != nil {
		t.Fatal(err)
	}
	if resp.StatusCode != 200 {
		b, _ := io.ReadAll(resp.Body)
		t.Fatalf("add %d %s", resp.StatusCode, b)
	}
	if err := json.NewDecoder(resp.Body).Decode(&created); err != nil {
		t.Fatal(err)
	}
	resp.Body.Close()
	if created.Epoch != 2 || len(created.Members) != 2 {
		t.Fatalf("after add %+v", created)
	}

	req = authReq(t, http.MethodPost, hs.URL+"/v1/groups/"+created.GroupID+"/remove", "/v1/groups/"+created.GroupID+"/remove", body, owner)
	resp, err = http.DefaultClient.Do(req)
	if err != nil {
		t.Fatal(err)
	}
	if resp.StatusCode != 200 {
		b, _ := io.ReadAll(resp.Body)
		t.Fatalf("remove %d %s", resp.StatusCode, b)
	}
	if err := json.NewDecoder(resp.Body).Decode(&created); err != nil {
		t.Fatal(err)
	}
	resp.Body.Close()
	if created.Epoch != 3 || len(created.Members) != 1 {
		t.Fatalf("after remove %+v", created)
	}

	req = authReq(t, http.MethodGet, hs.URL+"/v1/groups", "/v1/groups", nil, guest)
	resp, err = http.DefaultClient.Do(req)
	if err != nil {
		t.Fatal(err)
	}
	if resp.StatusCode != 200 {
		t.Fatal(resp.StatusCode)
	}
	var listed struct {
		Groups []groupJSON `json:"groups"`
	}
	if err := json.NewDecoder(resp.Body).Decode(&listed); err != nil {
		t.Fatal(err)
	}
	resp.Body.Close()
	if len(listed.Groups) != 0 {
		t.Fatalf("removed member still listed: %+v", listed.Groups)
	}
}

func TestGroupNonMemberCannotAdd(t *testing.T) {
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
	var created groupJSON
	_ = json.NewDecoder(resp.Body).Decode(&created)
	resp.Body.Close()

	body, _ := json.Marshal(map[string]string{"device_id": guest.id})
	req = authReq(t, http.MethodPost, hs.URL+"/v1/groups/"+created.GroupID+"/members", "/v1/groups/"+created.GroupID+"/members", body, guest)
	resp, err = http.DefaultClient.Do(req)
	if err != nil {
		t.Fatal(err)
	}
	io.Copy(io.Discard, resp.Body)
	resp.Body.Close()
	if resp.StatusCode != 403 {
		t.Fatalf("non-member add want 403 got %d", resp.StatusCode)
	}
}
