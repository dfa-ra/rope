package httpapi

import (
	"errors"
	"net/http"
	"strings"

	"github.com/dfa-ra/rope/server/go/internal/db"
	"github.com/go-chi/chi/v5"
	"github.com/google/uuid"
)

func validGroupID(raw string) (string, bool) {
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

func (s *Server) deleteGroup(w http.ResponseWriter, r *http.Request, a authed, _ []byte) {
	gid, ok := validGroupID(chi.URLParam(r, "id"))
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
	if err := s.Store.DeleteGroup(gid); err != nil {
		if errors.Is(err, db.ErrNotFound) {
			writeJSON(w, 404, map[string]string{"error": "not found"})
			return
		}
		writeJSON(w, 500, map[string]string{"error": "db"})
		return
	}
	writeJSON(w, 200, map[string]any{"ok": true, "group_id": gid})
}
