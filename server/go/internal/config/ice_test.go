package config

import (
	"crypto/hmac"
	"crypto/sha1"
	"encoding/base64"
	"os"
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
		"turn:203.0.113.9:3478?transport=tcp",
		"turn:203.0.113.9:3478?transport=udp",
		"turn:203.0.113.9:3478",
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
	if turn.Hostname != "" || ice[0].Hostname != "" {
		t.Fatalf("raw IP public_host has no SNI hostname: %+v", ice)
	}
	if cfg.PublicIPv4() != "203.0.113.9" {
		t.Fatalf("public_ip %s", cfg.PublicIPv4())
	}
}

func TestIceHostnameAndPublicIPForClientSNI(t *testing.T) {
	cfg := Default()
	cfg.PublicHost = "vps.example"
	cfg.PublicIP = "203.0.113.9"
	cfg.TurnSecret = "shared-hmac"
	cfg.TurnsPort = 443
	if cfg.IceHostname() != "vps.example" {
		t.Fatalf("sni %s", cfg.IceHostname())
	}
	if cfg.PublicIPv4() != "203.0.113.9" {
		t.Fatalf("ip %s", cfg.PublicIPv4())
	}
	ice := cfg.IceServers(time.Unix(1_700_000_000, 0))
	if ice[0].Hostname != "vps.example" || ice[1].Hostname != "vps.example" {
		t.Fatalf("hostname on both ICE entries: %+v", ice)
	}
	joined := strings.Join(ice[1].URLs, " ")
	if !strings.Contains(joined, "turns:vps.example:443?transport=tcp") {
		t.Fatalf("urls still use DNS host: %s", joined)
	}

	cfg.PublicHost = "198.51.100.20"
	cfg.TLSHostname = "rope.example"
	cfg.PublicIP = ""
	if cfg.IceHostname() != "rope.example" {
		t.Fatalf("tls_hostname %s", cfg.IceHostname())
	}
	ice = cfg.IceServers(time.Now())
	if ice[1].Hostname != "rope.example" {
		t.Fatalf("SNI when URL is raw IP: %+v", ice[1])
	}
	if !strings.Contains(ice[1].URLs[0], "198.51.100.20") {
		t.Fatalf("urls stay on IP: %v", ice[1].URLs)
	}
	if cfg.PublicIPv4() != "198.51.100.20" {
		t.Fatalf("ip from public_host %s", cfg.PublicIPv4())
	}
}

