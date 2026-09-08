# CryptoGalera Company State

PO-owned live snapshot. Not a changelog.

## Product Goal

**CONFIRMED:** Rope is a private self-hosted Android E2EE messenger. Shipped tags **v0.2.15**, **v0.3.0**, **v0.3.1** stay published; do not retag them.

This cycle: **background notifications**, Telegram-style tap/long-press chat chrome, call tone stop, speakerphone (громкая связь). No FCM. No VK/Yandex/WB. Kotlin never implements crypto.

## Current Phase

**CG-007 unfrozen for this user task only (CG-040–CG-043).** No iOS, no ratchet/MLS, no third-party call tunnels.

## Staffing this cycle (User asked; no approval wait)

```
User
 └── PO (this run; integrator)
      ├── RESEARCH-01     CG-040  read-only: offline notify + TG menus + tones
      └── ENG-LEAD        CG-041  integration owner (seated in this run)
           ├── AND-01     CG-041  apps/android: FGS notify, TG gestures, tones, speaker
           └── GO-01      CG-042  server/go: short-TTL in-memory pending call (no plaintext)
      └── REV-01          CG-043  independent review (after impl; ≠ authors)
```

Workers never spawn. Disjoint paths. Shared docs go through ENG-LEAD/PO.

## Canonical git

- Default: **`main`**
- Working branch: `cursor/notify-tg-menus-ae19`
- Base: `origin/main` @ eb78a31 (v0.3.1)
- REV-01: PASS_WITH_CONCERNS (no SDK/device; FGS type fallback added)
