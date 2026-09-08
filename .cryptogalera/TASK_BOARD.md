# Task Board

PO-owned. Canonical tracker. One owner per row.

| ID | Task | Owner | Status | Source | Depends On |
| --- | --- | --- | --- | --- | --- |
| CG-007 | Unfreeze for this user task | PO | IN_PROGRESS | unfreeze for 0.3.4 mute-vs-call | — |
| CG-050 | Slice: mute vs call ringtone tests as 0.3.4 | PO | IN_PROGRESS | REV-01: globalMuted must not silence incoming ring | CG-007 |
| CG-051 | Android: `CallToneRules` + wire RingIn/NotifyIncoming | AND-01 | IN_PROGRESS | D-016 / D-017 | CG-050 |
| CG-052 | Independent review | REV-01 | QUEUED | reviewer ≠ implementer | CG-051 |
| CG-053 | Ship **0.3.4** | PO | QUEUED | versionCode 28; do not retag v0.3.3 | CG-052 |
| CG-045 | Slice: real Settings (CG-012) as 0.3.3 | PO | DONE | shipped v0.3.3 | CG-007 |
| CG-046 | Android Settings: mute, appearance, fingerprint, about | AND-01 | DONE | CG-012 | CG-045 |
| CG-047 | Rust: harden `parse_rejects_timestamp_beyond_future_skew` | CORE-01 | DONE | known flake; test-only | CG-045 |
| CG-048 | Independent review | REV-01 | DONE | reviewer ≠ implementer | CG-046, CG-047 |
| CG-049 | Ship **0.3.3** | PO | DONE | tag v0.3.3 = 042c281; versionCode 27; v0.2.15 / v0.3.0 / v0.3.1 / v0.3.2 unchanged | CG-048 |
| CG-040 | Research: offline notify, TG menus, leftover ring, speaker | RESEARCH-01 | DONE | user follow-up | — |
| CG-041 | Android: FGS + lifecycle notify, TG tap/long-press, tones, громкая связь | AND-01 | DONE | this cycle | CG-040 |
| CG-042 | Go: in-memory pending RING when callee offline | GO-01 | DONE | this cycle | CG-040 |
| CG-043 | Independent review | REV-01 | DONE | PASS_WITH_CONCERNS; Android CI later green | CG-041, CG-042 |
| CG-044 | Ship **0.3.2** | PO | DONE | tag v0.3.2 = 477beaf; versionCode 26; v0.2.15 / v0.3.0 / v0.3.1 unchanged | CG-043 |
| CG-026 | Ship **0.3.0** | PO | DONE | tag v0.3.0 = 69c35cb | CG-025 |
| CG-036 | Ship **0.3.1** | PO | DONE | tag v0.3.1; versionCode 25; v0.2.15 and v0.3.0 unchanged | CG-035 |
| CG-003 | No extra overlapping feature agents | PO | IN_PROGRESS | D-006 | — |
| CG-012 | Settings stub | AND-01 | DONE | absorbed by CG-046; shipped in 0.3.3 | — |
| CG-013 | Owner revoke UI | — | BACKLOG | — | — |
| CG-014 | architecture.md Room vs SQLite | — | BACKLOG | — | — |

**Do not move tags `v0.2.15`, `v0.3.0`, `v0.3.1`, `v0.3.2`, or `v0.3.3`.**
