package db

import (
	"errors"
	"path/filepath"
	"sync"
	"testing"
)

func testStore(t *testing.T) *Store {
	t.Helper()
	s, err := Open(filepath.Join(t.TempDir(), "data.db"))
	if err != nil {
		t.Fatal(err)
	}
	t.Cleanup(func() { _ = s.Close() })
	return s
}

func seedOwnerDevice(t *testing.T, s *Store, memberID, name, deviceID string) {
	t.Helper()
	if err := s.InsertMember(memberID, name, "owner"); err != nil {
		t.Fatal(err)
	}
	if err := s.InsertDevice(Device{
		ID:             deviceID,
		MemberID:       memberID,
		PublicIdentity: []byte("blob-" + deviceID),
		SignPublic:     []byte("sign-" + deviceID),
	}); err != nil {
		t.Fatal(err)
	}
}

func TestRevokeDeviceGuardedConcurrentTwoOwners(t *testing.T) {
	s := testStore(t)
	seedOwnerDevice(t, s, "m1", "a", "dev-a")
	seedOwnerDevice(t, s, "m2", "b", "dev-b")

	errs := make(chan error, 2)
	var start sync.WaitGroup
	start.Add(2)
	for _, id := range []string{"dev-a", "dev-b"} {
		id := id
		go func() {
			start.Done()
			start.Wait()
			errs <- s.RevokeDeviceGuarded(id)
		}()
	}
	got := []error{<-errs, <-errs}
	ok, last := 0, 0
	for _, err := range got {
		switch {
		case err == nil:
			ok++
		case errors.Is(err, ErrLastOwner):
			last++
		default:
			t.Fatalf("unexpected errors %v %v", got[0], got[1])
		}
	}
	if ok != 1 || last != 1 {
		t.Fatalf("wanted one success and one last-owner, got %v %v", got[0], got[1])
	}
	n, err := s.OwnerDeviceCount()
	if err != nil || n != 1 {
		t.Fatalf("remaining owner devices %d %v", n, err)
	}
}

func TestCheckThenActConcurrentTwoOwnerRevokeBricks(t *testing.T) {
	s := testStore(t)
	seedOwnerDevice(t, s, "m1", "a", "dev-a")
	seedOwnerDevice(t, s, "m2", "b", "dev-b")

	var gate sync.WaitGroup
	gate.Add(2)
	AfterOwnerDeviceCount = func() {
		gate.Done()
		gate.Wait()
	}
	t.Cleanup(func() { AfterOwnerDeviceCount = nil })

	errs := make(chan error, 2)
	var start sync.WaitGroup
	start.Add(2)
	for _, id := range []string{"dev-a", "dev-b"} {
		id := id
		go func() {
			start.Done()
			start.Wait()
			n, err := s.OwnerDeviceCount()
			if err != nil {
				errs <- err
				return
			}
			if n <= 1 {
				errs <- ErrLastOwner
				return
			}
			errs <- s.RevokeDevice(id)
		}()
	}
	<-errs
	<-errs
	AfterOwnerDeviceCount = nil

	n, err := s.OwnerDeviceCount()
	if err != nil {
		t.Fatal(err)
	}
	if n != 0 {
		t.Fatalf("check-then-act race window closed: remaining owner devices %d (want 0 so HTTP concurrent test still catches a revert)", n)
	}
}

func TestRevokeDeviceGuardedAlreadyRevokedAndLastOwner(t *testing.T) {
	s := testStore(t)
	seedOwnerDevice(t, s, "m1", "a", "dev-a")
	if err := s.InsertDevice(Device{
		ID:             "spare",
		MemberID:       "m1",
		PublicIdentity: []byte("blob-spare"),
		SignPublic:     []byte("sign-spare"),
	}); err != nil {
		t.Fatal(err)
	}
	if err := s.RevokeDeviceGuarded("spare"); err != nil {
		t.Fatal(err)
	}
	if err := s.RevokeDeviceGuarded("spare"); !errors.Is(err, ErrNotFound) {
		t.Fatalf("already-revoked spare wanted ErrNotFound got %v", err)
	}
	if err := s.RevokeDeviceGuarded("missing"); !errors.Is(err, ErrNotFound) {
		t.Fatalf("unknown device wanted ErrNotFound got %v", err)
	}
	if err := s.RevokeDeviceGuarded("dev-a"); !errors.Is(err, ErrLastOwner) {
		t.Fatalf("last live owner device wanted ErrLastOwner got %v", err)
	}
	n, err := s.OwnerDeviceCount()
	if err != nil || n != 1 {
		t.Fatalf("last owner device still live %d %v", n, err)
	}
}

