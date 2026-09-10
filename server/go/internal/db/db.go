package db

import (
	"context"
	"crypto/sha256"
	"database/sql"
	"encoding/hex"
	"errors"
	"fmt"
	"time"

	_ "modernc.org/sqlite"
)

var (
	ErrNotFound   = errors.New("not found")
	ErrLastOwner  = errors.New("last owner")
	ErrLoginTaken = errors.New("login taken")
)

const liveOwnerDevicesSQL = `
SELECT COUNT(*) FROM devices d
JOIN members m ON m.member_id = d.member_id
WHERE m.role = 'owner' AND m.revoked_at IS NULL AND d.revoked_at IS NULL`

// AfterOwnerDeviceCount, if set, runs after OwnerDeviceCount and
// OwnerDeviceCountExceptMember SELECTs. Tests use it to overlap the old
// check-then-act revoke-device / revoke-member paths.
var AfterOwnerDeviceCount func()

type Store struct {
	SQL *sql.DB
}

func Open(path string) (*Store, error) {
	dsn := fmt.Sprintf("file:%s?_pragma=busy_timeout(5000)&_pragma=foreign_keys(1)", path)
	sqlDB, err := sql.Open("sqlite", dsn)
	if err != nil {
		return nil, err
	}
	sqlDB.SetMaxOpenConns(1)
	s := &Store{SQL: sqlDB}
	if err := s.migrate(); err != nil {
		_ = sqlDB.Close()
		return nil, err
	}
	return s, nil
}

func (s *Store) Close() error { return s.SQL.Close() }

func (s *Store) migrate() error {
	_, err := s.SQL.Exec(`
CREATE TABLE IF NOT EXISTS server_meta (
  server_id TEXT PRIMARY KEY,
  protocol_version INTEGER NOT NULL,
  created_at TEXT NOT NULL,
  version TEXT NOT NULL
);
CREATE TABLE IF NOT EXISTS members (
  member_id TEXT PRIMARY KEY,
  display_name TEXT,
  role TEXT NOT NULL,
  created_at TEXT NOT NULL,
  revoked_at TEXT
);
CREATE TABLE IF NOT EXISTS devices (
  device_id TEXT PRIMARY KEY,
  member_id TEXT NOT NULL,
  public_identity BLOB NOT NULL,
  sign_public BLOB NOT NULL,
  created_at TEXT NOT NULL,
  last_seen TEXT,
  revoked_at TEXT,
  FOREIGN KEY(member_id) REFERENCES members(member_id)
);
CREATE TABLE IF NOT EXISTS invites (
  invite_id TEXT PRIMARY KEY,
  token_hash TEXT NOT NULL UNIQUE,
  created_by TEXT NOT NULL,
  expires_at TEXT NOT NULL,
  used_at TEXT
);
CREATE TABLE IF NOT EXISTS mailbox (
  message_id TEXT PRIMARY KEY,
  recipient_device_id TEXT NOT NULL,
  sender_device_id TEXT NOT NULL,
  encrypted_blob BLOB NOT NULL,
  created_at TEXT NOT NULL,
  expires_at TEXT NOT NULL,
  delivery_state TEXT NOT NULL
);
CREATE INDEX IF NOT EXISTS idx_mailbox_recipient ON mailbox(recipient_device_id, delivery_state);
CREATE TABLE IF NOT EXISTS objects (
  object_id TEXT PRIMARY KEY,
  uploader_device_id TEXT NOT NULL,
  size_bytes INTEGER NOT NULL,
  sha256 TEXT,
  created_at TEXT NOT NULL,
  expires_at TEXT NOT NULL
);
CREATE TABLE IF NOT EXISTS chat_groups (
  group_id TEXT PRIMARY KEY,
  name TEXT NOT NULL,
  created_by TEXT NOT NULL,
  epoch INTEGER NOT NULL,
  created_at TEXT NOT NULL
);
CREATE TABLE IF NOT EXISTS group_members (
  group_id TEXT NOT NULL,
  device_id TEXT NOT NULL,
  added_at TEXT NOT NULL,
  removed_at TEXT,
  PRIMARY KEY (group_id, device_id)
);
`)
	return err
}

