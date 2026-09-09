# CryptoGalera Company State

PO-owned live snapshot. Not a changelog.

## Product Goal

**CONFIRMED:** Rope is a private self-hosted Android E2EE messenger. Trunk is `main` at tagged **v0.3.33** (`52178aa`). **0.3.33 is Voice notes 2.0** (1x/1.5x/2x playback, seekable waveform, кружок). Camera TextureView + stable ring stays v0.3.32. Tags **v0.2.15**, **v0.3.0**, **v0.3.1**, **v0.3.2**, **v0.3.3**, **v0.3.4**, **v0.3.5**, **v0.3.6**, **v0.3.7**, **v0.3.8**, **v0.3.9**, **v0.3.10**, **v0.3.11**, **v0.3.12**, **v0.3.13**, **v0.3.14**, **v0.3.15**, **v0.3.16**, **v0.3.17**, **v0.3.18**, **v0.3.19**, **v0.3.20**, **v0.3.21**, **v0.3.22**, **v0.3.23**, **v0.3.24**, **v0.3.25**, **v0.3.26**, **v0.3.27**, **v0.3.28**, **v0.3.29**, **v0.3.30**, **v0.3.31**, **v0.3.32**, and **v0.3.33** stay published; do not retag them. Do not merge **0.3.34** / `cursor/sec-0333-ae19` in this record.

## Current Phase

**Delivery law in force (D-015 + D-031):** PR → REV-01 → CI → ff-merge `main` → tagged GitHub Release → record on `main`. PO is Product Owner of Rope (D-015 unchanged). Default unit of work is an **epic slice**, not a one-string bump. Domain leads (Android UI / Go relay / Rust core) spawn subordinates and integrate on one branch; they do not self-review. Standing Research ranks Telegram-gap epics and does not merge product code. REV-01 is independent of every implementer and every lead; PASS_WITH_CONCERNS may ship; FAIL blocks tag. Org law D-031 does not bump product versions. CG-175 landed. Last ship **v0.3.33** (product D-047): Voice notes 2.0 (`versionName=0.3.33` / `versionCode=57`). 1x/1.5x/2x, seekable waveform, кружок. Not 0.3.34. Next: **STOP this record.** `cursor/sec-0333-ae19` (0.3.34) is a separate step after Latest is v0.3.33. No FCM. Kotlin never crypto. Do not redo call camera. REV-01 PASS_WITH_CONCERNS on 0.3.33 is accepted; not FAIL.

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

Current product working branch: **`main`** (v0.3.33 Voice notes 2.0 landed). Do not merge `cursor/sec-0333-ae19` (0.3.34) in this record.

## Canonical git

- Default: **`main`** = tag `v0.3.33` (`52178aa32b78affcec210b1c650d1f1593e6cd18`)
- Working: `main` (STOP this record; 0.3.34 is a separate step)
- GitHub Release: https://github.com/dfa-ra/rope/releases/tag/v0.3.33
- Next ship: **0.3.34** on `cursor/sec-0333-ae19` after this record (do not retag v0.2.15 / v0.3.0 / v0.3.1 / v0.3.2 / v0.3.3 / v0.3.4 / v0.3.5 / v0.3.6 / v0.3.7 / v0.3.8 / v0.3.9 / v0.3.10 / v0.3.11 / v0.3.12 / v0.3.13 / v0.3.14 / v0.3.15 / v0.3.16 / v0.3.17 / v0.3.18 / v0.3.19 / v0.3.20 / v0.3.21 / v0.3.22 / v0.3.23 / v0.3.24 / v0.3.25 / v0.3.26 / v0.3.27 / v0.3.28 / v0.3.29 / v0.3.30 / v0.3.31 / v0.3.32 / v0.3.33)
