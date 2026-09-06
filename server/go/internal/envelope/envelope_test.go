package envelope

import (
	"crypto/ed25519"
	"crypto/rand"
	"encoding/binary"
	"testing"

	"github.com/google/uuid"
)

func TestParseAndVerify(t *testing.T) {
	pub, priv, err := ed25519.GenerateKey(rand.Reader)
	if err != nil {
		t.Fatal(err)
	}
	id := uuid.New()
	raw := []byte("ROPE")
	ver := make([]byte, 2)
	binary.LittleEndian.PutUint16(ver, 1)
	raw = append(raw, ver...)
	raw = append(raw, 1, 0)
	raw = append(raw, id[:]...)
	raw = append(raw, make([]byte, 8)...)
	raw = append(raw, pub...)
	raw = append(raw, pub...)
	raw = append(raw, make([]byte, 24)...)
	ct := []byte("cipher")
	ln := make([]byte, 4)
	binary.LittleEndian.PutUint32(ln, uint32(len(ct)))
	raw = append(raw, ln...)
	raw = append(raw, ct...)
	raw = append(raw, ed25519.Sign(priv, raw)...)
	p, err := Parse(raw)
	if err != nil {
		t.Fatal(err)
	}
	if p.Meta.Version != 1 {
		t.Fatal(p.Meta.Version)
	}
	if err := VerifySender(p, pub); err != nil {
		t.Fatal(err)
	}
}

func TestRejectsShort(t *testing.T) {
	if _, err := Parse([]byte("ROPE")); err == nil {
		t.Fatal("expected error")
	}
}
