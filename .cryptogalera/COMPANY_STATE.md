# CryptoGalera Company State

PO-owned live snapshot. Not a changelog.

## Product Goal

**CONFIRMED:** Rope is a private self-hosted Android E2EE messenger. An organizer provisions a VPS, invites people via QR / `rope://join`, and exchanges encrypted text, voice, photos, files, groups, and calls through a Go relay that never sees plaintext.

**Trunk (D-005 A):** GitHub Latest **`v0.2.15`** hangs on **`main`**. Release assets were not retagged or replaced.

## Migration Status

**ADOPTED_EXISTING_WORKSPACE.** Control layer + shipped Stage-2 product are both on `main`.

## Current Phase

Stabilization on the promoted trunk. New feature implementation frozen (CG-007 BACKLOG; CG-003 freeze for overlapping agents).

## Existing System

Kotlin Compose Android client → Rust UniFFI (`rope_core`) → HTTPS/WSS Go `rope-server` + SQLite → `deployment/scripts/install.sh`. Trunk protocol includes objects, groups, call signaling. Android `versionName=0.2.15` `versionCode=23`.

Doc drift remains: architecture.md may still say Room; code is `SQLiteOpenHelper` (CG-014).

## Technology Stack

Unchanged: Android/Kotlin, Rust UniFFI, Go+SQLite, Ed25519/X25519/HKDF/XChaCha20-Poly1305, self-signed TLS + pin.

## Completed Capabilities

`main` @ `a869927` matches tag `v0.2.15` product trees plus CryptoGalera wrap. Published artifacts: `rope-0.2.15-debug.apk`, `rope-server-linux-amd64`, `rope-server-linux-arm64`, `SHA256SUMS`.

## Work In Progress / BACKLOG

- CG-012 Settings, CG-013 revoke UI, CG-014 Room vs SQLite docs
- Unique unshipped chrome on `cursor/telegram-chrome-872f` (`039e381`) — preserved, not merged

## Active Risks

See [RISKS.md](RISKS.md). R-001 (dual line) **closed**. R-009 unique chrome branch remains. R-010 do not retag.

## Current Priorities

1. Do not retag or replace `v0.2.15` assets
2. Do not merge unique chrome unless the user asks
3. No new overlapping feature agents
4. CG-012 / CG-013 / CG-014 when unfrozen

## Active Organization

```
User
 └── PO
```

ENG-LEAD released after CG-004. REV-01 released after CG-018.

## Canonical git

- Default branch: **`main`** @ `a869927`
- Product tip contained: tag `v0.2.15` = `9694e804691bfb32b9a2ecc9e1f9e7e2cf48f0a5`
- Remaining extra branch: `origin/cursor/telegram-chrome-872f` only
