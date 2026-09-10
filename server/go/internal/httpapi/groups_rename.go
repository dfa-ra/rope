package httpapi

import (
	"encoding/json"
	"net/http"
	"strings"
	"unicode/utf8"

	"github.com/go-chi/chi/v5"
)

func validGroupName(raw string) (string, bool) {
	if strings.ContainsAny(raw, "\n\r\x00") {
		return "", false
	}
	name := strings.TrimSpace(raw)
	n := utf8.RuneCountInString(name)
	if n < 1 || n > 40 {
		return "", false
	}
	return name, true
}

func (s *Server) renameGroup(w http.ResponseWriter, r *http.Request, a authed, body []byte) {
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
		Name string `json:"name"`
	}
	if err := json.Unmarshal(body, &req); err != nil {
		writeJSON(w, 400, map[string]string{"error": "bad group name"})
		return
	}
	name, ok := validGroupName(req.Name)
	if !ok {
		writeJSON(w, 400, map[string]string{"error": "bad group name"})
		return
	}
	if err := s.Store.UpdateGroupName(gid, name); err != nil {
		writeJSON(w, 500, map[string]string{"error": "db"})
		return
	}
	s.writeGroup(w, gid)
}
