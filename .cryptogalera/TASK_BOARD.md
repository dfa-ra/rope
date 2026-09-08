# Task Board

PO-owned. Canonical tracker. One owner per row.

Statuses: BACKLOG · READY · IN_PROGRESS · BLOCKED · REVIEW · INTEGRATING · DONE · CANCELLED

| ID | Task | Owner | Status | Source | Depends On |
| --- | --- | --- | --- | --- | --- |
| CG-003 | Feature-agent freeze (new overlapping Stage-2) | PO | IN_PROGRESS | D-006 | — |
| CG-007 | Unfreeze for this user UI/calls/core cycle | PO | IN_PROGRESS | user 2026-09-08 | CG-006 |
| CG-012 | Settings screen stub | — | BACKLOG | not this cycle | — |
| CG-013 | Owner revoke Android UI | — | BACKLOG | not this cycle | — |
| CG-014 | architecture.md Room vs SQLite | — | BACKLOG | not this cycle | — |
| CG-020 | Research: explainers, messenger UX, TURN map | RESEARCH-01 | IN_PROGRESS | this cycle | CG-007 |
| CG-021 | Integrate UI + calls + core on one branch | ENG-LEAD | READY | this cycle | CG-020 |
| CG-022 | Android: drop lectures, chat/call UI, WebRTC client | AND-01 | READY | this cycle | CG-020 |
| CG-023 | Go/deploy: TURN/ICE reliability | GO-01 | READY | this cycle | CG-020 |
| CG-024 | Rust core: security + reliability (no ratchet) | RUST-01 | READY | this cycle | CG-020 |
| CG-025 | Independent review | REV-01 | READY | this cycle | CG-021 |
| CG-026 | Ship app+server **0.3.0** (tag, do not retag 0.2.15) | PO | READY | user | CG-025 |

**This cycle ACs:** user orients quickly; no primitive “what is Groups/Calls” copy under the chrome title; calls/TURN actually usable; core has no plaintext leaks; existing features stay.

**Out of scope:** iOS, web messenger, desktop, ratchet/MLS, new landing brand, retagging `v0.2.15`.

Prior cycle CG-001–CG-019 remain DONE (see git history). Unique `cursor/telegram-chrome-872f` may be **read** for chrome ideas; do not merge the whole unique commit unless it matches ACs.