func TestPublicIPv4FromTurnStatusFile(t *testing.T) {
	dir := t.TempDir()
	cfg := Default()
	cfg.PublicHost = "vps.example"
	cfg.TurnSecret = "s"
	cfg.TLSCert = dir + "/tls/cert.pem"
	if err := os.WriteFile(dir+"/turn-status.json", []byte(`{"ok":true,"external_ip":"203.0.113.9/10.0.0.4"}`), 0o644); err != nil {
		t.Fatal(err)
	}
	if got := cfg.PublicIPv4(); got != "203.0.113.9" {
		t.Fatalf("got %s", got)
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

func TestIceServersPrivateOnlyHostReturnsNil(t *testing.T) {
	for _, host := range []string{"10.0.0.4", "192.168.1.1", "172.16.9.1", "100.64.1.2", "127.0.0.1", "169.254.1.1"} {
		cfg := Default()
		cfg.PublicHost = host
		cfg.TurnSecret = "shared-hmac"
		cfg.TurnsPort = 443
		if ice := cfg.IceServers(time.Now()); ice != nil {
			t.Fatalf("%s: must not advertise private ICE %+v", host, ice)
		}
	}
	cfg := Default()
	cfg.PublicHost = "10.0.0.4"
	cfg.PublicIP = "192.168.0.9"
	cfg.TurnSecret = "s"
	if ice := cfg.IceServers(time.Now()); ice != nil {
		t.Fatalf("private public_ip still advertised: %+v", ice)
	}
}

func TestIceServersPublicIPSetsHostname(t *testing.T) {
	cfg := Default()
	cfg.PublicHost = "vps.example"
	cfg.PublicIP = "203.0.113.9"
	cfg.TurnSecret = "shared-hmac"
	cfg.TurnsPort = 443
	ice := cfg.IceServers(time.Unix(1_700_000_000, 0))
	if len(ice) != 2 {
		t.Fatalf("len %d", len(ice))
	}
	if ice[0].Hostname != "vps.example" || ice[1].Hostname != "vps.example" {
		t.Fatalf("hostname field required when urls include IP literal: %+v", ice)
	}
	joined := strings.Join(ice[1].URLs, " ")
	if !strings.Contains(joined, "203.0.113.9") {
		t.Fatalf("need IP url: %s", joined)
	}
}

func TestIceServersDNSHostSkipsPrivatePublicIP(t *testing.T) {
	cfg := Default()
	cfg.PublicHost = "vps.example"
	cfg.PublicIP = "10.0.0.4"
	cfg.TurnSecret = "s"
	cfg.TurnsPort = 443
	ice := cfg.IceServers(time.Now())
	if len(ice) != 2 {
		t.Fatalf("DNS host is public enough: %+v", ice)
	}
	joined := strings.Join(ice[1].URLs, " ")
	if strings.Contains(joined, "10.0.0.4") {
		t.Fatalf("must not duplicate RFC1918: %s", joined)
	}
	if !strings.Contains(joined, "turns:vps.example:443") {
		t.Fatalf("need dns turns: %s", joined)
	}
	if cfg.PublicIPv4() != "" {
		t.Fatalf("private public_ip leaked: %s", cfg.PublicIPv4())
	}
}

func TestPublicIPv4SkipsPrivate(t *testing.T) {
	cfg := Default()
	cfg.PublicHost = "10.0.0.4"
	if cfg.PublicIPv4() != "" {
		t.Fatalf("private public_host: %s", cfg.PublicIPv4())
	}
	cfg.PublicIP = "100.64.1.2"
	if cfg.PublicIPv4() != "" {
		t.Fatalf("CGNAT public_ip: %s", cfg.PublicIPv4())
	}
	cfg.PublicIP = "203.0.113.9"
	if cfg.PublicIPv4() != "203.0.113.9" {
		t.Fatalf("got %s", cfg.PublicIPv4())
	}
}

func TestIceTTLSecondsMatchesDuration(t *testing.T) {
	if Default().IceTTLSeconds() != 7*24*3600 {
		t.Fatalf("default ttl seconds %d", Default().IceTTLSeconds())
	}
	cfg := Default()
	cfg.TurnTTLSeconds = 3600
	if cfg.IceTTLSeconds() != 3600 {
		t.Fatalf("override %d", cfg.IceTTLSeconds())
	}
}

func TestIceServersUsesPublicIPWhenHostIsPrivate(t *testing.T) {
	cfg := Default()
	cfg.PublicHost = "10.0.0.4"
	cfg.PublicIP = "203.0.113.9"
	cfg.TurnSecret = "shared-hmac"
	cfg.TurnsPort = 443
	ice := cfg.IceServers(time.Unix(1_700_000_000, 0))
	joined := strings.Join(ice[1].URLs, " ")
	if strings.Contains(joined, "10.0.0.4") {
		t.Fatalf("must not advertise RFC1918 TURN URL: %s", joined)
	}
	if !strings.Contains(joined, "turns:203.0.113.9:443?transport=tcp") {
		t.Fatalf("need public IP TURNS: %s", joined)
	}
	if !strings.Contains(joined, "turn:203.0.113.9:3478?transport=tcp") {
		t.Fatalf("need public IP TCP TURN: %s", joined)
	}
}

func TestIceServersDuplicatesIPWhenDNSHost(t *testing.T) {
	cfg := Default()
	cfg.PublicHost = "vps.example"
	cfg.PublicIP = "203.0.113.9"
	cfg.TurnSecret = "s"
	cfg.TurnsPort = 443
	ice := cfg.IceServers(time.Now())
	joined := strings.Join(ice[1].URLs, " ")
	if !strings.Contains(joined, "turns:vps.example:443") {
		t.Fatalf("dns: %s", joined)
	}
	if !strings.Contains(joined, "turns:203.0.113.9:443") {
		t.Fatalf("ip duplicate: %s", joined)
	}
	if !strings.Contains(joined, "turn:203.0.113.9:3478?transport=tcp") {
		t.Fatalf("ip tcp: %s", joined)
	}
}

func TestIsPrivateIPv4(t *testing.T) {
	for _, ip := range []string{"10.1.2.3", "192.168.0.1", "172.16.9.1", "127.0.0.1", "100.64.1.2", "169.254.1.1"} {
		if !IsPrivateIPv4(ip) {
			t.Fatalf("%s should be private", ip)
		}
	}
	if IsPrivateIPv4("203.0.113.9") || IsPrivateIPv4("vps.example") {
		t.Fatal("public / dns")
	}
}

func TestProbeTurnPrivateHostDoesNotAdvertise(t *testing.T) {
	origD := DialTCP
	origA := RunTurnAllocate
	t.Cleanup(func() {
		DialTCP = origD
		RunTurnAllocate = origA
		ResetTurnAllocCache()
	})
	ResetTurnAllocCache()
	cfg := Default()
	cfg.PublicHost = "10.0.0.4"
	cfg.TurnSecret = "s"
	cfg.DataDir = t.TempDir()
	DialTCP = func(string, time.Duration) error { return errProbeDown }
	rep := cfg.ProbeTurn(10 * time.Millisecond)
	if !rep.Configured {
		t.Fatal("secret+host is configured")
	}
	if rep.Running || rep.AllocateOK {
		t.Fatalf("must not pretend TURN is up: %+v", rep)
	}
	if len(rep.Advertised) != 0 {
		t.Fatalf("must not advertise private URLs: %v", rep.Advertised)
	}
	if rep.Error == "" {
		t.Fatal("expected listen error")
	}
}

func TestProbeTurnAllocateFailureSetsError(t *testing.T) {
	origD := DialTCP
	origA := RunTurnAllocate
	t.Cleanup(func() {
		DialTCP = origD
		RunTurnAllocate = origA
		ResetTurnAllocCache()
	})
	ResetTurnAllocCache()
	cfg := Default()
	cfg.PublicHost = "203.0.113.9"
	cfg.TurnSecret = "s"
	cfg.DataDir = t.TempDir()
	DialTCP = func(string, time.Duration) error { return nil }
	RunTurnAllocate = func(Config, time.Duration) AllocResult {
		return AllocResult{Error: "HMAC 401 — turn_secret не совпадает с static-auth-secret"}
	}
	rep := cfg.ProbeTurn(10 * time.Millisecond)
	if !rep.Running {
		t.Fatal("listen")
	}
	if rep.AllocateOK {
		t.Fatal("allocate must fail")
	}
	if !strings.Contains(rep.Error, "HMAC 401") {
		t.Fatalf("error %s", rep.Error)
	}
}

func TestProbeTurnDistinguishes443ForeignVsListening(t *testing.T) {
	orig := DialTCP
	origA := RunTurnAllocate
	t.Cleanup(func() {
		DialTCP = orig
		RunTurnAllocate = origA
		ResetTurnAllocCache()
	})
	ResetTurnAllocCache()
	RunTurnAllocate = func(Config, time.Duration) AllocResult {
		return AllocResult{OK: true, RelayedIP: "203.0.113.9", Proto: "udp"}
	}
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
	if !rep.AllocateOK {
		t.Fatalf("allocate %+v", rep)
	}
	if rep.Error != "" {
		t.Fatalf("error %s", rep.Error)
	}
}

var errProbeDown = errString("down")

type errString string

func (e errString) Error() string { return string(e) }
