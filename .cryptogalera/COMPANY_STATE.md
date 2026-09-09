# CryptoGalera Company State

PO-owned live snapshot. Not a changelog.

## Product Goal

**CONFIRMED:** Rope is a private self-hosted Android E2EE messenger. Trunk is `main` at tagged **v0.3.34** (`1f1b81b`). **0.3.34 is peer-bound call signaling + install-status redirect close**. Voice notes 2.0 stays v0.3.33. Tags **v0.2.15**, **v0.3.0**, **v0.3.1**, **v0.3.2**, **v0.3.3**, **v0.3.4**, **v0.3.5**, **v0.3.6**, **v0.3.7**, **v0.3.8**, **v0.3.9**, **v0.3.10**, **v0.3.11**, **v0.3.12**, **v0.3.13**, **v0.3.14**, **v0.3.15**, **v0.3.16**, **v0.3.17**, **v0.3.18**, **v0.3.19**, **v0.3.20**, **v0.3.21**, **v0.3.22**, **v0.3.23**, **v0.3.24**, **v0.3.25**, **v0.3.26**, **v0.3.27**, **v0.3.28**, **v0.3.29**, **v0.3.30**, **v0.3.31**, **v0.3.32**, **v0.3.33**, and **v0.3.34** stay published; do not retag them. Do not merge **0.3.35** / `cursor/sec-next-ae19` in this record.

## Current Phase

**Delivery law in force (D-015 + D-031):** PR → REV-01 → CI → ff-merge `main` → tagged GitHub Release → record on `main`. PO is Product Owner of Rope (D-015 unchanged). Default unit of work is an **epic slice**, not a one-string bump. Domain leads (Android UI / Go relay / Rust core) spawn subordinates and integrate on one branch; they do not self-review. Standing Research ranks Telegram-gap epics and does not merge product code. REV-01 is independent of every implementer and every lead; PASS_WITH_CONCERNS may ship; FAIL blocks tag. Org law D-031 does not bump product versions. CG-179 landed. Last ship **v0.3.34** (product D-048): peer-bound call signaling + install-status redirect close (`versionName=0.3.34` / `versionCode=58`). Live offer/answer/ICE bind to the current peer; SDP/ICE drop CRLF and `file:` / `javascript:` / `data:` schemes; APK install status is a non-exported receiver. Not 0.3.35. Next: **STOP this record.** `cursor/sec-next-ae19` (0.3.35) is a separate step after Latest is v0.3.34. No FCM. Kotlin never crypto. Do not redo voice notes. REV-01 PASS_WITH_CONCERNS on 0.3.34 is accepted; not FAIL.

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

Current product working branch: **`main`** (v0.3.34 peer-bound call signaling landed). Do not merge `cursor/sec-next-ae19` (0.3.35) in this record.

## Canonical git

- Default: **`main`** = tag `v0.3.34` (`1f1b81bf2a695b09f7c2f592ef2a987954effac4`)
- Working: `main` (STOP this record; 0.3.35 is a separate step)
- GitHub Release: https://github.com/dfa-ra/rope/releases/tag/v0.3.34
- Next ship: **0.3.35** on `cursor/sec-next-ae19` after this record (do not retag v0.2.15 / v0.3.0 / v0.3.1 / v0.3.2 / v0.3.3 / v0.3.4 / v0.3.5 / v0.3.6 / v0.3.7 / v0.3.8 / v0.3.9 / v0.3.10 / v0.3.11 / v0.3.12 / v0.3.13 / v0.3.14 / v0.3.15 / v0.3.16 / v0.3.17 / v0.3.18 / v0.3.19 / v0.3.20 / v0.3.21 / v0.3.22 / v0.3.23 / v0.3.24 / v0.3.25 / v0.3.26 / v0.3.27 / v0.3.28 / v0.3.29 / v0.3.30 / v0.3.31 / v0.3.32 / v0.3.33 / v0.3.34)
