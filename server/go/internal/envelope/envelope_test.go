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

func sampleEnvelope(t *testing.T, msgType byte) []byte {
	t.Helper()
	pub, priv, err := ed25519.GenerateKey(rand.Reader)
	if err != nil {
		t.Fatal(err)
	}
	id := uuid.New()
	raw := []byte("ROPE")
	ver := make([]byte, 2)
	binary.LittleEndian.PutUint16(ver, 1)
	raw = append(raw, ver...)
	raw = append(raw, msgType, 0)
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
	return raw
}

func TestParseRejectsUnknownType(t *testing.T) {
	raw := sampleEnvelope(t, 99)
	if _, err := Parse(raw); err == nil {
		t.Fatal("type 99 must fail")
	}
	raw0 := sampleEnvelope(t, 0)
	if _, err := Parse(raw0); err == nil {
		t.Fatal("type 0 must fail")
	}
}

func TestParseAcceptsKnownTypes(t *testing.T) {
	for _, ty := range []byte{TypeText, TypeMedia, TypeGroupText, TypeCall, TypeReceipt} {
		p, err := Parse(sampleEnvelope(t, ty))
		if err != nil {
			t.Fatalf("type %d: %v", ty, err)
		}
		if p.Meta.Type != ty {
			t.Fatalf("type %d got %d", ty, p.Meta.Type)
		}
	}
}
