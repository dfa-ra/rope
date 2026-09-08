# Task Board

PO-owned. Canonical tracker. One owner per row.

| ID | Task | Owner | Status | Source | Depends On |
| --- | --- | --- | --- | --- | --- |
| CG-007 | Unfreeze for UI/calls/core 0.3.0 | PO | DONE | user | CG-006 |
| CG-020 | Research: explainers, messenger UX, TURN map | RESEARCH-01 | DONE | this cycle | CG-007 |
| CG-021 | Integrate UI + calls + core | ENG-LEAD/PO | DONE | this cycle | CG-020 |
| CG-022 | Android UI + WebRTC client | AND-01 | DONE | this cycle | CG-020 |
| CG-023 | Go TURN/ICE | GO-01 | DONE | this cycle | CG-020 |
| CG-024 | Rust envelope/object harden | RUST-01 | DONE | this cycle | CG-020 |
| CG-025 | Independent review | REV-01 | DONE | PASS_WITH_CONCERNS then ICE client follow-up | CG-021 |
| CG-026 | Ship **0.3.0** | PO | DONE | tag v0.3.0 = 69c35cb | CG-025 |
| CG-003 | No extra overlapping feature agents | PO | IN_PROGRESS | D-006 | — |
| CG-012 | Settings stub | — | BACKLOG | — | — |
| CG-013 | Owner revoke UI | — | BACKLOG | — | — |
| CG-014 | architecture.md Room vs SQLite | — | BACKLOG | — | — |

**CG-025:** PASS_WITH_CONCERNS. Follow-up: client `expandHosts` skips RFC1918/CGNAT; `ice_ttl_seconds` stored and used in `shouldRefresh`.

**CG-026 evidence (pending tag):** Android `0.3.0` / 24; `ServerVersion = 0.3.0`; `rope_core` crate `0.3.0`. Do not move tag `v0.2.15`.
