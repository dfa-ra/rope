package db

import (
	"bytes"
	"errors"
	"testing"
	"time"
)

func TestPutMailboxKeepsFirstPendingBlob(t *testing.T) {
	s := testStore(t)
	first := []byte("blob-one")
	second := []byte("blob-two")
	if err := s.PutMailbox("mid-1", "bob", "alice", first, time.Hour); err != nil {
		t.Fatal(err)
	}
	err := s.PutMailbox("mid-1", "eve", "mallory", second, time.Hour)
	if !errors.Is(err, ErrMailboxExists) {
		t.Fatalf("second put: %v", err)
	}
	n, err := s.MailboxCount()
	if err != nil || n != 1 {
		t.Fatalf("count %d %v", n, err)
	}
	pending, err := s.PendingMailbox("bob")
	if err != nil {
		t.Fatal(err)
	}
	if len(pending) != 1 {
		t.Fatalf("pending %d", len(pending))
	}
	if !bytes.Equal(pending[0].Blob, first) {
		t.Fatalf("blob %q", pending[0].Blob)
	}
	if pending[0].SenderID != "alice" || pending[0].RecipientID != "bob" {
		t.Fatalf("row %+v", pending[0])
	}
	eve, err := s.PendingMailbox("eve")
	if err != nil {
		t.Fatal(err)
	}
	if len(eve) != 0 {
		t.Fatalf("eve pending %d", len(eve))
	}
}

func TestPutMailboxDistinctIdsBothStore(t *testing.T) {
	s := testStore(t)
	if err := s.PutMailbox("mid-a", "bob", "alice", []byte("a"), time.Hour); err != nil {
		t.Fatal(err)
	}
	if err := s.PutMailbox("mid-b", "bob", "alice", []byte("b"), time.Hour); err != nil {
		t.Fatal(err)
	}
	n, err := s.MailboxCount()
	if err != nil || n != 2 {
		t.Fatalf("count %d %v", n, err)
	}
}
