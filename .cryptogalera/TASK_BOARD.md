# Task Board

PO-owned. Canonical tracker. One owner per task. IDs: `CG-XXX`. Aliases `T-00N` are the session docs ids.

| ID | Alias | Task | Owner | Status | Depends On |
| --- | --- | --- | --- | --- | --- |
| CG-001 | T-001 | Encode operating model (session docs) | PO | DONE | — |
| CG-002 | T-002 | User chooses product trunk (D-005) | User | BLOCKED | CG-001 |
| CG-003 | T-003 | Feature-agent freeze | PO | IN_PROGRESS | D-006 |
| CG-004 | T-004 | Single integration after trunk | ENG-LEAD (unseated) | BACKLOG | CG-002 |
| CG-005 | T-005 | Threat model + protocol sync | ENG-LEAD (unseated) | BACKLOG | CG-004 |
| CG-006 | T-006 | Install company runtime around Rope | PO | DONE | CG-001, D-007 |
| CG-007 | T-007 | Unfreeze product implementation | PO | BACKLOG | CG-006; user must say so |
| CG-008 | — | Install Cursor multi-agent runtime protocol | PO | DONE | CG-006 |
| CG-008-A | — | Persist RUNTIME.md + Cursor pointers | PROC-LEAD | DONE | CG-008 |
| CG-008-B | — | Create `.cryptogalera/` memory files | PO | DONE | CG-008 |

**DONE** requires acceptance criteria, not “files written.”

Not tasks: Telegram polish, ICE/TURN unstick, ship APK 0.2.x, landing, iOS/web/desktop/ratchet/MLS.
