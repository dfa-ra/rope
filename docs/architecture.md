# Rope architecture

Rope is a private self-hosted messenger. The Android app provisions a personal VPS, then two or more devices on that server exchange 1-to-1 E2EE text messages. The server authenticates devices and relays encrypted envelopes. It never sees plaintext and never stores conversation history in the clear.

## Components

| Layer | Location | Responsibility |
| --- | --- | --- |
| Android UI | `apps/android` | Screens, lifecycle, HTTPS/WSS, QR, SSH provisioning, local history |
| Rust core | `core/rust` | Device identity, keys, envelope, encrypt/sign/verify, invite + fingerprint checks |
| Go relay | `server/go` | REST + WSS, members/devices, invites, encrypted mailbox, encrypted objects, groups, call signaling, ICE advertisement, health |
| Deployment | `deployment/` | Idempotent VPS install, systemd, TLS material, coturn on the same host |

Kotlin never implements cryptography and never touches raw private keys. It calls the UniFFI API exported by `rope_core` and stores the serialized identity blob after wrapping it with an Android Keystore key.

## Data flow

```
Organizer Android                 VPS                         Guest Android
┌─────────────────┐         ┌──────────────────┐         ┌─────────────────┐
│ Compose UI      │  HTTPS  │ Go rope-server   │  HTTPS  │ Compose UI      │
│ Room (local)    │◄───────►│ SQLite           │◄───────►│ Room (local)    │
│ Rust UniFFI     │  WSS    │ encrypted mailbox│  WSS    │ Rust UniFFI     │
└─────────────────┘         └──────────────────┘         └─────────────────┘
        │ SSH provision only          │
        └─────────────────────────────┘
```

1. Organizer enters VPS SSH credentials. The app uploads `rope-server` and runs `deployment/scripts/install.sh`.
2. Installer creates a system user, self-signed TLS cert, `setup_token`, systemd unit, coturn (TURN/TURNS on the same IP), and a health endpoint.
3. The app pins the TLS fingerprint, bootstraps the owner device, and stores the server profile locally.
4. Organizer creates a one-time invite. The QR/deep link carries endpoint, server id, fingerprint, and token.
5. Guest device generates identity in Rust, exchanges the invite for membership, then chats over WSS.

## Trust boundaries

- **Device**: holds Ed25519 + X25519 keys. Private material never leaves the device in ordinary operation.
- **Server**: stores public identities, invite token hashes, and opaque mailbox blobs. It routes by envelope header fields and verifies the sender signature against the registered device key.
- **Transport**: TLS protects the channel. E2EE protects content from the server. TURN on the VPS relays DTLS-SRTP ciphertext for calls; it does not terminate media.
- **Invite link**: binds the guest to `server_id` + TLS fingerprint so a swapped endpoint is rejected.

## Protocol version

Application protocol version is `1`. Envelopes and handshake payloads carry this number. Mismatched versions are rejected; the client asks the user to update the app or server core.

See [protocol/docs](../protocol/docs) for binary layout, REST, WSS, and invite URL formats.
