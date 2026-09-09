# Product — Rope

Owner: Founder / Product Owner. This file is the product contract. If it conflicts with a prompt, a cloud-agent name, or an older MVP description, **this file plus [README.md](../README.md) win**.

## Goal

Rope is a **private self-hosted E2EE messenger for Android**. An organizer provisions a personal VPS from the app, invites people with a QR / deep link, and exchanges encrypted text, voice, photos, files, group chats, and calls through a Go relay that never sees plaintext.

Stack (not negotiable without the user): **Kotlin UI → Rust UniFFI security core → HTTPS/WSS → Go + SQLite**.

## Canonical line (D-005 A)

The product trunk is **`main`**, shipping **0.3.25**. GitHub Releases `v0.2.15`, `v0.3.0`, `v0.3.1`, `v0.3.2`, `v0.3.3`, `v0.3.4`, `v0.3.5`, `v0.3.6`, `v0.3.7`, `v0.3.8`, `v0.3.9`, `v0.3.10`, `v0.3.11`, `v0.3.12`, `v0.3.13`, `v0.3.14`, `v0.3.15`, `v0.3.16`, `v0.3.17`, `v0.3.18`, `v0.3.19`, `v0.3.20`, `v0.3.21`, `v0.3.22`, `v0.3.23`, and `v0.3.24` remain published history.

| Line | What it is |
| --- | --- |
| **`main`** | Shipped tree: Android `versionName=0.3.25` / `versionCode=49`, plus CryptoGalera operating layer |
| **GitHub Release `v0.2.15`** | Previous line. Do not retag `v0.2.15`. |
| **GitHub Release `v0.3.0`** | Previous line. Do not retag `v0.3.0`. |
| **GitHub Release `v0.3.1`** | Previous line. Do not retag `v0.3.1`. |
| **GitHub Release `v0.3.2`** | Previous line. Do not retag `v0.3.2`. |
| **GitHub Release `v0.3.3`** | Previous line. Do not retag `v0.3.3`. |
| **GitHub Release `v0.3.4`** | Previous line. Do not retag `v0.3.4`. |
| **GitHub Release `v0.3.5`** | Previous line. Do not retag `v0.3.5`. |
| **GitHub Release `v0.3.6`** | Previous line. Do not retag `v0.3.6`. |
| **GitHub Release `v0.3.7`** | Previous line. Do not retag `v0.3.7`. |
| **GitHub Release `v0.3.8`** | Previous line. Do not retag `v0.3.8`. |
| **GitHub Release `v0.3.9`** | Previous line. Do not retag `v0.3.9`. |
| **GitHub Release `v0.3.10`** | Previous line. Do not retag `v0.3.10`. |
| **GitHub Release `v0.3.11`** | Previous line. Do not retag `v0.3.11`. |
| **GitHub Release `v0.3.12`** | Previous line. Do not retag `v0.3.12`. |
| **GitHub Release `v0.3.13`** | Previous line. Do not retag `v0.3.13`. |
| **GitHub Release `v0.3.14`** | Previous line. Do not retag `v0.3.14`. |
| **GitHub Release `v0.3.15`** | Previous line. Do not retag `v0.3.15`. |
| **GitHub Release `v0.3.16`** | Previous line. Do not retag `v0.3.16`. |
| **GitHub Release `v0.3.17`** | Previous line. Do not retag `v0.3.17`. |
| **GitHub Release `v0.3.18`** | Previous line. Do not retag `v0.3.18`. |
| **GitHub Release `v0.3.19`** | Previous line. Do not retag `v0.3.19`. |
| **GitHub Release `v0.3.20`** | Previous line. Do not retag `v0.3.20`. |
| **GitHub Release `v0.3.21`** | Previous line. Do not retag `v0.3.21`. |
| **GitHub Release `v0.3.22`** | Previous line. Do not retag `v0.3.22`. |
| **GitHub Release `v0.3.23`** | Previous line. Do not retag `v0.3.23`. |
| **GitHub Release `v0.3.24`** | Previous Latest until `v0.3.25` publishes. Do not retag `v0.3.24`. |

Older tags `v0.1.0`–`v0.2.14` remain published history. They are ancestors of `v0.2.15`.

Unique unshipped UI on `cursor/telegram-chrome-872f` (`039e381`) is **not** part of this trunk.

## Success

The product succeeds when a non-developer organizer can:

1. Install the Android app (this cycle ships `v0.3.25`).
2. Provision (or point at) a VPS and complete owner bootstrap with `setup_token`.
3. Invite others via QR / `rope://join?...` with TLS fingerprint binding.
4. Exchange E2EE text (and the Stage-2 media/group/call features already in this tree).
5. Trust the guarantees in [threat-model.md](threat-model.md) (server cannot read plaintext; cannot forge as another device).

Engineering success: `cargo test` (Rust), `go test ./...` (relay), `./gradlew test` (Android) green on `main`.

## In scope (current trunk)

- Android client: provision, join, chats, groups, media, call UI already in `v0.2.15`
- **0.3.24 Replies 2.0:** left-swipe to reply plus optional quote-span (`qt` / `qo` inside encrypted JSON)
- **0.3.25 1:1 video calls:** camera + WebRTC video tracks, `OfferToReceiveVideo=true`, in-call surface on the existing WSS/WebRTC path (16 KiB SDP cap unchanged)
- Device identity and envelope crypto in Rust (UniFFI); Kotlin never implements crypto
- Go relay: REST + WSS mailbox, members/devices, invites, objects, groups, call signaling as in this tree
- Self-signed TLS + client fingerprint pin; debug `--allow-http` only for local/emulator
- Local message history on device; opaque mailbox blobs on server
- Idempotent VPS installer + systemd unit (TURN/coturn as in shipped installer)
- Marketing landing at `web/index.html`
- GitHub Actions CI and tagged release artifacts from `main`

## Out of scope

Do not implement these because a prompt mentioned them:

- iOS, web app as a messenger, desktop
- Federation
- Signal/X3DH ratchet / MLS / forward secrecy
- Public CA instead of pin-or-warn self-signed TLS
- New overlapping chrome/calls/landing agents (D-006 freeze)

## What CryptoGalera is optimizing for

Professional process: one product contract, one merge trunk, one accountable owner per task, independent review for crypto/auth/protocol. See [company.md](company.md) and [AGENTS.md](../AGENTS.md).
