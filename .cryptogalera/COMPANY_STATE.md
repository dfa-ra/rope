# CryptoGalera Company State

PO-owned live snapshot. Not a changelog.

## Product Goal

**CONFIRMED:** Rope is a private self-hosted Android E2EE messenger. This cycle ships **0.3.1** (Telegram-class chat UI + call path that still works when WebRTC/UDP is blocked). `v0.2.15` and `v0.3.0` stay published; do not retag them.

## Current Phase

**CG-007 unfrozen for this user task only.** Workstream: composer feel, attach sheet, photo bubbles, glass selection, quiet errors, WebRTC + WSS E2EE audio fallback, then tag `v0.3.1`.

Not a general unfreeze: no iOS, no ratchet/MLS, no VK/Yandex/WB call tunnels, no overlapping landing agents.

## Staffing this cycle (User asked; no approval wait)

```
User
 └── PO (this run; integrator)
      ├── RESEARCH-01     CG-030  read-only: call protocols when WebRTC is blocked
      └── ENG-LEAD        CG-031  integration owner (seated in this run)
           ├── AND-01     CG-032  apps/android UI + WSS audio fallback
           ├── GO-01      CG-033  server/go call-media forwarding (only if needed)
           └── RUST-01    CG-034  core/rust only if envelope/type needed
      └── REV-01          CG-035  independent review (after impl; ≠ authors)
```

PO then bumps version and tags **v0.3.1** (CG-036) after PASS.

Workers never spawn. Disjoint paths. Shared ICE JSON / UniFFI / CI go through ENG-LEAD.

## Canonical git

- Default: **`main`**
- Working branch: `cursor/telegram-calls-031-ae19`
- Base: `origin/main` @ 3082bbf (v0.3.0 wrap)
