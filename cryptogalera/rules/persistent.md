# Persistent constraints

Always on. Mode does not relax these.

- **Adopt existing Rope.** Do not rewrite, replace, or re-home product code. Product lives in `apps/`, `core/`, `server/`, `deployment/`, `protocol/`.
- **Do not revert or delete** user work, session markdown, or existing branches/PRs/tags. Unverified is not a reason to reset.
- **Product work is allowed when tasked.** Do not invent iOS/ratchet/landing or extra overlapping agents (T-003 / D-006).
- **D-005 A is done.** Trunk is `main`. Feature-agent freeze (T-003 / D-006) remains in force against overlapping new agents.
- **Session docs are unverified and preserved.** `AGENTS.md` and `docs/*.md` are SoT for process/product as adopted; do not treat unverified as license to rewrite them.
- **Crypto stays in Rust.** Kotlin never implements cryptography and never handles raw private keys (`core/rust` only).
- **One task, one owner.** No overlapping agents on the same paths. Integration owner required before parallel work.
- **Delivery (D-015 + D-031).** Default unit of work is an **epic slice**, not a one-string bump. After a completed product epic: PR → **REV-01** (independent of every implementer and every lead) → CI → ff-merge `main` → tagged GitHub Release → record on `main`. PASS_WITH_CONCERNS may ship; FAIL blocks tag. An open unmerged PR is not done. The PO is Product Owner of Rope and owns that miss.
- **Org (D-031).** PO assigns Android UI / Go relay / Rust core **domain leads** for the trees the epic needs. Leads spawn `AND-n` / `GO-n` / `CORE-n` subordinates and integrate on one branch. Leads do not self-review. Standing **Research** ranks Telegram-gap epics and does not merge product code. No FCM. Name/logo colors stay.
