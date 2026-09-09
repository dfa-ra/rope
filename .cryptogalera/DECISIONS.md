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

**D-014 (2026-09-08):** Ship **0.3.2** from `cursor/notify-tg-menus-ae19` onto `main`. Android `versionName=0.3.2` / `versionCode=26`. Do not retag `v0.2.15`, `v0.3.0`, or `v0.3.1`. GitHub Release assets for those tags stay published.

**D-015 (2026-09-08):** After every completed product task the PO lands PR → independent review → merge to `main` → tagged GitHub Release. An open unmerged PR is not done. The Cursor agent in this repo acts as Product Owner of Rope and is accountable for that loop. Every user request is staffed as if through the company (PO → Lead → Worker), then the delivery loop. This is law, not optional.

**D-016 (2026-09-08):** Ship **0.3.3** as real Settings (CG-012). The Settings screen was a stub and not reachable. Telegram-like sections, Rope chrome and logo colors: (1) global message mute persisted locally — no FCM; incoming call overlay is not silenced by this mute; (2) appearance light/dark using the existing palette; (3) server host/port + TLS fingerprint (copyable) so the user can see the pin; (4) about. Do not retag `v0.2.15` / `v0.3.0` / `v0.3.1` / `v0.3.2`.

**D-017 (2026-09-08):** Ship **0.3.4** as mute-vs-call lock-in (REV-01). Unit tests prove Settings `globalMuted` and per-chat mute silence message alerts only. Incoming ringtone, incoming-call overlay, and outgoing ringback still play. No FCM. Do not retag `v0.2.15` / `v0.3.0` / `v0.3.1` / `v0.3.2` / `v0.3.3`.

**D-018 (2026-09-08):** Ship **0.3.5** as Telegram-like chat list search + pinned polish. Rank title-prefix over title-contains over last-message preview; highlight the query; compact pill search; pin icon next to time with a divider under the pinned block. Do not redo 0.3.2 tap/long-press menus. Do not change the product name Rope or logo colors. No FCM. Self-hosted relay. Kotlin never implements crypto. Do not retag `v0.2.15` / `v0.3.0` / `v0.3.1` / `v0.3.2` / `v0.3.3` / `v0.3.4`.

**D-019 (2026-09-08):** Ship **0.3.6** as owner revoke UI (CG-013). Protocol already has `POST /v1/admin/revoke-member` and `POST /v1/admin/revoke-device`. Owner sees **Исключить** on People (hide, do not disable, for guests). Confirm then call revoke-member. Do not revoke self or the last owner (`409`). Do not redo 0.3.2 tap/long-press menus. Do not change the product name Rope or logo colors. No FCM. Self-hosted relay. Kotlin never implements crypto. Do not retag `v0.2.15` / `v0.3.0` / `v0.3.1` / `v0.3.2` / `v0.3.3` / `v0.3.4` / `v0.3.5`.

**D-020 (2026-09-09):** Ship **0.3.7** as last-owner `POST /v1/admin/revoke-device` 409 (REV-01). Same rule as revoke-member: last remaining owner must not brick the instance. Android does not expose unused `revokeDevice` for last owner (People stays revoke-member). Android `versionName=0.3.7` / `versionCode=31`. No FCM. Kotlin never implements crypto. Name/logo colors unchanged. Do not retag `v0.2.15` / `v0.3.0` / `v0.3.1` / `v0.3.2` / `v0.3.3` / `v0.3.4` / `v0.3.5` / `v0.3.6`.

**D-021 (2026-09-09):** Ship **0.3.8** as revoke-device 409 keyed off remaining owner **devices**, not `OwnerCount` members (REV-01 co-owner gap). Two owner members can otherwise revoke every owner device while member count stays > 1. Last owner with a spare device may revoke the spare. Android `versionName=0.3.8` / `versionCode=32`. No FCM. Kotlin never implements crypto. Name/logo colors unchanged. Self-hosted relay. Do not retag `v0.2.15` / `v0.3.0` / `v0.3.1` / `v0.3.2` / `v0.3.3` / `v0.3.4` / `v0.3.5` / `v0.3.6` / `v0.3.7`.

**D-022 (2026-09-09):** Ship **0.3.9** as last-owner `POST /v1/admin/revoke-member` 409 in the same SQLite `BEGIN IMMEDIATE` transaction as `RevokeDeviceGuarded` (REV-01: concurrent two-owner revoke-member can both 200). Android `versionName=0.3.9` / `versionCode=33`. No FCM. Kotlin never implements crypto. Name/logo colors unchanged. Self-hosted relay. Do not retag `v0.2.15` / `v0.3.0` / `v0.3.1` / `v0.3.2` / `v0.3.3` / `v0.3.4` / `v0.3.5` / `v0.3.6` / `v0.3.7` / `v0.3.8`.

