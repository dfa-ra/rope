package login

import "testing"

func TestValid(t *testing.T) {
	ok := []string{"ann", "Владик", "user_1", "a.b", "x-y"}
	for _, s := range ok {
		if !Valid(s) {
			t.Fatalf("expected valid: %q", s)
		}
	}
	bad := []string{"", "a", "has space", "bad!", stringsOf(25), "ann\n", "ann\r", "ann\x00"}
	for _, s := range bad {
		if Valid(s) {
			t.Fatalf("expected invalid: %q", s)
		}
	}
}

func stringsOf(n int) string {
	b := make([]byte, n)
	for i := range b {
		b[i] = 'a'
	}
	return string(b)
}
