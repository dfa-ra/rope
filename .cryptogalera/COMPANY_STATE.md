# CryptoGalera Company State

PO-owned live snapshot. Not a changelog.

## Product Goal

**CONFIRMED:** Rope is a private self-hosted Android E2EE messenger. Trunk is `main` at tagged **v0.3.35** (`71d504a`). **0.3.35 is sealed-envelope call SDP + private backup dump + TLS pin fail-closed + unauthenticated /v1/info without TURN**. Peer-bound call signaling stays v0.3.34. Tags **v0.2.15**, **v0.3.0**, **v0.3.1**, **v0.3.2**, **v0.3.3**, **v0.3.4**, **v0.3.5**, **v0.3.6**, **v0.3.7**, **v0.3.8**, **v0.3.9**, **v0.3.10**, **v0.3.11**, **v0.3.12**, **v0.3.13**, **v0.3.14**, **v0.3.15**, **v0.3.16**, **v0.3.17**, **v0.3.18**, **v0.3.19**, **v0.3.20**, **v0.3.21**, **v0.3.22**, **v0.3.23**, **v0.3.24**, **v0.3.25**, **v0.3.26**, **v0.3.27**, **v0.3.28**, **v0.3.29**, **v0.3.30**, **v0.3.31**, **v0.3.32**, **v0.3.33**, **v0.3.34**, and **v0.3.35** stay published; do not retag them. Do not merge **ui-previews**, **sec-wss-sig**, **sec-main-p1**, **ui-disappear**, or **sec-devicebackup** in this record.

## Current Phase

**Delivery law in force (D-015 + D-031):** PR → REV-01 → CI → ff-merge `main` → tagged GitHub Release → record on `main`. PO is Product Owner of Rope (D-015 unchanged). Default unit of work is an **epic slice**, not a one-string bump. Domain leads (Android UI / Go relay / Rust core) spawn subordinates and integrate on one branch; they do not self-review. Standing Research ranks Telegram-gap epics and does not merge product code. REV-01 is independent of every implementer and every lead; PASS_WITH_CONCERNS may ship; FAIL blocks tag. Org law D-031 does not bump product versions. CG-184 landed. Last ship **v0.3.35** (product D-049): sealed-envelope SDP, private backup dump, TLS pin fail-closed, unauthenticated `/v1/info` omits TURN (`versionName=0.3.35` / `versionCode=59`). WSS `sig` query stays. Not ui-previews / sec-wss-sig / sec-main-p1 / ui-disappear / sec-devicebackup. Next: **STOP this record.** No FCM. Kotlin never crypto. REV-01 PASS_WITH_CONCERNS (bc-f3c33111) on 0.3.35 is accepted; not FAIL.

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

Current product working branch: **`main`** (v0.3.35 sealed-envelope SDP landed). Do not merge `cursor/ui-previews-ae19`, `cursor/sec-wss-sig-ae19`, `cursor/sec-main-p1-ae19`, `cursor/ui-disappear-ae19`, or `cursor/sec-devicebackup-ae19` in this record.

## Canonical git

- Default: **`main`** = tag `v0.3.35` (`71d504a31270f9d8cd24e816611650db622b8296`)
- Working: `main` (STOP this record)
- GitHub Release: https://github.com/dfa-ra/rope/releases/tag/v0.3.35
- Next ship: **STOP this record** (do not retag v0.2.15 / v0.3.0 / v0.3.1 / v0.3.2 / v0.3.3 / v0.3.4 / v0.3.5 / v0.3.6 / v0.3.7 / v0.3.8 / v0.3.9 / v0.3.10 / v0.3.11 / v0.3.12 / v0.3.13 / v0.3.14 / v0.3.15 / v0.3.16 / v0.3.17 / v0.3.18 / v0.3.19 / v0.3.20 / v0.3.21 / v0.3.22 / v0.3.23 / v0.3.24 / v0.3.25 / v0.3.26 / v0.3.27 / v0.3.28 / v0.3.29 / v0.3.30 / v0.3.31 / v0.3.32 / v0.3.33 / v0.3.34 / v0.3.35)
