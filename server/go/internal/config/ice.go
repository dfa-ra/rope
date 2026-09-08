package config

import (
	"crypto/hmac"
	"crypto/sha1"
	"encoding/base64"
	"encoding/json"
	"fmt"
	"net"
	"os"
	"path/filepath"
	"strings"
	"time"
)

// IceServer is the RTCIceServer JSON object advertised on GET /v1/info.
type IceServer struct {
	URLs       []string `json:"urls"`
	Username   string   `json:"username,omitempty"`
	Credential string   `json:"credential,omitempty"`
	// Hostname is the DNS/SNI name for self-signed TURNS when urls use a raw IP.
	Hostname string `json:"hostname,omitempty"`
}

func (c Config) EffectiveTurnPort() int {
	if c.TurnPort > 0 {
		return c.TurnPort
	}
	return 3478
}

// EffectiveTurnsPort is the advertised TURNS TCP port.
// 0 means do not advertise turns: (TLS listen failed; UDP/TCP 3478 still used).
func (c Config) EffectiveTurnsPort() int {
	if c.TurnsPort > 0 {
		return c.TurnsPort
	}
	return 0
}

func (c Config) IceTTL() time.Duration {
	if c.TurnTTLSeconds > 0 {
		return time.Duration(c.TurnTTLSeconds) * time.Second
	}
	// Clients cache GET /v1/info HMAC credentials. Keep 7d so a day-long
	// cache cannot expire mid-call. Do not shorten without client cache tests;
	// advertise ice_ttl_seconds so clients refresh before expiry.
	return 7 * 24 * time.Hour
}

// IceTTLSeconds is the HMAC lifetime advertised on GET /v1/info.
func (c Config) IceTTLSeconds() int {
	return int(c.IceTTL() / time.Second)
}

// IceEnabled is "HMAC + host configured", not "coturn is listening".
// Use ProbeTurn().Running or GET /health turn_running for the listen check.
func (c Config) IceEnabled() bool {
	return c.TurnSecret != "" && strings.TrimSpace(c.PublicHost) != ""
}

func IceHost(host string) string {
	h := strings.TrimSpace(host)
	if strings.Contains(h, ":") && !strings.HasPrefix(h, "[") {
		return "[" + h + "]"
	}
	return h
}

func IsIPv4(s string) bool {
	ip := net.ParseIP(strings.TrimSpace(s))
	return ip != nil && ip.To4() != nil
}

func isIPLiteral(s string) bool {
	h := strings.TrimSpace(s)
	h = strings.TrimPrefix(h, "[")
	h = strings.TrimSuffix(h, "]")
	return net.ParseIP(h) != nil
}

// IsPrivateIPv4 is RFC1918 / loopback / link-local / CGNAT (100.64/10).
// Phones behind Russian mobile CGNAT cannot reach these as TURN URLs.
func IsPrivateIPv4(s string) bool {
	ip := net.ParseIP(strings.TrimSpace(s))
	if ip == nil {
		return false
	}
	ip4 := ip.To4()
	if ip4 == nil {
		return false
	}
	if ip4.IsPrivate() || ip4.IsLoopback() || ip4.IsUnspecified() || ip4.IsLinkLocalUnicast() {
		return true
	}
	// RFC 6598 CGNAT — net.IP.IsPrivate does not include 100.64/10.
	return ip4[0] == 100 && ip4[1] >= 64 && ip4[1] <= 127
}

// unusableICEHost is true for hosts phones cannot reach as TURN/STUN:
// RFC1918, loopback, link-local, CGNAT 100.64/10, IPv6 ULA/link-local.
func unusableICEHost(h string) bool {
	h = strings.TrimSpace(h)
	if h == "" || strings.EqualFold(h, "localhost") {
		return true
	}
	bare := strings.TrimSuffix(strings.TrimPrefix(h, "["), "]")
	ip := net.ParseIP(bare)
	if ip == nil {
		return false // DNS hostname is usable
	}
	if ip4 := ip.To4(); ip4 != nil {
		return IsPrivateIPv4(ip4.String())
	}
	return ip.IsLoopback() || ip.IsUnspecified() || ip.IsLinkLocalUnicast() || ip.IsPrivate()
}

func (c Config) iceURLHosts() []string {
	host := strings.TrimSpace(c.PublicHost)
	ip := c.PublicIPv4()
	add := func(out []string, h string) []string {
		h = strings.TrimSpace(h)
		if unusableICEHost(h) {
			return out
		}
		h = IceHost(h)
		for _, e := range out {
			if strings.EqualFold(e, h) {
				return out
			}
		}
		return append(out, h)
	}
	// A private public_host is a common «Обновить ядро» mistake (SSH to 10.x).
	// Never fall through to advertising that host when PublicIPv4 is empty or also private.
	var out []string
	out = add(out, host)
	out = add(out, ip)
	return out
}

