# CryptoGalera Company State

PO-owned live snapshot. Not a changelog.

## Product Goal

**CONFIRMED:** Rope is a private self-hosted Android E2EE messenger. Trunk is `main` at tagged **v0.3.7** (`66c04fc`). Next slice **0.3.8** is revoke-device 409 keyed off remaining owner devices (REV-01). Tags **v0.2.15**, **v0.3.0**, **v0.3.1**, **v0.3.2**, **v0.3.3**, **v0.3.4**, **v0.3.5**, **v0.3.6**, and **v0.3.7** stay published; do not retag them.

## Current Phase

**Delivery law in force (D-015):** PR → review → merge to `main` → tagged GitHub Release. PO is Product Owner of Rope. CG-067 landed. Next: **0.3.8** owner-device 409 (D-021).

## Staffing this cycle

```
User
 └── PO (this run; integrator)
      └── GO-01 / AND-01 — owner-device-count 409 on cursor/owner-device-count-38b9
```

## Canonical git

- Default: **`main`** = tag `v0.3.7` (`66c04fc73510e18ae4cdd2e627d21e1a6eb7e335`)
- Working: `cursor/owner-device-count-38b9`
- GitHub Release: https://github.com/dfa-ra/rope/releases/tag/v0.3.7
- Next ship: **v0.3.8** (do not retag v0.2.15 / v0.3.0 / v0.3.1 / v0.3.2 / v0.3.3 / v0.3.4 / v0.3.5 / v0.3.6 / v0.3.7)
