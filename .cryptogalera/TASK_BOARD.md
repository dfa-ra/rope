# Task Board

PO-owned. Canonical tracker. One owner per row. Converted from **existing** work first; process tasks kept.

Statuses: BACKLOG · READY · IN_PROGRESS · BLOCKED · REVIEW · INTEGRATING · DONE · CANCELLED

| ID | Task | Owner | Status | Source | Depends On |
| --- | --- | --- | --- | --- | --- |
| CG-001 | Encode operating model (session docs) | PO | DONE | process | — |
| CG-002 | User chooses product trunk (D-005) | User | BLOCKED | product decision | CG-001 |
| CG-003 | Feature-agent freeze (Stage-2) | PO | IN_PROGRESS | D-006 | — |
| CG-004 | Integrate chosen trunk to `main` | ENG-LEAD (unseated) | BACKLOG | Stage-2 vs MVP | CG-002 |
| CG-005 | Sync threat model + protocol to trunk | ENG-LEAD (unseated) | BACKLOG | docs drift after trunk | CG-004 |
| CG-006 | Install company runtime around Rope | PO | DONE | wrap | — |
| CG-007 | Unfreeze product implementation | PO | BACKLOG | user must say so | CG-006 |
| CG-008 | Cursor multi-agent runtime protocol | PO | DONE | wrap | CG-006 |
| CG-010 | Workspace reconnaissance (read-only) | RESEARCH-01 | DONE | this adoption | — |
| CG-011 | Record test/build baseline | PO | DONE | Phase 11 | CG-010 |
| CG-012 | Settings screen stub / unreachable | — | BACKLOG | existing UI | CG-002 |
| CG-013 | Owner revoke: Android client + UI | — | BACKLOG | server-only today | CG-002 |
| CG-014 | Fix architecture.md Room vs SQLite | — | BACKLOG | doc drift | — |
| CG-015 | Install reusable Cursor agents | PO | DONE | Phase 9 | CG-008 |
| CG-016 | Adopt-workspace state reconstruction | PO | DONE | this directive | CG-010 |

**DONE requires evidence.** CG-011 evidence: `cargo test` 11 passed; `go test ./...` ok; Android tests **not run** (no SDK in this environment) — not claimed green.

CG-012 / CG-013 stay BACKLOG until D-005: Stage-2 UI may supersede them. Do not duplicate Stage-2 call/UI/landing work as new tasks.

Not tasks: Telegram polish, ICE/TURN unstick, ship APK 0.2.x, landing, iOS/web/desktop/ratchet/MLS.
