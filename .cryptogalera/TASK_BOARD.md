# Task Board

PO-owned. Canonical tracker. One owner per row. Converted from **existing** work first; process tasks kept.

Statuses: BACKLOG · READY · IN_PROGRESS · BLOCKED · REVIEW · INTEGRATING · DONE · CANCELLED

| ID | Task | Owner | Status | Source | Depends On |
| --- | --- | --- | --- | --- | --- |
| CG-001 | Encode operating model (session docs) | PO | DONE | process | — |
| CG-002 | User chooses product trunk (D-005) | User | DONE | product decision | CG-001 |
| CG-003 | Feature-agent freeze (new overlapping Stage-2) | PO | IN_PROGRESS | D-006 | — |
| CG-004 | Integrate chosen trunk (`v0.2.15`) to `main` | ENG-LEAD | INTEGRATING | D-005 A | CG-002 |
| CG-005 | Confirm threat model + protocol match trunk | ENG-LEAD | READY | docs after trunk | CG-004 |
| CG-006 | Install company runtime around Rope | PO | DONE | wrap | — |
| CG-007 | Unfreeze new product implementation | PO | BACKLOG | not this cycle | CG-006 |
| CG-008 | Cursor multi-agent runtime protocol | PO | DONE | wrap | CG-006 |
| CG-010 | Workspace reconnaissance (read-only) | RESEARCH-01 | DONE | this adoption | — |
| CG-011 | Record test/build baseline | PO | DONE | Phase 11 | CG-010 |
| CG-012 | Settings screen stub / unreachable | — | BACKLOG | may be superseded by Stage-2 UI | CG-002 |
| CG-013 | Owner revoke: Android client + UI | — | BACKLOG | server-only today | CG-002 |
| CG-014 | Fix architecture.md Room vs SQLite | — | BACKLOG | doc drift | — |
| CG-015 | Install reusable Cursor agents | PO | DONE | Phase 9 | CG-008 |
| CG-016 | Adopt-workspace state reconstruction | PO | DONE | this adoption | CG-010 |
| CG-017 | Maintenance: review PRs, merge safe, cleanup | PO | DONE | prior cycle | — |
| CG-017-A | Independent review of PR #4 | REV-01 | DONE | CG-017 | CG-017 |
| CG-018 | Independent review of v0.2.15 promotion | REV-01 | READY | CG-004 | CG-004 |
| CG-019 | Close contained PRs/branches; keep unique work | PO | READY | cleanup | CG-004 |

**DONE requires evidence.** CG-002 evidence: user task 2026-09-08 — put current releases on `main`, nothing lost.

CG-004 evidence (in progress): merge tag `v0.2.15` (`9694e80`) into `main` keeping CryptoGalera wrap. After merge: `git diff v0.2.15 -- apps core server deployment protocol` must be empty.

CG-007 stays BACKLOG: promoting the already-shipped line is T-004, not a license to start new feature work. T-003 freeze remains for new overlapping agents.

CG-012 / CG-013: re-check against Stage-2 UI after CG-004 lands; do not duplicate shipped chrome/calls.

Not tasks: Telegram polish, ICE/TURN unstick, new APK version bump, landing brand, iOS/web/desktop/ratchet/MLS. Unique unshipped commit on `cursor/telegram-chrome-872f` (`039e381`) is **preserved as a branch**, not merged (would change the shipped line).