func (s *Store) EnsureMeta(serverID, version string, protocol uint16) error {
	var existing string
	err := s.SQL.QueryRow(`SELECT server_id FROM server_meta LIMIT 1`).Scan(&existing)
	if err == sql.ErrNoRows {
		_, err = s.SQL.Exec(
			`INSERT INTO server_meta(server_id, protocol_version, created_at, version) VALUES(?,?,?,?)`,
			serverID, protocol, nowRFC(), version,
		)
		return err
	}
	return err
}

type Member struct {
	ID          string
	DisplayName string
	Role        string
	Revoked     bool
}

type Device struct {
	ID             string
	MemberID       string
	PublicIdentity []byte
	SignPublic     []byte
	LastSeen       string
	Revoked        bool
}

func (s *Store) InsertMember(id, name, role string) error {
	_, err := s.SQL.Exec(
		`INSERT INTO members(member_id, display_name, role, created_at) VALUES(?,?,?,?)`,
		id, name, role, nowRFC(),
	)
	return err
}

func (s *Store) LoginTaken(name string) (bool, error) {
	var n int
	err := s.SQL.QueryRow(
		`SELECT COUNT(*) FROM members WHERE revoked_at IS NULL AND lower(display_name) = lower(?)`,
		name,
	).Scan(&n)
	return n > 0, err
}

func (s *Store) UpdateMemberDisplayName(memberID, name string) error {
	ctx := context.Background()
	conn, err := s.SQL.Conn(ctx)
	if err != nil {
		return err
	}
	defer conn.Close()
	if _, err := conn.ExecContext(ctx, "BEGIN IMMEDIATE"); err != nil {
		return err
	}
	committed := false
	defer func() {
		if !committed {
			_, _ = conn.ExecContext(ctx, "ROLLBACK")
		}
	}()
	var revoked sql.NullString
	err = conn.QueryRowContext(ctx,
		`SELECT revoked_at FROM members WHERE member_id = ?`, memberID,
	).Scan(&revoked)
	if errors.Is(err, sql.ErrNoRows) {
		return ErrNotFound
	}
	if err != nil {
		return err
	}
	if revoked.Valid {
		return ErrNotFound
	}
	var n int
	if err := conn.QueryRowContext(ctx,
		`SELECT COUNT(*) FROM members WHERE revoked_at IS NULL AND lower(display_name) = lower(?) AND member_id != ?`,
		name, memberID,
	).Scan(&n); err != nil {
		return err
	}
	if n > 0 {
		return ErrLoginTaken
	}
	res, err := conn.ExecContext(ctx,
		`UPDATE members SET display_name = ? WHERE member_id = ? AND revoked_at IS NULL`,
		name, memberID,
	)
	if err != nil {
		return err
	}
	rows, err := res.RowsAffected()
	if err != nil {
		return err
	}
	if rows == 0 {
		return ErrNotFound
	}
	if _, err := conn.ExecContext(ctx, "COMMIT"); err != nil {
		return err
	}
	committed = true
	return nil
}

func (s *Store) InsertDevice(d Device) error {
	_, err := s.SQL.Exec(
		`INSERT INTO devices(device_id, member_id, public_identity, sign_public, created_at, last_seen)
		 VALUES(?,?,?,?,?,?)`,
		d.ID, d.MemberID, d.PublicIdentity, d.SignPublic, nowRFC(), nowRFC(),
	)
	return err
}

func (s *Store) Device(id string) (Device, error) {
	var d Device
	var revoked sql.NullString
	err := s.SQL.QueryRow(
		`SELECT device_id, member_id, public_identity, sign_public, COALESCE(last_seen,''), revoked_at
		 FROM devices WHERE device_id = ?`, id,
	).Scan(&d.ID, &d.MemberID, &d.PublicIdentity, &d.SignPublic, &d.LastSeen, &revoked)
	if err != nil {
		return d, err
	}
	d.Revoked = revoked.Valid
	return d, nil
}

