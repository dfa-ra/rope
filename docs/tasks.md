# Tasks

Current work queue. One task → one accountable owner. Do not start work that is not on this list unless the Product Owner adds it.

Status: `doing` · `blocked` · `queued` · `done`

---

### T-001: Encode CryptoGalera operating model

Status: done
Owner: Product Owner
Goal: Company OS in-repo so future agents follow one product, one trunk, and one owner per task.
Deliverables: `AGENTS.md`, [company.md](company.md), [organization.md](organization.md), this file, [product.md](product.md), [decisions.md](decisions.md), README pointer.
Definition of Done: Docs exist and match repo reality (including the `main` vs `v0.2.x` split); no application-code changes.
Notes: Session docs exist, are **unverified**, and are **preserved** (D-007). Independent verification remains available as later review; do not revert them.
Depends on: —

### T-002: User chooses the product trunk (D-005)

Status: blocked (needs user)
Owner: User (single decision). Product Owner records it in [decisions.md](decisions.md).
Goal: End the dual-product split.
Options: **A** promote Stage-2 to `main` · **B** keep MVP and freeze/close Stage-2 · **C** named subset.
Definition of Done: D-005 status is `accepted` with a single chosen option.
Depends on: T-001 (so the choice is recorded against a real contract)

### T-003: Feature-agent freeze

Status: doing (in force)
Owner: Product Owner. Constraint: every lead refuses Stage-2 work that is not on this list.
Goal: No new overlapping agents for calls, TURN/ICE, Telegram UI, groups, landing, or version bumps until T-002.
Definition of Done: New cloud agents only work items on this list (or explicit PO additions).
Depends on: D-006

### T-004: Single integration after T-002

Status: queued
Owner: Engineering Lead (named when T-002 is answered)
Goal: One branch to `main` that matches the chosen trunk; close nested PRs #2 / #3 accordingly; sync README, protocol docs, and threat model.
Definition of Done: `main` and GitHub Releases describe the same product; stale `cursor/*` branches archived or closed.
Depends on: T-002
Forbidden until then: merging PR #2 or #3 “to make progress,” tagging from a side branch, spawning UI and call agents in parallel.

### T-005: Threat model + protocol sync (after trunk)

Status: queued
Owner: Engineering Lead. Required reviewer: independent QA/Security (reports to PO; reviewer ≠ author).
Goal: [threat-model.md](threat-model.md) and [protocol/docs](../protocol/docs) match the chosen trunk (TURN, objects, groups, call signaling — or explicitly remain out).
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
Goal: Resume product work only when the user says so, and only after D-005 if that work would change the trunk.
Depends on: T-006
Forbidden until then: seating Engineering Lead for product coding; merging Stage-2; implementing calls/groups/UI.

---

## Not tasks (do not staff)

- Another Telegram-chrome polish agent
- Another ICE/TURN unstick agent
- Another “ship APK 0.2.x” agent
- Landing-page brand work
- iOS / web / desktop / ratchet / MLS

Those are either out of MVP or absorbed by T-002 / T-004.
