# CryptoGalera Company State

PO-owned live snapshot. Not a changelog.

## Product Goal

**CONFIRMED:** Rope is a private self-hosted Android E2EE messenger. Trunk is `main` at tagged **v0.3.17** (`6a1b84e`). Next slice **0.3.18** is Telegram-like **grouped photo albums** (already in flight on `cursor/media-albums-ae19`; not 0.3.17). Tags **v0.2.15**, **v0.3.0**, **v0.3.1**, **v0.3.2**, **v0.3.3**, **v0.3.4**, **v0.3.5**, **v0.3.6**, **v0.3.7**, **v0.3.8**, **v0.3.9**, **v0.3.10**, **v0.3.11**, **v0.3.12**, **v0.3.13**, **v0.3.14**, **v0.3.15**, **v0.3.16**, and **v0.3.17** stay published; do not retag them.

## Current Phase

**Delivery law in force (D-015):** PR → review → merge to `main` → tagged GitHub Release. PO is Product Owner of Rope. CG-109 landed. Next: **0.3.18** grouped photo albums (in flight on `cursor/media-albums-ae19`; must be 0.3.18 / versionCode 42, not 0.3.17). Do not start a tiny copy slice. User sprint is core + UI; continue until the user says stop. REV-01 PASS_WITH_CONCERNS on chat-list preview (idle group «N участников» can hit search as preview; draft accent heuristic; no compose test) is accepted; not FAIL.

## Staffing this cycle

```
User
 └── PO (this run; integrator)
      └── AND-01 — grouped photo albums on cursor/media-albums-ae19
```

## Canonical git

- Default: **`main`** = tag `v0.3.17` (`6a1b84e8d8abc1f44cf69e735549f6e0e3299388`)
- Working: `cursor/media-albums-ae19`
- GitHub Release: https://github.com/dfa-ra/rope/releases/tag/v0.3.17
- Next ship: **v0.3.18** (do not retag v0.2.15 / v0.3.0 / v0.3.1 / v0.3.2 / v0.3.3 / v0.3.4 / v0.3.5 / v0.3.6 / v0.3.7 / v0.3.8 / v0.3.9 / v0.3.10 / v0.3.11 / v0.3.12 / v0.3.13 / v0.3.14 / v0.3.15 / v0.3.16 / v0.3.17)
