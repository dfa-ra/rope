# Decisions

PO-owned. Reconstructed vs formally accepted are marked. Full prose: [docs/decisions.md](../docs/decisions.md).

## Reconstructed from existing implementation

These were already true in Rope **before** CryptoGalera. Not invented here.

| ID | Decision | Status | Evidence |
| --- | --- | --- | --- |
| D-002 | Kotlin UI → Rust UniFFI crypto → Go + SQLite relay | RECONSTRUCTED FROM EXISTING IMPLEMENTATION | `apps/android`, `core/rust`, `server/go`, README |
| D-003 | Ed25519 + X25519 + HKDF-SHA256 + XChaCha20-Poly1305; no ratchet | RECONSTRUCTED FROM EXISTING IMPLEMENTATION | `core/rust`, `docs/threat-model.md` |
| D-004 | Self-signed TLS + fingerprint pin; server never sees plaintext | RECONSTRUCTED FROM EXISTING IMPLEMENTATION | installer, `PinnedClient`, threat model |
| D-010 | Local history via Android SQLiteOpenHelper + Keystore wrap (not Room) | RECONSTRUCTED FROM EXISTING IMPLEMENTATION | `LocalStore.kt`, `IdentityVault.kt` vs architecture.md claiming Room |

## CryptoGalera formal

| ID | Status | Decision |
| --- | --- | --- |
| D-001 | **superseded by D-005 A** | Was: documented product is `main` MVP 0.1.0 until D-005 |
| D-005 | **accepted (A)** | Promote shipped Stage-2 line: merge tag `v0.2.15` (`9694e80`) into `main`. Keep existing GitHub Releases. Do not retag. |
| D-006 | accepted | One owner, one branch; no overlapping new Stage-2 feature agents |
| D-007 | accepted | Wrap Rope; do not replace; do not delete unique existing work |
| D-008 | accepted | `cryptogalera/RUNTIME.md` + `.cryptogalera/` PO memory; Workers never spawn |
| D-009 | accepted | ADOPTED_EXISTING_WORKSPACE; continue from this checkout; `.cursor/agents/` reusable roles only |

**D-005 A (2026-09-08):** User asked to put current releases on `main`, verify they match published artifacts, and tidy extra branches without losing work. Integration tip is GitHub Latest tag `v0.2.15` = `origin/cursor/release-0215-872f` = `9694e804691bfb32b9a2ecc9e1f9e7e2cf48f0a5`. Path is merge (not fast-forward): `main` has 6 CryptoGalera commits; tag has 92 Stage-2 commits from merge-base `30ebaa7`.

**CG-004 (2026-09-08):** Tag `v0.2.15` (`9694e80`) merged onto `main` (`0fb2ea5` then docs `a869927`). Product trees match the tag. GitHub Release assets unchanged. 31 contained `cursor/*` branches deleted; `telegram-chrome-872f` kept.

**D-011 (2026-09-08):** Calls stay on the organizer VPS. WebRTC + self-hosted TURNS/TCP 443 remains the fast path. If ICE cannot connect, fall back to E2EE audio frames on the existing live WSS `type=call` (UniFFI `encryptTyped` CALL). Do not tunnel via VK / Yandex / WB / Cloudflare-as-only-path. Do not put realtime audio on mailbox `send` (60/min + SQLite). No new Rust envelope type for 0.3.1.

**D-012 (2026-09-08):** Notifications stay self-hosted. No FCM. Keep the live WSS with an Android foreground service while signed in so background/Doze does not silently drop the socket. Message notify when the process is not resumed (not merely “another chat”). Offline callee: short-TTL **in-memory** pending `type=call` (no SDP/audio in SQLite); caller gets `queued`, not immediate `not_found`. Mailbox still holds opaque CALL envelopes from the client. No plaintext in any wake path.

**D-013 (2026-09-08):** Chat gestures match Telegram, Rope chrome: long-press → selection (check circles, «Выбрано N», bottom Ответить/Переслать). Single tap → reaction pill + vertical context menu (not a Material action sheet). Call overlay speaker control is **громкая связь** (speakerphone), not mute-sound. Stop ringback/ringtone when the call leaves RINGING or WSS/WebRTC media starts.
