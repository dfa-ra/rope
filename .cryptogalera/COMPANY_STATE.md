# CryptoGalera Company State

PO-owned live snapshot. Not a changelog.

## Product Goal

**CONFIRMED:** Rope is a private self-hosted Android E2EE messenger. Trunk is `main` at tagged **v0.3.30** (`b8276d7`). **0.3.30 is user calls** (1:1 video bind both directions; explicit hangup/BYE tears both sides to idle). Voice notes and pinned-in-thread already shipped on main. Tags **v0.2.15**, **v0.3.0**, **v0.3.1**, **v0.3.2**, **v0.3.3**, **v0.3.4**, **v0.3.5**, **v0.3.6**, **v0.3.7**, **v0.3.8**, **v0.3.9**, **v0.3.10**, **v0.3.11**, **v0.3.12**, **v0.3.13**, **v0.3.14**, **v0.3.15**, **v0.3.16**, **v0.3.17**, **v0.3.18**, **v0.3.19**, **v0.3.20**, **v0.3.21**, **v0.3.22**, **v0.3.23**, **v0.3.24**, **v0.3.25**, **v0.3.26**, **v0.3.27**, **v0.3.28**, **v0.3.29**, and **v0.3.30** stay published; do not retag them. Do not tag qa-round4 as 0.3.30. Do not start **0.3.32**. Do not tag link-previews.

## Current Phase

**Delivery law in force (D-015 + D-031):** PR → REV-01 → CI → ff-merge `main` → tagged GitHub Release → record on `main`. PO is Product Owner of Rope (D-015 unchanged). Default unit of work is an **epic slice**, not a one-string bump. Domain leads (Android UI / Go relay / Rust core) spawn subordinates and integrate on one branch; they do not self-review. Standing Research ranks Telegram-gap epics and does not merge product code. REV-01 is independent of every implementer and every lead; PASS_WITH_CONCERNS may ship; FAIL blocks tag. Org law D-031 does not bump product versions. CG-163 landed. Last ship **v0.3.30** (product D-044): user calls (`versionName=0.3.30` / `versionCode=54`). Video bind both directions; clean hangup. Not qa-round4, not link-previews. Next: remaining-bug QA as **0.3.31** on `cursor/qa-round4-ae19` (PR #49) — callee renegotiation glare; leftover overlay callNotice; profile Back can dump staged album — do not merge or tag `v0.3.31` until REV-01. No FCM. Kotlin never crypto. Do not start a tiny copy slice. User sprint is core + UI; continue until the user says stop. REV-01 PASS_WITH_CONCERNS on 0.3.30 (leftover 0.3.29 QA is 0.3.31) is accepted; not FAIL. User call acceptance is v0.3.30.

## Staffing this cycle

```
User
 └── PO (this run; Product Owner of Rope; D-015 + D-031)
      ├── Research subteam — standing; ranked Telegram-gap epics; no product merges
      ├── Android UI lead → AND-n subordinates (files/tests they own)
      ├── Go relay lead → GO-n (when server/API/storage)
      ├── Rust core lead → CORE-n (when crypto/protocol/UniFFI)
      └── REV-01 — independent of every implementer and every lead on the slice
```

Current product working branch: **`main`** (v0.3.30 user calls landed). Next feature: `cursor/qa-round4-ae19` as 0.3.31 (do not merge).

## Canonical git

- Default: **`main`** = tag `v0.3.30` (`b8276d78146fb00421dbd91bca209f8bdd5ce8d1`)
- Working: `cursor/qa-round4-ae19` (0.3.31 remaining-bug QA; do not merge or tag until REV-01)
- GitHub Release: https://github.com/dfa-ra/rope/releases/tag/v0.3.30
- Next ship: **0.3.31 after REV-01** (do not retag v0.2.15 / v0.3.0 / v0.3.1 / v0.3.2 / v0.3.3 / v0.3.4 / v0.3.5 / v0.3.6 / v0.3.7 / v0.3.8 / v0.3.9 / v0.3.10 / v0.3.11 / v0.3.12 / v0.3.13 / v0.3.14 / v0.3.15 / v0.3.16 / v0.3.17 / v0.3.18 / v0.3.19 / v0.3.20 / v0.3.21 / v0.3.22 / v0.3.23 / v0.3.24 / v0.3.25 / v0.3.26 / v0.3.27 / v0.3.28 / v0.3.29 / v0.3.30)
