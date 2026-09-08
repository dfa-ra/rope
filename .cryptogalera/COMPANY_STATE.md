# CryptoGalera Company State

PO-owned live snapshot. Not a changelog.

## Product Goal

**CONFIRMED:** Rope is a private self-hosted 1-to-1 Android E2EE messenger. An organizer provisions a VPS, invites a guest via QR / `rope://join`, and exchanges encrypted text through a Go relay that never sees plaintext.

**INFERRED (not confirmed):** Users may eventually want calls, groups, Telegram-like UI (those exist off-tree as Stage-2). **UNKNOWN:** which trunk the user wants (D-005).

## Migration Status

**ADOPTED_EXISTING_WORKSPACE**

This is not a new project. CryptoGalera wraps Rope. Existing files are legacy company assets.

## Current Phase

Stabilization of the control layer + **Discovery** of product trunk. Product coding frozen until D-005 or explicit CG-007.

## Existing System

Kotlin Compose Android client → Rust UniFFI (`rope_core`) for identity/crypto/invites → HTTPS/WSS Go `rope-server` + SQLite mailbox → `deployment/scripts/install.sh` on a personal VPS. Protocol v1, text envelopes only **in this tree**.

Architecture evidence: `docs/architecture.md` plus code. Doc says Room; code uses `SQLiteOpenHelper` (`LocalStore.kt`) — doc drift.

## Technology Stack

CONFIRMED from this checkout:

- Android / Kotlin (Compose), JDK 17+
- Rust 1.83+ UniFFI (`core/rust`)
- Go 1.22 + modernc SQLite (`server/go`)
- Ed25519, X25519, HKDF-SHA256, XChaCha20-Poly1305
- Self-signed TLS + client fingerprint pin
- Make + GitHub Actions CI

## Completed Capabilities

Evidence-backed **in this tree** (lib/server tests, not device E2E):

- Rust identity, text E2EE, invite URL, fingerprint checks — `cargo test`: **11 passed** (CG-011)
- Go relay REST/WSS mailbox, invites, auth, rate limits — `go test ./...`: **ok** (CG-011)
- Protocol docs v1; CI workflow definitions

Android UI flows exist (provision, join, chats, chat, invite) but are **UNVERIFIED** on device; unit tests in-tree are thin (`InviteCodecTest`).

## Work In Progress / unfinished in-tree

- Settings screen: stub `Text("Settings")`, unreachable — PARTIAL
- Owner revoke: server APIs exist; no Android client/UI — PARTIAL
- Admin status: JSON dump in Status pane — PARTIAL
- App `--upgrade` UI hardcoded `false` — PARTIAL
- No `jniLibs/` in tree; native `.so` must be built
- Architecture doc “Room” vs SQLite helper — docs drift

## Off-tree (preserve, do not merge)

Stage-2: PRs #2/#3, tags `v0.2.x` (calls, groups, media, Telegram chrome, landing). **UNVERIFIED** in this workspace.

## Active Risks

See [RISKS.md](RISKS.md). Highest: dual product line (R-001).

## Current Priorities

1. User: D-005 trunk A / B / C
2. Keep freeze on Stage-2 implementation
3. Do not rewrite MVP

## Active Organization

```
User
 └── PO
```

Reusable Cursor roles exist under `.cursor/agents/` but are **not seated** until a workstream needs them. ENG-LEAD not seated.

## Commands (discovered)

| Kind | Command | Notes |
| --- | --- | --- |
| Dev server | `make server` | Go `--allow-http` local |
| Test (default) | `make test` | rust + go only |
| Rust test | `cd core/rust && cargo test` | Baseline: 11 passed |
| Go test | `cd server/go && go test ./...` | Baseline: ok |
| Android test | `cd apps/android && ./gradlew test` | CI; **not run here** (no SDK) |
| Lint / typecheck | UNKNOWN as named targets | compile via build/test |
