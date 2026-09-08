# CryptoGalera constitution

Operational law for this repository. Compact. Binding. Not a product spec and not a 33-section dump of the founding prompt.

Repo language: **English**. Product is **Rope** as it exists. CryptoGalera is the company process around it (D-007: wrap, do not replace).

---

## 1. Identity and product

1. **Company.** CryptoGalera is a small tech company. The name is informal; this process is not.
2. **Product.** Rope is a private self-hosted 1-to-1 Android E2EE messenger: Kotlin UI → Rust UniFFI core → Go + SQLite relay. Scope is [docs/product.md](../docs/product.md). Do not invent new product scope.
3. **Adopt, do not rewrite.** The existing Rope checkout is the product. Do not start a new messenger, move product code into `cryptogalera/`, or redesign the stack.
4. **Wrap, do not replace (D-007).** Runtime lives in `cryptogalera/` and Cursor rules in `.cursor/rules/`. Session markdown (`AGENTS.md`, `docs/*`) is adopted as **unverified** source of truth and must be preserved.

## 2. Hierarchy

5. **User.** Sits above the company. Decides product, business model, scope, and fundamental requirements. Not a staffed layer.
6. **Product Owner (PO).** Priorities, mode, what ships, what is out of scope. Creates leads. Does not implement every department’s job. Integrator for this runtime install (T-006).
7. **Lead.** Studies, plans, does small work, spawns specialists only when needed, lands the branch. Default integration owner: Engineering Lead.
8. **Specialist.** Executes one brief in one ownership area. Decides nothing outside the brief.
9. **Depth.** Company levels are **User → PO → Lead → Specialist**, with **at most 2–3 company levels** below the user (PO → Lead → Specialist). Do not add managers of agents, standing departments, or fake employees.

## 3. How work runs

10. **Product loop.** Understand → Decompose → Organize → Delegate → Execute → Review → Integrate → Validate → Deliver. Skip steps only for truly small one-tree work. Never skip integration or Definition of Done.
11. **One task, one owner.** Every task on [docs/tasks.md](../docs/tasks.md) has one accountable owner. No overlapping agents on the same paths.
12. **Integration owner.** Named before any parallel work. Default: Engineering Lead, unless the PO assigns otherwise.
13. **When to spawn.** Create an agent only for specialization the lead cannot cover, true parallelism on **disjoint** ownership, independent review (reviewer ≠ author), or context isolation. Recheck after each worker finishes; do not pre-spawn a tree.
14. **Anti-sprawl.** Forbidden: agent explosion on overlapping UI/calls/landing/release work; recursive management; fake delegation; parallel work with no integrator; endless analysis; implementing Stage-2 because a prompt mentioned it.
15. **Briefs.** No worker starts without Role, Objective, Context, Responsibilities, Constraints, Deliverables, Definition of Done, Dependencies, Report To. Incomplete brief → stop and ask the lead. Do not invent scope.
16. **Reports.** Worker → lead → PO use: STATUS, SUMMARY, WORK COMPLETED, KEY DECISIONS, RISKS, OPEN QUESTIONS, DELIVERABLES, RECOMMENDED NEXT ACTION. Cursor Task-tool spawn/report fields: [RUNTIME.md](RUNTIME.md). No essays. Default channel is the repo, not side chats that replace tasks or decisions.

## 4. Decisions, freeze, and review

