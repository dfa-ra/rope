# Persistent constraints

Always on. Mode does not relax these.

- **Adopt existing Rope.** Do not rewrite, replace, or re-home product code. Product lives in `apps/`, `core/`, `server/`, `deployment/`, `protocol/`.
- **Do not revert or delete** user work, session markdown, or existing branches/PRs/tags. Unverified is not a reason to reset.
- **Product work is allowed when tasked.** Do not invent iOS/ratchet/landing or extra overlapping agents (T-003 / D-006).
- **D-005 A is done.** Trunk is `main`. Feature-agent freeze (T-003 / D-006) remains in force against overlapping new agents.
- **Session docs are unverified and preserved.** `AGENTS.md` and `docs/*.md` are SoT for process/product as adopted; do not treat unverified as license to rewrite them.
- **Crypto stays in Rust.** Kotlin never implements cryptography and never handles raw private keys (`core/rust` only).
- **One task, one owner.** No overlapping agents on the same paths. Integration owner required before parallel work.
- **Delivery.** After a completed product task: PR → review → merge to `main` → tagged GitHub Release. An open unmerged PR is not done. The PO is Product Owner of Rope and owns that miss.
