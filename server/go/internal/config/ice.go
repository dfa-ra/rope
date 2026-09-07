package config

import (
	"crypto/hmac"
	"crypto/sha1"
	"encoding/base64"
	"fmt"
	"strings"
	"time"
)

// IceServer is the RTCIceServer JSON object advertised on GET /v1/info.
type IceServer struct {
	URLs       []string `json:"urls"`
	Username   string   `json:"username,omitempty"`
	Credential string   `json:"credential,omitempty"`
}

func (c Config) EffectiveTurnPort() int {
	if c.TurnPort > 0 {
		return c.TurnPort
	}
	return 3478
}

func (c Config) EffectiveTurnsPort() int {
	if c.TurnsPort > 0 {
		return c.TurnsPort
	}
	return 443
}

func (c Config) IceTTL() time.Duration {
	if c.TurnTTLSeconds > 0 {
		return time.Duration(c.TurnTTLSeconds) * time.Second
	}
	return 24 * time.Hour
}

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

func TurnUsername(now time.Time, ttl time.Duration) string {
	return fmt.Sprintf("%d:rope", now.Add(ttl).Unix())
}

// TurnCredential is the coturn REST (time-limited HMAC-SHA1) password.
func TurnCredential(secret, username string) string {
	mac := hmac.New(sha1.New, []byte(secret))
	_, _ = mac.Write([]byte(username))
	return base64.StdEncoding.EncodeToString(mac.Sum(nil))
}

func (c Config) IceServers(now time.Time) []IceServer {
	if !c.IceEnabled() {
		return nil
	}
	host := IceHost(c.PublicHost)
	turnPort := c.EffectiveTurnPort()
	turnsPort := c.EffectiveTurnsPort()
	user := TurnUsername(now, c.IceTTL())
	cred := TurnCredential(c.TurnSecret, user)
	return []IceServer{
		{URLs: []string{fmt.Sprintf("stun:%s:%d", host, turnPort)}},
		{
			URLs: []string{
				fmt.Sprintf("turns:%s:%d?transport=tcp", host, turnsPort),
				fmt.Sprintf("turns:%s:%d", host, turnsPort),
				fmt.Sprintf("turn:%s:%d?transport=udp", host, turnPort),
				fmt.Sprintf("turn:%s:%d", host, turnPort),
				fmt.Sprintf("turn:%s:%d?transport=tcp", host, turnPort),
			},
			Username:   user,
			Credential: cred,
		},
	}
}
