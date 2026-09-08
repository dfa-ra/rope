# CryptoGalera Company State

PO-owned live snapshot. Not a changelog.

## Product Goal

**CONFIRMED:** Rope is a private self-hosted Android E2EE messenger. This cycle ships **0.3.0** (UI density, calls/TURN, core parse hardening). `v0.2.15` stays published; do not retag it.

## Current Phase

**CG-007 unfrozen for this user task only.** Workstream: UI chrome (no primitive explainers), chat/call design, TURN/calls actually connect, core security/reliability, then tag `v0.3.0`.

Not a general unfreeze: no iOS, no ratchet/MLS, no overlapping landing agents.

## Staffing this cycle (User asked; no approval wait)

```
User
 └── PO (this run; integrator)
      ├── RESEARCH-01     CG-020  read-only: UI inventory + open-messenger patterns + TURN map
      └── ENG-LEAD        CG-021  integration owner (seated)
           ├── AND-01     CG-022  apps/android UI + WebRTC client  (owns Android)
           ├── GO-01      CG-023  server/go + deployment TURN     (owns Go/install)
           └── RUST-01    CG-024  core/rust envelope/media        (owns Rust)
      └── REV-01          CG-025  independent review (after impl; ≠ authors)
```

PO then bumps version and tags **v0.3.0** (CG-026) after PASS.

Workers never spawn. Disjoint paths. Shared ICE JSON / UniFFI / CI go through ENG-LEAD.

## Canonical git

- Default: **`main`**
- Working branch: `cursor/ui-calls-core-030-ae19`
