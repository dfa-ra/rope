# CryptoGalera Company State

PO-owned live snapshot. Not a changelog.

## Product Goal

**CONFIRMED:** Rope is a private self-hosted Android E2EE messenger. An organizer provisions a VPS, invites people via QR / `rope://join`, and exchanges encrypted text (and, on the shipped trunk, voice/photos/files/groups/calls) through a Go relay that never sees plaintext.

**Trunk (D-005 A, user 2026-09-08):** GitHub Latest release **`v0.2.15`** is the product line to hang on `main`. Existing release assets stay; do not retag.

## Migration Status

**ADOPTED_EXISTING_WORKSPACE** — control layer is **on `main`** (PR #4). Product trunk promotion **in progress** (CG-004): merge `v0.2.15` (`9694e80`) into `main` while keeping `cryptogalera/`, `.cryptogalera/`, `.cursor/`.

## Current Phase

Trunk promotion (T-004 / CG-004). New feature implementation remains frozen (CG-007 BACKLOG; CG-003 freeze for overlapping agents).

## Existing System

Kotlin Compose Android client → Rust UniFFI (`rope_core`) for identity/crypto/invites → HTTPS/WSS Go `rope-server` + SQLite mailbox → `deployment/scripts/install.sh` on a personal VPS. Shipped trunk (tag `v0.2.15`): protocol includes objects, groups, call signaling; Android `versionName=0.2.15` `versionCode=23`.

Architecture evidence: `docs/architecture.md` plus code. Doc may still say Room; code uses `SQLiteOpenHelper` (`LocalStore.kt`) — doc drift (CG-014).

## Technology Stack

Unchanged: Android/Kotlin, Rust UniFFI, Go+SQLite, Ed25519/X25519/HKDF/XChaCha20-Poly1305, self-signed TLS + pin.

## Completed Capabilities

CryptoGalera wrap on `main` @ `c6c1015`. Stage-2 product is published as GitHub Release `v0.2.15` (APK + `rope-server-linux-amd64` / `arm64` + `SHA256SUMS`). Integration onto `main` is CG-004.

## Work In Progress

- CG-004: merge shipped tip onto `main`
- CG-018: independent review of that merge
- CG-019: delete only fully contained `cursor/*` branches; keep `telegram-chrome-872f` (1 unique commit `039e381`)
- CG-012 / CG-013 / CG-014 still BACKLOG

## Active Risks

See [RISKS.md](RISKS.md). R-001 (dual line) is being closed by D-005 A. Do not delete GitHub Releases. Do not merge unique chrome work into this promotion.

## Current Priorities

1. Land `v0.2.15` product trees on `main` without dropping CryptoGalera
2. Prove `apps/ core/ server/ deployment/ protocol/` match the tag
3. Keep published release assets; close contained PRs #2/#3
4. Delete contained branches only

## Active Organization

```
User
 └── PO
      └── ENG-LEAD (CG-004, seated this cycle)
           └── REV-01 (CG-018, after merge; reviewer ≠ author)
```

## Canonical git

- Default branch: **`main`** (not `master`)
- Pre-promotion HEAD: `c6c1015` (CryptoGalera wrap + MVP)
- Integration tip: tag `v0.2.15` = `9694e804691bfb32b9a2ecc9e1f9e7e2cf48f0a5`
- Working branch: `cursor/promote-release-main-ae19`
