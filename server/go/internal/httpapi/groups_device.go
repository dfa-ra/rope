package httpapi

import "strings"

// validDeviceID rejects CR/LF/NUL in group member device_id before trim.
// Add/remove used to take the raw JSON string; a newline prefix must not
// become a live directory id after trim. Spaces still trim.
func validDeviceID(raw string) (string, bool) {
	if strings.ContainsAny(raw, "\n\r\x00") {
		return "", false
	}
	id := strings.TrimSpace(raw)
	if id == "" {
		return "", false
	}
	return id, true
}
