# AGENTS.md — CryptoGalera / Rope

Cursor cloud agents and humans working this repository must follow this file. The full operating model is [docs/company.md](docs/company.md). Org, ownership, and staffing: [docs/organization.md](docs/organization.md).

## Runtime

Company process is the **runtime** at [cryptogalera/README.md](cryptogalera/README.md) and [cryptogalera/CONSTITUTION.md](cryptogalera/CONSTITUTION.md). Live state: `cryptogalera/state/` (State Lead). Cursor enforcement: `.cursor/rules/cryptogalera.mdc`.

- Spawn / task / report protocol: [cryptogalera/RUNTIME.md](cryptogalera/RUNTIME.md).
- Company memory: `.cryptogalera/` (PO-only edits: `COMPANY_STATE`, `TASK_BOARD`, `DECISIONS`; optional `RISKS`, `ARCHITECTURE`).
- Reusable Cursor roles: `.cursor/agents/` (engineering-lead, researcher, implementation-worker, verification-engineer, security-reviewer). Not a department per microtask.
- Task IDs: `CG-XXX`.

Mode is **MIGRATION / trunk promotion**. D-005 A is accepted: `main` must match shipped `v0.2.15`. New feature coding stays frozen (CG-007). Do not touch `apps/`, `core/`, `server/`, `deployment/`, `protocol/`, `scripts/`, `Makefile`, or `.github/` except CG-004 (merge those trees from the tag). The documents below are unverified session SoT — preserve them; do not revert.

## Company

CryptoGalera is a small tech company. Rope is the product. The company name is informal; the process is not.

Company hierarchy is **2–3 levels: Product Owner → Lead → Specialist**. The user sits above the PO and is not a layer to staff. Do not add managers of agents.

Org size matches the task, not a real-world org chart. Do not invent departments, fake employees, or parallel workstreams. One task has one accountable owner. The Product Owner (PO) is the only person who may change product, business model, scope, or fundamental requirements — and only with the user.

## Product

