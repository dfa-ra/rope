# CryptoGalera Company State

PO-owned live snapshot. Not a changelog.

## Product Goal

**CONFIRMED:** Rope is a private self-hosted Android E2EE messenger. This cycle ships **0.3.2** (background notify + Telegram tap/long-press + speakerphone). Tags **v0.2.15**, **v0.3.0**, and **v0.3.1** stay published; do not retag them.

## Current Phase

**CG-007 unfrozen for CG-044 ship only.** Product work CG-040–043 is done and CI-green. No iOS, no ratchet/MLS, no VK/Yandex/WB.

## Staffing this cycle (User asked; no approval wait)

```
User
 └── PO (this run; integrator)
      └── ENG-LEAD/PO   CG-044  bump 0.3.2, land main, tag v0.3.2
```

Workers never spawn. Do not retag published releases.

## Canonical git

- Default: **`main`**
- Working branch: `cursor/notify-tg-menus-ae19`
- Base: `origin/main` @ eb78a31 (v0.3.1)
- REV-01: PASS_WITH_CONCERNS; Android CI later green
