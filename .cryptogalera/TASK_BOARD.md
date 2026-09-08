# Task Board

PO-owned. Canonical tracker. One owner per row.

| ID | Task | Owner | Status | Source | Depends On |
| --- | --- | --- | --- | --- | --- |
| CG-007 | Unfreeze for this user task | PO | DONE | user 0.3.1 | CG-006 |
| CG-026 | Ship **0.3.0** | PO | DONE | tag v0.3.0 = 69c35cb | CG-025 |
| CG-030 | Research: WebRTC blocked / self-hosted alternatives | RESEARCH-01 | DONE | WSS E2EE audio fallback; keep WebRTC first | CG-007 |
| CG-031 | Integrate Telegram UI + call fallback | ENG-LEAD/PO | DONE | this cycle | CG-030 |
| CG-032 | Android UI + WSS E2EE audio fallback | AND-01 | DONE | worker + integrator + CI fixes | CG-030 |
| CG-033 | Go call-media relay | GO-01 | DONE | 16KiB cap + 40/s audio | CG-030 |
| CG-034 | Rust envelope only if needed | RUST-01 | CANCELLED | reuse encryptTyped CALL | CG-030 |
| CG-035 | Independent review | REV-01 | DONE | PASS_WITH_CONCERNS; follow-ups landed; CI green | CG-031 |
| CG-036 | Ship **0.3.1** | PO | DONE | tag v0.3.1; versionCode 25; v0.2.15 and v0.3.0 unchanged | CG-035 |
| CG-003 | No extra overlapping feature agents | PO | IN_PROGRESS | D-006 | — |
| CG-012 | Settings stub | — | BACKLOG | — | — |
| CG-013 | Owner revoke UI | — | BACKLOG | — | — |
| CG-014 | architecture.md Room vs SQLite | — | BACKLOG | — | — |

**Do not move tags `v0.2.15` or `v0.3.0`.**
