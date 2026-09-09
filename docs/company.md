# CryptoGalera operating model

This is how CryptoGalera runs Rope. [AGENTS.md](../AGENTS.md) is the short form agents must follow. This file is the readable full model. Current seating and file owners: [organization.md](organization.md).

The company name is informal. The operating model is not. The failure mode this document exists to prevent is **agent sprawl**: many overlapping cloud agents, no integration owner, and a dual product line (`main` vs GitHub Releases). D-005 A closed that split: `main` matches `v0.2.15`.

## Roles

Roles are functions, not headcount. One person (or one agent) may hold several. Do not staff a role that the current task does not need.

| Role | Decides | Does |
| --- | --- | --- |
| **User** | Product, business model, scope, fundamental requirements | Approves irreversible direction |
| **Founder / Product Owner (PO)** | Priorities, what ships, merge, release; which epic is next | Acts as Product Owner of Rope (D-015; unchanged by D-031); assigns domain leads; picks the next epic from the Research brief; owns PR → REV-01 → ff-merge → tagged release → record |
| **Android UI lead** | Android plan and integration inside `apps/android` | Domain lead for UI/UX slices; may spawn `AND-n` subordinates; integrates on the epic branch; does **not** self-review |
| **Go relay lead** | Server/API/storage plan inside `server/go` (and deployment when tasked) | Domain lead when the epic touches relay; may spawn `GO-n`; does **not** self-review |
| **Rust core lead** | Crypto/protocol/UniFFI plan inside `core/rust` | Domain lead when the epic touches the core; may spawn `CORE-n`; does **not** self-review |
| **Engineering Lead** | Technical plan and shared-surface conflicts when PO names them integrator | Default integration owner if only one tree, or when PO does not name a domain lead |
| **REV-01 / QA / Security** | Whether a change is safe to ship | Independent of every implementer **and every lead** on that slice. PASS_WITH_CONCERNS may ship; FAIL blocks tag |
| **Specialist (`AND-n` / `GO-n` / `CORE-n`)** | Nothing outside the brief | Executes one concrete brief in one ownership area. Never spawns |
| **Design** | Visual/UX choices **inside** an assigned UI task | Only when the task is a visual redesign. Not a standing department |
| **Research subteam** | How to rank the next Telegram-gap epics | **Standing** (D-031). Internet + Telegram Android UX + Rope gap analysis → ranked brief. Does **not** merge product code |

Company hierarchy is 2–3 levels: Product Owner → Domain Lead → Specialist. The user sits above the PO. There is no layer of “managers of agents.” Research reports to PO and is not a merge path.

## Current default org for Rope

Until PO says otherwise, staff **this** (D-031):

1. **Product Owner** (this Cursor agent; D-015 unchanged)
2. **Domain leads** the epic needs: Android UI lead always for UI-visible gaps; Go relay lead if server/API/storage; Rust core lead if crypto/protocol/UniFFI
3. **Subordinates** (`AND-n`, `GO-n`, `CORE-n`) only for files/tests those leads own
4. **Standing Research subteam** — ranked Telegram-gap epics; no product merges
5. **REV-01** — independent of every implementer and every lead on that slice

Default unit of work is an **epic slice**, not a one-string or version bump. Tiny copy nits are a hotfix after FAIL / PASS_WITH_CONCERNS leftover, not the sprint cadence.

Add **Design** only for a visual-redesign task. Do not add marketing, growth, platform, data, or “AI ops” departments. Leads integrate on **one** feature branch. Leads do **not** self-review.

## Lead rules

A lead’s job is to finish the outcome, not to populate an org.

1. **Study** the brief, [product.md](product.md), [architecture.md](architecture.md), [threat-model.md](threat-model.md), and the owned code.
2. **Plan** in a short written decomposition: epic slice, owners, sequence, risks, what will *not* be done.
3. **Do it yourself** if the work fits in one ownership area and one context window (hotfix leftovers included).
4. **Create subordinates** only for specialization, true parallelism on disjoint paths they own, or context isolation. Each worker gets a complete task brief (template in [AGENTS.md](../AGENTS.md)). Independent review is always **REV-01**, not the lead.
5. **Name the integration owner** before any parallel work. Default: the domain lead the PO names.
6. **Integrate on one feature branch**, validate, report. Recheck whether further agents are needed after each worker returns. Do not pre-spawn a tree. Do **not** self-review.
7. **Escalate** irreversible or high-impact calls; do not silently assume.

Leads do not exist to relay status to the PO. If the PO is doing the lead’s job, delegation has failed.

## Worker brief

No worker starts without a brief that includes: Role, Objective, Context, Responsibilities, Constraints, Deliverables, Definition of Done, Dependencies, Report To.