**D-023 (2026-09-09):** Ship **0.3.10** as already-revoked `POST /v1/admin/revoke-member` **404** (same as revoke-device). Today a second revoke of a guest member returns 200; unknown members already 404. Android treats 404 as already-revoked (idempotent). `versionName=0.3.10` / `versionCode=34`. No FCM. Kotlin never implements crypto. Name/logo colors unchanged. Self-hosted relay. Do not redo 0.3.2 tap/long-press menus. Do not retag `v0.2.15` / `v0.3.0` / `v0.3.1` / `v0.3.2` / `v0.3.3` / `v0.3.4` / `v0.3.5` / `v0.3.6` / `v0.3.7` / `v0.3.8` / `v0.3.9`.

**D-024 (2026-09-09):** Ship **0.3.11** as Telegram-like chat-list **word-prefix** search. `TITLE_PREFIX` if the title starts with the query **or** any Unicode letter/digit word in the title starts with it (REV-01: "Мария Анна" + "анн" is `TITLE_PREFIX`, not `TITLE`). Mid-word substring stays `TITLE` ("Марианна"). Highlight prefers the word-prefix range. Do not redo 0.3.2 tap/long-press. Do not change the product name Rope or logo colors. No FCM. Self-hosted relay. Kotlin never implements crypto. Android `versionName=0.3.11` / `versionCode=35`. Do not retag `v0.2.15` / `v0.3.0` / `v0.3.1` / `v0.3.2` / `v0.3.3` / `v0.3.4` / `v0.3.5` / `v0.3.6` / `v0.3.7` / `v0.3.8` / `v0.3.9` / `v0.3.10`.

**D-025 (2026-09-09):** Ship **0.3.12** as Telegram-like chat-list **unread badge**. Unmuted unread uses the existing accent (primary). Muted unread uses gray (`onSurfaceVariant`), not accent. Title is semibold when unread > 0 (including muted). Cap label at `99+`. Do not redo 0.3.2 tap/long-press. Do not change the product name Rope or logo colors. No FCM. Self-hosted relay. Kotlin never implements crypto. Android `versionName=0.3.12` / `versionCode=36`. Do not retag `v0.2.15` / `v0.3.0` / `v0.3.1` / `v0.3.2` / `v0.3.3` / `v0.3.4` / `v0.3.5` / `v0.3.6` / `v0.3.7` / `v0.3.8` / `v0.3.9` / `v0.3.10` / `v0.3.11`.

**D-026 (2026-09-09):** Ship **0.3.13** as Telegram-like chat **date separators**. Insert a day chip when the local calendar day changes: Сегодня, Вчера, weekday if 2–6 days ago, otherwise `d MMMM` (same year) or `d MMMM yyyy`. Clusters do not span a day boundary. Do not redo 0.3.2 tap/long-press. Do not change the product name Rope or logo colors. No FCM. Self-hosted relay. Kotlin never implements crypto. Android `versionName=0.3.13` / `versionCode=37`. Do not retag `v0.2.15` / `v0.3.0` / `v0.3.1` / `v0.3.2` / `v0.3.3` / `v0.3.4` / `v0.3.5` / `v0.3.6` / `v0.3.7` / `v0.3.8` / `v0.3.9` / `v0.3.10` / `v0.3.11` / `v0.3.12`.

**D-027 (2026-09-09):** Ship **0.3.14** as Telegram-like chat-list **empty-search**. Search miss is «Ничего не найдено» with a mode-aware «Нет чатов/групп/звонков по запросу «q»» body; it beats idle and forward empty. Hide FAB and «Новая группа». Do not redo 0.3.2 tap/long-press. Do not change the product name Rope or logo colors. No FCM. Self-hosted relay. Kotlin never implements crypto. Android `versionName=0.3.14` / `versionCode=38`. Do not retag `v0.2.15` / `v0.3.0` / `v0.3.1` / `v0.3.2` / `v0.3.3` / `v0.3.4` / `v0.3.5` / `v0.3.6` / `v0.3.7` / `v0.3.8` / `v0.3.9` / `v0.3.10` / `v0.3.11` / `v0.3.12` / `v0.3.13`.

**D-028 (2026-09-09):** Ship **0.3.15** as Telegram-like **composer / reply bar** polish. Reply title is the quote name (not «Ответ · name»). Edit title stays «Редактирование». One-line clipped preview, left accent stripe, X dismiss (not «Отмена»). Do not redo 0.3.2 tap/long-press. Do not change the product name Rope or logo colors. No FCM. Self-hosted relay. Kotlin never implements crypto. Android `versionName=0.3.15` / `versionCode=39`. Do not retag `v0.2.15` / `v0.3.0` / `v0.3.1` / `v0.3.2` / `v0.3.3` / `v0.3.4` / `v0.3.5` / `v0.3.6` / `v0.3.7` / `v0.3.8` / `v0.3.9` / `v0.3.10` / `v0.3.11` / `v0.3.12` / `v0.3.13` / `v0.3.14`.
