package db

import (
	"testing"
	"time"
)

func TestPeekInviteDoesNotConsume(t *testing.T) {
	s := testStore(t)
	seedOwnerDevice(t, s, "m1", "owner", "dev-owner")
	if err := s.InsertInvite("inv-1", "secret-token", "m1", time.Now().Add(time.Hour)); err != nil {
		t.Fatal(err)
	}
	if err := s.PeekInvite("secret-token"); err != nil {
		t.Fatalf("peek live: %v", err)
	}
	if err := s.PeekInvite("secret-token"); err != nil {
		t.Fatalf("peek twice must not consume: %v", err)
	}
	if err := s.PeekInvite("wrong"); err == nil {
		t.Fatal("peek unknown want error")
	}
	if _, err := s.ConsumeToken("secret-token"); err != nil {
		t.Fatal(err)
	}
	if err := s.PeekInvite("secret-token"); err == nil {
		t.Fatal("peek used want error")
	}
}