Context is the minimum: relevant docs, paths, and the current task id from [tasks.md](tasks.md). It is not a paste of this operating model.

Constraints must name: do not change product/scope; do not touch others’ trees; do not rewrite working crypto; tests to run; whether a reviewer is required.

If the brief is incomplete, the worker stops and asks the lead. Guessing is not a substitute for a brief.

## Communication protocol

Default channel is the repository plus the report template. Do not create side chats that replace [tasks.md](tasks.md) or [decisions.md](decisions.md).

**Reports** (worker → lead → PO) use:

```
STATUS:             (done | blocked | in progress)
SUMMARY:
WORK COMPLETED:
KEY DECISIONS:
RISKS:
OPEN QUESTIONS:
DELIVERABLES:
RECOMMENDED NEXT ACTION:
```

**Questions** are specific, with options when possible. “What should I do?” is not a question; “A vs B, recommendation A because …, blast radius …” is.

**No silent high-impact assumptions.** If shipping depends on an unstated product or crypto choice, stop.

## Escalation

| Class | Examples | Path |
| --- | --- | --- |
| Reversible, local | Copy, layout in an existing screen, test-only change | Worker decides; mention in the report |
| Cross-cutting | Protocol field, API shape, shared docs | Engineering Lead; record in [decisions.md](decisions.md) |
| Irreversible / high-impact | Crypto primitive or envelope change, threat-model claim, auth, data-dropping migration, anything that changes product/scope | PO. If it changes product, business model, scope, or fundamental requirements → **user** |

Escalate with: the decision needed, options, a recommendation, blast radius, and cost of waiting. Do not escalate trivia. Do not hide blockers until the deadline.

## Cross-team sync

There are no standing meetings. Sync is:

- **Before coding a shared surface:** the named integration owner coordinates Android / Rust / Go / protocol owners. Protocol and UniFFI changes are never “drive-by.”
- **During parallel work:** each specialist stays in their tree; integration owner merges and resolves conflicts on **one** feature branch.
- **When blocked on another tree:** write it in the report and on [tasks.md](tasks.md). Do not start a second agent on the same files.
- **After landing:** update architecture, protocol docs, threat model, and decisions if behavior changed.

The integration owner is the domain lead the PO names unless the PO names Engineering Lead. Parallel work without an integrator is forbidden.

## Decision log format

Durable decisions go in [decisions.md](decisions.md). One entry per decision:

```
### D-NNN: short title
Date:
Status: proposed | accepted | superseded
Decider: (role)
Context:
Decision:
Consequences:
Alternatives considered:
```

Do not log reversible local choices. Do log protocol, crypto, auth, architecture, and product-scope decisions. A new high-impact choice without an entry is a process bug — add it and tell the PO.

## Review policy

Reviewer ≠ author. **REV-01** is independent of every implementer and every lead on that slice (D-031). Required **before merge**. Leads do not self-review.

Verdicts:

- **PASS** — may ship
- **PASS_WITH_CONCERNS** — may ship; leftover is a hotfix, not a new epic cadence
- **FAIL** — blocks the tag

Required surfaces include:

- Security and threat-model claims
- Architecture and protocol
- Cryptography
- Auth (bootstrap, invites, device signatures, TLS pinning)
- Migrations and anything that can drop or rewrite mailbox / member data
- Ordinary product UI on an epic (no longer “Engineering Lead review is enough”)

QA/Security uses [threat-model.md](threat-model.md) as the checklist. A change that weakens a stated guarantee is not done.

## Cost awareness

Cloud agents, reviews, and context windows are scarce.

- Prefer one capable worker per owned tree over three overlapping ones.
- Do not spawn an agent to write a status report.
- Do not staff a one-string copy nit as the sprint cadence (D-031). Hotfix leftovers are allowed.
- Stop when the brief’s Definition of Done is met. Extra polish is new scope.
- If a prompt asks to fork a second product line off `main`, that is a PO/user decision, not a staffing exercise.

## Product development loop

**Understand** the user request against product and architecture. **Decompose** into **epic slices** (Telegram gaps that may span Android + Go + Rust). **Organize** the domain leads the slice needs. **Delegate** only with full briefs. **Execute** in owned trees on one feature branch. **REV-01** with a different person than every implementer and every lead. **PR** against `main`. **Validate** with the tests in [dev-setup.md](dev-setup.md) and CI. **Fast-forward merge** onto `main`. **Release** with a version bump and tag `vX.Y.Z` so GitHub publishes APK and server binaries. **Record** on `main`. **Deliver** with the report template and updated source of truth. An open PR is not delivery.

Skip spawn steps only for a hotfix leftover from FAIL / PASS_WITH_CONCERNS. Never skip REV-01, integration, or DoD.
