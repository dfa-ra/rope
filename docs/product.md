# Product — Rope

Owner: Founder / Product Owner. This file is the product contract. If it conflicts with a prompt, a cloud-agent name, or an older MVP description, **this file plus [README.md](../README.md) win**.

## Goal

Rope is a **private self-hosted E2EE messenger for Android**. An organizer provisions a personal VPS from the app, invites people with a QR / deep link, and exchanges encrypted text, voice, photos, files, group chats, and calls through a Go relay that never sees plaintext.

Stack (not negotiable without the user): **Kotlin UI → Rust UniFFI security core → HTTPS/WSS → Go + SQLite**.

## Canonical line (D-005 A)

The product trunk is **`main`**, shipping **0.3.33**. GitHub Releases `v0.2.15`, `v0.3.0`, `v0.3.1`, `v0.3.2`, `v0.3.3`, `v0.3.4`, `v0.3.5`, `v0.3.6`, `v0.3.7`, `v0.3.8`, `v0.3.9`, `v0.3.10`, `v0.3.11`, `v0.3.12`, `v0.3.13`, `v0.3.14`, `v0.3.15`, `v0.3.16`, `v0.3.17`, `v0.3.18`, `v0.3.19`, `v0.3.20`, `v0.3.21`, `v0.3.22`, `v0.3.23`, `v0.3.24`, `v0.3.25`, `v0.3.26`, `v0.3.27`, `v0.3.28`, `v0.3.29`, `v0.3.30`, `v0.3.31`, and `v0.3.32` remain published history.

| Line | What it is |
| --- | --- |
| **`main`** | Shipped tree: Android `versionName=0.3.33` / `versionCode=57`, plus CryptoGalera operating layer |
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
| **GitHub Release `v0.3.32`** | Previous Latest until `v0.3.33` publishes. Do not retag `v0.3.32`. |

Older tags `v0.1.0`–`v0.2.14` remain published history. They are ancestors of `v0.2.15`.

Unique unshipped UI on `cursor/telegram-chrome-872f` (`039e381`) is **not** part of this trunk.

## Success

The product succeeds when a non-developer organizer can:

1. Install the Android app (this cycle ships `v0.3.33`).
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
