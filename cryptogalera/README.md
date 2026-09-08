# CryptoGalera runtime

This directory is the **company runtime** wrapping the existing Rope product. It is not a second product and not a replacement for Rope.

Product code stays in `apps/`, `core/`, `server/`, `deployment/`, and `protocol/`. Do not move or rewrite it.

## Mode

**STABILIZATION.** Trunk is `main`. Product work uses the delivery law: PR → review → merge → tagged release. Do not leave shippable work only on `cursor/*`.

## Map

| Path | Role |
| --- | --- |
| [CONSTITUTION.md](CONSTITUTION.md) | Operational constitution (who is who; binding process law) |
| [RUNTIME.md](RUNTIME.md) | Spawn / task / report protocol (how Cursor subagents run) |
| [rules/persistent.md](rules/persistent.md) | Always-on constraints |
| [rules/file-ownership.md](rules/file-ownership.md) | Path owners for this repo |
| `state/` | Live company state YAML — PO owns content; State Lead writes files |
| `../.cryptogalera/` | PO-owned company memory (`COMPANY_STATE`, `TASK_BOARD`, `DECISIONS`; optional `RISKS`). Process does not edit. |
| [../AGENTS.md](../AGENTS.md) | Agent operating rules (unverified session SoT, preserved) |
| [../docs/](../docs/) | Product, org, architecture, threat model, decisions, tasks, [api.md](../docs/api.md) pointer |
| [../.cursor/rules/cryptogalera.mdc](../.cursor/rules/cryptogalera.mdc) | Cursor always-on enforcement |

Read this file, `CONSTITUTION.md`, and `RUNTIME.md` before doing work. Then read `AGENTS.md` and the relevant `docs/*`. Live mode and owners live in `state/`. Company memory in `.cryptogalera/` is PO-managed; do not create or edit those files unless you are the PO.
