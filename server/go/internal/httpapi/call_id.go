package httpapi

import "strings"

// validCallID rejects CR/LF/NUL in the WSS call_id before trim.
// Trim used to turn "\n<id>" into a live pending/forwarded call key.
// Spaces still trim. Not the Android CallLink matcher.
func validCallID(raw string) (string, bool) {
	if strings.ContainsAny(raw, "\n\r\x00") {
		return "", false
	}
	id := strings.TrimSpace(raw)
	if id == "" {
		return "", false
	}
	return id, true
}
