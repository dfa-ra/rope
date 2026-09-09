# CryptoGalera Company State

PO-owned live snapshot. Not a changelog.

## Product Goal

**CONFIRMED:** Rope is a private self-hosted Android E2EE messenger. Trunk is `main` at tagged **v0.3.6** (`b9c4eb8`). Next slice **0.3.7** is last-owner `revoke-device` 409 (REV-01). Tags **v0.2.15**, **v0.3.0**, **v0.3.1**, **v0.3.2**, **v0.3.3**, **v0.3.4**, **v0.3.5**, and **v0.3.6** stay published; do not retag them.

## Current Phase

**Delivery law in force (D-015):** PR → review → merge to `main` → tagged GitHub Release. PO is Product Owner of Rope. Slice **0.3.7** is last-owner `POST /v1/admin/revoke-device` 409 (D-020).

## Staffing this cycle

```
User
 └── PO (this run; integrator)
      └── GO-01 / AND-01 — last-owner revoke-device 409 on cursor/last-owner-revoke-device-ae19
```

## Canonical git

- Default: **`main`** = tag `v0.3.6` (`b9c4eb8bd0ddb49afebf4d385322b880ac1c6f99`)
- Working: `cursor/last-owner-revoke-device-ae19`
- GitHub Release: https://github.com/dfa-ra/rope/releases/tag/v0.3.6
- Next ship: **v0.3.7** (do not retag v0.2.15 / v0.3.0 / v0.3.1 / v0.3.2 / v0.3.3 / v0.3.4 / v0.3.5 / v0.3.6)
