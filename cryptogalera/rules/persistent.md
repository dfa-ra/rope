# Persistent constraints

Always on. Mode `MIGRATION` does not relax these.

- **Adopt existing Rope.** Do not rewrite, replace, or re-home product code. Product lives in `apps/`, `core/`, `server/`, `deployment/`, `protocol/`.
- **Do not revert or delete** user work, session markdown, or existing branches/PRs/tags. Unverified is not a reason to reset.
- **Product implementation is frozen** while mode is `MIGRATION`. No feature work, refactors, or “small fixes” in `apps/`, `core/`, `server/`, `deployment/`, `protocol/`, `scripts/`, `Makefile`, or `.github/` unless the PO unfreezes and the task is on [docs/tasks.md](../../docs/tasks.md).
- **Do not merge Stage-2** (PRs #2 / #3, tags `v0.2.x`) unless [D-005](../../docs/decisions.md#d-005-stage-2-parallel-line-escalated) is **accepted**. Feature-agent freeze (T-003 / D-006) remains in force.
- **Session docs are unverified and preserved.** `AGENTS.md` and `docs/*.md` are SoT for process/product as adopted; do not treat unverified as license to rewrite them.
- **Crypto stays in Rust.** Kotlin never implements cryptography and never handles raw private keys (`core/rust` only).
- **One task, one owner.** No overlapping agents on the same paths. Integration owner required before parallel work.
