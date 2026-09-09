package authz

import (
	"crypto/ed25519"
	"crypto/sha256"
	"encoding/base64"
	"encoding/hex"
	"fmt"
	"strconv"
	"strings"
	"time"
)

const MaxSkew = 300 * time.Second

// WsAuthHeader is Phase A WSS auth. Same three fields as the query, same WSMessage.
// Do not reuse Authorization: Rope (that signs REST AuthMessage).
const WsAuthHeader = "X-Rope-Ws-Auth"

func AuthMessage(method, path string, ts int64, body []byte) string {
	sum := sha256.Sum256(body)
	return fmt.Sprintf("rope-auth-v1\n%s\n%s\n%d\n%s", strings.ToUpper(method), path, ts, hex.EncodeToString(sum[:]))
}

func WSMessage(ts int64) string {
	return fmt.Sprintf("rope-ws-v1\n%d", ts)
}

type Parts struct {
	DeviceID  string
	Timestamp int64
	Signature []byte
}

func DecodeB64URL(s string) ([]byte, error) {
	return base64.RawURLEncoding.DecodeString(s)
}

func ParseHeader(header string) (Parts, error) {
	if !strings.HasPrefix(header, "Rope ") {
		return Parts{}, fmt.Errorf("missing Rope scheme")
	}
	return ParseWsParts(strings.TrimPrefix(header, "Rope "))
}

// ParseWsParts reads <device_id>.<unix>.<base64url-sig> (no Rope scheme).
func ParseWsParts(raw string) (Parts, error) {
	rest := strings.TrimSpace(raw)
	parts := strings.Split(rest, ".")
	if len(parts) != 3 {
		return Parts{}, fmt.Errorf("bad auth format")
	}
	ts, err := strconv.ParseInt(parts[1], 10, 64)
	if err != nil {
		return Parts{}, err
	}
	sig, err := DecodeB64URL(parts[2])
	if err != nil {
		return Parts{}, err
	}
	id := strings.ToLower(parts[0])
	if id == "" {
		return Parts{}, fmt.Errorf("missing device")
	}
	return Parts{DeviceID: id, Timestamp: ts, Signature: sig}, nil
}

// WsCreds prefers X-Rope-Ws-Auth when set; otherwise query device_id/ts/sig.
func WsCreds(header, deviceID, ts, sig string) (Parts, error) {
	if strings.TrimSpace(header) != "" {
		return ParseWsParts(header)
	}
	if deviceID == "" || ts == "" || sig == "" {
		return Parts{}, fmt.Errorf("missing auth")
	}
	return ParseWsParts(deviceID + "." + ts + "." + sig)
}

func CheckTimestamp(ts int64, now time.Time) error {
	t := time.Unix(ts, 0)
	if now.Sub(t) > MaxSkew || t.Sub(now) > MaxSkew {
		return fmt.Errorf("timestamp skew")
	}
	return nil
}

func Verify(pub ed25519.PublicKey, message string, sig []byte) error {
	if !ed25519.Verify(pub, []byte(message), sig) {
		return fmt.Errorf("bad signature")
	}
	return nil
}
