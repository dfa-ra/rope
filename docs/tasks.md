# Tasks

Current work queue. Canonical live board: [`.cryptogalera/TASK_BOARD.md`](../.cryptogalera/TASK_BOARD.md). One task → one accountable owner.

Status: `doing` · `blocked` · `queued` · `done`

---

### T-001: Encode CryptoGalera operating model

Status: done
Owner: Product Owner
Goal: Company OS in-repo so future agents follow one product, one trunk, and one owner per task.
Deliverables: `AGENTS.md`, [company.md](company.md), [organization.md](organization.md), this file, [product.md](product.md), [decisions.md](decisions.md), README pointer.
Definition of Done: Docs exist and match repo reality; no application-code changes.
Notes: Session docs exist, are **unverified**, and are **preserved** (D-007). Independent verification remains available as later review; do not revert them.
Depends on: —

### T-002: User chooses the product trunk (D-005)

Status: done
Owner: User (single decision). Product Owner recorded it in [decisions.md](decisions.md).
Goal: End the dual-product split.
Options: **A** promote Stage-2 to `main` (chosen) · **B** keep MVP and freeze/close Stage-2 · **C** named subset.
Definition of Done: D-005 status is `accepted` with a single chosen option.
Depends on: T-001 (so the choice is recorded against a real contract)
Notes: User 2026-09-08 asked to hang current APK/core releases on `main` and tidy extra branches without losing work. That is option A. Integration tip: tag `v0.2.15` (`9694e80`).

### T-003: Feature-agent freeze

Status: doing (in force)
Owner: Product Owner. Constraint: every lead refuses Stage-2 work that is not on this list.
Goal: No new overlapping agents for calls, TURN/ICE, Telegram UI, groups, landing, or version bumps. T-002 is answered (A); freeze still blocks **new** duplicate feature agents.
Definition of Done: New cloud agents only work items on this list (or explicit PO additions).
Depends on: D-006
Notes: T-004 (promote `v0.2.15`) is the allowed integration, not a new feature stream.

### T-004: Single integration after T-002

Status: doing
Owner: Engineering Lead (CG-004)
Goal: One branch to `main` that matches tag `v0.2.15`; keep CryptoGalera wrap; close nested PRs #2 / #3 after their commits are on `main`; sync README, protocol docs, and threat model.
Definition of Done: `main` product trees match `v0.2.15`; GitHub Release `v0.2.15` still published with the same assets; contained `cursor/*` branches deleted; unique `telegram-chrome-872f` kept.
Depends on: T-002
Forbidden: merging PR #2 or #3 as the integration path (use the tag); retagging `v0.2.15`; deleting unique work.

### T-005: Threat model + protocol sync (after trunk)

Status: queued (lands with T-004 merge: take `docs/threat-model.md` + `protocol/docs` from tag `v0.2.15`)
Owner: Engineering Lead. Required reviewer: independent QA/Security (reports to PO; reviewer ≠ author).
Goal: [threat-model.md](threat-model.md) and [protocol/docs](../protocol/docs) match the chosen trunk (TURN, objects, groups, call signaling).
Depends on: T-004

### T-006: Install CryptoGalera runtime around Rope

Status: done
Owner: Product Owner (integrator). Workers: Process Lead (constitution, rules, Cursor rule, pointers); State Lead (`cryptogalera/state/*.yml`).
Goal: Persist company operating law as a runtime layer wrapping existing Rope. Migration, not product implementation.
Deliverables: `cryptogalera/README.md`, `cryptogalera/CONSTITUTION.md`, `cryptogalera/rules/*`, `.cursor/rules/cryptogalera.mdc`, `cryptogalera/state/*.yml`, surgical pointers in `AGENTS.md` / README / this file, [api.md](api.md).
Definition of Done: Runtime exists; product trees untouched; no session docs deleted; mode MIGRATION documented; D-005 still user-only.
Depends on: T-001, D-007
Forbidden: implementing or redesigning Rope; creating a second API; merging Stage-2.

### T-007: Unfreeze product implementation

Status: queued
Owner: Product Owner
Goal: Resume **new** product work only when the user says so. Promoting `v0.2.15` (T-004) is not a general unfreeze.
Depends on: T-006
Forbidden until then: seating extra feature agents for chrome/calls/landing; implementing iOS/web/desktop/ratchet.

### CG-008: Install Cursor multi-agent runtime protocol

Status: done
Owner: Product Owner. Worker: PROC-LEAD (CG-008-A).
Goal: Persist spawn/report protocol and PO-owned `.cryptogalera/` memory. Alias T-008.
Deliverables: `cryptogalera/RUNTIME.md`, `.cryptogalera/*`, Cursor/AGENTS pointers, D-008.
Definition of Done: Protocol on disk; management files exist; product trees untouched; real subagent used for CG-008-A; independent review of the wrap.
Depends on: CG-006
Forbidden: product implementation; `.cursor/agents/*` sprawl; Workers spawning agents.

---

## Not tasks (do not staff)

- Another Telegram-chrome polish agent
- Another ICE/TURN unstick agent
- Another “ship APK 0.2.x” agent
- Landing-page brand work
- iOS / web / desktop / ratchet / MLS

Those are either out of MVP or absorbed by T-002 / T-004.
