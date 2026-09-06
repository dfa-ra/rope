package config

import "testing"

func TestHTTPDevFingerprintStable(t *testing.T) {
	fp := HTTPDevFingerprint()
	if len(fp) != 64 {
		t.Fatalf("len %d", len(fp))
	}
	if fp != HTTPDevFingerprint() {
		t.Fatal("not stable")
	}
}
