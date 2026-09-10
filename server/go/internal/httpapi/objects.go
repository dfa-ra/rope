package httpapi

import (
	"crypto/sha256"
	"encoding/hex"
	"net/http"
	"os"
	"path/filepath"
	"time"

	"github.com/go-chi/chi/v5"
	"github.com/google/uuid"
)

func (s *Server) uploadObject(w http.ResponseWriter, r *http.Request, a authed, body []byte) {
	max := s.Cfg.MaxObjectBytes
	if max <= 0 {
		max = 25 * 1024 * 1024
	}
	used, _ := s.Store.ObjectBytesSum()
	quota := s.Cfg.ObjectQuotaBytes
	if quota <= 0 {
		quota = 512 * 1024 * 1024
	}
	if len(body) == 0 || len(body) > max {
		writeJSON(w, 413, map[string]string{"error": "object too large"})
		return
	}
	if used+int64(len(body)) > quota {
		writeJSON(w, 507, map[string]string{"error": "quota exceeded"})
		return
	}
	sum := sha256.Sum256(body)
	sha := hex.EncodeToString(sum[:])
	if want := r.Header.Get("X-Rope-SHA256"); want != "" && want != sha {
		writeJSON(w, 400, map[string]string{"error": "hash mismatch"})
		return
	}
	id := uuid.NewString()
	if err := os.MkdirAll(s.Cfg.ObjectsDir(), 0o750); err != nil {
		writeJSON(w, 500, map[string]string{"error": "store"})
		return
	}
	path := filepath.Join(s.Cfg.ObjectsDir(), id)
	if err := os.WriteFile(path, body, 0o640); err != nil {
		writeJSON(w, 500, map[string]string{"error": "store"})
		return
	}
	if err := s.Store.InsertObject(id, a.Device.ID, sha, int64(len(body)), s.Cfg.ObjectTTL()); err != nil {
		_ = os.Remove(path)
		writeJSON(w, 500, map[string]string{"error": "db"})
		return
	}
	writeJSON(w, 200, map[string]any{
		"object_id": id,
		"sha256":    sha,
		"size":      len(body),
		"expires_at": time.Now().Add(s.Cfg.ObjectTTL()).UTC().Format(time.RFC3339),
	})
}

func objectIDOK(id string) bool {
	_, err := uuid.Parse(id)
	return err == nil
}

func objectNotFound(w http.ResponseWriter) {
	writeJSON(w, 404, map[string]string{"error": "not found"})
}

func (s *Server) downloadObject(w http.ResponseWriter, r *http.Request, _ authed, _ []byte) {
	id := chi.URLParam(r, "id")
	if !objectIDOK(id) {
		objectNotFound(w)
		return
	}
	meta, err := s.Store.Object(id)
	if err != nil {
		objectNotFound(w)
		return
	}
	exp, err := time.Parse(time.RFC3339, meta.ExpiresAt)
	if err == nil && time.Now().After(exp) {
		s.gcObject(meta.ID)
		objectNotFound(w)
		return
	}
	raw, err := os.ReadFile(filepath.Join(s.Cfg.ObjectsDir(), id))
	if err != nil {
		objectNotFound(w)
		return
	}
	w.Header().Set("Content-Type", "application/octet-stream")
	w.Header().Set("X-Rope-SHA256", meta.SHA256)
	w.Header().Set("Cache-Control", "no-store")
	w.WriteHeader(200)
	_, _ = w.Write(raw)
}

func (s *Server) gcExpiredObjects() {
	items, err := s.Store.ExpiredObjects()
	if err != nil {
		return
	}
	for _, o := range items {
		s.gcObject(o.ID)
	}
}

func (s *Server) gcObject(id string) {
	_ = os.Remove(filepath.Join(s.Cfg.ObjectsDir(), id))
	_ = s.Store.DeleteObject(id)
}
