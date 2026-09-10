package httpapi

import (
	"encoding/json"
	"errors"
	"net/http"
	"strings"

	"github.com/dfa-ra/rope/server/go/internal/db"
	"github.com/dfa-ra/rope/server/go/internal/login"
)

func validDisplayName(raw string) (string, bool) {
	if strings.ContainsAny(raw, "\n\r\x00") {
		return "", false
	}
	if !login.Valid(raw) {
		return "", false
	}
	return login.Normalize(raw), true
}

func (s *Server) renameMe(w http.ResponseWriter, _ *http.Request, a authed, body []byte) {
	var req struct {
		DisplayName string `json:"display_name"`
	}
	if err := json.Unmarshal(body, &req); err != nil {
		writeJSON(w, 400, map[string]string{"error": "bad login"})
		return
	}
	name, ok := validDisplayName(req.DisplayName)
	if !ok {
		writeJSON(w, 400, map[string]string{"error": "login must be 2-24 letters, digits, _ . -"})
		return
	}
	if err := s.Store.UpdateMemberDisplayName(a.Member.ID, name); err != nil {
		if errors.Is(err, db.ErrLoginTaken) {
			writeJSON(w, 409, map[string]string{"error": "login taken"})
			return
		}
		if errors.Is(err, db.ErrNotFound) {
			writeJSON(w, 404, map[string]string{"error": "not found"})
			return
		}
		writeJSON(w, 500, map[string]string{"error": "db"})
		return
	}
	writeJSON(w, 200, map[string]any{
		"member_id":    a.Member.ID,
		"display_name": name,
		"role":         a.Member.Role,
	})
}
