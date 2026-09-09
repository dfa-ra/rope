# CryptoGalera runtime protocol

How Cursor subagents are **spawned, tasked, and reported**. Binding. Compact.

**Who is who:** [CONSTITUTION.md](CONSTITUTION.md). **Who may work:** [AGENTS.md](../AGENTS.md). **Live company memory:** `.cryptogalera/` (PO-owned; see below). This file is the spawn/report contract, not a product spec.

Mode is **STABILIZATION**. D-005 A is done. Last ship is **v0.3.17**. Every completed product epic ends with PR → REV-01 → CI → ff-merge to `main` → tagged release → record on `main`. Do not invent iOS/ratchet/landing. No VK/Yandex/WB tunnels. Do not retag v0.2.15 / v0.3.0 / v0.3.1 / v0.3.2 / v0.3.3 / v0.3.4 / v0.3.5 / v0.3.6 / v0.3.7 / v0.3.8 / v0.3.9 / v0.3.10 / v0.3.11 / v0.3.12 / v0.3.13 / v0.3.14 / v0.3.15 / v0.3.16 / v0.3.17.

---

## Prime directive

**Ship, do not simulate.** Real files, real diffs, real reports. Do not roleplay a company, invent workers, or claim work that was not done. A simulated team is a process failure.

**The PO is Product Owner of Rope (D-015; unchanged by D-031).** Every user request is staffed as a company workstream: Research ranks epics → PO picks an **epic slice** → domain leads + subordinates implement → **REV-01** (independent of every implementer and every lead) → PR → CI → ff-merge to `main` → tagged GitHub Release → record on `main` → next epic. Do not report a product task DONE while its PR is still open. Do not leave shippable work only on `cursor/*`. Tiny copy nits are a hotfix after FAIL / PASS_WITH_CONCERNS leftover, not the sprint cadence.

---

## Hierarchy and spawn rights

```
User → Product Owner (PO) → Domain Lead → Subordinate (AND-n / GO-n / CORE-n)
                         ↘ Research subteam (standing; no product merges)
                         ↘ REV-01 (independent of every implementer and every lead)
```

| Role | Spawns? | Job |
| --- | --- | --- |
| **PO** | Domain leads, Research, REV-01 (real subagents) | Priorities, mode, pick next epic from Research brief, briefs to leads, company memory, merge, release |
| **Domain Lead** | Subordinates (real subagents) only for files/tests they own | Android UI / Go relay / Rust core as assigned. Plan, small work, spawn, integrate on **one** feature branch, verify, report to PO. **Does not self-review.** |
| **Research** | **Never** (unless PO seats a read-only helper on a disjoint scan) | Standing: internet + Telegram Android UX + Rope gap analysis → ranked epics. Does **not** merge product code. |
| **Subordinate / Worker** | **Never** | One brief, one ownership area (`AND-n`, `GO-n`, `CORE-n`), one report. Stop. |
| **REV-01** | **Never** | Independent of every implementer and every lead on that slice. PASS / FAIL / PASS_WITH_CONCERNS. FAIL blocks tag; PASS_WITH_CONCERNS may ship. |

Workers must not spawn agents. Recursive management is forbidden. Depth below the user is at most PO → Lead → Worker. Domain leads are the Lead layer (D-031). Research is a standing subteam under PO, not a fourth management layer.

---

## Real subagents only

A subagent exists only as a **real Cursor Task-tool invocation**. That is the only legal spawn.

Forbidden:

- Roleplaying “Android agent” / “reviewer” in the same context
- Writing a fake worker report for work the parent did
- Creating `.cursor/agents/*` files to stand in for people
- Claiming parallel work that was a single sequential session

If the Task tool is unavailable, use **degraded mode**. Do not fake the missing spawn.

Workers are **ephemeral**: one task, then gone. Do not accumulate standing agent files or dozens of `.cursor/agents` definitions. Specialists are not a headcount. Reusable roles (Android UI lead, Go relay lead, Rust core lead, researcher, REV-01) stay small.

**Standing Research (D-031)** is the exception to “research only when unknowns dominate”: it is always seated as a subteam, produces ranked epic briefs, and still does not merge product code. It is not a license to spawn overlapping feature agents (D-006).

---

## Task IDs

Every tasked unit of work has a **Task ID `CG-XXX`** (optional suffix, e.g. `CG-008-A`). Use it in the Context Package, reports, and (when present) `.cryptogalera/TASK_BOARD.md`.

Do not start work with no Task ID. Do not invent a product task that is not on the board / `docs/tasks.md` / PO brief.

---

## Context Package

No spawn without a Context Package. Incomplete package → worker stops and asks the lead. Do not invent scope.

```
ROLE:
AGENT ID:
TASK:               (CG-XXX)
REPORT TO:
MISSION:
PRODUCT CONTEXT:    (mode, freeze, what this is not)
CURRENT STATE:      (branch, files that already exist, owners)
RESPONSIBILITIES:
CONSTRAINTS:
ACCEPTANCE CRITERIA:
VALIDATION:
REPORT PROTOCOL:    (this file)
```

Context is the minimum to execute: paths, docs, Task ID, freeze, ownership. It is not a paste of the constitution.

---

## Company memory (`.cryptogalera/`)

`.cryptogalera/` is **PO-owned company memory**. Process Lead, Leads, and Workers **do not create or edit it**.