**Rope** is a private self-hosted Android E2EE messenger: Kotlin UI → Rust UniFFI core → Go + SQLite relay. Trunk is `main` matching GitHub Latest `v0.2.15` ([D-005](docs/decisions.md#d-005-stage-2-parallel-line-escalated) A).

Product contract: [docs/product.md](docs/product.md). Architecture: [docs/architecture.md](docs/architecture.md). Do not spawn overlapping chrome/calls/landing agents. Feature freeze for **new** duplicate work: [T-003](docs/tasks.md#t-003-feature-agent-freeze).

## How to organize

Product loop: **Understand → Decompose → Organize → Delegate → Execute → Review → Integrate → Validate → Deliver**.

1. Study the task and the source of truth. Plan in writing.
2. If the work is small and in one ownership area, the lead does it. Do not spawn an agent.
3. Create workers only when the criteria in the next section are met. Each worker gets a full task brief.
4. Name an **integration owner** before parallel work starts. Default: Engineering Lead (unless PO assigns otherwise).
5. Review, integrate on one branch, validate with tests, then report. Do not open overlapping agents on the same files.

Default Rope org is minimal: PO, Engineering Lead, QA/Security Lead (same person as Engineering Lead when the task is small). Design exists only for a UI-shaped task. Research Lead exists only when unknowns dominate. See [docs/organization.md](docs/organization.md).

## When to create an agent

Create an agent **only if at least one is true**:

- **Specialization** the lead cannot cover (e.g. crypto review vs Android layout).
- **True parallelism** on **disjoint** file ownership, with a named integrator.
- **Independent review** required (reviewer ≠ author).
- **Context isolation** so a large investigation does not pollute the integration context.

Do **not** create an agent for status, “a second opinion,” recursive management, or because a department name exists. Recheck after each worker finishes; do not pre-spawn a tree.

## Task brief template

Every worker brief must include all of:

```
Role:
Objective:
Context:            (repo state, files, docs, related tasks — not a dump)
Responsibilities:
Constraints:
Deliverables:
Definition of Done:
Dependencies:
Report To:
```

If a brief is missing, the worker stops and asks the lead. Do not invent scope.

## Report template

Workers and leads report in this shape (no essay):

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

## Source of truth

| Document | Role |
| --- | --- |
| [docs/product.md](docs/product.md) | Product goal, success, in/out of scope (PO) |
| [docs/architecture.md](docs/architecture.md) | System design |
| [docs/decisions.md](docs/decisions.md) | Durable decisions (PO + Engineering Lead) |
| [docs/tasks.md](docs/tasks.md) | Current work and owners (PO) |
| [docs/api.md](docs/api.md) | Pointer to current-trunk `protocol/docs` |
| [docs/threat-model.md](docs/threat-model.md) | Security claims and non-guarantees |
| [docs/company.md](docs/company.md) | Operating model |
| [docs/organization.md](docs/organization.md) | Org, ownership, staffing |
| [docs/dev-setup.md](docs/dev-setup.md) | How to build and test |
| [protocol/docs/](protocol/docs/) | Wire format, REST, WSS, invites |
| [cryptogalera/README.md](cryptogalera/README.md) | Company runtime (mode, map) |
| [cryptogalera/RUNTIME.md](cryptogalera/RUNTIME.md) | Spawn / task / report protocol |

Do not duplicate these in prompts. Do not invent a roadmap. If a prompt conflicts with `docs/product.md`, stop and escalate.

## File ownership

| Area | Path | Owner |
| --- | --- | --- |
| Android | `apps/android/**` | Android specialist / Engineering Lead |
| Rust core | `core/rust/**` | Rust specialist / Engineering Lead |
| Go server | `server/go/**` | Server specialist / Engineering Lead |
| Protocol | `protocol/**` | Engineering Lead (coordinated) |
| Deployment | `deployment/**` | Server specialist / Engineering Lead |
| Shared docs | `docs/**` | PO + Engineering Lead |
| Bindings scripts | `scripts/**` | Engineering Lead (Android + Rust) |
| CI | `.github/**`, `Makefile` | Engineering Lead |
| Runtime law | `cryptogalera/CONSTITUTION.md`, `cryptogalera/README.md`, `cryptogalera/RUNTIME.md`, `cryptogalera/rules/**` | Process Lead |
| Live state | `cryptogalera/state/**` | PO (content); State Lead (files) |
| Company memory | `.cryptogalera/**` | PO |
| Cursor rules | `.cursor/rules/**` | Process Lead |

Shared and protocol changes must be coordinated before coding. Do not edit another owner’s tree without the integration owner’s OK. Do not change `apps/`, `core/`, `server/`, `deployment/`, `protocol/`, `scripts/`, or CI unless that is the assigned task.

## Escalation

Silent high-impact assumptions are forbidden. If you would have to guess, stop and escalate.

| Kind | Who decides |
| --- | --- |
| Reversible, local, in-ownership | Worker or lead |
| Cross-cutting, protocol, architecture | Engineering Lead; log in `docs/decisions.md` |
| Irreversible / high-impact (crypto, threat model, auth, product/scope, migrations that drop data) | PO; user if it changes product, business model, scope, or fundamental requirements |

Escalate with: decision needed, options, recommendation, blast radius, what happens if we wait.

## Definition of Done

A task is done when **all** hold:

- Deliverables match the brief; no extra scope.
- Owned tests for the change pass (`cargo test`, `go test ./...`, `./gradlew test` as applicable).
- Required review completed (reviewer ≠ author) for security, architecture, crypto, auth, migrations.
- Docs in the source-of-truth table updated if behavior or protocol changed.
- Integration owner merged the work; no leftover parallel agents on the same files.
- Report filed in the template above.

## Git rules

- Branch from the default branch (`main`), not from a random feature branch, unless PO says otherwise.
- Minimal diffs. Do not reformat, rename, or “clean up” unrelated files.
- Do not rewrite working cryptography. Changes to `core/rust` crypto, envelopes, or auth need a security review and PO awareness.
- Run the tests that cover the change before reporting done.
- One accountable branch per task. Do not open overlapping agents on the same paths.

## Anti-patterns (forbidden)

- **One-man army** — PO or one agent doing every department’s job while pretending to delegate.
- **Agent explosion** — many cloud agents on overlapping UI / calls / landing / release work while `main` stays MVP.
- **Recursive management** — agents whose only job is to spawn more agents.
- **Fake delegation** — a “lead” that immediately re-asks the PO to do the work.
- **No integration owner** — parallel work with nobody landing a coherent branch.
- **Endless analysis** — research with no decision, no owner, and no stop condition.
- **Scope mutation** — implementing calls, groups, iOS, web, or a new business model because a prompt mentioned them.
