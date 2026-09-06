package db

import (
	"crypto/sha256"
	"database/sql"
	"encoding/hex"
	"fmt"
	"time"

	_ "modernc.org/sqlite"
)

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
	ID              string
	MemberID        string
	PublicIdentity  []byte
	SignPublic      []byte
	LastSeen        string
	Revoked         bool
}

func (s *Store) InsertMember(id, name, role string) error {
	_, err := s.SQL.Exec(
		`INSERT INTO members(member_id, display_name, role, created_at) VALUES(?,?,?,?)`,
		id, name, role, nowRFC(),
	)
	return err
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
	var n int
	err := s.SQL.QueryRow(`SELECT COUNT(*) FROM members WHERE role = 'owner' AND revoked_at IS NULL`).Scan(&n)
	return n > 0, err
}

func (s *Store) InsertInvite(id, token, createdBy string, expires time.Time) error {
	sum := sha256.Sum256([]byte(token))
	_, err := s.SQL.Exec(
		`INSERT INTO invites(invite_id, token_hash, created_by, expires_at) VALUES(?,?,?,?)`,
		id, hex.EncodeToString(sum[:]), createdBy, expires.UTC().Format(time.RFC3339),
	)
	return err
}

func (s *Store) ConsumeToken(token string) (string, error) {
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

func (s *Store) RevokeDevice(id string) error {
	_, err := s.SQL.Exec(`UPDATE devices SET revoked_at = ? WHERE device_id = ? AND revoked_at IS NULL`, nowRFC(), id)
	return err
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
