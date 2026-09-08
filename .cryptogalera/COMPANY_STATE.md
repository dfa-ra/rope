# CryptoGalera Company State

PO-owned live snapshot. Not a changelog.

## Product Goal

**CONFIRMED:** Rope is a private self-hosted Android E2EE messenger. This cycle shipped **0.3.1** (Telegram-class chat UI + WSS E2EE call fallback when WebRTC/ICE is blocked). `v0.2.15` and `v0.3.0` stay published; do not retag them.

## Current Phase

**CG-007 unfrozen for this user task only — now closed with v0.3.1.** Next work needs a new user task. No iOS, no ratchet/MLS, no VK/Yandex/WB call tunnels.

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
