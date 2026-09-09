# Task Board

PO-owned. Canonical tracker. One owner per row.

| ID | Task | Owner | Status | Source | Depends On |
| --- | --- | --- | --- | --- | --- |
| CG-007 | Unfreeze for this user task | PO | IN_PROGRESS | unfreeze for 0.3.13 chat date separators | — |
| CG-090 | Slice: chat date separators as 0.3.13 | PO | IN_PROGRESS | Telegram Сегодня/Вчера/weekday/date | CG-007 |
| CG-091 | Android: DateSeparatorRules + sticky day chips | AND-01 | IN_PROGRESS | D-026; no 0.3.2 tap/long-press redo | CG-090 |
| CG-092 | Independent review | REV-01 | IN_PROGRESS | reviewer ≠ author | CG-091 |
| CG-093 | Ship **0.3.13** | PO | IN_PROGRESS | versionCode 37; do not retag v0.3.12 | CG-092 |
| CG-086 | Slice: muted unread badge as 0.3.12 | PO | DONE | shipped v0.3.12 | CG-007 |
| CG-087 | Android: UnreadBadgeRules MUTED vs ACCENT | AND-01 | DONE | D-025; no 0.3.2 tap/long-press redo | CG-086 |
| CG-088 | Independent review | REV-01 | DONE | PASS; muted vs accent unread badge shipped in 0.3.12 | CG-087 |
| CG-089 | Ship **0.3.12** | PO | DONE | tag v0.3.12 = 57a9bf2; versionCode 36; release published; v0.2.15 / v0.3.0 / v0.3.1 / v0.3.2 / v0.3.3 / v0.3.4 / v0.3.5 / v0.3.6 / v0.3.7 / v0.3.8 / v0.3.9 / v0.3.10 / v0.3.11 unchanged | CG-088 |
| CG-082 | Slice: chat-list word-prefix search as 0.3.11 | PO | DONE | shipped v0.3.11 | CG-007 |
| CG-083 | Android: TITLE_PREFIX on any title word | AND-01 | DONE | D-024; no 0.3.2 tap/long-press redo | CG-082 |
| CG-084 | Independent review | REV-01 | DONE | PASS; word-prefix + İ highlight shipped in 0.3.11 | CG-083 |
| CG-085 | Ship **0.3.11** | PO | DONE | tag v0.3.11 = 8fb333d; versionCode 35; release published; v0.2.15 / v0.3.0 / v0.3.1 / v0.3.2 / v0.3.3 / v0.3.4 / v0.3.5 / v0.3.6 / v0.3.7 / v0.3.8 / v0.3.9 / v0.3.10 unchanged | CG-084 |
| CG-077 | Slice: already-revoked revoke-member 404 as 0.3.10 | PO | DONE | shipped v0.3.10 | CG-007 |
| CG-078 | Go: RevokeMemberGuarded already-revoked → ErrNotFound | GO-01 | DONE | same as RevokeDeviceGuarded | CG-077 |
| CG-079 | Android: revoke-member 404 is already-revoked | AND-01 | DONE | idempotent; no crypto | CG-077 |
| CG-080 | Independent review | REV-01 | DONE | PASS; already-revoked member 404 shipped in 0.3.10 | CG-078, CG-079 |
| CG-081 | Ship **0.3.10** | PO | DONE | tag v0.3.10 = f7d1f96; versionCode 34; release published; v0.2.15 / v0.3.0 / v0.3.1 / v0.3.2 / v0.3.3 / v0.3.4 / v0.3.5 / v0.3.6 / v0.3.7 / v0.3.8 / v0.3.9 unchanged | CG-080 |
| CG-073 | Slice: atomic revoke-member last-owner 409 as 0.3.9 | PO | DONE | shipped v0.3.9 | CG-007 |
| CG-074 | Go: RevokeMemberGuarded BEGIN IMMEDIATE | GO-01 | DONE | same txn as RevokeDeviceGuarded | CG-073 |
| CG-075 | Independent review | REV-01 | DONE | PASS_WITH_CONCERNS; already-revoked member 200 vs 404 is 0.3.10 | CG-074 |
| CG-076 | Ship **0.3.9** | PO | DONE | tag v0.3.9 = 7432f81; versionCode 33; release published; v0.2.15 / v0.3.0 / v0.3.1 / v0.3.2 / v0.3.3 / v0.3.4 / v0.3.5 / v0.3.6 / v0.3.7 / v0.3.8 unchanged | CG-075 |
| CG-068 | Slice: revoke-device 409 on owner devices as 0.3.8 | PO | DONE | shipped v0.3.8 | CG-007 |
| CG-069 | Go: OwnerDeviceCount gate on POST /v1/admin/revoke-device | GO-01 | DONE | not OwnerCount members | CG-068 |
| CG-070 | Android: canRevokeDevice uses ownerDeviceCount | AND-01 | DONE | unused API; People stays revoke-member | CG-068 |
| CG-071 | Independent review | REV-01 | DONE | PASS_WITH_CONCERNS; concurrent revoke-member gap is 0.3.9 | CG-069, CG-070 |
| CG-072 | Ship **0.3.8** | PO | DONE | tag v0.3.8 = ff9301c; versionCode 32; release published; v0.2.15 / v0.3.0 / v0.3.1 / v0.3.2 / v0.3.3 / v0.3.4 / v0.3.5 / v0.3.6 / v0.3.7 unchanged | CG-071 |
| CG-063 | Slice: last-owner revoke-device 409 as 0.3.7 | PO | DONE | shipped v0.3.7 | CG-007 |
| CG-064 | Go: last-owner POST /v1/admin/revoke-device 409 | GO-01 | DONE | like revoke-member | CG-063 |
| CG-065 | Android: do not expose unused revokeDevice for last owner | AND-01 | DONE | RevokeRules.canRevokeDevice; drop unused API | CG-063 |
| CG-066 | Independent review | REV-01 | DONE | PASS_WITH_CONCERNS; co-owner device-count gap is 0.3.8 | CG-064, CG-065 |
| CG-067 | Ship **0.3.7** | PO | DONE | tag v0.3.7 = 66c04fc; versionCode 31; release published; v0.2.15 / v0.3.0 / v0.3.1 / v0.3.2 / v0.3.3 / v0.3.4 / v0.3.5 / v0.3.6 unchanged | CG-066 |
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

**Do not move tags `v0.2.15`, `v0.3.0`, `v0.3.1`, `v0.3.2`, `v0.3.3`, `v0.3.4`, `v0.3.5`, `v0.3.6`, `v0.3.7`, `v0.3.8`, `v0.3.9`, `v0.3.10`, `v0.3.11`, or `v0.3.12`.**
