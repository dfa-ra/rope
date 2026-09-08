# CryptoGalera Company State

Live snapshot. PO-owned. Not a changelog.

## Product Goal

Adopt existing **Rope** (private self-hosted 1-to-1 Android E2EE messenger) into CryptoGalera. Wrap it with company runtime. Do not rewrite the messenger in this phase.

## Current Phase

**MIGRATION** — runtime installed. Product implementation frozen.

## Active Organization

```
User
 └── PO
```

PROC-LEAD released (CG-008 done). ENG-LEAD exists as a role, **not seated**. No coding workers.

## Current Priorities

1. User: D-005 trunk (A / B / C)
2. Do not implement Rope until CG-007

## Active Blockers

- **CG-002 / D-005** — waiting on user (promote Stage-2 vs keep MVP vs subset)

## Current Risks

See [RISKS.md](RISKS.md). Dual product line (`main` 0.1.0 vs tags `v0.2.x`). Agent sprawl history.

## Next Integration Point

Stop for user (D-005 or CG-007 unfreeze). No READY product tasks.

## Mode flags

- `product_implementation`: frozen
- Canonical documented line: `main` MVP 0.1.0
- Stage-2: preserve, do not merge
