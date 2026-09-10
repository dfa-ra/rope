# Product — Rope

Owner: Founder / Product Owner. This file is the product contract. If it conflicts with a prompt, a cloud-agent name, or an older MVP description, **this file plus [README.md](../README.md) win**.

## Goal

Rope is a **private self-hosted E2EE messenger for Android**. An organizer provisions a personal VPS from the app, invites people with a QR / deep link, and exchanges encrypted text, voice, photos, files, group chats, and calls through a Go relay that never sees plaintext.

Stack (not negotiable without the user): **Kotlin UI → Rust UniFFI security core → HTTPS/WSS → Go + SQLite**.

## Canonical line (D-005 A)

The product trunk is **`main`**, shipping **0.3.51**. GitHub Releases `v0.2.15`, `v0.3.0`, `v0.3.1`, `v0.3.2`, `v0.3.3`, `v0.3.4`, `v0.3.5`, `v0.3.6`, `v0.3.7`, `v0.3.8`, `v0.3.9`, `v0.3.10`, `v0.3.11`, `v0.3.12`, `v0.3.13`, `v0.3.14`, `v0.3.15`, `v0.3.16`, `v0.3.17`, `v0.3.18`, `v0.3.19`, `v0.3.20`, `v0.3.21`, `v0.3.22`, `v0.3.23`, `v0.3.24`, `v0.3.25`, `v0.3.26`, `v0.3.27`, `v0.3.28`, `v0.3.29`, `v0.3.30`, `v0.3.31`, `v0.3.32`, `v0.3.33`, `v0.3.34`, `v0.3.35`, `v0.3.36`, `v0.3.37`, `v0.3.38`, `v0.3.39`, `v0.3.40`, `v0.3.41`, `v0.3.42`, `v0.3.43`, `v0.3.44`, `v0.3.45`, `v0.3.46`, `v0.3.47`, `v0.3.48`, `v0.3.49`, and `v0.3.50` remain published history.

| Line | What it is |
| --- | --- |
| **`main`** | Shipped tree: Android `versionName=0.3.51` / `versionCode=75`, plus CryptoGalera operating layer |
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
| **GitHub Release `v0.3.24`** | Previous line. Do not retag `v0.3.24`. |
| **GitHub Release `v0.3.25`** | Previous line. Do not retag `v0.3.25`. |
| **GitHub Release `v0.3.26`** | Previous line. Do not retag `v0.3.26`. |
| **GitHub Release `v0.3.27`** | Previous line. Do not retag `v0.3.27`. |
| **GitHub Release `v0.3.28`** | Previous line. Do not retag `v0.3.28`. |
| **GitHub Release `v0.3.29`** | Previous line. Do not retag `v0.3.29`. |
| **GitHub Release `v0.3.30`** | Previous line. Do not retag `v0.3.30`. |
| **GitHub Release `v0.3.31`** | Previous line. Do not retag `v0.3.31`. |
| **GitHub Release `v0.3.32`** | Previous line. Do not retag `v0.3.32`. |
| **GitHub Release `v0.3.33`** | Previous line. Do not retag `v0.3.33`. |
| **GitHub Release `v0.3.34`** | Previous line. Do not retag `v0.3.34`. |
| **GitHub Release `v0.3.35`** | Previous line. Do not retag `v0.3.35`. |
| **GitHub Release `v0.3.36`** | Previous line. Do not retag `v0.3.36`. |
| **GitHub Release `v0.3.37`** | Previous line. Do not retag `v0.3.37`. |
| **GitHub Release `v0.3.38`** | Previous line. Do not retag `v0.3.38`. |
| **GitHub Release `v0.3.39`** | Previous line. Do not retag `v0.3.39`. |
| **GitHub Release `v0.3.40`** | Previous line. Do not retag `v0.3.40`. |
| **GitHub Release `v0.3.41`** | Previous line. Do not retag `v0.3.41`. |
| **GitHub Release `v0.3.42`** | Previous line. Do not retag `v0.3.42`. |
| **GitHub Release `v0.3.43`** | Previous line. Do not retag `v0.3.43`. |
| **GitHub Release `v0.3.44`** | Previous line. Do not retag `v0.3.44`. |
| **GitHub Release `v0.3.45`** | Previous line. Do not retag `v0.3.45`. |
| **GitHub Release `v0.3.46`** | Previous line. Do not retag `v0.3.46`. |
| **GitHub Release `v0.3.47`** | Previous line. Do not retag `v0.3.47`. |
| **GitHub Release `v0.3.48`** | Previous line. Do not retag `v0.3.48`. |
| **GitHub Release `v0.3.49`** | Previous line. Do not retag `v0.3.49`. |
| **GitHub Release `v0.3.50`** | Previous Latest until `v0.3.51` publishes. Do not retag `v0.3.50`. |