func (s *Store) Member(id string) (Member, error) {
	var m Member
	var revoked sql.NullString
	var name sql.NullString
	err := s.SQL.QueryRow(
		`SELECT member_id, display_name, role, revoked_at FROM members WHERE member_id = ?`, id,
	).Scan(&m.ID, &name, &m.Role, &revoked)
	if err != nil {
		return m, err
	}
	if name.Valid {
		m.DisplayName = name.String
	}
	m.Revoked = revoked.Valid
	return m, nil
}

func (s *Store) TouchDevice(id string) {
	_, _ = s.SQL.Exec(`UPDATE devices SET last_seen = ? WHERE device_id = ?`, nowRFC(), id)
}

func (s *Store) HasOwner() (bool, error) {
	n, err := s.OwnerCount()
	return n > 0, err
}

func (s *Store) OwnerCount() (int, error) {
	var n int
	err := s.SQL.QueryRow(`SELECT COUNT(*) FROM members WHERE role = 'owner' AND revoked_at IS NULL`).Scan(&n)
	return n, err
}

// OwnerDeviceCount is live owner devices, not owner members.
// revoke-device 409 keys off this so two owners cannot brick the instance
// by revoking every device while OwnerCount stays > 1.
func (s *Store) OwnerDeviceCount() (int, error) {
	var n int
	err := s.SQL.QueryRow(liveOwnerDevicesSQL).Scan(&n)
	if err == nil && AfterOwnerDeviceCount != nil {
		AfterOwnerDeviceCount()
	}
	return n, err
}

// OwnerDeviceCountExceptMember is live owner devices that would remain if
// memberID were fully revoked. revoke-member 409 keys off this so a leftover
// owner member with zero devices cannot be used to kick the last live owner.
func (s *Store) OwnerDeviceCountExceptMember(memberID string) (int, error) {
	var n int
	err := s.SQL.QueryRow(liveOwnerDevicesSQL+` AND d.member_id != ?`, memberID).Scan(&n)
	if err == nil && AfterOwnerDeviceCount != nil {
		AfterOwnerDeviceCount()
	}
	return n, err
}

func (s *Store) InsertInvite(id, token, createdBy string, expires time.Time) error {
	sum := sha256.Sum256([]byte(token))
	_, err := s.SQL.Exec(
		`INSERT INTO invites(invite_id, token_hash, created_by, expires_at) VALUES(?,?,?,?)`,
		id, hex.EncodeToString(sum[:]), createdBy, expires.UTC().Format(time.RFC3339),
	)
	return err
}

func (s *Store) lookupUnusedInvite(token string) (string, error) {
	sum := sha256.Sum256([]byte(token))
	hash := hex.EncodeToString(sum[:])
	var id, expires string
	var used sql.NullString
	err := s.SQL.QueryRow(
		`SELECT invite_id, expires_at, used_at FROM invites WHERE token_hash = ?`, hash,
	).Scan(&id, &expires, &used)
	if err == sql.ErrNoRows {
		return "", fmt.Errorf("unknown token")
	}
	if err != nil {
		return "", err
	}
	if used.Valid {
		return "", fmt.Errorf("invite already used")
	}
	exp, err := time.Parse(time.RFC3339, expires)
	if err != nil {
		return "", err
	}
	if time.Now().After(exp) {
		return "", fmt.Errorf("invite expired")
	}
	return id, nil
}

// PeekInvite reports whether token is a live unused invite without consuming it.
func (s *Store) PeekInvite(token string) error {
	_, err := s.lookupUnusedInvite(token)
	return err
}