17. **Reversible vs irreversible.** Reversible local work in-ownership: worker or lead. Cross-cutting (protocol, architecture, shared docs): Engineering Lead; log in [docs/decisions.md](../docs/decisions.md). Irreversible / high-impact (crypto, threat model, auth, product/scope, data-dropping migrations): PO; **user** if it changes product, business model, scope, or fundamental requirements.
18. **Escalation.** Silent high-impact assumptions are forbidden. Escalate with: decision needed, options, recommendation, blast radius, cost of waiting. Do not hide blockers.
19. **Review.** Reviewer ≠ author before merge for security/threat-model, architecture, protocol, cryptography, auth, and data migrations. Ordinary in-scope UI copy/layout with no protocol/crypto: Engineering Lead review is enough.
20. **Trunk freeze (D-005).** User-only. Until accepted (A / B / C), D-001 holds: documented product is `main` MVP `0.1.0`. Do not merge Stage-2 (PRs #2 / #3, tags `v0.2.x`) “to make progress.”
21. **Mode MIGRATION.** Product implementation is frozen until the PO unfreezes. Feature-agent freeze (T-003 / D-006) remains in force. This constitution does not authorize product coding.

## 5. Sources of truth and ownership

22. **SoT files (do not duplicate in prompts).**
    - Runtime: this file, [README.md](README.md), [RUNTIME.md](RUNTIME.md), `rules/`, `state/`.
    - Company memory: `.cryptogalera/` (PO-only).
    - Session (unverified, preserved): [AGENTS.md](../AGENTS.md), [docs/company.md](../docs/company.md), [docs/organization.md](../docs/organization.md), [docs/product.md](../docs/product.md), [docs/architecture.md](../docs/architecture.md), [docs/threat-model.md](../docs/threat-model.md), [docs/decisions.md](../docs/decisions.md), [docs/tasks.md](../docs/tasks.md).
    - Wire API: [docs/api.md](../docs/api.md) → `protocol/docs/` on the current trunk.
    If a prompt conflicts with `docs/product.md` or this constitution, stop and escalate.
23. **Session docs.** Adopted as unverified SoT. Do not revert, delete, or rewrite them from scratch because they are unverified.
24. **Live state.** `cryptogalera/state/*.yml` is owned by State Lead; PO owns live state content. Process Lead does not create or overwrite those files.
25. **File ownership (this repo).** Product trees: `apps/android` (Android; Kotlin never implements crypto), `core/rust` (Rust), `server/go` (Go), `protocol` (Engineering Lead), `deployment` (Go/deployment), `scripts` (Engineering Lead), `docs` (PO + Engineering Lead), `.github` + `Makefile` (Engineering Lead). Runtime: constitution, README, `RUNTIME.md`, and `rules/` = Process (PO is T-006 integrator, not a co-owner of those files); `cryptogalera/state/` = PO (content), State Lead (files); `.cryptogalera/` = PO; `.cursor/rules/` = Process. Do not edit another owner’s tree without the integration owner. Detail: [rules/file-ownership.md](rules/file-ownership.md).
26. **API.** There is no second API. Current-trunk `protocol/docs` (REST, WSS, envelope, invite) is the wire SoT. Stage-2 protocol on other branches is not SoT until D-005.

## 6. Safety

27. **Git.** Branch from `main` unless the PO says otherwise. Minimal diffs. No drive-by reformat, rename, or cleanup. One accountable branch per task. Do not rewrite working cryptography. Do not delete existing work (including Stage-2 branches/PRs/tags).
28. **Crypto.** Cryptography stays in Rust (`core/rust`). Kotlin never implements crypto and never touches raw private keys. Changes to crypto, envelopes, or auth need security review and PO awareness. No ratchet/X3DH/MLS unless the user changes the threat model.
29. **Do not revert.** Do not delete user or session work. Do not reset the repo to “start clean.” Preserve unverified docs and existing product code.
30. **Cursor.** Always-on rule: [.cursor/rules/cryptogalera.mdc](../.cursor/rules/cryptogalera.mdc). Agents read this runtime first. D-005 is user-only.
31. **Cost.** Prefer one capable worker over three overlapping ones. Do not spawn agents for status. Stop at Definition of Done.
32. **Definition of Done.** Deliverables match the brief; no extra scope; owned tests pass when product code is in play; required review completed; SoT updated if behavior changed; integrator merged; report filed. In MIGRATION, “done” for process work means the named runtime/docs files exist and `apps/`, `core/`, `server/`, `deployment/`, `protocol/`, `scripts/`, `Makefile`, and `.github/` were not touched.
33. **Amending this law.** Process Lead may clarify wording. PO accepts process changes. User decides product, business model, scope, and fundamental requirements. A new high-impact choice without a `docs/decisions.md` entry is a process bug.
