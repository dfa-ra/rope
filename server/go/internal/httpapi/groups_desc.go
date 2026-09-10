package httpapi

import (
	"encoding/json"
	"net/http"
	"strings"
	"unicode/utf8"

	"github.com/go-chi/chi/v5"
	"github.com/google/uuid"
)

const maxGroupDescRunes = 120

func validGroupDesc(raw string) (string, bool) {
	if strings.ContainsAny(raw, "\n\r\x00") {
		return "", false
	}
	desc := strings.TrimSpace(raw)
	if utf8.RuneCountInString(desc) > maxGroupDescRunes {
		return "", false
	}
	return desc, true
}

func groupDescPathID(raw string) (string, bool) {
	if strings.ContainsAny(raw, "\n\r\x00") {
		return "", false
	}
	id := strings.TrimSpace(raw)
	if id == "" || id != raw {
		return "", false
	}
	if _, err := uuid.Parse(id); err != nil {
		return "", false
	}
	return id, true
}

func (s *Server) patchGroupDesc(w http.ResponseWriter, r *http.Request, a authed, body []byte) {
	gid, ok := groupDescPathID(chi.URLParam(r, "id"))
	if !ok {
		writeJSON(w, 400, map[string]string{"error": "bad group id"})
		return
	}
	member, err := s.Store.IsGroupMember(gid, a.Device.ID)
	if err != nil || !member {
		writeJSON(w, 403, map[string]string{"error": "not a member"})
		return
	}
	ok, err = s.canManageGroup(a, gid)
	if err != nil || !ok {
		writeJSON(w, 403, map[string]string{"error": "organizer only"})
		return
	}
	var req struct {
		Description string `json:"description"`
	}
	if err := json.Unmarshal(body, &req); err != nil {
		writeJSON(w, 400, map[string]string{"error": "bad group description"})
		return
	}
	desc, ok := validGroupDesc(req.Description)
	if !ok {
		writeJSON(w, 400, map[string]string{"error": "bad group description"})
		return
	}
	if err := s.Store.UpdateGroupDescription(gid, desc); err != nil {
		writeJSON(w, 500, map[string]string{"error": "db"})
		return
	}
	s.writeGroup(w, gid)
}
