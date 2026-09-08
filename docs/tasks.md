# Tasks

Current work queue. One task → one accountable owner. Do not start work that is not on this list unless the Product Owner adds it.

Status: `doing` · `blocked` · `queued` · `done`

---

### T-001: Encode CryptoGalera operating model

Status: doing
Owner: Product Owner (this change)
Goal: Company OS in-repo so future agents follow one product, one trunk, and one owner per task.
Deliverables: `AGENTS.md`, [company.md](company.md), [organization.md](organization.md), this file, [product.md](product.md), [decisions.md](decisions.md), README pointer.
Definition of Done: Docs match repo reality (including the `main` vs `v0.2.x` split); no application-code changes; independent review of the docs.
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

---

## Not tasks (do not staff)

- Another Telegram-chrome polish agent
- Another ICE/TURN unstick agent
- Another “ship APK 0.2.x” agent
- Landing-page brand work
- iOS / web / desktop / ratchet / MLS

Those are either out of MVP or absorbed by T-002 / T-004.
