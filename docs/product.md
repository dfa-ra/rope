# Product — Rope

Owner: Founder / Product Owner. This file is the product contract. If it conflicts with a prompt, a cloud-agent name, or a GitHub Release tag, **this file plus [README.md](../README.md) win** until the user changes them.

## Goal

Rope is a **private self-hosted 1-to-1 E2EE messenger for Android**. An organizer provisions a personal VPS from the app, invites a second device with a QR / deep link, and exchanges encrypted text through a Go relay that never sees plaintext.

Stack (not negotiable without the user): **Kotlin UI → Rust UniFFI security core → HTTPS/WSS → Go + SQLite**.

## Canonical line (operational)

Until the user answers [D-005](decisions.md#d-005-stage-2-parallel-line-escalated):

| Line | What it is | What agents may do |
| --- | --- | --- |
| **`main` (documented product)** | MVP `0.1.0` as described in this file and the README | Implement, fix, document, and review **this** product |
| **GitHub Releases `v0.2.x` / open PRs** | A parallel Stage-2 fork (calls, groups, media, Telegram chrome, landing). **Not merged to `main`.** Latest published tag at the time of this writing: `v0.2.15` | Do **not** treat as scope. Do **not** spawn overlapping feature agents. Do **not** “catch main up” unless the user chooses that trunk |

A GitHub Release is **not** proof that `main` contains the feature. Tags `v0.2.0`–`v0.2.15` are **not ancestors of `main`**.

## Success (MVP on `main`)

The product succeeds when a non-developer organizer can:

1. Install the Android app.
2. Provision (or point at) a VPS and complete owner bootstrap with `setup_token`.
3. Invite a guest via QR / `rope://join?...` with TLS fingerprint binding.
4. Exchange E2EE text in a 1-to-1 chat.
5. Trust the guarantees in [threat-model.md](threat-model.md) (server cannot read plaintext; cannot forge as another device).

Engineering success: `cargo test` (Rust), `go test ./...` (relay), `./gradlew test` (Android) green on `main`.

## In scope (MVP)

- Android client: provision, join, chats, 1-to-1 chat, invite, server status
- Device identity and envelope crypto in Rust (UniFFI); Kotlin never implements crypto
- Go relay: REST + WSS mailbox, members/devices, single-use invites, owner revoke APIs
- Self-signed TLS + client fingerprint pin; debug `--allow-http` only for local/emulator
- Local message history on device; opaque mailbox blobs on server
- Idempotent VPS installer + systemd unit
- GitHub Actions CI and tagged release artifacts **from the chosen trunk**

## Out of scope (MVP)

Do not implement these because a prompt mentioned them. They require a user decision ([D-005](decisions.md#d-005-stage-2-parallel-line-escalated)):

- iOS, web app, desktop
- Calls / WebRTC / TURN
- Voice notes, files / object store, photos as a product feature
- Groups (including pairwise fan-out “groups”)
- Federation
- Polished Telegram / Amnezia UI as a redesign
- Signal/X3DH ratchet / forward secrecy
- Public CA instead of pin-or-warn self-signed TLS
- Marketing landing site as part of the messenger

## Parallel line (facts, not approval)

Unmerged work and off-`main` tags already contain some of the out-of-scope items:

- PR #2 `cursor/rope-mvp-872f` — WebRTC audio, objects, groups, Stage-2 UX
- PR #3 `cursor/telegram-ergonomics-872f` — superset of PR #2 plus Telegram-like chrome
- Tags `v0.2.0`–`v0.2.15` — Stage-2 binaries published from side branches (all are GitHub Releases; latest is `v0.2.15`)

Ancestry at the time of this writing: MVP content on `main` is in PR #2; PR #2 ⊂ PR #3 ⊂ tag `v0.2.15`. Nested PRs are not two independent features. Tag `v0.2.15` is **ahead of** PR #3 (extra TURN/ICE/call-stack commits). Do not treat PR #3 and `v0.2.15` as the same tip. The only `main` commit missing from that line is the PR #1 merge commit.

This is an organizational failure (agent sprawl), not a product decision. New agents must not continue that line until D-005 is resolved.

## What CryptoGalera is optimizing for

Professional process: one product contract, one merge trunk, one accountable owner per task, independent review for crypto/auth/protocol. See [company.md](company.md) and [AGENTS.md](../AGENTS.md).