func (s *Store) ConsumeToken(token string) (string, error) {
	id, err := s.lookupUnusedInvite(token)
	if err != nil {
		return "", err
	}
	res, err := s.SQL.Exec(`UPDATE invites SET used_at = ? WHERE invite_id = ? AND used_at IS NULL`, nowRFC(), id)
	if err != nil {
		return "", err
	}
	n, _ := res.RowsAffected()
	if n != 1 {
		return "", fmt.Errorf("invite already used")
	}
	return id, nil
}

func (s *Store) ListMembers() ([]Member, error) {
	rows, err := s.SQL.Query(`SELECT member_id, display_name, role, revoked_at FROM members`)
	if err != nil {
		return nil, err
	}
	defer rows.Close()
	var out []Member
	for rows.Next() {
		var m Member
		var revoked sql.NullString
		var name sql.NullString
		if err := rows.Scan(&m.ID, &name, &m.Role, &revoked); err != nil {
			return nil, err
		}
		if name.Valid {
			m.DisplayName = name.String
		}
		m.Revoked = revoked.Valid
		out = append(out, m)
	}
	return out, rows.Err()
}

func (s *Store) ListDevices() ([]Device, error) {
	rows, err := s.SQL.Query(`SELECT device_id, member_id, public_identity, sign_public, COALESCE(last_seen,''), revoked_at FROM devices`)
	if err != nil {
		return nil, err
	}
	defer rows.Close()
	var out []Device
	for rows.Next() {
		var d Device
		var revoked sql.NullString
		if err := rows.Scan(&d.ID, &d.MemberID, &d.PublicIdentity, &d.SignPublic, &d.LastSeen, &revoked); err != nil {
			return nil, err
		}
		d.Revoked = revoked.Valid
		out = append(out, d)
	}
	return out, rows.Err()
}

func (s *Store) RevokeMember(id string) error {
	now := nowRFC()
	if _, err := s.SQL.Exec(`UPDATE members SET revoked_at = ? WHERE member_id = ? AND revoked_at IS NULL`, now, id); err != nil {
		return err
	}
	_, err := s.SQL.Exec(`UPDATE devices SET revoked_at = ? WHERE member_id = ? AND revoked_at IS NULL`, now, id)
	return err
}

// RevokeMemberGuarded revokes a live member and their devices in one IMMEDIATE
// transaction. Unknown or already-revoked members return ErrNotFound. If
// revoking this member would leave no live owner device, it returns
// ErrLastOwner. Two concurrent two-owner revoke-member calls cannot both
// succeed: the write lock is taken before the remaining-owner-device count
// and held through both UPDATEs.
func (s *Store) RevokeMemberGuarded(id string) error {
	ctx := context.Background()
	conn, err := s.SQL.Conn(ctx)
	if err != nil {
		return err
	}
	defer conn.Close()
	if _, err := conn.ExecContext(ctx, "BEGIN IMMEDIATE"); err != nil {
		return err
	}
	committed := false
	defer func() {
		if !committed {
			_, _ = conn.ExecContext(ctx, "ROLLBACK")
		}
	}()

	var role string
	var revoked sql.NullString
	err = conn.QueryRowContext(ctx,
		`SELECT role, revoked_at FROM members WHERE member_id = ?`, id,
	).Scan(&role, &revoked)
	if errors.Is(err, sql.ErrNoRows) {
		return ErrNotFound
	}
	if err != nil {
		return err
	}
	if revoked.Valid {
		return ErrNotFound
	}
	if role == "owner" {
		var n int
		if err := conn.QueryRowContext(ctx, liveOwnerDevicesSQL+` AND d.member_id != ?`, id).Scan(&n); err != nil {
			return err
		}
		if n == 0 {
			return ErrLastOwner
		}
	}
	now := nowRFC()
	if _, err := conn.ExecContext(ctx,
		`UPDATE members SET revoked_at = ? WHERE member_id = ? AND revoked_at IS NULL`,
		now, id); err != nil {
		return err
	}
	if _, err := conn.ExecContext(ctx,
		`UPDATE devices SET revoked_at = ? WHERE member_id = ? AND revoked_at IS NULL`,
		now, id); err != nil {
		return err
	}
	if _, err := conn.ExecContext(ctx, "COMMIT"); err != nil {
		return err
	}
	committed = true
	return nil
}

