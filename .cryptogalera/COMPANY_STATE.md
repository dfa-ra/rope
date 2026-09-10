# CryptoGalera Company State

PO-owned live snapshot. Not a changelog.

## Product Goal

**CONFIRMED:** Rope is a private self-hosted Android E2EE messenger. Trunk is `main` at tagged **v0.3.53** (`b3a536f0`). **0.3.53 is FileProvider open for cache/updates APKs fail-closed; CallSignal.parseEvent, group member device_id, WSS call events, and ICE searching-path names reject CR/LF/NUL**. LocalStore 12-byte IV stays v0.3.51. Tags **v0.2.15**, **v0.3.0**, **v0.3.1**, **v0.3.2**, **v0.3.3**, **v0.3.4**, **v0.3.5**, **v0.3.6**, **v0.3.7**, **v0.3.8**, **v0.3.9**, **v0.3.10**, **v0.3.11**, **v0.3.12**, **v0.3.13**, **v0.3.14**, **v0.3.15**, **v0.3.16**, **v0.3.17**, **v0.3.18**, **v0.3.19**, **v0.3.20**, **v0.3.21**, **v0.3.22**, **v0.3.23**, **v0.3.24**, **v0.3.25**, **v0.3.26**, **v0.3.27**, **v0.3.28**, **v0.3.29**, **v0.3.30**, **v0.3.31**, **v0.3.32**, **v0.3.33**, **v0.3.34**, **v0.3.35**, **v0.3.36**, **v0.3.37**, **v0.3.38**, **v0.3.39**, **v0.3.40**, **v0.3.41**, **v0.3.42**, **v0.3.43**, **v0.3.44**, **v0.3.45**, **v0.3.46**, **v0.3.47**, **v0.3.48**, **v0.3.49**, **v0.3.50**, **v0.3.51**, **v0.3.52**, and **v0.3.53** stay published; do not retag them. Do not merge **ui-disappear**, **ui-silent**, **ui-polls**, **attach-cam #64**, **folders**, **global-search #77**, **media-hub #82**, or **mention-picker #83** in this record.

## Current Phase

**Delivery law in force (D-015 + D-031):** PR → REV-01 → CI → ff-merge `main` → tagged GitHub Release → record on `main`. PO is Product Owner of Rope (D-015 unchanged). Default unit of work is an **epic slice**, not a one-string bump. Domain leads (Android UI / Go relay / Rust core) spawn subordinates and integrate on one branch; they do not self-review. Standing Research ranks Telegram-gap epics and does not merge product code. REV-01 is independent of every implementer and every lead; PASS_WITH_CONCERNS may ship; FAIL blocks tag. Org law D-031 does not bump product versions. CG-273 landed. Last ship **v0.3.53** (product D-067): FileProvider APK fail-closed, CallSignal.parseEvent, group member device_id, WSS call events, ICE searching-path CR/LF/NUL (`versionName=0.3.53` / `versionCode=77`). D-066 remains 0.3.52. Not disappear / silent / polls / attach-cam #64 / folders / global-search #77 / media-hub #82 / mention-picker #83. Next: **STOP this record.** No FCM. Kotlin never crypto. CI 6/6 (push 34468105526 + main 34468326917); Release 34468329056.

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

Current product working branch: **`main`** (v0.3.53 FileProvider APK + CR/LF/NUL pack landed). Do not merge `cursor/ui-disappear-ae19`, `cursor/ui-silent-ae19`, `cursor/ui-polls-ae19`, `cursor/ui-attach-cam-ae19` (#64), `cursor/ui-folders-ae19`, `cursor/ui-global-search-0f6f` (#77), `cursor/ui-media-hub-0f6f` (#82), or `cursor/ui-mention-picker-0f6f` (#83) in this record.

## Canonical git

- Default: **`main`** = tag `v0.3.53` (`b3a536f0d3e7bba6a72cd510e9f32daa41690a0c`)
- Working: `main` (STOP this record)
- GitHub Release: https://github.com/dfa-ra/rope/releases/tag/v0.3.53
- Next ship: **STOP this record** (do not retag v0.2.15 / v0.3.0 / v0.3.1 / v0.3.2 / v0.3.3 / v0.3.4 / v0.3.5 / v0.3.6 / v0.3.7 / v0.3.8 / v0.3.9 / v0.3.10 / v0.3.11 / v0.3.12 / v0.3.13 / v0.3.14 / v0.3.15 / v0.3.16 / v0.3.17 / v0.3.18 / v0.3.19 / v0.3.20 / v0.3.21 / v0.3.22 / v0.3.23 / v0.3.24 / v0.3.25 / v0.3.26 / v0.3.27 / v0.3.28 / v0.3.29 / v0.3.30 / v0.3.31 / v0.3.32 / v0.3.33 / v0.3.34 / v0.3.35 / v0.3.36 / v0.3.37 / v0.3.38 / v0.3.39 / v0.3.40 / v0.3.41 / v0.3.42 / v0.3.43 / v0.3.44 / v0.3.45 / v0.3.46 / v0.3.47 / v0.3.48 / v0.3.49 / v0.3.50 / v0.3.51 / v0.3.52 / v0.3.53)
