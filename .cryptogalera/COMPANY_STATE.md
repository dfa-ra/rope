# CryptoGalera Company State

PO-owned live snapshot. Not a changelog.

## Product Goal

**CONFIRMED:** Rope is a private self-hosted Android E2EE messenger. Trunk is `main` at tagged **v0.3.8** (`ff9301c`). Next slice **0.3.9** is last-owner `revoke-member` 409 in the same `BEGIN IMMEDIATE` transaction as `RevokeDeviceGuarded` (REV-01). Tags **v0.2.15**, **v0.3.0**, **v0.3.1**, **v0.3.2**, **v0.3.3**, **v0.3.4**, **v0.3.5**, **v0.3.6**, **v0.3.7**, and **v0.3.8** stay published; do not retag them.

## Current Phase

**Delivery law in force (D-015):** PR → review → merge to `main` → tagged GitHub Release. PO is Product Owner of Rope. CG-072 landed. Next: **0.3.9** atomic revoke-member 409 (D-022).

## Staffing this cycle

```
User
 └── PO (this run; integrator)
      └── GO-01 — atomic revoke-member 409 on cursor/revoke-member-atomic-8698
```

## Canonical git

- Default: **`main`** = tag `v0.3.8` (`ff9301c1604210647616fb826cf04c4cd03b4b08`)
- Working: `cursor/revoke-member-atomic-8698`
- GitHub Release: https://github.com/dfa-ra/rope/releases/tag/v0.3.8
- Next ship: **v0.3.9** (do not retag v0.2.15 / v0.3.0 / v0.3.1 / v0.3.2 / v0.3.3 / v0.3.4 / v0.3.5 / v0.3.6 / v0.3.7 / v0.3.8)