func (s *Store) RevokeDevice(id string) error {
	_, err := s.SQL.Exec(`UPDATE devices SET revoked_at = ? WHERE device_id = ? AND revoked_at IS NULL`, nowRFC(), id)
	return err
}

// RevokeDeviceGuarded revokes a live device in one IMMEDIATE transaction.
// Unknown or already-revoked devices return ErrNotFound. The last live owner
// device returns ErrLastOwner. Two concurrent owner-device revokes cannot both
// succeed: the write lock is taken before the count and held through UPDATE.
func (s *Store) RevokeDeviceGuarded(id string) error {
	ctx := context.Background()
	conn, err := s.SQL.Conn(ctx)
	if err != nil {
		return err
	}
	defer conn.Close()
	if _, err := conn.ExecContext(ctx, "BEGIN IMMEDIATE"); err != nil {
		return err
	}
	committed := false
	defer func() {
		if !committed {
			_, _ = conn.ExecContext(ctx, "ROLLBACK")
		}
	}()

	var revoked sql.NullString
	var role string
	var memberRevoked sql.NullString
	err = conn.QueryRowContext(ctx, `
		SELECT d.revoked_at, m.role, m.revoked_at
		FROM devices d
		JOIN members m ON m.member_id = d.member_id
		WHERE d.device_id = ?`, id).Scan(&revoked, &role, &memberRevoked)
	if errors.Is(err, sql.ErrNoRows) {
		return ErrNotFound
	}
	if err != nil {
		return err
	}
	if revoked.Valid {
		return ErrNotFound
	}
	if role == "owner" && !memberRevoked.Valid {
		var n int
		if err := conn.QueryRowContext(ctx, liveOwnerDevicesSQL).Scan(&n); err != nil {
			return err
		}
		if n <= 1 {
			return ErrLastOwner
		}
	}
	res, err := conn.ExecContext(ctx,
		`UPDATE devices SET revoked_at = ? WHERE device_id = ? AND revoked_at IS NULL`,
		nowRFC(), id)
	if err != nil {
		return err
	}
	affected, err := res.RowsAffected()
	if err != nil {
		return err
	}
	if affected != 1 {
		return ErrNotFound
	}
	if _, err := conn.ExecContext(ctx, "COMMIT"); err != nil {
		return err
	}
	committed = true
	return nil
}

type MailboxRow struct {
	MessageID   string
	RecipientID string
	SenderID    string
	Blob        []byte
}

func (s *Store) PutMailbox(messageID, recipient, sender string, blob []byte, ttl time.Duration) error {
	_, err := s.SQL.Exec(
		`INSERT OR REPLACE INTO mailbox(message_id, recipient_device_id, sender_device_id, encrypted_blob, created_at, expires_at, delivery_state)
		 VALUES(?,?,?,?,?,?, 'pending')`,
		messageID, recipient, sender, blob, nowRFC(), time.Now().Add(ttl).UTC().Format(time.RFC3339),
	)
	return err
}

func (s *Store) PendingMailbox(recipient string) ([]MailboxRow, error) {
	_, _ = s.SQL.Exec(`DELETE FROM mailbox WHERE expires_at < ?`, nowRFC())
	rows, err := s.SQL.Query(
		`SELECT message_id, recipient_device_id, sender_device_id, encrypted_blob
		 FROM mailbox WHERE recipient_device_id = ? AND delivery_state = 'pending'`, recipient,
	)
	if err != nil {
		return nil, err
	}
	defer rows.Close()
	var out []MailboxRow
	for rows.Next() {
		var r MailboxRow
		if err := rows.Scan(&r.MessageID, &r.RecipientID, &r.SenderID, &r.Blob); err != nil {
			return nil, err
		}
		out = append(out, r)
	}
	return out, rows.Err()
}

