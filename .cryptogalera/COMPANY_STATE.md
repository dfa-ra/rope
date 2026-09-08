# CryptoGalera Company State

PO-owned live snapshot. Not a changelog.

## Product Goal

**CONFIRMED:** Rope is a private self-hosted 1-to-1 Android E2EE messenger. An organizer provisions a VPS, invites a guest via QR / `rope://join`, and exchanges encrypted text through a Go relay that never sees plaintext.

**INFERRED (not confirmed):** Users may eventually want calls, groups, Telegram-like UI (those exist off-tree as Stage-2). **UNKNOWN:** which trunk the user wants (D-005).

## Migration Status

**ADOPTED_EXISTING_WORKSPACE** — control layer is **on `main`** (PR #4 merged, `99956d8`).

This is not a new project. CryptoGalera wraps Rope. Existing product trees were not rewritten.

## Current Phase

Stabilization. Product expansion (Stage-2) still frozen until D-005. MVP tree on `main` includes CryptoGalera memory/rules.

## Existing System

Kotlin Compose Android client → Rust UniFFI (`rope_core`) for identity/crypto/invites → HTTPS/WSS Go `rope-server` + SQLite mailbox → `deployment/scripts/install.sh` on a personal VPS. Protocol v1, text envelopes only **in this tree**.

Architecture evidence: `docs/architecture.md` plus code. Doc says Room; code uses `SQLiteOpenHelper` (`LocalStore.kt`) — doc drift (CG-014).

## Technology Stack

Unchanged: Android/Kotlin, Rust UniFFI, Go+SQLite, Ed25519/X25519/HKDF/XChaCha20-Poly1305, self-signed TLS + pin.

## Completed Capabilities

Unchanged vs MVP. Post-merge validation on `main` @ `99956d8`: `cargo test` 11 passed; `go test ./...` ok. Android Gradle not run here (no SDK); GitHub CI android was green on PR #4.

## Work In Progress / unfinished in-tree

- Settings stub (CG-012), revoke UI (CG-013), Room vs SQLite docs (CG-014)
- Stage-2 off-tree: PRs #2/#3, tags `v0.2.x` — **not merged**

## Active Risks

See [RISKS.md](RISKS.md). Highest: dual product line (R-001). Merge freeze on Stage-2 remains.

## Current Priorities

1. User: D-005 trunk A / B / C
2. Do not merge PRs #2/#3 until D-005
3. Preserve Stage-2 branches (unique unverified work)

## Active Organization

```
User
 └── PO
```

## Canonical git

- Default branch: **`main`** (not `master`)
- HEAD after CG-017: `99956d8` (CryptoGalera wrap + MVP)
