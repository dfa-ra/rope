# CryptoGalera runtime

This directory is the **company runtime** wrapping the existing Rope product. It is not a second product and not a replacement for Rope.

Product code stays in `apps/`, `core/`, `server/`, `deployment/`, and `protocol/`. Do not move or rewrite it.

## Mode

**MIGRATION.** Product implementation is frozen until the Product Owner changes mode (D-007). Do not implement, merge, or redesign Rope in this phase. D-005 remains a user-only trunk choice.

## Map

| Path | Role |
| --- | --- |
| [CONSTITUTION.md](CONSTITUTION.md) | Operational constitution (binding process law) |
| [rules/persistent.md](rules/persistent.md) | Always-on constraints |
| [rules/file-ownership.md](rules/file-ownership.md) | Path owners for this repo |
| `state/` | Live company state YAML — PO owns content; State Lead writes files |
| [../AGENTS.md](../AGENTS.md) | Agent operating rules (unverified session SoT, preserved) |
| [../docs/](../docs/) | Product, org, architecture, threat model, decisions, tasks, [api.md](../docs/api.md) pointer |
| [../.cursor/rules/cryptogalera.mdc](../.cursor/rules/cryptogalera.mdc) | Cursor always-on enforcement |

Read this file and `CONSTITUTION.md` before doing work. Then read `AGENTS.md` and the relevant `docs/*`. Live mode and owners live in `state/`.
