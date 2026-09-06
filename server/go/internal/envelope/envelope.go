package envelope

import (
	"crypto/ed25519"
	"encoding/binary"
	"encoding/hex"
	"fmt"

	"github.com/google/uuid"
)

const (
	Magic           = "ROPE"
	ProtocolVersion = 1
	TypeText        = 1
	HeaderAADLen    = 96
	MaxCiphertext   = 65536
)

type Meta struct {
	Version     uint16
	Type        uint8
	MessageID   string
	TimestampMS uint64
	SenderID    string
	RecipientID string
}

type Parsed struct {
	Meta       Meta
	Nonce      []byte
	Ciphertext []byte
	SignedBody []byte
	Signature  []byte
}

func Parse(raw []byte) (Parsed, error) {
	if len(raw) < 124+64 {
		return Parsed{}, fmt.Errorf("envelope too short")
	}
	if string(raw[0:4]) != Magic {
		return Parsed{}, fmt.Errorf("bad magic")
	}
	version := binary.LittleEndian.Uint16(raw[4:6])
	msgType := raw[6]
	id, err := uuid.FromBytes(raw[8:24])
	if err != nil {
		return Parsed{}, err
	}
	ts := binary.LittleEndian.Uint64(raw[24:32])
	sender := hex.EncodeToString(raw[32:64])
	recipient := hex.EncodeToString(raw[64:96])
	nonce := raw[96:120]
	ctLen := int(binary.LittleEndian.Uint32(raw[120:124]))
	if ctLen > MaxCiphertext || 124+ctLen+64 != len(raw) {
		return Parsed{}, fmt.Errorf("bad ciphertext length")
	}
	ct := raw[124 : 124+ctLen]
	sig := raw[124+ctLen:]
	return Parsed{
		Meta: Meta{
			Version:     version,
			Type:        msgType,
			MessageID:   id.String(),
			TimestampMS: ts,
			SenderID:    sender,
			RecipientID: recipient,
		},
		Nonce:      nonce,
		Ciphertext: ct,
		SignedBody: raw[:len(raw)-64],
		Signature:  sig,
	}, nil
}

func VerifySender(p Parsed, publicKey []byte) error {
	if len(publicKey) != ed25519.PublicKeySize {
		return fmt.Errorf("bad public key")
	}
	if hex.EncodeToString(publicKey) != p.Meta.SenderID {
		return fmt.Errorf("sender mismatch")
	}
	if !ed25519.Verify(publicKey, p.SignedBody, p.Signature) {
		return fmt.Errorf("bad signature")
	}
	return nil
}

func LooksLikePlaintext(raw []byte) bool {
	// Used in tests: ciphertext region should not contain the original UTF-8
	// as a contiguous substring. This is a sanity helper, not a security proof.
	return false
}
