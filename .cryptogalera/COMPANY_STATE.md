# CryptoGalera Company State

PO-owned live snapshot. Not a changelog.

## Product Goal

**CONFIRMED:** Rope is a private self-hosted Android E2EE messenger. Trunk is `main` at tagged **v0.3.19** (`39628f0`). Next slice **0.3.20** is Telegram-like **peer profile + shared media** (1:1 header → local profile with name + photo grid from LocalStore). Tags **v0.2.15**, **v0.3.0**, **v0.3.1**, **v0.3.2**, **v0.3.3**, **v0.3.4**, **v0.3.5**, **v0.3.6**, **v0.3.7**, **v0.3.8**, **v0.3.9**, **v0.3.10**, **v0.3.11**, **v0.3.12**, **v0.3.13**, **v0.3.14**, **v0.3.15**, **v0.3.16**, **v0.3.17**, **v0.3.18**, and **v0.3.19** stay published; do not retag them.

## Current Phase

**Delivery law in force (D-015 + D-031):** PR → REV-01 → CI → ff-merge `main` → tagged GitHub Release → record on `main`. PO is Product Owner of Rope (D-015 unchanged). Default unit of work is an **epic slice**, not a one-string bump. Domain leads (Android UI / Go relay / Rust core) spawn subordinates and integrate on one branch; they do not self-review. Standing Research ranks Telegram-gap epics and does not merge product code. REV-01 is independent of every implementer and every lead; PASS_WITH_CONCERNS may ship; FAIL blocks tag. Org law D-031 does not bump product versions. CG-119 landed. Last ship **v0.3.19** (product D-033). Next: **0.3.20** peer profile + shared media (must be 0.3.20 / versionCode 44, not 0.3.19). Do not start a tiny copy slice. User sprint is core + UI; continue until the user says stop. REV-01 PASS_WITH_CONCERNS on attributed forwards (re-forward uses last senderName; no hide-sender; re-seal only with localPath; forwarding one album photo can keep album_id) is accepted; not FAIL.

## Staffing this cycle

```
User
 └── PO (this run; Product Owner of Rope; D-015 + D-031)
      ├── Research subteam — standing; ranked Telegram-gap epics; no product merges
      ├── Android UI lead → AND-n subordinates (files/tests they own)
      ├── Go relay lead → GO-n (when server/API/storage)
      ├── Rust core lead → CORE-n (when crypto/protocol/UniFFI)
      ├── AND-01 — peer profile + shared media on cursor/peer-profile-ae19 (0.3.20)
      └── REV-01 — independent of every implementer and every lead on the slice
```

Current product working branch: `cursor/peer-profile-ae19` (0.3.20 peer profile + shared media).

## Canonical git

- Default: **`main`** = tag `v0.3.19` (`39628f0b5c177b19fa19a8692717cb3c748ee86e`)
- Working: `cursor/peer-profile-ae19`
- GitHub Release: https://github.com/dfa-ra/rope/releases/tag/v0.3.19
- Next ship: **v0.3.20** (do not retag v0.2.15 / v0.3.0 / v0.3.1 / v0.3.2 / v0.3.3 / v0.3.4 / v0.3.5 / v0.3.6 / v0.3.7 / v0.3.8 / v0.3.9 / v0.3.10 / v0.3.11 / v0.3.12 / v0.3.13 / v0.3.14 / v0.3.15 / v0.3.16 / v0.3.17 / v0.3.18 / v0.3.19)
