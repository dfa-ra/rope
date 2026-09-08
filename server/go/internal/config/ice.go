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
	// Clients cache GET /v1/info; 24h HMAC expires under that cache.
	return 7 * 24 * time.Hour
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
	if IsIPv4(c.PublicIP) {
		return strings.TrimSpace(c.PublicIP)
	}
	if IsIPv4(c.PublicHost) {
		return strings.TrimSpace(c.PublicHost)
	}
	if file, ok := ReadFileTurnStatus(c.TurnStatusFile()); ok {
		ext := strings.TrimSpace(strings.Split(file.ExternalIP, "/")[0])
		if IsIPv4(ext) {
			return ext
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
	host := IceHost(c.PublicHost)
	sni := c.IceHostname()
	turnPort := c.EffectiveTurnPort()
	user := TurnUsername(now, c.IceTTL())
	cred := TurnCredential(c.TurnSecret, user)
	stun := IceServer{URLs: []string{fmt.Sprintf("stun:%s:%d", host, turnPort)}, Hostname: sni}
	turnURLs := []string{
		fmt.Sprintf("turn:%s:%d?transport=udp", host, turnPort),
		fmt.Sprintf("turn:%s:%d", host, turnPort),
		fmt.Sprintf("turn:%s:%d?transport=tcp", host, turnPort),
	}
	if turns := c.EffectiveTurnsPort(); turns > 0 {
		turnURLs = append([]string{
			fmt.Sprintf("turns:%s:%d?transport=tcp", host, turns),
			fmt.Sprintf("turns:%s:%d", host, turns),
		}, turnURLs...)
	}
	return []IceServer{
		stun,
		{URLs: turnURLs, Username: user, Credential: cred, Hostname: sni},
	}
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
	Listen         string   `json:"listen"`
	ExternalIP     string   `json:"external_ip,omitempty"`
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
	return rep
}