Expected management files (PO):

| File | Role |
| --- | --- |
| `COMPANY_STATE.md` | Live mode, seating, freeze |
| `TASK_BOARD.md` | `CG-XXX` work and owners |
| `DECISIONS.md` | Durable process/product calls in this layer |
| `RISKS.md` | Optional |

Session SoT (`AGENTS.md`, `docs/*`) remains unverified and preserved. Live YAML in `cryptogalera/state/` stays State Lead files / PO content. Do not duplicate those stores into prompts.

---

## Spawn justification

A Lead (or PO) spawns only when **at least one** is true:

1. **Specialization** the parent cannot cover in one ownership area.
2. **True parallelism** on **disjoint** file ownership, with a named Integration Owner.
3. **Independent review** — always **REV-01**, never the lead reviewing themselves.
4. **Context isolation** so a large investigation does not pollute integration.

Before each Task call, the parent must be able to state: why this spawn, which paths the worker owns, who integrates. Recheck after each worker returns. Do not pre-spawn a tree. Do not spawn for status, ceremony, or a department name.

**Epic staffing (D-031):** PO assigns the domain leads the slice needs (Android UI; Go relay if server/API/storage; Rust core if crypto/protocol/UniFFI). Those leads spawn `AND-n` / `GO-n` / `CORE-n` subordinates for files/tests they own. All of them land on **one** feature branch. A one-string copy nit is not an epic; staff it as a hotfix after FAIL / PASS_WITH_CONCERNS leftover.

**Parallelism:** only on disjoint ownership. Two workers on the same paths is forbidden.

**Shared files:** name an **Integration Owner** before parallel work. Default: the domain lead the PO names (Engineering Lead if only one tree). Only that owner edits or sequences shared surfaces. Workers stay in their assigned trees.

---

## Report protocol

Default channel is the repo plus this report. Do not replace tasks or decisions with side chats.

### Worker → Lead

```
TASK: CG-XXX
AGENT: <id>
STATUS: COMPLETED | BLOCKED | PARTIAL | NEEDS_REVIEW
SUMMARY:
FILES READ:
FILES CHANGED:
VALIDATION:
DECISIONS:
ASSUMPTIONS:
RISKS:
BLOCKERS:
FOLLOW-UP:
IMPORTANT FOR PARENT:
```

No essays. STATUS `COMPLETED` means the brief’s Definition of Done holds, not that the worker feels finished.

### Lead → PO

Same fields. Lead adds: integration result, what was verified (not trusted), remaining owners, whether degraded mode was used. Lead does not rubber-stamp worker STATUS.

---

## Parent does not blindly trust DONE

The parent who spawned the worker **owns the outcome**.

On `COMPLETED` / `PARTIAL`, the parent checks: diff matches the brief, owned paths only, freeze respected, links/tests that the brief required, no extra scope. If the report and the tree disagree, the report is wrong.

Fake DONE is a process bug. Send it back or fix it; do not forward it to the PO as fact.

---

## Review

Adversarial reviewer **≠** implementer **and ≠** every domain lead on that slice. Required before merge (see constitution). The reviewer is **REV-01**: a **real** subagent or a seated human who did not author the change and did not lead the slice — not the implementer or lead wearing a second hat in the same context.

**REV-01 verdicts (D-031):** `PASS` ships. `PASS_WITH_CONCERNS` may ship (PO accepts leftover as a later hotfix, not a new epic cadence). `FAIL` blocks the tag.

---

## Degraded mode

If real subagents cannot be spawned (tool missing, policy, or failure):

1. The Lead (or PO) **does the work in-process**.
2. Report `DEGRADED` in ASSUMPTIONS / IMPORTANT FOR PARENT: what could not be spawned, and that no fake workers were used.
3. Independent review still cannot be the same context pretending to be two people; escalate or mark `NEEDS_REVIEW`.
4. Do not create `.cursor/agents/*` as a substitute.

Degraded mode is honest sequential work. It is not a license to skip the freeze or invent product.

---

## Freeze and Definition of Done

CG-007 unfreeze is **per user task**, recorded on the task board. Do not invent iOS/ratchet/landing or extra overlapping agents.

**Product Definition of Done (binding):**

1. Work is an **epic slice** on a `cursor/*` branch with a PR targeting `main` (or a hotfix leftover from FAIL / PASS_WITH_CONCERNS).
2. **REV-01** completed, independent of every implementer and every lead on the slice. FAIL blocks tag; PASS_WITH_CONCERNS may ship.
3. CI green on that PR.
4. **Fast-forward merged onto `main`** (when `gh pr merge` is unavailable: `git checkout main && git merge --ff-only <branch> && git push origin main`).
5. **Release:** bump Android `versionName`/`versionCode`, Go `ServerVersion`, crate version as needed; `git tag vX.Y.Z` and `git push origin vX.Y.Z` so Release publishes APK + `rope-server`. Do not retag existing published tags.
6. **Record on `main`**, then PO picks the next epic from the Research brief. Then and only then is the task DONE.

Leaving an open PR, or tagging without `main`, or calling the work finished in chat only, is a process failure. The PO owns it.

Prefer one capable worker per owned tree over three overlapping ones. Cost of a spawn is real. Domain leads exist so an epic can span Android + Go + Rust without the PO becoming a one-man army.
