package httpapi

import (
	"encoding/json"
	"net/http"
	"strings"

	"github.com/go-chi/chi/v5"
	"github.com/google/uuid"
)

func (s *Server) createGroup(w http.ResponseWriter, _ *http.Request, a authed, body []byte) {
	var req struct {
		Name string `json:"name"`
	}
	_ = json.Unmarshal(body, &req)
	name, ok := validGroupName(req.Name)
	if !ok {
		writeJSON(w, 400, map[string]string{"error": "bad group name"})
		return
	}
	id := uuid.NewString()
	if err := s.Store.InsertGroup(id, name, a.Device.ID, 1); err != nil {
		writeJSON(w, 500, map[string]string{"error": "db"})
		return
	}
	_ = s.Store.AddGroupMember(id, a.Device.ID)
	s.writeGroup(w, id)
}

func (s *Server) listGroups(w http.ResponseWriter, _ *http.Request, a authed, _ []byte) {
	groups, err := s.Store.ListGroupsForDevice(a.Device.ID)
	if err != nil {
		writeJSON(w, 500, map[string]string{"error": "db"})
		return
	}
	type gJSON struct {
		GroupID   string   `json:"group_id"`
		Name      string   `json:"name"`
		Epoch     uint32   `json:"epoch"`
		Members   []string `json:"members"`
		CreatedBy string   `json:"created_by"`
	}
	out := []gJSON{}
	for _, g := range groups {
		members, _ := s.Store.GroupMembers(g.ID)
		out = append(out, gJSON{g.ID, g.Name, g.Epoch, members, g.CreatedBy})
	}
	writeJSON(w, 200, map[string]any{"groups": out})
}

func (s *Server) canManageGroup(a authed, gid string) (bool, error) {
	ok, err := s.Store.IsGroupMember(gid, a.Device.ID)
	if err != nil || !ok {
		return false, err
	}
	if a.Member.Role == "owner" {
		return true, nil
	}
	g, err := s.Store.Group(gid)
	if err != nil {
		return false, err
	}
	return strings.EqualFold(g.CreatedBy, a.Device.ID), nil
}

func (s *Server) groupAdd(w http.ResponseWriter, r *http.Request, a authed, body []byte) {
	gid := chi.URLParam(r, "id")
	member, err := s.Store.IsGroupMember(gid, a.Device.ID)
	if err != nil || !member {
		writeJSON(w, 403, map[string]string{"error": "not a member"})
		return
	}
	ok, err := s.canManageGroup(a, gid)
	if err != nil || !ok {
		writeJSON(w, 403, map[string]string{"error": "organizer only"})
		return
	}
	var req struct {
		DeviceID string `json:"device_id"`
	}
	if err := json.Unmarshal(body, &req); err != nil || req.DeviceID == "" {
		writeJSON(w, 400, map[string]string{"error": "device_id"})
		return
	}
	if !s.liveGroupDevice(req.DeviceID) {
		writeJSON(w, 404, map[string]string{"error": "unknown device"})
		return
	}
	if err := s.Store.AddGroupMember(gid, req.DeviceID); err != nil {
		writeJSON(w, 500, map[string]string{"error": "db"})
		return
	}
	_, _ = s.Store.BumpGroupEpoch(gid)
	s.writeGroup(w, gid)
}

func (s *Server) groupRemove(w http.ResponseWriter, r *http.Request, a authed, body []byte) {
	gid := chi.URLParam(r, "id")
	member, err := s.Store.IsGroupMember(gid, a.Device.ID)
	if err != nil || !member {
		writeJSON(w, 403, map[string]string{"error": "not a member"})
		return
	}
	var req struct {
		DeviceID string `json:"device_id"`
	}
	if err := json.Unmarshal(body, &req); err != nil || req.DeviceID == "" {
		writeJSON(w, 400, map[string]string{"error": "device_id"})
		return
	}
	self := strings.EqualFold(req.DeviceID, a.Device.ID)
	if !self {
		ok, err := s.canManageGroup(a, gid)
		if err != nil || !ok {
			writeJSON(w, 403, map[string]string{"error": "organizer only"})
			return
		}
	}
	if err := s.Store.RemoveGroupMember(gid, req.DeviceID); err != nil {
		writeJSON(w, 500, map[string]string{"error": "db"})
		return
	}
	_, _ = s.Store.BumpGroupEpoch(gid)
	s.writeGroup(w, gid)
}

// liveGroupDevice is a directory-live device: not revoked, member not revoked.
// Same 404 as unknown so group-add cannot confirm a kicked id.
func (s *Server) liveGroupDevice(id string) bool {
	dev, err := s.Store.Device(id)
	if err != nil || dev.Revoked {
		return false
	}
	mem, err := s.Store.Member(dev.MemberID)
	return err == nil && !mem.Revoked
}

func (s *Server) writeGroup(w http.ResponseWriter, id string) {
	g, err := s.Store.Group(id)
	if err != nil {
		writeJSON(w, 404, map[string]string{"error": "not found"})
		return
	}
	members, _ := s.Store.GroupMembers(id)
	if members == nil {
		members = []string{}
	}
	writeJSON(w, 200, map[string]any{
		"group_id":   g.ID,
		"name":       g.Name,
		"epoch":      g.Epoch,
		"members":    members,
		"created_by": g.CreatedBy,
	})
}
