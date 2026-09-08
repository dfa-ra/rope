# Task Board

PO-owned. Canonical tracker. One owner per row.

Statuses: BACKLOG · READY · IN_PROGRESS · BLOCKED · REVIEW · INTEGRATING · DONE · CANCELLED

| ID | Task | Owner | Status | Source | Depends On |
| --- | --- | --- | --- | --- | --- |
| CG-001 | Encode operating model (session docs) | PO | DONE | process | — |
| CG-002 | User chooses product trunk (D-005) | User | DONE | product decision | CG-001 |
| CG-003 | Feature-agent freeze (new overlapping Stage-2) | PO | IN_PROGRESS | D-006 | — |
| CG-004 | Integrate chosen trunk (`v0.2.15`) to `main` | ENG-LEAD | DONE | D-005 A | CG-002 |
| CG-005 | Confirm threat model + protocol match trunk | ENG-LEAD | DONE | taken from tag | CG-004 |
| CG-006 | Install company runtime around Rope | PO | DONE | wrap | — |
| CG-007 | Unfreeze new product implementation | PO | BACKLOG | not this cycle | CG-006 |
| CG-008 | Cursor multi-agent runtime protocol | PO | DONE | wrap | CG-006 |
| CG-010 | Workspace reconnaissance (read-only) | RESEARCH-01 | DONE | this adoption | — |
| CG-011 | Record test/build baseline | PO | DONE | Phase 11 | CG-010 |
| CG-012 | Settings screen stub / unreachable | — | BACKLOG | re-check vs Stage-2 UI | CG-002 |
| CG-013 | Owner revoke: Android client + UI | — | BACKLOG | server-only today | CG-002 |
| CG-014 | Fix architecture.md Room vs SQLite | — | BACKLOG | doc drift | — |
| CG-015 | Install reusable Cursor agents | PO | DONE | Phase 9 | CG-008 |
| CG-016 | Adopt-workspace state reconstruction | PO | DONE | this adoption | CG-010 |
| CG-017 | Maintenance: review PRs, merge safe, cleanup | PO | DONE | prior cycle | — |
| CG-018 | Independent review of v0.2.15 promotion | REV-01 | DONE | CG-004 | CG-004 |
| CG-019 | Close contained PRs/branches; keep unique work | PO | DONE | cleanup | CG-004 |

**CG-004 evidence:** `origin/main` @ `a869927` contains tag `v0.2.15` (`9694e80`). `git diff v0.2.15 -- apps core server deployment protocol` empty. Android `0.2.15` / 23. GitHub Release `v0.2.15` assets unchanged. PR #5 merged. `cargo test` 15 passed; `go test ./...` ok.

**CG-018 evidence:** REV-01 verdict PASS_WITH_CONCERNS (stale company/org wording patched before land). Reviewer ≠ implementer.

**CG-019 evidence:** 31 contained `cursor/*` branches deleted. Kept `cursor/telegram-chrome-872f` (unique `039e381`). PRs #2/#3/#5 closed/merged. Tags `v0.1.0`–`v0.2.15` retained.

CG-007 stays BACKLOG. CG-003 freeze remains for new overlapping feature agents.

Not tasks: Telegram polish, ICE/TURN unstick, new APK bump, landing brand, iOS/web/desktop/ratchet/MLS.
