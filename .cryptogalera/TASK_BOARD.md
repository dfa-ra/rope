# Task Board

PO-owned. Canonical tracker. One owner per row.

| ID | Task | Owner | Status | Source | Depends On |
| --- | --- | --- | --- | --- | --- |
| CG-110 | Encode D-031 expanded delivery org (leads + research) | ORG-01 | DONE | user 2026-09-09; docs-only; no version bump; D-030 kept as 0.3.17 product | — |
| CG-111 | Standing Research: ranked Telegram-gap epics | RESEARCH | STANDING | D-031; no product merges; PO picks next epic | CG-110 |
| CG-007 | Unfreeze for this user task | PO | IN_PROGRESS | freeze; FLAG_SECURE on Provision/Join tagged v0.3.49; do not merge disappear / silent / polls / attach-cam #64 / folders / chat-archive #75 / global-search #77 / media-hub #82 / mention-picker #83 in this record | — |
| CG-250 | Slice: FLAG_SECURE on Provision and Join as 0.3.49 | PO | DONE | shipped v0.3.49; D-063; FLAG_SECURE on Provision/Join; Invite QR stays shareable; D-062 stays 0.3.48; D-031 stays org law | CG-007 |
| CG-251 | Android: FLAG_SECURE on Provision/Join | AND-01 | DONE | D-063; Invite unlocked; Kotlin never crypto | CG-250 |
| CG-252 | Independent review | REV-01 | DONE | PWC on 4d7f41c (bc-8defc1c6, bc-ce141aca); CI 6/6; PO accepts PWC (Screen.Status leftover) | CG-251 |
| CG-253 | Ship **0.3.49** | PO | DONE | tag v0.3.49 = 4d7f41c; versionCode 73; release published; v0.2.15 / v0.3.0 / v0.3.1 / v0.3.2 / v0.3.3 / v0.3.4 / v0.3.5 / v0.3.6 / v0.3.7 / v0.3.8 / v0.3.9 / v0.3.10 / v0.3.11 / v0.3.12 / v0.3.13 / v0.3.14 / v0.3.15 / v0.3.16 / v0.3.17 / v0.3.18 / v0.3.19 / v0.3.20 / v0.3.21 / v0.3.22 / v0.3.23 / v0.3.24 / v0.3.25 / v0.3.26 / v0.3.27 / v0.3.28 / v0.3.29 / v0.3.30 / v0.3.31 / v0.3.32 / v0.3.33 / v0.3.34 / v0.3.35 / v0.3.36 / v0.3.37 / v0.3.38 / v0.3.39 / v0.3.40 / v0.3.41 / v0.3.42 / v0.3.43 / v0.3.44 / v0.3.45 / v0.3.46 / v0.3.47 / v0.3.48 unchanged | CG-252 |
| CG-245 | Slice: omit public_ip from unauth /v1/info as 0.3.48 | PO | DONE | shipped v0.3.48; D-062; unauth GET /v1/info is server_id + fingerprint only; public_ip stays on authed ICE; D-061 stays 0.3.47; D-031 stays org law | CG-007 |
| CG-246 | Go: unauth GET /v1/info omits public_ip | GO-01 | DONE | D-062; Join still gets server_id and fingerprint; Kotlin never crypto | CG-245 |
| CG-247 | Independent review | REV-01 | DONE | PASS on 2ff77f2; CI 6/6; PO accepts PASS | CG-246 |
| CG-248 | Ship **0.3.48** | PO | DONE | tag v0.3.48 = 2ff77f2; versionCode 72; release published; v0.2.15 / v0.3.0 / v0.3.1 / v0.3.2 / v0.3.3 / v0.3.4 / v0.3.5 / v0.3.6 / v0.3.7 / v0.3.8 / v0.3.9 / v0.3.10 / v0.3.11 / v0.3.12 / v0.3.13 / v0.3.14 / v0.3.15 / v0.3.16 / v0.3.17 / v0.3.18 / v0.3.19 / v0.3.20 / v0.3.21 / v0.3.22 / v0.3.23 / v0.3.24 / v0.3.25 / v0.3.26 / v0.3.27 / v0.3.28 / v0.3.29 / v0.3.30 / v0.3.31 / v0.3.32 / v0.3.33 / v0.3.34 / v0.3.35 / v0.3.36 / v0.3.37 / v0.3.38 / v0.3.39 / v0.3.40 / v0.3.41 / v0.3.42 / v0.3.43 / v0.3.44 / v0.3.45 / v0.3.46 / v0.3.47 unchanged | CG-247 |
| CG-240 | Slice: omit TURN details on public /health as 0.3.47 | PO | DONE | shipped v0.3.47; D-061; public /health ok + turn_running / turn_allocate_ok; loopback keeps TURN IP/ports/error; D-060 stays 0.3.46; D-031 stays org law | CG-007 |
| CG-241 | Go: public /health hides TURN address details | GO-01 | DONE | D-061; RemoteAddr not X-Forwarded-For; install.sh unchanged; Kotlin never crypto | CG-240 |
| CG-242 | Independent review | REV-01 | DONE | PASS on 4d27afe (bc-1d34cae6, bc-c521e024); CI 6/6; PO accepts PASS | CG-241 |
| CG-243 | Ship **0.3.47** | PO | DONE | tag v0.3.47 = 4d27afe; versionCode 71; release published; v0.2.15 / v0.3.0 / v0.3.1 / v0.3.2 / v0.3.3 / v0.3.4 / v0.3.5 / v0.3.6 / v0.3.7 / v0.3.8 / v0.3.9 / v0.3.10 / v0.3.11 / v0.3.12 / v0.3.13 / v0.3.14 / v0.3.15 / v0.3.16 / v0.3.17 / v0.3.18 / v0.3.19 / v0.3.20 / v0.3.21 / v0.3.22 / v0.3.23 / v0.3.24 / v0.3.25 / v0.3.26 / v0.3.27 / v0.3.28 / v0.3.29 / v0.3.30 / v0.3.31 / v0.3.32 / v0.3.33 / v0.3.34 / v0.3.35 / v0.3.36 / v0.3.37 / v0.3.38 / v0.3.39 / v0.3.40 / v0.3.41 / v0.3.42 / v0.3.43 / v0.3.44 / v0.3.45 / v0.3.46 unchanged | CG-242 |
| CG-235 | Slice: restrict GitHub PAT to github.com hosts as 0.3.46 | PO | DONE | shipped v0.3.46; D-060; Bearer only HTTPS github.com / api.github.com; D-059 stays 0.3.45; D-031 stays org law | CG-007 |
| CG-236 | Android: GitHubAuth.bearerFor + ReleaseFetcher | AND-01 | DONE | D-060; Kotlin never crypto; custom binary URLs get no PAT | CG-235 |
| CG-237 | Independent review | REV-01 | DONE | PASS on f65ab30 (bc-92afb394); CI 6/6; 1ffe77e rebase onto e03f6e8; PO accepts PASS | CG-236 |
| CG-238 | Ship **0.3.46** | PO | DONE | tag v0.3.46 = 1ffe77e; versionCode 70; release published; v0.2.15 / v0.3.0 / v0.3.1 / v0.3.2 / v0.3.3 / v0.3.4 / v0.3.5 / v0.3.6 / v0.3.7 / v0.3.8 / v0.3.9 / v0.3.10 / v0.3.11 / v0.3.12 / v0.3.13 / v0.3.14 / v0.3.15 / v0.3.16 / v0.3.17 / v0.3.18 / v0.3.19 / v0.3.20 / v0.3.21 / v0.3.22 / v0.3.23 / v0.3.24 / v0.3.25 / v0.3.26 / v0.3.27 / v0.3.28 / v0.3.29 / v0.3.30 / v0.3.31 / v0.3.32 / v0.3.33 / v0.3.34 / v0.3.35 / v0.3.36 / v0.3.37 / v0.3.38 / v0.3.39 / v0.3.40 / v0.3.41 / v0.3.42 / v0.3.43 / v0.3.44 / v0.3.45 unchanged | CG-237 |
| CG-230 | Slice: wipe SSH PEM after install auth as 0.3.45 | PO | DONE | shipped v0.3.45; D-059; overwrite-unlink temp PEM after authPublickey; D-058 stays 0.3.44; D-031 stays org law | CG-007 |
| CG-231 | Android: CacheSecret wipe + stale ssh-key.pem | AND-01 | DONE | D-059; Kotlin never crypto; FileProvider stays off this path | CG-230 |
| CG-232 | Independent review | REV-01 | DONE | PASS_WITH_CONCERNS on be1c0cc (bc-c788c346); CI 6/6; PO accepts PWC | CG-231 |
| CG-233 | Ship **0.3.45** | PO | DONE | tag v0.3.45 = be1c0cc; versionCode 69; release published; v0.2.15 / v0.3.0 / v0.3.1 / v0.3.2 / v0.3.3 / v0.3.4 / v0.3.5 / v0.3.6 / v0.3.7 / v0.3.8 / v0.3.9 / v0.3.10 / v0.3.11 / v0.3.12 / v0.3.13 / v0.3.14 / v0.3.15 / v0.3.16 / v0.3.17 / v0.3.18 / v0.3.19 / v0.3.20 / v0.3.21 / v0.3.22 / v0.3.23 / v0.3.24 / v0.3.25 / v0.3.26 / v0.3.27 / v0.3.28 / v0.3.29 / v0.3.30 / v0.3.31 / v0.3.32 / v0.3.33 / v0.3.34 / v0.3.35 / v0.3.36 / v0.3.37 / v0.3.38 / v0.3.39 / v0.3.40 / v0.3.41 / v0.3.42 / v0.3.43 / v0.3.44 unchanged | CG-232 |
| CG-225 | Slice: stop bootstrap login enumeration as 0.3.44 | PO | DONE | shipped v0.3.44; D-058; PeekInvite before login uniqueness; D-057 stays 0.3.43; D-031 stays org law | CG-007 |
| CG-226 | Go: PeekInvite before bootstrap display-name check | GO-01 | DONE | D-058; taken login still 409 without consuming invite | CG-225 |
| CG-227 | Independent review | REV-01 | DONE | PASS on b63fc18; CI 6/6; PO accepts PASS | CG-226 |
| CG-228 | Ship **0.3.44** | PO | DONE | tag v0.3.44 = b63fc18; versionCode 68; release published; v0.2.15 / v0.3.0 / v0.3.1 / v0.3.2 / v0.3.3 / v0.3.4 / v0.3.5 / v0.3.6 / v0.3.7 / v0.3.8 / v0.3.9 / v0.3.10 / v0.3.11 / v0.3.12 / v0.3.13 / v0.3.14 / v0.3.15 / v0.3.16 / v0.3.17 / v0.3.18 / v0.3.19 / v0.3.20 / v0.3.21 / v0.3.22 / v0.3.23 / v0.3.24 / v0.3.25 / v0.3.26 / v0.3.27 / v0.3.28 / v0.3.29 / v0.3.30 / v0.3.31 / v0.3.32 / v0.3.33 / v0.3.34 / v0.3.35 / v0.3.36 / v0.3.37 / v0.3.38 / v0.3.39 / v0.3.40 / v0.3.41 / v0.3.42 / v0.3.43 unchanged | CG-227 |
| CG-220 | Slice: busy/flood RING REJECT as 0.3.43 | PO | DONE | shipped v0.3.43; D-057; REJECT surplus RING; D-056 stays 0.3.42; D-031 stays org law | CG-007 |
| CG-221 | Android: REJECT surplus RING without TearDown | AND-01 | DONE | D-057; Kotlin never crypto; glare and live-call RING unchanged | CG-220 |
| CG-222 | Independent review | REV-01 | DONE | PASS on b9b931e; prior PASS on 08e427d; PO accepts PASS | CG-221 |
| CG-223 | Ship **0.3.43** | PO | DONE | tag v0.3.43 = b9b931e; versionCode 67; release published; v0.2.15 / v0.3.0 / v0.3.1 / v0.3.2 / v0.3.3 / v0.3.4 / v0.3.5 / v0.3.6 / v0.3.7 / v0.3.8 / v0.3.9 / v0.3.10 / v0.3.11 / v0.3.12 / v0.3.13 / v0.3.14 / v0.3.15 / v0.3.16 / v0.3.17 / v0.3.18 / v0.3.19 / v0.3.20 / v0.3.21 / v0.3.22 / v0.3.23 / v0.3.24 / v0.3.25 / v0.3.26 / v0.3.27 / v0.3.28 / v0.3.29 / v0.3.30 / v0.3.31 / v0.3.32 / v0.3.33 / v0.3.34 / v0.3.35 / v0.3.36 / v0.3.37 / v0.3.38 / v0.3.39 / v0.3.40 / v0.3.41 / v0.3.42 unchanged | CG-222 |
| CG-215 | Slice: ICE cap 64 + PIP un-mirror as 0.3.42 | PO | DONE | shipped v0.3.42; D-056; ICE_PER_SESSION_CAP 64; rear PIP not mirrored; D-055 stays 0.3.41; D-031 stays org law | CG-007 |
| CG-216 | Android: ICE per-session cap + localPreviewMirrored | AND-01 | DONE | D-056; Kotlin never crypto; front mirrored, rear not | CG-215 |
| CG-217 | Independent review | REV-01 | DONE | PASS on 2222549; 71d4dcb rebase onto 29047b2 record; PO accepts PASS | CG-216 |
| CG-218 | Ship **0.3.42** | PO | DONE | tag v0.3.42 = 71d4dcb; versionCode 66; release published; v0.2.15 / v0.3.0 / v0.3.1 / v0.3.2 / v0.3.3 / v0.3.4 / v0.3.5 / v0.3.6 / v0.3.7 / v0.3.8 / v0.3.9 / v0.3.10 / v0.3.11 / v0.3.12 / v0.3.13 / v0.3.14 / v0.3.15 / v0.3.16 / v0.3.17 / v0.3.18 / v0.3.19 / v0.3.20 / v0.3.21 / v0.3.22 / v0.3.23 / v0.3.24 / v0.3.25 / v0.3.26 / v0.3.27 / v0.3.28 / v0.3.29 / v0.3.30 / v0.3.31 / v0.3.32 / v0.3.33 / v0.3.34 / v0.3.35 / v0.3.36 / v0.3.37 / v0.3.38 / v0.3.39 / v0.3.40 / v0.3.41 unchanged | CG-217 |
| CG-210 | Slice: RECEIPT author/thread checks as 0.3.41 | PO | DONE | shipped v0.3.41; D-055; edit/delete author; pin/react shared thread; D-054 stays 0.3.40; D-031 stays org law | CG-007 |
| CG-211 | Android: ChatControlRules author/thread on RECEIPT | AND-01 | DONE | D-055; Kotlin never crypto; same message_id does not clobber another sender | CG-210 |
| CG-212 | Independent review | REV-01 | DONE | PASS_WITH_CONCERNS on ce70aaa; prior PWC on cc09102; PO accepts PWC | CG-211 |
| CG-213 | Ship **0.3.41** | PO | DONE | tag v0.3.41 = ce70aaa; versionCode 65; release published; v0.2.15 / v0.3.0 / v0.3.1 / v0.3.2 / v0.3.3 / v0.3.4 / v0.3.5 / v0.3.6 / v0.3.7 / v0.3.8 / v0.3.9 / v0.3.10 / v0.3.11 / v0.3.12 / v0.3.13 / v0.3.14 / v0.3.15 / v0.3.16 / v0.3.17 / v0.3.18 / v0.3.19 / v0.3.20 / v0.3.21 / v0.3.22 / v0.3.23 / v0.3.24 / v0.3.25 / v0.3.26 / v0.3.27 / v0.3.28 / v0.3.29 / v0.3.30 / v0.3.31 / v0.3.32 / v0.3.33 / v0.3.34 / v0.3.35 / v0.3.36 / v0.3.37 / v0.3.38 / v0.3.39 / v0.3.40 unchanged | CG-212 |
| CG-205 | Slice: remote 1:1 video + RING/RELAY limits as 0.3.40 | PO | DONE | shipped v0.3.40; D-054; TextureView EGL / onAddStream / SDP CRLF; Go RING/RELAY 6/30s; D-053 stays 0.3.39; D-031 stays org law | CG-007 |
| CG-206 | Android+Go: remote video bind + RING flood / RELAY 6/30s | AND-01 / GO-01 | DONE | D-054; Kotlin never crypto; same-peer RING drop 2s | CG-205 |
| CG-207 | Independent review | REV-01 | DONE | PASS_WITH_CONCERNS on 5f1b1e9/211cc64; 6789b4d rebase onto record + RELAY 6/30s; PO accepts PWC | CG-206 |
| CG-208 | Ship **0.3.40** | PO | DONE | tag v0.3.40 = 6789b4d; versionCode 64; release published; v0.2.15 / v0.3.0 / v0.3.1 / v0.3.2 / v0.3.3 / v0.3.4 / v0.3.5 / v0.3.6 / v0.3.7 / v0.3.8 / v0.3.9 / v0.3.10 / v0.3.11 / v0.3.12 / v0.3.13 / v0.3.14 / v0.3.15 / v0.3.16 / v0.3.17 / v0.3.18 / v0.3.19 / v0.3.20 / v0.3.21 / v0.3.22 / v0.3.23 / v0.3.24 / v0.3.25 / v0.3.26 / v0.3.27 / v0.3.28 / v0.3.29 / v0.3.30 / v0.3.31 / v0.3.32 / v0.3.33 / v0.3.34 / v0.3.35 / v0.3.36 / v0.3.37 / v0.3.38 / v0.3.39 unchanged | CG-207 |
| CG-200 | Slice: DeviceBackup RODB + IdentityVault Keystore wrap as 0.3.39 | PO | DONE | shipped v0.3.39; D-053; wrap API for future writer; toBytes() still clear JSON; D-052 stays 0.3.38; D-031 stays org law | CG-007 |
| CG-201 | Android: DeviceBackup wrap API (RODB + Keystore AES-GCM) | AND-01 | DONE | D-053; Kotlin never crypto; leftover v1 JSON not auto-applied | CG-200 |
| CG-202 | Independent review | REV-01 | DONE | PASS (bc-fceeea88) | CG-201 |
| CG-203 | Ship **0.3.39** | PO | DONE | tag v0.3.39 = 3e7a718; versionCode 63; release published; v0.2.15 / v0.3.0 / v0.3.1 / v0.3.2 / v0.3.3 / v0.3.4 / v0.3.5 / v0.3.6 / v0.3.7 / v0.3.8 / v0.3.9 / v0.3.10 / v0.3.11 / v0.3.12 / v0.3.13 / v0.3.14 / v0.3.15 / v0.3.16 / v0.3.17 / v0.3.18 / v0.3.19 / v0.3.20 / v0.3.21 / v0.3.22 / v0.3.23 / v0.3.24 / v0.3.25 / v0.3.26 / v0.3.27 / v0.3.28 / v0.3.29 / v0.3.30 / v0.3.31 / v0.3.32 / v0.3.33 / v0.3.34 / v0.3.35 / v0.3.36 / v0.3.37 / v0.3.38 unchanged | CG-202 |
| CG-195 | Slice: ICE TURN cache wrap as 0.3.38 | PO | DONE | shipped v0.3.38; D-052; Keystore-wrap cached TURN HMAC; ICE URL allowlist; D-051 stays 0.3.37; D-031 stays org law | CG-007 |
| CG-196 | Android: wrap TURN creds + allowlist ICE URL schemes | AND-01 | DONE | D-052; Kotlin never crypto; stun/turn/turns only | CG-195 |
| CG-197 | Independent review | REV-01 | DONE | PASS_WITH_CONCERNS (bc-7fac14ec) on a7e8a82; delta onto 37ce745 clean (bc-b6cf4a89) | CG-196 |
| CG-198 | Ship **0.3.38** | PO | DONE | tag v0.3.38 = 37ce745; versionCode 62; release published; v0.2.15 / v0.3.0 / v0.3.1 / v0.3.2 / v0.3.3 / v0.3.4 / v0.3.5 / v0.3.6 / v0.3.7 / v0.3.8 / v0.3.9 / v0.3.10 / v0.3.11 / v0.3.12 / v0.3.13 / v0.3.14 / v0.3.15 / v0.3.16 / v0.3.17 / v0.3.18 / v0.3.19 / v0.3.20 / v0.3.21 / v0.3.22 / v0.3.23 / v0.3.24 / v0.3.25 / v0.3.26 / v0.3.27 / v0.3.28 / v0.3.29 / v0.3.30 / v0.3.31 / v0.3.32 / v0.3.33 / v0.3.34 / v0.3.35 / v0.3.36 / v0.3.37 unchanged | CG-197 |
| CG-190 | Slice: WSS auth Phase A as 0.3.37 | PO | DONE | shipped v0.3.37; D-051; header or query, keep query; D-050 stays 0.3.36; D-031 stays org law | CG-007 |
| CG-191 | Go+Android: X-Rope-Ws-Auth or query; Android sends both | GO-01 / AND-01 | DONE | D-051; Kotlin never crypto; query sig stays (Phase B later) | CG-190 |
| CG-192 | Independent review | REV-01 | DONE | PASS_WITH_CONCERNS (bc-fc30413e); leftovers: wss.md wording, WsAuthTest local URL, PR body was stale | CG-191 |
| CG-193 | Ship **0.3.37** | PO | DONE | tag v0.3.37 = aea02ae; versionCode 61; release published; v0.2.15 / v0.3.0 / v0.3.1 / v0.3.2 / v0.3.3 / v0.3.4 / v0.3.5 / v0.3.6 / v0.3.7 / v0.3.8 / v0.3.9 / v0.3.10 / v0.3.11 / v0.3.12 / v0.3.13 / v0.3.14 / v0.3.15 / v0.3.16 / v0.3.17 / v0.3.18 / v0.3.19 / v0.3.20 / v0.3.21 / v0.3.22 / v0.3.23 / v0.3.24 / v0.3.25 / v0.3.26 / v0.3.27 / v0.3.28 / v0.3.29 / v0.3.30 / v0.3.31 / v0.3.32 / v0.3.33 / v0.3.34 / v0.3.35 / v0.3.36 unchanged | CG-192 |
| CG-185 | Slice: sender-packed HTTPS link previews as 0.3.36 | PO | DONE | shipped v0.3.36; D-050; sender-packed OG `lp` in ciphertext; D-031 stays org law | CG-007 |
| CG-186 | Android: HTTPS link previews packed on send | AND-01 | DONE | D-050; Kotlin never crypto; Go does not crawl | CG-185 |
| CG-187 | Independent review | REV-01 | DONE | PASS (bc-2c62eaa3); do not merge leftover ui/sec branches in this record | CG-186 |
| CG-188 | Ship **0.3.36** | PO | DONE | tag v0.3.36 = 2340313; versionCode 60; release published; v0.2.15 / v0.3.0 / v0.3.1 / v0.3.2 / v0.3.3 / v0.3.4 / v0.3.5 / v0.3.6 / v0.3.7 / v0.3.8 / v0.3.9 / v0.3.10 / v0.3.11 / v0.3.12 / v0.3.13 / v0.3.14 / v0.3.15 / v0.3.16 / v0.3.17 / v0.3.18 / v0.3.19 / v0.3.20 / v0.3.21 / v0.3.22 / v0.3.23 / v0.3.24 / v0.3.25 / v0.3.26 / v0.3.27 / v0.3.28 / v0.3.29 / v0.3.30 / v0.3.31 / v0.3.32 / v0.3.33 / v0.3.34 / v0.3.35 unchanged | CG-187 |
| CG-180 | Slice: sealed-envelope SDP + backup dump + TLS pin + /v1/info as 0.3.35 | PO | DONE | shipped v0.3.35; D-049; sealed CALL SDP, private dump, pin fail-closed, no unauth TURN; D-031 stays org law | CG-007 |
| CG-181 | Android: sealed SDP + PublicBackup + TLS pin + github_token wrap | AND-01 | DONE | D-049; Kotlin never crypto | CG-180 |
| CG-182 | Go: /v1/info omits TURN; ignore X-Forwarded-For | GO-01 | DONE | D-049; unauthenticated info has no TURN creds | CG-180 |
| CG-183 | Independent review | REV-01 | DONE | PASS_WITH_CONCERNS (bc-f3c33111); WSS sig query stays; do not merge leftover sec/ui branches in this record | CG-181, CG-182 |
| CG-184 | Ship **0.3.35** | PO | DONE | tag v0.3.35 = 71d504a; versionCode 59; release published; v0.2.15 / v0.3.0 / v0.3.1 / v0.3.2 / v0.3.3 / v0.3.4 / v0.3.5 / v0.3.6 / v0.3.7 / v0.3.8 / v0.3.9 / v0.3.10 / v0.3.11 / v0.3.12 / v0.3.13 / v0.3.14 / v0.3.15 / v0.3.16 / v0.3.17 / v0.3.18 / v0.3.19 / v0.3.20 / v0.3.21 / v0.3.22 / v0.3.23 / v0.3.24 / v0.3.25 / v0.3.26 / v0.3.27 / v0.3.28 / v0.3.29 / v0.3.30 / v0.3.31 / v0.3.32 / v0.3.33 / v0.3.34 unchanged | CG-183 |
| CG-176 | Slice: peer-bound call signaling + install-status as 0.3.34 | PO | DONE | shipped v0.3.34; D-048; CallLink peer bind + non-exported install receiver; D-031 stays org law | CG-007 |
| CG-177 | Android: peer-bound SDP/ICE + InstallStatusReceiver | AND-01 | DONE | D-048; Kotlin never crypto | CG-176 |
| CG-178 | Independent review | REV-01 | DONE | PASS_WITH_CONCERNS; peer-bound signaling tagged; do not merge 0.3.35 in this record | CG-177 |
| CG-179 | Ship **0.3.34** | PO | DONE | tag v0.3.34 = 1f1b81b; versionCode 58; release published; v0.2.15 / v0.3.0 / v0.3.1 / v0.3.2 / v0.3.3 / v0.3.4 / v0.3.5 / v0.3.6 / v0.3.7 / v0.3.8 / v0.3.9 / v0.3.10 / v0.3.11 / v0.3.12 / v0.3.13 / v0.3.14 / v0.3.15 / v0.3.16 / v0.3.17 / v0.3.18 / v0.3.19 / v0.3.20 / v0.3.21 / v0.3.22 / v0.3.23 / v0.3.24 / v0.3.25 / v0.3.26 / v0.3.27 / v0.3.28 / v0.3.29 / v0.3.30 / v0.3.31 / v0.3.32 / v0.3.33 unchanged | CG-178 |
| CG-172 | Slice: Voice notes 2.0 as 0.3.33 | PO | DONE | shipped v0.3.33; D-047; 1.5x/2x, seekable waveform, кружок; D-031 stays org law | CG-007 |
| CG-173 | Android: Voice notes 2.0 + round video notes | AND-01 | DONE | D-047; Kotlin never crypto; in-call camera untouched | CG-172 |
| CG-174 | Independent review | REV-01 | DONE | PASS_WITH_CONCERNS; Voice notes 2.0 tagged; do not merge 0.3.34 in this record | CG-173 |
| CG-175 | Ship **0.3.33** | PO | DONE | tag v0.3.33 = 52178aa; versionCode 57; release published; v0.2.15 / v0.3.0 / v0.3.1 / v0.3.2 / v0.3.3 / v0.3.4 / v0.3.5 / v0.3.6 / v0.3.7 / v0.3.8 / v0.3.9 / v0.3.10 / v0.3.11 / v0.3.12 / v0.3.13 / v0.3.14 / v0.3.15 / v0.3.16 / v0.3.17 / v0.3.18 / v0.3.19 / v0.3.20 / v0.3.21 / v0.3.22 / v0.3.23 / v0.3.24 / v0.3.25 / v0.3.26 / v0.3.27 / v0.3.28 / v0.3.29 / v0.3.30 / v0.3.31 / v0.3.32 unchanged | CG-174 |
| CG-168 | Slice: camera TextureView + stable ring as 0.3.32 | PO | DONE | shipped v0.3.32; D-046; TextureView EGL sink + stable ring; D-031 stays org law | CG-007 |
| CG-169 | Android: TextureView EGL sink + stable ring | AND-01 | DONE | D-046; Kotlin never crypto | CG-168 |
| CG-170 | Independent review | REV-01 | DONE | PASS_WITH_CONCERNS; user camera/tones tagged; do not start 0.3.33 | CG-169 |
| CG-171 | Ship **0.3.32** | PO | DONE | tag v0.3.32 = a681bb7; versionCode 56; release published; v0.2.15 / v0.3.0 / v0.3.1 / v0.3.2 / v0.3.3 / v0.3.4 / v0.3.5 / v0.3.6 / v0.3.7 / v0.3.8 / v0.3.9 / v0.3.10 / v0.3.11 / v0.3.12 / v0.3.13 / v0.3.14 / v0.3.15 / v0.3.16 / v0.3.17 / v0.3.18 / v0.3.19 / v0.3.20 / v0.3.21 / v0.3.22 / v0.3.23 / v0.3.24 / v0.3.25 / v0.3.26 / v0.3.27 / v0.3.28 / v0.3.29 / v0.3.30 / v0.3.31 unchanged | CG-170 |
| CG-164 | Slice: remaining-bug QA as 0.3.31 | PO | DONE | shipped v0.3.31; D-045; glare + callNotice + profile Back; D-031 stays org law | CG-007 |
| CG-165 | Android: callee glare + callNotice + profile Back | AND-01 | DONE | D-045; Kotlin never crypto | CG-164 |
| CG-166 | Independent review | REV-01 | DONE | PASS_WITH_CONCERNS; leftovers are not 0.3.32; user call task stays v0.3.30 | CG-165 |
| CG-167 | Ship **0.3.31** | PO | DONE | tag v0.3.31 = 9f7e039; versionCode 55; release published; v0.2.15 / v0.3.0 / v0.3.1 / v0.3.2 / v0.3.3 / v0.3.4 / v0.3.5 / v0.3.6 / v0.3.7 / v0.3.8 / v0.3.9 / v0.3.10 / v0.3.11 / v0.3.12 / v0.3.13 / v0.3.14 / v0.3.15 / v0.3.16 / v0.3.17 / v0.3.18 / v0.3.19 / v0.3.20 / v0.3.21 / v0.3.22 / v0.3.23 / v0.3.24 / v0.3.25 / v0.3.26 / v0.3.27 / v0.3.28 / v0.3.29 / v0.3.30 unchanged | CG-166 |
| CG-160 | Slice: user calls as 0.3.30 | PO | DONE | shipped v0.3.30; D-044; video bind + clean hangup; D-031 stays org law | CG-007 |
| CG-161 | Android: video bind both directions + clean hangup | AND-01 | DONE | D-044; Kotlin never crypto | CG-160 |
| CG-162 | Independent review | REV-01 | DONE | PASS_WITH_CONCERNS; leftover 0.3.29 QA (glare, callNotice, profile Back) is 0.3.31 on qa-round4 | CG-161 |
| CG-163 | Ship **0.3.30** | PO | DONE | tag v0.3.30 = b8276d7; versionCode 54; release published; v0.2.15 / v0.3.0 / v0.3.1 / v0.3.2 / v0.3.3 / v0.3.4 / v0.3.5 / v0.3.6 / v0.3.7 / v0.3.8 / v0.3.9 / v0.3.10 / v0.3.11 / v0.3.12 / v0.3.13 / v0.3.14 / v0.3.15 / v0.3.16 / v0.3.17 / v0.3.18 / v0.3.19 / v0.3.20 / v0.3.21 / v0.3.22 / v0.3.23 / v0.3.24 / v0.3.25 / v0.3.26 / v0.3.27 / v0.3.28 / v0.3.29 unchanged | CG-162 |
| CG-156 | Slice: remaining-bug QA as 0.3.29 | PO | DONE | shipped v0.3.29; D-043; overlay mic-deny + camera unmute + frozen send; D-031 stays org law | CG-007 |
| CG-157 | Android: overlay mic-deny + camera unmute + frozen send | AND-01 | DONE | D-043; Kotlin never crypto | CG-156 |
| CG-158 | Independent review | REV-01 | DONE | PASS_WITH_CONCERNS; callee renegotiation glare; leftover overlay callNotice; profile Back can dump staged album | CG-157 |
| CG-159 | Ship **0.3.29** | PO | DONE | tag v0.3.29 = 0d23501; versionCode 53; release published; v0.2.15 / v0.3.0 / v0.3.1 / v0.3.2 / v0.3.3 / v0.3.4 / v0.3.5 / v0.3.6 / v0.3.7 / v0.3.8 / v0.3.9 / v0.3.10 / v0.3.11 / v0.3.12 / v0.3.13 / v0.3.14 / v0.3.15 / v0.3.16 / v0.3.17 / v0.3.18 / v0.3.19 / v0.3.20 / v0.3.21 / v0.3.22 / v0.3.23 / v0.3.24 / v0.3.25 / v0.3.26 / v0.3.27 / v0.3.28 unchanged | CG-158 |
| CG-152 | Slice: remaining-bug QA as 0.3.28 | PO | DONE | shipped v0.3.28; D-042; reply+media + camera-deny mute; D-031 stays org law | CG-007 |
| CG-153 | Android: reply+media keep quote + camera-deny mute | AND-01 | DONE | D-042; Kotlin never crypto | CG-152 |
| CG-154 | Independent review | REV-01 | DONE | PASS_WITH_CONCERNS; later enabling camera after deny may not send; onRenegotiationNeeded no-op | CG-153 |
| CG-155 | Ship **0.3.28** | PO | DONE | tag v0.3.28 = aba78e1; versionCode 52; release published; v0.2.15 / v0.3.0 / v0.3.1 / v0.3.2 / v0.3.3 / v0.3.4 / v0.3.5 / v0.3.6 / v0.3.7 / v0.3.8 / v0.3.9 / v0.3.10 / v0.3.11 / v0.3.12 / v0.3.13 / v0.3.14 / v0.3.15 / v0.3.16 / v0.3.17 / v0.3.18 / v0.3.19 / v0.3.20 / v0.3.21 / v0.3.22 / v0.3.23 / v0.3.24 / v0.3.25 / v0.3.26 / v0.3.27 unchanged | CG-154 |
| CG-148 | Slice: icon-only call chrome as 0.3.27 | PO | DONE | shipped v0.3.27; D-041; call-chrome not link-previews; D-031 stays org law | CG-007 |
| CG-149 | Android: CallChromeRules + icon-only rounds | AND-01 | DONE | D-041; a11y contentDescription only; Kotlin never crypto | CG-148 |
| CG-150 | Independent review | REV-01 | DONE | PASS_WITH_CONCERNS; incoming video + camera-deny still video session | CG-149 |
| CG-151 | Ship **0.3.27** | PO | DONE | tag v0.3.27 = bdf155b; versionCode 51; release published; v0.2.15 / v0.3.0 / v0.3.1 / v0.3.2 / v0.3.3 / v0.3.4 / v0.3.5 / v0.3.6 / v0.3.7 / v0.3.8 / v0.3.9 / v0.3.10 / v0.3.11 / v0.3.12 / v0.3.13 / v0.3.14 / v0.3.15 / v0.3.16 / v0.3.17 / v0.3.18 / v0.3.19 / v0.3.20 / v0.3.21 / v0.3.22 / v0.3.23 / v0.3.24 / v0.3.25 / v0.3.26 unchanged | CG-150 |
| CG-144 | Slice: media captions and video-in-album as 0.3.26 | PO | DONE | shipped v0.3.26; D-040; D-031 stays org law | CG-007 |
| CG-145 | Android: MediaSendRules + caption/video-in-album | AND-01 | DONE | D-040; inner JSON caption; Kotlin never crypto | CG-144 |
| CG-146 | Independent review | REV-01 | DONE | PASS_WITH_CONCERNS; composer titles staged video «Фото»; no crop; staged media drops reply; CAPTION_MAX only at send | CG-145 |
| CG-147 | Ship **0.3.26** | PO | DONE | tag v0.3.26 = 3e7cc7e; versionCode 50; release published; v0.2.15 / v0.3.0 / v0.3.1 / v0.3.2 / v0.3.3 / v0.3.4 / v0.3.5 / v0.3.6 / v0.3.7 / v0.3.8 / v0.3.9 / v0.3.10 / v0.3.11 / v0.3.12 / v0.3.13 / v0.3.14 / v0.3.15 / v0.3.16 / v0.3.17 / v0.3.18 / v0.3.19 / v0.3.20 / v0.3.21 / v0.3.22 / v0.3.23 / v0.3.24 / v0.3.25 unchanged | CG-146 |
| CG-140 | Slice: 1:1 video calls as 0.3.25 | PO | DONE | shipped v0.3.25; D-039; D-031 stays org law | CG-007 |
| CG-141 | Android: VideoCallRules + camera/tracks | AND-01 | DONE | D-039; OfferToReceiveVideo; Kotlin never crypto | CG-140 |
| CG-142 | Independent review | REV-01 | DONE | PASS_WITH_CONCERNS; camera deny no audio fallback; no group video; 640×480@24; oversized live offer dropped without teardown; audio WebRtcSession always inits EGL | CG-141 |
| CG-143 | Ship **0.3.25** | PO | DONE | tag v0.3.25 = 9c7fe2b; versionCode 49; release published; v0.2.15 / v0.3.0 / v0.3.1 / v0.3.2 / v0.3.3 / v0.3.4 / v0.3.5 / v0.3.6 / v0.3.7 / v0.3.8 / v0.3.9 / v0.3.10 / v0.3.11 / v0.3.12 / v0.3.13 / v0.3.14 / v0.3.15 / v0.3.16 / v0.3.17 / v0.3.18 / v0.3.19 / v0.3.20 / v0.3.21 / v0.3.22 / v0.3.23 / v0.3.24 unchanged | CG-142 |
| CG-136 | Slice: swipe-to-reply / Replies 2.0 as 0.3.24 | PO | DONE | shipped v0.3.24; D-038; D-031 stays org law | CG-007 |
| CG-137 | Android: SwipeToReplyRules + quote-span qt/qo | AND-01 | DONE | D-038; reuse startReply; Kotlin never crypto | CG-136 |
| CG-138 | Independent review | REV-01 | DONE | PASS_WITH_CONCERNS; gesture tuning; no quote on media-only | CG-137 |
| CG-139 | Ship **0.3.24** | PO | DONE | tag v0.3.24 = 1e989f5; versionCode 48; release published; v0.2.15 / v0.3.0 / v0.3.1 / v0.3.2 / v0.3.3 / v0.3.4 / v0.3.5 / v0.3.6 / v0.3.7 / v0.3.8 / v0.3.9 / v0.3.10 / v0.3.11 / v0.3.12 / v0.3.13 / v0.3.14 / v0.3.15 / v0.3.16 / v0.3.17 / v0.3.18 / v0.3.19 / v0.3.20 / v0.3.21 / v0.3.22 / v0.3.23 unchanged | CG-138 |
| CG-132 | Slice: local Saved Messages as 0.3.23 | PO | DONE | shipped v0.3.23; D-037; D-031 stays org law | CG-007 |
| CG-133 | Android: SavedMessagesRules + LocalStore thread | AND-01 | DONE | D-037; peer_id=saved:; Kotlin never crypto | CG-132 |
| CG-134 | Independent review | REV-01 | DONE | PASS_WITH_CONCERNS; Saved missing from Groups-tab forward picker; uncached media cannot forward into Saved; unused SavedMessagesRules.visible; icon/multi-forward/empty nits | CG-133 |
| CG-135 | Ship **0.3.23** | PO | DONE | tag v0.3.23 = e57be67; versionCode 47; release published; v0.2.15 / v0.3.0 / v0.3.1 / v0.3.2 / v0.3.3 / v0.3.4 / v0.3.5 / v0.3.6 / v0.3.7 / v0.3.8 / v0.3.9 / v0.3.10 / v0.3.11 / v0.3.12 / v0.3.13 / v0.3.14 / v0.3.15 / v0.3.16 / v0.3.17 / v0.3.18 / v0.3.19 / v0.3.20 / v0.3.21 / v0.3.22 unchanged | CG-134 |
| CG-128 | Slice: in-chat video as 0.3.22 | PO | DONE | shipped v0.3.22; D-036; D-031 stays org law | CG-007 |
| CG-129 | Android: VideoRules + compress + in-thread player | AND-01 | DONE | D-036; kind=video; Kotlin never crypto | CG-128 |
| CG-130 | Independent review | REV-01 | DONE | PASS_WITH_CONCERNS; transcode/player quality nits; no streaming; no video albums | CG-129 |
| CG-131 | Ship **0.3.22** | PO | DONE | tag v0.3.22 = db4cd5e; versionCode 46; release published; v0.2.15 / v0.3.0 / v0.3.1 / v0.3.2 / v0.3.3 / v0.3.4 / v0.3.5 / v0.3.6 / v0.3.7 / v0.3.8 / v0.3.9 / v0.3.10 / v0.3.11 / v0.3.12 / v0.3.13 / v0.3.14 / v0.3.15 / v0.3.16 / v0.3.17 / v0.3.18 / v0.3.19 / v0.3.20 / v0.3.21 unchanged | CG-130 |
| CG-124 | Slice: in-thread unread separator as 0.3.21 | PO | DONE | shipped v0.3.21; D-035; D-031 stays org law | CG-007 |
| CG-125 | Android: UnreadSeparatorRules + «Непрочитанные» chip + jump FAB | AND-01 | DONE | D-035; snapshot unread on enter; Kotlin never crypto | CG-124 |
| CG-126 | Independent review | REV-01 | DONE | PASS_WITH_CONCERNS; separator not sticky; DOWN FAB on fully-read when scrolled up; return from peer profile remounts at latest; cluster ignores unread row | CG-125 |
| CG-127 | Ship **0.3.21** | PO | DONE | tag v0.3.21 = 9cd69b3; versionCode 45; release published; v0.2.15 / v0.3.0 / v0.3.1 / v0.3.2 / v0.3.3 / v0.3.4 / v0.3.5 / v0.3.6 / v0.3.7 / v0.3.8 / v0.3.9 / v0.3.10 / v0.3.11 / v0.3.12 / v0.3.13 / v0.3.14 / v0.3.15 / v0.3.16 / v0.3.17 / v0.3.18 / v0.3.19 / v0.3.20 unchanged | CG-126 |
| CG-120 | Slice: peer profile + shared media as 0.3.20 | PO | DONE | shipped v0.3.20; D-034; D-031 stays org law | CG-007 |
| CG-121 | Android: PeerProfileRules + 1:1 header → photo grid | AND-01 | DONE | D-034; LocalStore IMAGE rows; Kotlin never crypto | CG-120 |
| CG-122 | Independent review | REV-01 | DONE | PASS_WITH_CONCERNS; empty grid copy; initials avatar; photos-only; no compose click test; flat pager not album mosaic | CG-121 |
| CG-123 | Ship **0.3.20** | PO | DONE | tag v0.3.20 = f024b13; versionCode 44; release published; v0.2.15 / v0.3.0 / v0.3.1 / v0.3.2 / v0.3.3 / v0.3.4 / v0.3.5 / v0.3.6 / v0.3.7 / v0.3.8 / v0.3.9 / v0.3.10 / v0.3.11 / v0.3.12 / v0.3.13 / v0.3.14 / v0.3.15 / v0.3.16 / v0.3.17 / v0.3.18 / v0.3.19 unchanged | CG-122 |
| CG-116 | Slice: attributed forwards as 0.3.19 | PO | DONE | shipped v0.3.19; D-033; D-031 stays org law | CG-007 |
| CG-117 | Android: ForwardRules + «Переслано от» header | AND-01 | DONE | D-033; origin in encrypted JSON ff; Kotlin never crypto | CG-116 |
| CG-118 | Independent review | REV-01 | DONE | PASS_WITH_CONCERNS; re-forward uses last senderName; no hide-sender; re-seal only with localPath; forwarding one album photo can keep album_id | CG-117 |
| CG-119 | Ship **0.3.19** | PO | DONE | tag v0.3.19 = 39628f0; versionCode 43; release published; v0.2.15 / v0.3.0 / v0.3.1 / v0.3.2 / v0.3.3 / v0.3.4 / v0.3.5 / v0.3.6 / v0.3.7 / v0.3.8 / v0.3.9 / v0.3.10 / v0.3.11 / v0.3.12 / v0.3.13 / v0.3.14 / v0.3.15 / v0.3.16 / v0.3.17 / v0.3.18 unchanged | CG-118 |
| CG-112 | Slice: grouped photo albums as 0.3.18 | PO | DONE | shipped v0.3.18; D-032; D-031 stays org law | CG-007 |
| CG-113 | Android: AlbumRules mosaic/viewer/multi-select | AND-01 | DONE | D-032; keep 0.3.17 chat-list preview; Kotlin never crypto | CG-112 |
| CG-114 | Independent review | REV-01 | DONE | PASS_WITH_CONCERNS; insert-order mosaic split; incomplete album looks like single; quota=N singles | CG-113 |
| CG-115 | Ship **0.3.18** | PO | DONE | tag v0.3.18 = 4961ee9; versionCode 42; release published; v0.2.15 / v0.3.0 / v0.3.1 / v0.3.2 / v0.3.3 / v0.3.4 / v0.3.5 / v0.3.6 / v0.3.7 / v0.3.8 / v0.3.9 / v0.3.10 / v0.3.11 / v0.3.12 / v0.3.13 / v0.3.14 / v0.3.15 / v0.3.16 / v0.3.17 unchanged | CG-114 |
| CG-106 | Slice: chat-list last-message preview as 0.3.17 | PO | DONE | shipped v0.3.17 | CG-007 |
| CG-107 | Android: ChatListPreviewRules + row chrome | AND-01 | DONE | D-030; no 0.3.2 tap/long-press redo | CG-106 |
| CG-108 | Independent review | REV-01 | DONE | PASS_WITH_CONCERNS; idle group «N участников» can hit search as preview; draft accent heuristic; no compose test | CG-107 |
| CG-109 | Ship **0.3.17** | PO | DONE | tag v0.3.17 = 6a1b84e; versionCode 41; release published; v0.2.15 / v0.3.0 / v0.3.1 / v0.3.2 / v0.3.3 / v0.3.4 / v0.3.5 / v0.3.6 / v0.3.7 / v0.3.8 / v0.3.9 / v0.3.10 / v0.3.11 / v0.3.12 / v0.3.13 / v0.3.14 / v0.3.15 / v0.3.16 unchanged | CG-108 |
| CG-102 | Slice: in-thread search empty as 0.3.16 | PO | DONE | shipped v0.3.16 | CG-007 |
| CG-103 | Android: ThreadEmptyRules search miss | AND-01 | DONE | D-029; no 0.3.2 tap/long-press redo | CG-102 |
| CG-104 | Independent review | REV-01 | DONE | PASS_WITH_CONCERNS; lowercase quoted q; miss is «Ничего не найдено» not idle / «Ничего не нашли» | CG-103 |
| CG-105 | Ship **0.3.16** | PO | DONE | tag v0.3.16 = 2a659cb; versionCode 40; release published; v0.2.15 / v0.3.0 / v0.3.1 / v0.3.2 / v0.3.3 / v0.3.4 / v0.3.5 / v0.3.6 / v0.3.7 / v0.3.8 / v0.3.9 / v0.3.10 / v0.3.11 / v0.3.12 / v0.3.13 / v0.3.14 / v0.3.15 unchanged | CG-104 |
| CG-098 | Slice: composer / reply bar as 0.3.15 | PO | DONE | shipped v0.3.15 | CG-007 |
| CG-099 | Android: ComposerHintRules + hint chrome | AND-01 | DONE | D-028; no 0.3.2 tap/long-press redo | CG-098 |
| CG-100 | Independent review | REV-01 | DONE | PASS_WITH_CONCERNS; stripe vs X height; possible double ellipsis; reply/edit targeting intact | CG-099 |
| CG-101 | Ship **0.3.15** | PO | DONE | tag v0.3.15 = db835cc; versionCode 39; release published; v0.2.15 / v0.3.0 / v0.3.1 / v0.3.2 / v0.3.3 / v0.3.4 / v0.3.5 / v0.3.6 / v0.3.7 / v0.3.8 / v0.3.9 / v0.3.10 / v0.3.11 / v0.3.12 / v0.3.13 / v0.3.14 unchanged | CG-100 |
| CG-094 | Slice: chat-list empty-search as 0.3.14 | PO | DONE | shipped v0.3.14 | CG-007 |
| CG-095 | Android: ChatListEmptyRules search miss | AND-01 | DONE | D-027; no 0.3.2 tap/long-press redo | CG-094 |
| CG-096 | Independent review | REV-01 | DONE | PASS_WITH_CONCERNS; lowercase quoted query; in-thread «Ничего не нашли» out of slice | CG-095 |
| CG-097 | Ship **0.3.14** | PO | DONE | tag v0.3.14 = 38bcbc3; versionCode 38; release published; v0.2.15 / v0.3.0 / v0.3.1 / v0.3.2 / v0.3.3 / v0.3.4 / v0.3.5 / v0.3.6 / v0.3.7 / v0.3.8 / v0.3.9 / v0.3.10 / v0.3.11 / v0.3.12 / v0.3.13 unchanged | CG-096 |
| CG-090 | Slice: chat date separators as 0.3.13 | PO | DONE | shipped v0.3.13 | CG-007 |
| CG-091 | Android: DateSeparatorRules + day chips | AND-01 | DONE | D-026; DST daysAgo YEAR/DAY_OF_YEAR | CG-090 |
| CG-092 | Independent review | REV-01 | DONE | PASS_WITH_CONCERNS; idle midnight chip freeze acceptable; DST calendar-day math fixed | CG-091 |
| CG-093 | Ship **0.3.13** | PO | DONE | tag v0.3.13 = dc19da1; versionCode 37; release published; v0.2.15 / v0.3.0 / v0.3.1 / v0.3.2 / v0.3.3 / v0.3.4 / v0.3.5 / v0.3.6 / v0.3.7 / v0.3.8 / v0.3.9 / v0.3.10 / v0.3.11 / v0.3.12 unchanged | CG-092 |
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

**Do not move tags `v0.2.15`, `v0.3.0`, `v0.3.1`, `v0.3.2`, `v0.3.3`, `v0.3.4`, `v0.3.5`, `v0.3.6`, `v0.3.7`, `v0.3.8`, `v0.3.9`, `v0.3.10`, `v0.3.11`, `v0.3.12`, `v0.3.13`, `v0.3.14`, `v0.3.15`, `v0.3.16`, `v0.3.17`, `v0.3.18`, `v0.3.19`, `v0.3.20`, `v0.3.21`, `v0.3.22`, `v0.3.23`, `v0.3.24`, `v0.3.25`, `v0.3.26`, `v0.3.27`, `v0.3.28`, `v0.3.29`, `v0.3.30`, `v0.3.31`, `v0.3.32`, `v0.3.33`, `v0.3.34`, or `v0.3.35`.**
