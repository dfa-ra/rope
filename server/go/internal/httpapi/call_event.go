package httpapi

import "strings"

// validCallEvent rejects CR/LF/NUL in the WSS call event before trim.
// handleCall used to forward the raw JSON string; a newline prefix skipped
// EqualFold("ring") rate-limit but still reached the peer. Spaces still trim.
// Not the Android CallSignal parseEvent matcher.
func validCallEvent(raw string) (string, bool) {
	if strings.ContainsAny(raw, "\n\r\x00") {
		return "", false
	}
	event := strings.TrimSpace(raw)
	if event == "" {
		return "", false
	}
	return event, true
}