func (s *Store) AckMailbox(messageID, recipient string) (MailboxRow, error) {
	var r MailboxRow
	err := s.SQL.QueryRow(
		`SELECT message_id, recipient_device_id, sender_device_id, encrypted_blob FROM mailbox WHERE message_id = ?`,
		messageID,
	).Scan(&r.MessageID, &r.RecipientID, &r.SenderID, &r.Blob)
	if err != nil {
		return r, err
	}
	if r.RecipientID != recipient {
		return r, fmt.Errorf("ack recipient mismatch")
	}
	_, err = s.SQL.Exec(`DELETE FROM mailbox WHERE message_id = ?`, messageID)
	return r, err
}

func (s *Store) MailboxCount() (int, error) {
	var n int
	err := s.SQL.QueryRow(`SELECT COUNT(*) FROM mailbox`).Scan(&n)
	return n, err
}

func (s *Store) MemberCount() (int, error) {
	var n int
	err := s.SQL.QueryRow(`SELECT COUNT(*) FROM members WHERE revoked_at IS NULL`).Scan(&n)
	return n, err
}

func (s *Store) ScanPlaintext(needle string) (int, error) {
	var n int
	err := s.SQL.QueryRow(`SELECT COUNT(*) FROM mailbox WHERE instr(encrypted_blob, ?) > 0`, needle).Scan(&n)
	return n, err
}

func nowRFC() string { return time.Now().UTC().Format(time.RFC3339) }

type ObjectMeta struct {
	ID        string
	Uploader  string
	Size      int64
	SHA256    string
	ExpiresAt string
}

func (s *Store) InsertObject(id, uploader, sha string, size int64, ttl time.Duration) error {
	_, err := s.SQL.Exec(
		`INSERT INTO objects(object_id, uploader_device_id, size_bytes, sha256, created_at, expires_at) VALUES(?,?,?,?,?,?)`,
		id, uploader, size, sha, nowRFC(), time.Now().Add(ttl).UTC().Format(time.RFC3339),
	)
	return err
}

func (s *Store) Object(id string) (ObjectMeta, error) {
	var o ObjectMeta
	err := s.SQL.QueryRow(
		`SELECT object_id, uploader_device_id, size_bytes, COALESCE(sha256,''), expires_at FROM objects WHERE object_id = ?`, id,
	).Scan(&o.ID, &o.Uploader, &o.Size, &o.SHA256, &o.ExpiresAt)
	return o, err
}

func (s *Store) ObjectBytesSum() (int64, error) {
	var n int64
	err := s.SQL.QueryRow(`SELECT COALESCE(SUM(size_bytes),0) FROM objects`).Scan(&n)
	return n, err
}

func (s *Store) ExpiredObjects() ([]ObjectMeta, error) {
	rows, err := s.SQL.Query(`SELECT object_id, uploader_device_id, size_bytes, COALESCE(sha256,''), expires_at FROM objects WHERE expires_at < ?`, nowRFC())
	if err != nil {
		return nil, err
	}
	defer rows.Close()
	var out []ObjectMeta
	for rows.Next() {
		var o ObjectMeta
		if err := rows.Scan(&o.ID, &o.Uploader, &o.Size, &o.SHA256, &o.ExpiresAt); err != nil {
			return nil, err
		}
		out = append(out, o)
	}
	return out, rows.Err()
}

func (s *Store) DeleteObject(id string) error {
	_, err := s.SQL.Exec(`DELETE FROM objects WHERE object_id = ?`, id)
	return err
}

func (s *Store) ObjectCount() (int, error) {
	var n int
	err := s.SQL.QueryRow(`SELECT COUNT(*) FROM objects`).Scan(&n)
	return n, err
}

type Group struct {
	ID        string
	Name      string
	CreatedBy string
	Epoch     uint32
}