// IceHostname is the DNS/SNI name for TURNS when ICE URLs use a raw IP.
// Empty when public_host is itself an IP and tls_hostname is unset.
func (c Config) IceHostname() string {
	if h := strings.TrimSpace(c.TLSHostname); h != "" && !isIPLiteral(h) {
		return h
	}
	if h := strings.TrimSpace(c.PublicHost); h != "" && !isIPLiteral(h) {
		return h
	}
	return ""
}

// PublicIPv4 is advertised as top-level public_ip so the client can
// duplicate turn/turns URLs when public_host DNS does not resolve.
func (c Config) PublicIPv4() string {
	pick := func(s string) string {
		s = strings.TrimSpace(s)
		if IsIPv4(s) && !IsPrivateIPv4(s) {
			return s
		}
		return ""
	}
	if ip := pick(c.PublicIP); ip != "" {
		return ip
	}
	if ip := pick(c.PublicHost); ip != "" {
		return ip
	}
	if file, ok := ReadFileTurnStatus(c.TurnStatusFile()); ok {
		ext := strings.TrimSpace(strings.Split(file.ExternalIP, "/")[0])
		if ip := pick(ext); ip != "" {
			return ip
		}
	}
	return ""
}

func TurnUsername(now time.Time, ttl time.Duration) string {
	return fmt.Sprintf("%d:rope", now.Add(ttl).Unix())
}

// TurnCredential is the coturn REST (time-limited HMAC-SHA1) password.
func TurnCredential(secret, username string) string {
	mac := hmac.New(sha1.New, []byte(secret))
	_, _ = mac.Write([]byte(username))
	return base64.StdEncoding.EncodeToString(mac.Sum(nil))
}

func (c Config) IceURLs() []string {
	ice := c.IceServers(time.Unix(0, 0))
	if len(ice) == 0 {
		return nil
	}
	out := append([]string{}, ice[0].URLs...)
	if len(ice) > 1 {
		out = append(out, ice[1].URLs...)
	}
	return out
}

func (c Config) IceServers(now time.Time) []IceServer {
	if !c.IceEnabled() {
		return nil
	}
	hosts := c.iceURLHosts()
	if len(hosts) == 0 {
		// No public host: client falls back to STUN. Never advertise 10.x / CGNAT.
		return nil
	}
	// Advertise public URLs even if ProbeTurn is down on first boot (coturn
	// can race the listen). Health / TurnReport stay honest (turn_running,
	// allocate_ok). Do not hide ICE just because the probe is flaky.
	sni := c.IceHostname()
	turnPort := c.EffectiveTurnPort()
	user := TurnUsername(now, c.IceTTL())
	cred := TurnCredential(c.TurnSecret, user)
	var stunURLs, turnURLs []string
	for _, host := range hosts {
		stunURLs = append(stunURLs, fmt.Sprintf("stun:%s:%d", host, turnPort))
		if turns := c.EffectiveTurnsPort(); turns > 0 {
			turnURLs = append(turnURLs,
				fmt.Sprintf("turns:%s:%d?transport=tcp", host, turns),
				fmt.Sprintf("turns:%s:%d", host, turns),
			)
		}
		turnURLs = append(turnURLs,
			fmt.Sprintf("turn:%s:%d?transport=tcp", host, turnPort),
			fmt.Sprintf("turn:%s:%d?transport=udp", host, turnPort),
			fmt.Sprintf("turn:%s:%d", host, turnPort),
		)
	}
	return []IceServer{
		{URLs: stunURLs, Hostname: iceSNI(sni, stunURLs)},
		{URLs: turnURLs, Username: user, Credential: cred, Hostname: iceSNI(sni, turnURLs)},
	}
}

// iceSNI sets IceServer.Hostname when urls contain an IP literal and a DNS
// name is known (IceHostname / TLSHostname) so TURNS can present the cert SNI.
func iceSNI(sni string, urls []string) string {
	sni = strings.TrimSpace(sni)
	if sni == "" {
		return ""
	}
	for _, u := range urls {
		if iceURLHasIPLiteral(u) {
			return sni
		}
	}
	return ""
}

func iceURLHasIPLiteral(u string) bool {
	u = strings.TrimSpace(u)
	for _, p := range []string{"stuns:", "turns:", "stun:", "turn:"} {
		if strings.HasPrefix(strings.ToLower(u), p) {
			u = u[len(p):]
			break
		}
	}
	if i := strings.IndexByte(u, '?'); i >= 0 {
		u = u[:i]
	}
	host, _, err := net.SplitHostPort(u)
	if err != nil {
		host = u
	}
	return isIPLiteral(host)
}

