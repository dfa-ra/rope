package config

import (
	"crypto/hmac"
	"crypto/sha1"
	"encoding/base64"
	"strings"
	"testing"
	"time"
)

func TestTurnCredentialMatchesCoturnREST(t *testing.T) {
	const secret = "unit-test-secret"
	const user = "2000000000:rope"
	mac := hmac.New(sha1.New, []byte(secret))
	_, _ = mac.Write([]byte(user))
	want := base64.StdEncoding.EncodeToString(mac.Sum(nil))
	if got := TurnCredential(secret, user); got != want {
		t.Fatalf("cred %s want %s", got, want)
	}
}

func TestIceServersEmptyWithoutSecretOrHost(t *testing.T) {
	cfg := Default()
	if cfg.IceEnabled() {
		t.Fatal("default must not advertise ICE")
	}
	if ice := cfg.IceServers(time.Now()); ice != nil {
		t.Fatalf("got %+v", ice)
	}
	cfg.TurnSecret = "x"
	if cfg.IceEnabled() {
		t.Fatal("secret alone is not enough")
	}
	cfg.TurnSecret = ""
	cfg.PublicHost = "1.2.3.4"
	if cfg.IceEnabled() {
		t.Fatal("host alone is not enough")
	}
}

func TestIceServersShapeAndHMAC(t *testing.T) {
	cfg := Default()
	cfg.PublicHost = "203.0.113.9"
	cfg.TurnSecret = "shared-hmac"
	cfg.TurnPort = 3478
	cfg.TurnsPort = 443
	now := time.Unix(1_700_000_000, 0)
	ice := cfg.IceServers(now)
	if len(ice) != 2 {
		t.Fatalf("len %d", len(ice))
	}
	if len(ice[0].URLs) != 1 || ice[0].URLs[0] != "stun:203.0.113.9:3478" {
		t.Fatalf("stun %+v", ice[0])
	}
	if ice[0].Username != "" || ice[0].Credential != "" {
		t.Fatal("stun must not carry credentials")
	}
	turn := ice[1]
	wantURLs := []string{
		"turns:203.0.113.9:443?transport=tcp",
		"turns:203.0.113.9:443",
		"turn:203.0.113.9:3478?transport=udp",
		"turn:203.0.113.9:3478",
		"turn:203.0.113.9:3478?transport=tcp",
	}
	if strings.Join(turn.URLs, ",") != strings.Join(wantURLs, ",") {
		t.Fatalf("urls %+v", turn.URLs)
	}
	wantUser := TurnUsername(now, 24*time.Hour)
	if turn.Username != wantUser {
		t.Fatalf("user %s want %s", turn.Username, wantUser)
	}
	if turn.Credential != TurnCredential("shared-hmac", wantUser) {
		t.Fatal("credential mismatch")
	}
	if !strings.Contains(turn.Username, ":rope") {
		t.Fatal(turn.Username)
	}
}

func TestIceHostBracketsIPv6(t *testing.T) {
	if IceHost("2001:db8::1") != "[2001:db8::1]" {
		t.Fatal(IceHost("2001:db8::1"))
	}
	if IceHost("[2001:db8::1]") != "[2001:db8::1]" {
		t.Fatal("already bracketed")
	}
	if IceHost("vps.example") != "vps.example" {
		t.Fatal(IceHost("vps.example"))
	}
}
