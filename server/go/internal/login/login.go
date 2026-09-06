package login

import (
	"strings"
	"unicode"
	"unicode/utf8"
)

func Normalize(raw string) string {
	return strings.TrimSpace(raw)
}

func Valid(raw string) bool {
	s := Normalize(raw)
	n := utf8.RuneCountInString(s)
	if n < 2 || n > 24 {
		return false
	}
	for _, r := range s {
		if unicode.IsLetter(r) || unicode.IsDigit(r) || r == '_' || r == '-' || r == '.' {
			continue
		}
		return false
	}
	return true
}
