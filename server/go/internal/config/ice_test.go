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
	wantUser := TurnUsername(now, cfg.IceTTL())
	if turn.Username != wantUser {
		t.Fatalf("user %s want %s", turn.Username, wantUser)
	}
	if turn.Credential != TurnCredential("shared-hmac", wantUser) {
		t.Fatal("credential mismatch")
	}
	if !strings.Contains(turn.Username, ":rope") {
		t.Fatal(turn.Username)
	}
	if !strings.HasSuffix(turn.Username, ":rope") {
		t.Fatalf("use-auth-secret user must be expiry:rope, got %s", turn.Username)
	}
}

func TestIceTTLDefaultCoversCachedInfo(t *testing.T) {
	if Default().IceTTL() < 48*time.Hour {
		t.Fatalf("HMAC ttl %s is too short for cached /v1/info", Default().IceTTL())
	}
	cfg := Default()
	cfg.TurnTTLSeconds = 3600
	if cfg.IceTTL() != time.Hour {
		t.Fatalf("override %s", cfg.IceTTL())
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

func TestTurnUsernameCoturnRESTFormat(t *testing.T) {
	now := time.Unix(1_700_000_000, 0)
	user := TurnUsername(now, time.Hour)
	if user != "1700003600:rope" {
		t.Fatalf("user %s", user)
	}
	if !strings.Contains(user, ":rope") {
		t.Fatal(user)
	}
}

func TestIceServersAdvertises5349When443Busy(t *testing.T) {
	cfg := Default()
	cfg.PublicHost = "203.0.113.9"
	cfg.TurnSecret = "shared-hmac"
	cfg.TurnsPort = 5349
	ice := cfg.IceServers(time.Unix(1_700_000_000, 0))
	joined := strings.Join(ice[1].URLs, " ")
	if strings.Contains(joined, ":443") {
		t.Fatalf("must not advertise 443 when TURNS is 5349: %s", joined)
	}
	if !strings.Contains(joined, "turns:203.0.113.9:5349?transport=tcp") {
		t.Fatalf("missing turns 5349: %s", joined)
	}
}

func TestIceServersOmitsTurnsWhenTLSFailed(t *testing.T) {
	cfg := Default()
	cfg.PublicHost = "203.0.113.9"
	cfg.TurnSecret = "shared-hmac"
	cfg.TurnsPort = 0
	ice := cfg.IceServers(time.Now())
	joined := strings.Join(ice[1].URLs, " ")
	if strings.Contains(joined, "turns:") {
		t.Fatalf("tls failed so no turns: %s", joined)
	}
	if !strings.Contains(joined, "turn:203.0.113.9:3478?transport=udp") {
		t.Fatalf("still need udp turn: %s", joined)
	}
}

func TestProbeTurnDistinguishes443ForeignVsListening(t *testing.T) {
	orig := DialTCP
	t.Cleanup(func() { DialTCP = orig })
	cfg := Default()
	cfg.PublicHost = "203.0.113.9"
	cfg.TurnSecret = "s"
	cfg.TurnsPort = 443
	cfg.DataDir = t.TempDir()

	DialTCP = func(addr string, _ time.Duration) error {
		if strings.HasSuffix(addr, ":443") {
			return nil
		}
		return errProbeDown
	}
	rep := cfg.ProbeTurn(10 * time.Millisecond)
	if rep.Running {
		t.Fatal("3478 down")
	}
	if rep.TurnsListening {
		t.Fatal("443 without 3478 is not TURNS")
	}
	if !strings.Contains(rep.Error, "443") {
		t.Fatalf("error %s", rep.Error)
	}

	DialTCP = func(addr string, _ time.Duration) error {
		if strings.HasSuffix(addr, ":3478") || strings.HasSuffix(addr, ":5349") {
			return nil
		}
		return errProbeDown
	}
	cfg.TurnsPort = 5349
	rep = cfg.ProbeTurn(10 * time.Millisecond)
	if !rep.Running || !rep.TurnsListening {
		t.Fatalf("%+v", rep)
	}
	if rep.Error != "" {
		t.Fatalf("error %s", rep.Error)
	}
}

var errProbeDown = errString("down")

type errString string

func (e errString) Error() string { return string(e) }
