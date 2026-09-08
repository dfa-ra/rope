# Task Board

PO-owned. Canonical tracker. One owner per row.

| ID | Task | Owner | Status | Source | Depends On |
| --- | --- | --- | --- | --- | --- |
| CG-007 | Unfreeze for this user task | PO | IN_PROGRESS | notify + TG UI + call UX | CG-036 |
| CG-040 | Research: offline notify, TG menus, leftover ring, speaker | RESEARCH-01 | DONE | user 0.3.1 follow-up | CG-007 |
| CG-041 | Android: FGS + lifecycle notify, TG tap/long-press, tones, громкая связь | AND-01 | IN_PROGRESS | this cycle | CG-040 |
| CG-042 | Go: in-memory pending RING when callee offline (no plaintext, no audio mailbox) | GO-01 | IN_PROGRESS | this cycle | CG-040 |
| CG-043 | Independent review | REV-01 | PENDING | after impl | CG-041, CG-042 |
| CG-026 | Ship **0.3.0** | PO | DONE | tag v0.3.0 = 69c35cb | CG-025 |
| CG-036 | Ship **0.3.1** | PO | DONE | tag v0.3.1; versionCode 25; v0.2.15 and v0.3.0 unchanged | CG-035 |
| CG-003 | No extra overlapping feature agents | PO | IN_PROGRESS | D-006 | — |
| CG-012 | Settings stub | — | BACKLOG | — | — |
| CG-013 | Owner revoke UI | — | BACKLOG | — | — |
| CG-014 | architecture.md Room vs SQLite | — | BACKLOG | — | — |

**Do not move tags `v0.2.15`, `v0.3.0`, or `v0.3.1`.**
