package config

import (
	"crypto/rand"
	"crypto/sha256"
	"encoding/hex"
	"encoding/json"
	"fmt"
	"os"
	"path/filepath"
	"time"
)

const ProtocolVersion uint16 = 1
const ServerVersion = "0.3.15"
const HTTPDevFingerprintSeed = "rope-http-dev"

type Config struct {
	Listen              string `json:"listen"`
	DataDir             string `json:"data_dir"`
	TLSCert             string `json:"tls_cert"`
	TLSKey              string `json:"tls_key"`
	SetupToken          string `json:"setup_token"`
	ServerID            string `json:"server_id"`
	MailboxTTLSeconds   int    `json:"mailbox_ttl_seconds"`
	MaxEnvelopeBytes    int    `json:"max_envelope_bytes"`
	MaxObjectBytes      int    `json:"max_object_bytes"`
	ObjectTTLSeconds    int    `json:"object_ttl_seconds"`
	ObjectQuotaBytes    int64  `json:"object_quota_bytes"`
	AllowHTTP           bool   `json:"allow_http"`
	FingerprintOverride string `json:"fingerprint,omitempty"`
	PublicHost          string `json:"public_host,omitempty"`
	PublicIP            string `json:"public_ip,omitempty"`
	TLSHostname         string `json:"tls_hostname,omitempty"`
	TurnSecret          string `json:"turn_secret,omitempty"`
	TurnPort            int    `json:"turn_port,omitempty"`
	TurnsPort           int    `json:"turns_port,omitempty"`
	TurnTTLSeconds      int    `json:"turn_ttl_seconds,omitempty"`
}

func Default() Config {
	return Config{
		Listen:            "0.0.0.0:8443",
		DataDir:           "/var/lib/rope",
		TLSCert:           "/etc/rope/tls/cert.pem",
		TLSKey:            "/etc/rope/tls/key.pem",
		MailboxTTLSeconds: 7 * 24 * 3600,
		MaxEnvelopeBytes:  65536,
		MaxObjectBytes:    25 * 1024 * 1024,
		ObjectTTLSeconds:  7 * 24 * 3600,
		ObjectQuotaBytes:  512 * 1024 * 1024,
	}
}

func LoadOrInit(path string, init bool, allowHTTP bool, listen string, dataDir string) (Config, error) {
	cfg := Default()
	if listen != "" {
		cfg.Listen = listen
	}
	if dataDir != "" {
		cfg.DataDir = dataDir
	}
	cfg.AllowHTTP = allowHTTP
	if _, err := os.Stat(path); err == nil {
		raw, err := os.ReadFile(path)
		if err != nil {
			return cfg, err
		}
		if err := json.Unmarshal(raw, &cfg); err != nil {
			return cfg, err
		}
		if listen != "" {
			cfg.Listen = listen
		}
		if dataDir != "" {
			cfg.DataDir = dataDir
		}
		if allowHTTP {
			cfg.AllowHTTP = true
		}
		return cfg, nil
	}
	if !init {
		return cfg, fmt.Errorf("config %s not found (pass --init to create)", path)
	}
	if err := os.MkdirAll(filepath.Dir(path), 0o755); err != nil {
		return cfg, err
	}
	if dataDir == "" && cfg.DataDir == Default().DataDir && filepath.Clean(filepath.Dir(path)) != "/etc/rope" {
		cfg.DataDir = filepath.Join(filepath.Dir(path), "data")
	}
	if err := os.MkdirAll(cfg.DataDir, 0o750); err != nil {
		return cfg, err
	}
	cfg.ServerID = randomHex(16)
	cfg.SetupToken = randomHex(24)
	raw, err := json.MarshalIndent(cfg, "", "  ")
	if err != nil {
		return cfg, err
	}
	if err := os.WriteFile(path, raw, 0o640); err != nil {
		return cfg, err
	}
	return cfg, nil
}

func (c Config) DBPath() string {
	return filepath.Join(c.DataDir, "data.db")
}

func (c Config) MailboxTTL() time.Duration {
	return time.Duration(c.MailboxTTLSeconds) * time.Second
}

func (c Config) ObjectTTL() time.Duration {
	if c.ObjectTTLSeconds <= 0 {
		return 7 * 24 * time.Hour
	}
	return time.Duration(c.ObjectTTLSeconds) * time.Second
}

func (c Config) ObjectsDir() string {
	return filepath.Join(c.DataDir, "objects")
}

func HTTPDevFingerprint() string {
	sum := sha256.Sum256([]byte(HTTPDevFingerprintSeed))
	return hex.EncodeToString(sum[:])
}

func randomHex(n int) string {
	b := make([]byte, n)
	if _, err := rand.Read(b); err != nil {
		panic(err)
	}
	return hex.EncodeToString(b)
}
