package httpapi

import "strings"

// validCallTo rejects CR/LF/NUL in the WSS call `to` field before trim.
// Trim used to turn "\n<device>" into a live hub id.
func validCallTo(raw string) (string, bool) {
	if strings.ContainsAny(raw, "\n\r\x00") {
		return "", false
	}
	id := strings.ToLower(strings.TrimSpace(raw))
	if id == "" {
		return "", false
	}
	if strings.ToLower(raw) != id {
		return "", false
	}
	return id, true
}