func (s *Store) InsertGroup(id, name, createdBy string, epoch uint32) error {
	_, err := s.SQL.Exec(
		`INSERT INTO chat_groups(group_id, name, created_by, epoch, created_at) VALUES(?,?,?,?,?)`,
		id, name, createdBy, epoch, nowRFC(),
	)
	return err
}

func (s *Store) UpdateGroupName(id, name string) error {
	_, err := s.SQL.Exec(`UPDATE chat_groups SET name = ? WHERE group_id = ?`, name, id)
	return err
}

func (s *Store) Group(id string) (Group, error) {
	var g Group
	err := s.SQL.QueryRow(`SELECT group_id, name, created_by, epoch FROM chat_groups WHERE group_id = ?`, id).
		Scan(&g.ID, &g.Name, &g.CreatedBy, &g.Epoch)
	return g, err
}

func (s *Store) ListGroupsForDevice(deviceID string) ([]Group, error) {
	rows, err := s.SQL.Query(
		`SELECT g.group_id, g.name, g.created_by, g.epoch FROM chat_groups g
		 JOIN group_members m ON m.group_id = g.group_id
		 WHERE m.device_id = ? AND m.removed_at IS NULL`, deviceID,
	)
	if err != nil {
		return nil, err
	}
	defer rows.Close()
	var out []Group
	for rows.Next() {
		var g Group
		if err := rows.Scan(&g.ID, &g.Name, &g.CreatedBy, &g.Epoch); err != nil {
			return nil, err
		}
		out = append(out, g)
	}
	return out, rows.Err()
}

func (s *Store) AddGroupMember(groupID, deviceID string) error {
	_, err := s.SQL.Exec(
		`INSERT INTO group_members(group_id, device_id, added_at) VALUES(?,?,?)
		 ON CONFLICT(group_id, device_id) DO UPDATE SET removed_at = NULL, added_at = excluded.added_at`,
		groupID, deviceID, nowRFC(),
	)
	return err
}

func (s *Store) RemoveGroupMember(groupID, deviceID string) error {
	_, err := s.SQL.Exec(
		`UPDATE group_members SET removed_at = ? WHERE group_id = ? AND device_id = ? AND removed_at IS NULL`,
		nowRFC(), groupID, deviceID,
	)
	return err
}

func (s *Store) BumpGroupEpoch(groupID string) (uint32, error) {
	_, err := s.SQL.Exec(`UPDATE chat_groups SET epoch = epoch + 1 WHERE group_id = ?`, groupID)
	if err != nil {
		return 0, err
	}
	var epoch uint32
	err = s.SQL.QueryRow(`SELECT epoch FROM chat_groups WHERE group_id = ?`, groupID).Scan(&epoch)
	return epoch, err
}

func (s *Store) GroupMembers(groupID string) ([]string, error) {
	rows, err := s.SQL.Query(`SELECT device_id FROM group_members WHERE group_id = ? AND removed_at IS NULL`, groupID)
	if err != nil {
		return nil, err
	}
	defer rows.Close()
	var out []string
	for rows.Next() {
		var id string
		if err := rows.Scan(&id); err != nil {
			return nil, err
		}
		out = append(out, id)
	}
	return out, rows.Err()
}

func (s *Store) IsGroupMember(groupID, deviceID string) (bool, error) {
	var n int
	err := s.SQL.QueryRow(
		`SELECT COUNT(*) FROM group_members WHERE group_id = ? AND device_id = ? AND removed_at IS NULL`,
		groupID, deviceID,
	).Scan(&n)
	return n > 0, err
}

func (s *Store) GroupCount() (int, error) {
	var n int
	err := s.SQL.QueryRow(`SELECT COUNT(*) FROM chat_groups`).Scan(&n)
	return n, err
}

func (s *Store) DeviceCount() (int, error) {
	var n int
	err := s.SQL.QueryRow(`SELECT COUNT(*) FROM devices WHERE revoked_at IS NULL`).Scan(&n)
	return n, err
}
