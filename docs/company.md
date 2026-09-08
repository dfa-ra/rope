# CryptoGalera operating model

This is how CryptoGalera runs Rope. [AGENTS.md](../AGENTS.md) is the short form agents must follow. This file is the readable full model. Current seating and file owners: [organization.md](organization.md).

The company name is informal. The operating model is not. The failure mode this document exists to prevent is **agent sprawl**: many overlapping cloud agents, no integration owner, and a dual product line (`main` vs GitHub Releases). D-005 A closed that split: `main` matches `v0.2.15`.

## Roles

Roles are functions, not headcount. One person (or one agent) may hold several. Do not staff a role that the current task does not need.

| Role | Decides | Does |
| --- | --- | --- |
| **User** | Product, business model, scope, fundamental requirements | Approves irreversible direction |
| **Founder / Product Owner (PO)** | Priorities, what ships, what is out of scope | Writes product/tasks/decisions with Engineering Lead; creates leads; does not implement everything |
| **Engineering Lead** | Technical plan, integration, file-ownership conflicts | Studies, plans, implements small work, creates specialists only when needed, lands the branch |
| **QA / Security Lead** | Whether a change is safe to ship against the threat model | Test plan, review of crypto/auth/protocol; **same person as Engineering Lead** unless the change is security-critical and needs an independent reviewer |
| **Specialist** | Nothing outside the brief | Executes one concrete brief in one ownership area |
| **Design** | Visual/UX choices **inside** an assigned UI task | Only when the task is UI. Not a standing department |
| **Research Lead** | How to close a stated unknown | Only when unknowns dominate. Must return a recommendation, not a literature dump |

Company hierarchy is 2–3 levels: Product Owner → Lead → Specialist. The user sits above the PO. There is no layer of “managers of agents.”

## Current default org for Rope

Until PO says otherwise, staff **this** and nothing more:

1. **Product Owner**
2. **Engineering Lead** (integration owner by default)
3. **QA / Security Lead** — same person as Engineering Lead on ordinary work; a separate reviewer when the change is crypto, auth, protocol, or threat-model

Add **Design** only for a UI-shaped task. Add **Research Lead** only when the work is blocked on a real unknown (not “we might want calls someday”). Do not add marketing, growth, platform, data, or “AI ops” departments.

## Lead rules

A lead’s job is to finish the outcome, not to populate an org.

1. **Study** the brief, [product.md](product.md), [architecture.md](architecture.md), [threat-model.md](threat-model.md), and the owned code.
2. **Plan** in a short written decomposition: slices, owners, sequence, risks, what will *not* be done.
3. **Do it yourself** if the work fits in one ownership area and one context window.
4. **Create workers** only for specialization, true parallelism on disjoint paths, independent review, or context isolation. Each worker gets a complete task brief (template in [AGENTS.md](../AGENTS.md)).
5. **Name the integration owner** before any parallel work. Default: Engineering Lead.
6. **Integrate, review, validate, report.** Recheck whether further agents are needed after each worker returns. Do not pre-spawn a tree.
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

- **Before coding a shared surface:** Engineering Lead coordinates Android / Rust / Go / protocol owners. Protocol and UniFFI changes are never “drive-by.”
- **During parallel work:** each specialist stays in their tree; integration owner merges and resolves conflicts.
- **When blocked on another tree:** write it in the report and on [tasks.md](tasks.md). Do not start a second agent on the same files.
- **After landing:** update architecture, protocol docs, threat model, and decisions if behavior changed.

The integration owner is the Engineering Lead unless the PO names someone else. Parallel work without an integrator is forbidden.

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

Reviewer ≠ author. Required **before merge** for:

- Security and threat-model claims
- Architecture and protocol
- Cryptography
- Auth (bootstrap, invites, device signatures, TLS pinning)
- Migrations and anything that can drop or rewrite mailbox / member data

Ordinary UI copy or layout inside `apps/android` with no protocol or crypto change: Engineering Lead review is enough; do not spawn a review agent unless Design was part of the task.

QA/Security Lead uses [threat-model.md](threat-model.md) as the checklist. A change that weakens a stated guarantee is not done.

## Cost awareness

Cloud agents, reviews, and context windows are scarce.

- Prefer one capable worker over three overlapping ones.
- Do not spawn an agent to write a status report.
- Do not explore out-of-scope features (calls, groups, iOS, web, landing sites) “while we are here.”
- Stop when the brief’s Definition of Done is met. Extra polish is new scope.
- If a prompt asks to fork a second product line off `main`, that is a PO/user decision, not a staffing exercise.

## Product development loop

**Understand** the user request against product and architecture. **Decompose** into slices with one owner each. **Organize** the minimum roles. **Delegate** only with full briefs. **Execute** in owned trees. **Review** with a different person when the policy requires it. **Integrate** on one branch. **Validate** with the tests in [dev-setup.md](dev-setup.md). **Deliver** with the report template and updated source of truth.

Skip steps only when the task is truly small (one owner, one tree, no review trigger). Never skip integration or DoD.