// FileTurnStatus is written by install.sh so the owner sees the real bind result.
type FileTurnStatus struct {
	OK         bool   `json:"ok"`
	Error      string `json:"error"`
	TurnPort   int    `json:"turn_port"`
	TurnsPort  int    `json:"turns_port"`
	Listen     string `json:"listen"`
	ExternalIP string `json:"external_ip"`
}

type TurnReport struct {
	Configured     bool     `json:"configured"`
	Running        bool     `json:"running"`
	TurnsListening bool     `json:"turns_listening"`
	AllocateOK     bool     `json:"allocate_ok"`
	Listen         string   `json:"listen"`
	ExternalIP     string   `json:"external_ip,omitempty"`
	RelayedIP      string   `json:"relayed_ip,omitempty"`
	Error          string   `json:"error,omitempty"`
	TurnPort       int      `json:"turn_port"`
	TurnsPort      int      `json:"turns_port"`
	Advertised     []string `json:"advertised,omitempty"`
}

// DialTCP is replaced in tests.
var DialTCP = func(addr string, timeout time.Duration) error {
	conn, err := net.DialTimeout("tcp", addr, timeout)
	if err != nil {
		return err
	}
	_ = conn.Close()
	return nil
}

func (c Config) TurnStatusFile() string {
	if strings.HasPrefix(c.TLSCert, "/etc/rope/") {
		return "/etc/rope/turn-status.json"
	}
	if c.TLSCert != "" {
		dir := filepath.Dir(filepath.Dir(c.TLSCert))
		if dir != "" && dir != "." {
			return filepath.Join(dir, "turn-status.json")
		}
	}
	return filepath.Join(c.DataDir, "turn-status.json")
}

func ReadFileTurnStatus(path string) (FileTurnStatus, bool) {
	var st FileTurnStatus
	raw, err := os.ReadFile(path)
	if err != nil {
		return st, false
	}
	if err := json.Unmarshal(raw, &st); err != nil {
		return st, false
	}
	return st, true
}

func (c Config) ProbeTurn(timeout time.Duration) TurnReport {
	rep := TurnReport{
		Configured: c.IceEnabled(),
		TurnPort:   c.EffectiveTurnPort(),
		TurnsPort:  c.EffectiveTurnsPort(),
	}
	if !c.IceEnabled() {
		rep.Error = "нет public_host или turn_secret — обновите ядро"
		return rep
	}
	if ice := c.IceServers(time.Now()); len(ice) > 1 {
		rep.Advertised = ice[1].URLs
	}
	file, hasFile := ReadFileTurnStatus(c.TurnStatusFile())
	if hasFile {
		rep.Listen = file.Listen
		rep.ExternalIP = file.ExternalIP
		if file.Error != "" {
			rep.Error = file.Error
		}
		if file.TurnsPort > 0 && rep.TurnsPort == 0 {
			rep.TurnsPort = file.TurnsPort
		}
	}
	if timeout <= 0 {
		timeout = 400 * time.Millisecond
	}
	rep.Running = DialTCP(net.JoinHostPort("127.0.0.1", fmt.Sprintf("%d", rep.TurnPort)), timeout) == nil
	if rep.TurnsPort > 0 {
		rep.TurnsListening = DialTCP(net.JoinHostPort("127.0.0.1", fmt.Sprintf("%d", rep.TurnsPort)), timeout) == nil
		// Port 443 up without 3478 is almost always Caddy/nginx, not coturn.
		if !rep.Running && rep.TurnsListening && rep.TurnsPort == 443 {
			rep.TurnsListening = false
			if rep.Error == "" {
				rep.Error = "порт 443 занят другим сервисом, TURNS должен быть на 5349"
			}
		}
	}
	if rep.Listen == "" && rep.Running {
		rep.Listen = "0.0.0.0"
	}
	switch {
	case rep.Running && (rep.TurnsPort == 0 || rep.TurnsListening):
		rep.Error = ""
	case rep.Running && !rep.TurnsListening && rep.TurnsPort > 0:
		if rep.Error == "" {
			rep.Error = fmt.Sprintf("TURN %d слушает, TURNS %d не открыт", rep.TurnPort, rep.TurnsPort)
		}
	case !rep.Running:
		if rep.Error == "" {
			rep.Error = fmt.Sprintf("coturn не слушает 127.0.0.1:%d", rep.TurnPort)
		}
	}
	if !rep.Running {
		return rep
	}
	alloc := c.cachedAllocate(timeout)
	rep.AllocateOK = alloc.OK
	rep.RelayedIP = alloc.RelayedIP
	if !alloc.OK && alloc.Error != "" {
		rep.Error = alloc.Error
	}
	return rep
}
