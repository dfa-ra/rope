# Task Board

PO-owned. Canonical tracker. One owner per row.

| ID | Task | Owner | Status | Source | Depends On |
| --- | --- | --- | --- | --- | --- |
| CG-007 | Unfreeze for this user task | PO | DONE | unfreeze for 0.3.6 complete | — |
| CG-058 | Slice: owner revoke UI as 0.3.6 | PO | DONE | shipped v0.3.6 | CG-007 |
| CG-059 | Android: People Исключить + RevokeRules | AND-01 | DONE | D-019 | CG-058 |
| CG-060 | Go: last-owner 409 + drop WSS on revoke | GO-01 | DONE | existing POST /v1/admin/revoke-* | CG-058 |
| CG-061 | Independent review | REV-01 | DONE | PASS_WITH_CONCERNS | CG-059, CG-060 |
| CG-062 | Ship **0.3.6** | PO | DONE | tag v0.3.6 = b9c4eb8; versionCode 30; v0.2.15 / v0.3.0 / v0.3.1 / v0.3.2 / v0.3.3 / v0.3.4 / v0.3.5 unchanged | CG-061 |
| CG-054 | Slice: Telegram-like chat list search + pinned polish as 0.3.5 | PO | DONE | shipped v0.3.5 | CG-007 |
| CG-055 | Android: ranked search, highlight, pill field, pin divider | AND-01 | DONE | D-018 | CG-054 |
| CG-056 | Independent review | REV-01 | DONE | PASS after normalize assertion fix | CG-055 |
| CG-057 | Ship **0.3.5** | PO | DONE | tag v0.3.5 = cf83e48; versionCode 29; v0.2.15 / v0.3.0 / v0.3.1 / v0.3.2 / v0.3.3 / v0.3.4 unchanged | CG-056 |
| CG-050 | Slice: mute vs call ringtone tests as 0.3.4 | PO | DONE | shipped v0.3.4 | CG-007 |
| CG-051 | Android: `CallToneRules` + wire RingIn/NotifyIncoming | AND-01 | DONE | D-016 / D-017 | CG-050 |
| CG-052 | Independent review | REV-01 | DONE | reviewer ≠ implementer | CG-051 |
| CG-053 | Ship **0.3.4** | PO | DONE | tag v0.3.4 = 2066ead; versionCode 28; v0.2.15 / v0.3.0 / v0.3.1 / v0.3.2 / v0.3.3 unchanged | CG-052 |
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
| CG-013 | Owner revoke UI | AND-01 | DONE | D-019; shipped in 0.3.6 | CG-058 |
| CG-014 | architecture.md Room vs SQLite | — | BACKLOG | — | — |

**Do not move tags `v0.2.15`, `v0.3.0`, `v0.3.1`, `v0.3.2`, `v0.3.3`, `v0.3.4`, `v0.3.5`, or `v0.3.6`.**