Older tags `v0.1.0`–`v0.2.14` remain published history. They are ancestors of `v0.2.15`.

Unique unshipped UI on `cursor/telegram-chrome-872f` (`039e381`) is **not** part of this trunk.

## Success

The product succeeds when a non-developer organizer can:

1. Install the Android app (this cycle ships `v0.3.51`).
2. Provision (or point at) a VPS and complete owner bootstrap with `setup_token`.
3. Invite others via QR / `rope://join?...` with TLS fingerprint binding.
4. Exchange E2EE text (and the Stage-2 media/group/call features already in this tree).
5. Trust the guarantees in [threat-model.md](threat-model.md) (server cannot read plaintext; cannot forge as another device).

Engineering success: `cargo test` (Rust), `go test ./...` (relay), `./gradlew test` (Android) green on `main`.

## In scope (current trunk)

- Android client: provision, join, chats, groups, media, call UI already in `v0.2.15`
- **0.3.24 Replies 2.0:** left-swipe to reply plus optional quote-span (`qt` / `qo` inside encrypted JSON)
- **0.3.25 1:1 video calls:** camera + WebRTC video tracks, `OfferToReceiveVideo=true`, in-call surface on the existing WSS/WebRTC path (16 KiB SDP cap unchanged)
- **0.3.26 media send editor:** caption on photo/video send (inner JSON `caption`) and videos as album members
- **0.3.27 call chrome:** in-call / ringing controls are Telegram-style icon-only rounds (mute, speaker, camera, flip, hangup, accept); labels are a11y `contentDescription` only
- **0.3.28 QA:** reply+media keeps quote; incoming camera-deny mutes local camera (`только звук` on overlay); composer video hint; split reply vs album cancel; pending URIs do not leak across chats; outgoing camera mute applies when WebRTC starts
- **0.3.29 QA:** incoming Accept mic-deny shows on the call overlay; camera unmute after mute-before-connect renegotiates; in-call CAMERA re-asks permission; in-flight album stays on the chat that tapped Send; edit is not dropped by staged media; same-chat remount keeps pending URIs
- **0.3.30 calls:** 1:1 video establishes both directions (camera + remote surface) on the existing WebRTC/WSS path; `OfferToReceiveVideo` on video offers/answers; callee `onRenegotiationNeeded` does not createOffer; ICE blip / mute / flip / camera-deny / renegotiation does not hang up the remote; explicit hangup sends `hangup` (BYE alias accepted) so both leave a clean idle overlay; CAMERA result after hangup does not poison the next call
- **0.3.31 QA:** callee `onRenegotiationNeeded` offers only when this side is the offerer and signaling is STABLE; unmute still offers when STABLE; overlay `callNotice` clears on successful Accept/unmute; Back from peer profile / group info keeps the staged album; CallOverlay draws above the image viewer
- **0.3.32 calls:** in-call video uses a TextureView EGL sink (init before attach, bind after EGL); camera capturer starts on the main thread before createOffer; OfferToReceiveVideo stays true on video offers/answers; ring/tone plays only while RINGING_IN/OUT and stops on CONNECTED; ICE DISCONNECTED/FAILED/CHECKING after media-up must not restart RING or pulse the overlay
- **0.3.33 Voice notes 2.0:** 1x/1.5x/2x playback, seekable waveform (live amplitudes stored as inner JSON `wf`), and round video notes (кружок, kind `video_note`). Hold-to-record voice already shipped. In-call camera path unchanged.
- **0.3.34 security:** live call offer/answer/ICE bind to the current peer (call-id reuse from a third device cannot inject SDP); SDP/ICE drop CRLF and `file:` / `javascript:` / `data:` schemes; APK install status is a non-exported receiver and only launches PackageInstaller confirm intents.
- **0.3.35 security:** live call offer/answer/ICE apply only from sealed envelopes (VPS cannot swap DTLS fingerprints); identity is not written to public Downloads; TURNS/TLS pin fails closed; unauthenticated `GET /v1/info` omits TURN; bootstrap ignores `X-Forwarded-For`; `github_token` kv is Keystore-wrapped.
- **0.3.36 link previews:** sender fetches Open Graph on send and packs `lp` inside type=1/type=3 ciphertext; recipients render the card and never refetch; Go does not crawl. Skip unfurl while recording a voice note or кружок; keep HTTPS previews when send clears the draft.
- **0.3.37 security:** WSS auth Phase A — server prefers `X-Rope-Ws-Auth` when present, else `device_id`/`ts`/`sig` query; Android sends both. Query stays until Phase B.
- **0.3.38 security:** cached TURN HMAC credentials in local profile kv are Keystore-wrapped; PeerConnection ICE URLs are allowlisted to `stun:`, `turn:`, and `turns:`.
- **0.3.39 security:** DeviceBackup wrap API (`RODB` + Keystore AES-GCM) exists for a future identity export writer; `toBytes()` still emits clear JSON (identity + `github_token`) and must not be written to shared storage; leftover v1 JSON is not auto-applied.
- **0.3.40 calls:** remote 1:1 video — no Compose `graphicsLayer` around TextureView; EGL waits for a positive surface size; remote tracks bind from `onAddStream`; SDP is CRLF-normalized for setRemote without truncating the 16 KiB cap. Same-peer RING flood is dropped for 2s (Go RING/RELAY 6/30s); overlay `нет видео пира` after 4s one-way; flip is a no-op when camera is muted; rotate does not destroy the in-call TextureView.
- **0.3.41 security:** RECEIPT edit/delete apply only for the message author (sender device id); pin/react only in a shared thread (1:1 peer or local group membership). Same `message_id` from a different sender does not replace the row. Typing for unknown groups is ignored.
- **0.3.42 calls:** per-session remote ICE is capped at 64 candidates before setRemote; local PIP un-mirrors after flipping to the rear camera. 16 KiB SDP cap unchanged (refuse, do not truncate).
- **0.3.43 calls:** RING from another peer while live, or same-peer flood while idle, replies REJECT without TearDown so the caller stops ringing; glare and extra RING on the live call id stay unchanged.
- **0.3.44 security:** unauthenticated `POST /v1/bootstrap` peeks the invite before login enumeration so a garbage token no longer distinguishes taken vs free display names; a taken login still 409s without consuming the token.
- **0.3.45 security:** install SSH private key is written to a temp `rope-ssh-*.pem` and overwrite-unlinked immediately after `authPublickey`; stale `ssh-key.pem` and leftover temp PEMs are wiped at install start. App-private cache only — not envelope crypto.
- **0.3.46 security:** GitHub PAT (`Authorization: Bearer`) is attached only for HTTPS `github.com` / `api.github.com` download URLs; a saved PAT plus a pasted custom binary URL no longer leaks the token. Not envelope crypto.
- **0.3.47 security:** unauthenticated public `GET /health` returns only `ok` plus `turn_running` / `turn_allocate_ok`; TURN relayed IP, ports, and `turn_error` stay on loopback for `install.sh`. Loopback vs public is `RemoteAddr` (not `X-Forwarded-For`). Authed admin status is unchanged.
- **0.3.48 security:** unauthenticated `GET /v1/info` omits `public_ip`; join still gets `server_id` and TLS fingerprint. `public_ip` stays on the authenticated ICE response. 401 does not leak it.
- **0.3.49 security:** `FLAG_SECURE` on Provision and Join so recents/screenshots cannot capture SSH passwords, PEMs, GitHub PATs, or a live invite token. Invite QR stays shareable. Not envelope crypto.
- **0.3.50:** local Telegram-like chat archive on the chat list; object download failures are a uniform 404; invite TTL is capped at 24 hours; JSON API responses send `Cache-Control: no-store`; guest directory listings omit revoked members. Also: bootstrap does not echo invite/setup token status; group add/remove others is organizer/owner; WSS handshake failures do not echo reasons; incoming-call peer name is hidden on the lockscreen. Not envelope crypto.
- **0.3.51 security:** LocalStore `decryptBytes` and IdentityVault wrap require a 12-byte AES-GCM IV; `SecretKv.unwrap` returns null on a corrupt wrap; GitHub APK asset names reject CR/LF; APKs install only from `cache/updates`. Not envelope crypto.
- **0.3.51 UI:** В чате from the fullscreen photo/video viewer jumps to the bubble. LocalStore stays v6.
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
