# CryptoGalera Company State

PO-owned live snapshot. Not a changelog.

## Product Goal

**CONFIRMED:** Rope is a private self-hosted Android E2EE messenger. Trunk is `main` at tagged **v0.3.37** (`aea02ae`). **0.3.37 is WSS auth Phase A**. Sender-packed HTTPS link previews stay v0.3.36. Tags **v0.2.15**, **v0.3.0**, **v0.3.1**, **v0.3.2**, **v0.3.3**, **v0.3.4**, **v0.3.5**, **v0.3.6**, **v0.3.7**, **v0.3.8**, **v0.3.9**, **v0.3.10**, **v0.3.11**, **v0.3.12**, **v0.3.13**, **v0.3.14**, **v0.3.15**, **v0.3.16**, **v0.3.17**, **v0.3.18**, **v0.3.19**, **v0.3.20**, **v0.3.21**, **v0.3.22**, **v0.3.23**, **v0.3.24**, **v0.3.25**, **v0.3.26**, **v0.3.27**, **v0.3.28**, **v0.3.29**, **v0.3.30**, **v0.3.31**, **v0.3.32**, **v0.3.33**, **v0.3.34**, **v0.3.35**, **v0.3.36**, and **v0.3.37** stay published; do not retag them. Do not merge **ICE #58 / sec-main-p1**, **DeviceBackup #60**, **ui-disappear**, **ui-silent**, **ui-polls**, **attach-cam #64**, or **folders** in this record.

## Current Phase

**Delivery law in force (D-015 + D-031):** PR → REV-01 → CI → ff-merge `main` → tagged GitHub Release → record on `main`. PO is Product Owner of Rope (D-015 unchanged). Default unit of work is an **epic slice**, not a one-string bump. Domain leads (Android UI / Go relay / Rust core) spawn subordinates and integrate on one branch; they do not self-review. Standing Research ranks Telegram-gap epics and does not merge product code. REV-01 is independent of every implementer and every lead; PASS_WITH_CONCERNS may ship; FAIL blocks tag. Org law D-031 does not bump product versions. CG-193 landed. Last ship **v0.3.37** (product D-051): WSS auth Phase A (`versionName=0.3.37` / `versionCode=61`). Server accepts `X-Rope-Ws-Auth` or query `device_id`/`ts`/`sig`; prefers the header when present; Android sends both; query `sig=` stays (Phase B later). D-050 remains 0.3.36. Not ICE #58 / DeviceBackup #60 / disappear / silent / polls / attach-cam #64 / folders. Next: **STOP this record.** No FCM. Kotlin never crypto. REV-01 PASS_WITH_CONCERNS (bc-fc30413e) on 0.3.37 is accepted; not FAIL. Leftovers: `wss.md` wording, `WsAuthTest` local URL, PR body was stale.

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

Current product working branch: **`main`** (v0.3.37 WSS auth Phase A landed). Do not merge `cursor/sec-main-p1-ae19` (ICE #58), `cursor/sec-devicebackup-ae19` (#60), `cursor/ui-disappear-ae19`, `cursor/ui-silent-ae19`, `cursor/ui-polls-ae19`, `cursor/ui-attach-cam-ae19` (#64), or `cursor/ui-folders-ae19` in this record.

## Canonical git

- Default: **`main`** = tag `v0.3.37` (`aea02aeaef6df5f58dcd8509e8f31e5fdc2ca33c`)
- Working: `main` (STOP this record)
- GitHub Release: https://github.com/dfa-ra/rope/releases/tag/v0.3.37
- Next ship: **STOP this record** (do not retag v0.2.15 / v0.3.0 / v0.3.1 / v0.3.2 / v0.3.3 / v0.3.4 / v0.3.5 / v0.3.6 / v0.3.7 / v0.3.8 / v0.3.9 / v0.3.10 / v0.3.11 / v0.3.12 / v0.3.13 / v0.3.14 / v0.3.15 / v0.3.16 / v0.3.17 / v0.3.18 / v0.3.19 / v0.3.20 / v0.3.21 / v0.3.22 / v0.3.23 / v0.3.24 / v0.3.25 / v0.3.26 / v0.3.27 / v0.3.28 / v0.3.29 / v0.3.30 / v0.3.31 / v0.3.32 / v0.3.33 / v0.3.34 / v0.3.35 / v0.3.36 / v0.3.37)