func TestRevokeMemberGuardedConcurrentTwoOwners(t *testing.T) {
	s := testStore(t)
	seedOwnerDevice(t, s, "m1", "a", "dev-a")
	seedOwnerDevice(t, s, "m2", "b", "dev-b")

	errs := make(chan error, 2)
	var start sync.WaitGroup
	start.Add(2)
	for _, id := range []string{"m1", "m2"} {
		id := id
		go func() {
			start.Done()
			start.Wait()
			errs <- s.RevokeMemberGuarded(id)
		}()
	}
	got := []error{<-errs, <-errs}
	ok, last := 0, 0
	for _, err := range got {
		switch {
		case err == nil:
			ok++
		case errors.Is(err, ErrLastOwner):
			last++
		default:
			t.Fatalf("unexpected errors %v %v", got[0], got[1])
		}
	}
	if ok != 1 || last != 1 {
		t.Fatalf("wanted one success and one last-owner, got %v %v", got[0], got[1])
	}
	n, err := s.OwnerDeviceCount()
	if err != nil || n != 1 {
		t.Fatalf("remaining owner devices %d %v", n, err)
	}
}

func TestCheckThenActConcurrentTwoOwnerRevokeMemberBricks(t *testing.T) {
	s := testStore(t)
	seedOwnerDevice(t, s, "m1", "a", "dev-a")
	seedOwnerDevice(t, s, "m2", "b", "dev-b")

	var gate sync.WaitGroup
	gate.Add(2)
	AfterOwnerDeviceCount = func() {
		gate.Done()
		gate.Wait()
	}
	t.Cleanup(func() { AfterOwnerDeviceCount = nil })

	errs := make(chan error, 2)
	var start sync.WaitGroup
	start.Add(2)
	for _, id := range []string{"m1", "m2"} {
		id := id
		go func() {
			start.Done()
			start.Wait()
			n, err := s.OwnerDeviceCountExceptMember(id)
			if err != nil {
				errs <- err
				return
			}
			if n == 0 {
				errs <- ErrLastOwner
				return
			}
			errs <- s.RevokeMember(id)
		}()
	}
	<-errs
	<-errs
	AfterOwnerDeviceCount = nil

	n, err := s.OwnerDeviceCount()
	if err != nil {
		t.Fatal(err)
	}
	if n != 0 {
		t.Fatalf("check-then-act race window closed: remaining owner devices %d (want 0 so HTTP concurrent test still catches a revert)", n)
	}
}

func TestRevokeMemberGuardedLastOwnerAndUnknown(t *testing.T) {
	s := testStore(t)
	seedOwnerDevice(t, s, "m1", "a", "dev-a")
	if err := s.RevokeMemberGuarded("missing"); !errors.Is(err, ErrNotFound) {
		t.Fatalf("unknown member wanted ErrNotFound got %v", err)
	}
	if err := s.RevokeMemberGuarded("m1"); !errors.Is(err, ErrLastOwner) {
		t.Fatalf("last owner wanted ErrLastOwner got %v", err)
	}
	n, err := s.OwnerDeviceCount()
	if err != nil || n != 1 {
		t.Fatalf("last owner still live %d %v", n, err)
	}
	if err := s.InsertMember("g1", "guest", "guest"); err != nil {
		t.Fatal(err)
	}
	if err := s.RevokeMemberGuarded("g1"); err != nil {
		t.Fatal(err)
	}
	if err := s.RevokeMemberGuarded("g1"); err != nil {
		t.Fatalf("already-revoked guest wanted success got %v", err)
	}
}

func TestRevokeDeviceGuardedGuestDoesNotUseOwnerGate(t *testing.T) {
	s := testStore(t)
	seedOwnerDevice(t, s, "m1", "a", "dev-a")
	if err := s.InsertMember("g1", "guest", "guest"); err != nil {
		t.Fatal(err)
	}
	if err := s.InsertDevice(Device{
		ID:             "guest-dev",
		MemberID:       "g1",
		PublicIdentity: []byte("blob-g"),
		SignPublic:     []byte("sign-g"),
	}); err != nil {
		t.Fatal(err)
	}
	if err := s.RevokeDeviceGuarded("guest-dev"); err != nil {
		t.Fatal(err)
	}
	if err := s.RevokeDeviceGuarded("guest-dev"); !errors.Is(err, ErrNotFound) {
		t.Fatalf("already-revoked guest wanted ErrNotFound got %v", err)
	}
	if err := s.RevokeDeviceGuarded("dev-a"); !errors.Is(err, ErrLastOwner) {
		t.Fatalf("last owner device wanted ErrLastOwner got %v", err)
	}
}
