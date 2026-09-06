# Rope threat model (MVP)

## Assets

- Device private keys (Ed25519 signing, X25519 ECDH)
- Message plaintext (only on endpoints)
- Invite tokens (single use, short TTL)
- SSH credentials used only during VPS provisioning
- Server TLS private key and `setup_token`

## Trusted

- The organizer's Android device and the guest devices they invite
- The Rust core implementation and the chosen primitives (Ed25519, X25519, HKDF-SHA256, XChaCha20-Poly1305)
- Android Keystore for wrapping the serialized identity blob

## Untrusted

- The VPS operator / hosting provider
- The Go process and its SQLite file
- The network path between devices and the VPS
- Anyone who can view a QR code after it has been shown (invite is a capability)

## Guarantees (MVP)

- The server cannot read message plaintext (E2EE).
- The server cannot forge a message as another registered device (Ed25519 over the envelope).
- A guest cannot join a different host than the one named in the invite if the TLS fingerprint does not match.
- Invite tokens are stored as SHA-256 hashes and are single-use with TTL.
- Private device keys are not uploaded during ordinary messaging, bootstrap, or mailbox sync.
- Default logs omit payload bytes, invite tokens, and SSH secrets.

## Non-guarantees (honest limitations)

- The server and network provider see IP addresses, connection times, and traffic volume.
- The server sees envelope metadata needed for routing: version, type, message id, timestamp, sender device, recipient device, and ciphertext length.
- A compromised organizer device or a leaked SSH credential can take over the VPS.
- Self-signed TLS plus fingerprint pinning does not replace a public CA after a user ignores a mismatch warning.
- There is no forward-secrecy ratchet (Signal/X3DH) in MVP. Compromise of a device ECDH key exposes future messages to that device until keys are rotated (out of scope).
- Offline mailbox blobs live on disk until delivery ack or TTL expiry. They remain encrypted, but metadata remains.

## Abuse controls

- Authenticated REST and WSS (device signature over method/path/timestamp/body).
- Max envelope size (64 KiB).
- Per-IP rate limits on bootstrap, invite creation, and WSS sends.
- Revocation of members and devices by the owner.

## Incident notes

If a fingerprint mismatch is shown, the client must stop the connection. If an invite is expired or already used, the device must not register. If a mailbox blob expires, the message is treated as undelivered.
