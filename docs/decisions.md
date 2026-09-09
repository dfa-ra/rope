# Decisions

Durable decisions for Rope. Format: [company.md](company.md). Do not log reversible local choices.

Status: `accepted` = in force · `proposed` = needs user or PO · `superseded` = historical.

---

### D-001: Documented product is `main` MVP

Date: 2026-09-08
Status: **superseded** by [D-005](#d-005-stage-2-parallel-line-escalated) option A (2026-09-08)
Decider: Product Owner
Context: README and `main` described a 1-to-1 text MVP (`0.1.0`). Cloud agents shipped GitHub Releases `v0.2.x` from side branches that never merged to `main`. Agents were treating tags and PR titles as product.
Decision: Until the user picked a trunk in D-005, the product contract was [`main` + product.md + README](product.md). Off-`main` features were not in scope. A Release tag did not change scope.
Consequences: Held until D-005 A. Historical: new work branched from `main`; Stage-2 PRs stayed open.
Alternatives considered: Treat `v0.2.15` as canonical immediately (deferred to D-005). Ignore Releases (rejected — users may already have those APKs; the split must be visible).

---

### D-002: Client / core / relay stack

Date: 2026-09-08
Status: accepted (already shipped on `main`)
Decider: Engineering (implicit in repo); confirmed by Product Owner
Context: Need a small, auditable E2EE messenger that a single organizer can host.
Decision: Android Kotlin UI; cryptography only in Rust via UniFFI (`rope_core`); Go relay with SQLite; no FCM — the personal VPS is the transport.
Consequences: iOS/web/desktop are new products, not ports of the Kotlin layer. Kotlin must not touch raw private keys.
Alternatives considered: Pure Kotlin crypto (rejected — threat model wants a small native core). Single-language stack (rejected — UI, crypto, and relay have different constraints).

---

### D-003: Crypto primitives and no ratchet in v1

Date: 2026-09-08
Status: accepted
Decider: Engineering + PO (matches [threat-model.md](threat-model.md))
Context: MVP needs confidentiality and sender authenticity without a Signal-sized protocol.
Decision: Device keys Ed25519 (sign) + X25519 (ECDH). Envelope: HKDF-SHA256, XChaCha20-Poly1305. **No** X3DH / Double Ratchet / MLS. Compromise of a device ECDH key exposes future messages to that device until keys are rotated (out of scope).
Consequences: Do not “improve” crypto by adding a ratchet because it is industry-standard. That is a user-visible threat-model change (escalate).
Alternatives considered: Signal protocol (out of MVP). MLS for groups (no groups on `main`).

---

### D-004: Trust boundaries and transport

Date: 2026-09-08
Status: accepted
Decider: Engineering + PO (matches architecture and threat model)
Context: The VPS operator is untrusted for plaintext; devices are trusted.
Decision: Server stores public identities, invite token hashes, and opaque mailbox blobs. Self-signed TLS plus client fingerprint pinning; invite URL binds `server_id` + fingerprint. Auth header `Rope <device_id>.<unix_seconds>.<sig>`. One organizer VPS; owner bootstrap via one-time `setup_token`.
Consequences: Ignoring a fingerprint mismatch is a user action the product must not paper over. Public CA is not required for MVP. SSH credentials exist only for provisioning.
Alternatives considered: Public CA + Let’s Encrypt as default (possible later; not required). Trusting the VPS for keys (rejected).

---

### D-005: Stage-2 parallel line (escalated)

Date: 2026-09-08
Status: **accepted — option A**
Decider: User (task: put current releases on `main`; verify they match; tidy extra branches; nothing lost)
Context: README listed calls, files, groups, web/desktop, and polished Telegram UI as **not in MVP**. Unmerged PRs #2 and #3 and tags `v0.2.0`–`v0.2.15` implemented much of that anyway. `main` was still `0.1.0`. Nested PRs: PR #2 ⊂ PR #3 ⊂ `v0.2.15`. Tag `v0.2.15` is ahead of PR #3. `main` cannot fast-forward to the tag (6 CryptoGalera commits vs 92 Stage-2 commits from merge-base `30ebaa7`).
Decision: **A. Promote the currently shipped line.** Merge tag **`v0.2.15`** (`9694e804691bfb32b9a2ecc9e1f9e7e2cf48f0a5`, same as `origin/cursor/release-0215-872f`) into `main`. Keep CryptoGalera wrap files. Keep existing GitHub Releases and the tag (do not retag, do not replace APK/server assets). Close nested PRs #2 / #3 only after their commits are on `main`. Delete `cursor/*` branches only when fully contained in `main` or in a kept tag. Preserve unique work on `cursor/telegram-chrome-872f` (`039e381`, not in the shipped tag).
Consequences: Documented product becomes the Stage-2 tree that already shipped as Latest. D-001 is superseded. New overlapping feature agents stay frozen (D-006 / T-003). Do not rebuild `v0.2.15` unless product trees match and the user still wants a new tag.
Alternatives considered: **B** keep MVP (rejected — user asked for current releases on `main`). **C** subset (rejected — user asked for the same APK/core as now). Keep both lines forever (rejected — that was the failure mode).

---

### D-006: Merge policy and agent freeze

Date: 2026-09-08
Status: accepted
Decider: Product Owner
Context: ~30 `cursor/*` branches and dozens of cloud agents overlapped on calls, TURN/ICE, UI, landing, and release bumps with no integrator.
Decision: One task → one accountable owner → one branch. Nested supersets (PR #2 vs #3) are not two merge targets. New feature agents are frozen until D-005. Integration owner defaults to Engineering Lead. Reviewer ≠ author for crypto, auth, protocol, architecture.
Consequences: Overlapping agents on the same paths are a process bug. Release bumps are not a substitute for merging to `main`.
Alternatives considered: Continue slice-agents per screen (rejected — proven to fork the product).

---

### D-007: CryptoGalera runtime wraps Rope; product is frozen

Date: 2026-09-08
Status: accepted
Decider: Product Owner (user: adopt existing project; migration not implementation)
Context: The user sent the CryptoGalera constitution, then ordered a stop on autonomous product work, an inventory, and installation of runtime / persistent rules / company state / agent structure **around** the existing project. Session docs (`AGENTS.md`, `docs/company.md`, …) are unverified and must be preserved, not reverted.
Decision:
- Install a CryptoGalera **runtime** at `cryptogalera/` plus Cursor enforcement at `.cursor/rules/cryptogalera.mdc`.
- Do **not** implement, merge, or redesign Rope product code in this phase.
- Do **not** delete or reset existing work (including Stage-2 branches/PRs/tags and the unverified session docs).
- Previous session markdown is **adopted as unverified SoT**, not treated as the runtime.
- Product trunk remains D-001 / D-005 (user still must choose A/B/C before product implementation resumes).
Consequences: Agents in this repo first read `cryptogalera/` state. Mode is `MIGRATION` until the PO sets it otherwise. Feature freeze (T-003) still holds.
Alternatives considered: Replace Rope with a new CryptoGalera product (rejected). Delete session docs and start over (rejected — user forbade revert). Treat constitution prompt as a new product build (rejected).

---

### D-008: Cursor multi-agent runtime protocol

Date: 2026-09-08
Status: accepted
Decider: Product Owner (user loaded the Cursor Runtime Protocol)
Context: Constitution defines who decides. The new protocol defines how Cursor subagents spawn, report, and how company memory is stored. Product remains frozen (D-007).
Decision:
- Persist protocol as `cryptogalera/RUNTIME.md`.
- PO-owned live memory in `.cryptogalera/` (`COMPANY_STATE.md`, `TASK_BOARD.md`, `DECISIONS.md`, `RISKS.md`). Only the root agent edits those files.
- Task IDs `CG-XXX`. Workers never spawn. A subagent exists only as a real Cursor Task invocation.
- Does **not** unfreeze Rope implementation.
Consequences: New orchestration uses Context Packages and the task board. Fake “I sent this to Backend Lead” is a protocol violation.
Alternatives considered: Replace `cryptogalera/` with only `.cryptogalera/` (rejected — preserve existing runtime). Start product coding because the protocol says “begin” (rejected — D-007 / MIGRATION still holds).

---

### D-009: Adopt existing workspace (do not restart)

Date: 2026-09-08
Status: accepted
Decider: Product Owner (user: CRYPTOGALERA — ADOPT EXISTING WORKSPACE)
Context: User ordered observe → inventory → reconstruct → install control layer → continue from current standing. Forbade delete/rewrite/reset. D-005 remains a user trunk choice.
Decision: Treat this checkout as ADOPTED_EXISTING_WORKSPACE. Refresh `.cryptogalera/` from code evidence. Install a small `.cursor/agents/` set. Record baseline tests. Convert in-tree unfinished work (Settings stub, revoke UI) to BACKLOG tasks, not new products. Do not merge Stage-2. Do not unfreeze coding.
Consequences: Company memory must list CONFIRMED vs INFERRED vs UNKNOWN. Reconstructed stack decisions stay labeled reconstructed. Next product implementation still needs D-005 or explicit CG-007.
Alternatives considered: Rebuild Rope under CryptoGalera (rejected). Promote v0.2.15 as part of adoption (rejected — D-005).

---

Product ships **D-010 through D-030** (stack reconstruction, calls/notify, Telegram chrome slices 0.3.2–0.3.17, and D-015 PO-of-Rope delivery law) are recorded in [`.cryptogalera/DECISIONS.md`](../.cryptogalera/DECISIONS.md). Formal session log continues with process law D-031.

---

### D-031: Expanded delivery org (leads + research)

Date: 2026-09-09
Status: accepted
Decider: User (instruction: expand the Cursor PO team — larger slices, domain leads, standing research). Recorded by Product Owner / ORG-01.
Context: Delivery law D-015 already makes this Cursor agent Product Owner of Rope and requires PR → review → merge → tagged release. Sprint cadence had collapsed into one-string / version-bump slices staffed as a single AND-01 under the PO. The user ordered larger Telegram-gap epics, domain leads who spawn subordinates, and a standing research subteam that scans the internet for Telegram-like features. After this law was drafted as D-030 against older `main`, v0.3.17 landed as product **D-030** (CG-109). Org law therefore takes the next free ID **D-031**. Do not drop the 0.3.17 product decision.
Decision:
- **D-015 is unchanged.** The Cursor agent on this run remains Product Owner of Rope.
- Default unit of work is an **epic slice**: a user-visible Telegram gap that may span Android + Go + Rust. Tiny copy nits are a hotfix after FAIL / PASS_WITH_CONCERNS leftover, not the sprint cadence.
- PO assigns **domain leads** per slice: Android UI lead; Go relay lead if server/API/storage; Rust core lead if crypto/protocol/UniFFI. Leads may spawn `AND-n` / `GO-n` / `CORE-n` subordinates for files/tests they own. Leads integrate on **one** feature branch. Leads do **not** self-review.
- Standing **Research subteam**: internet + Telegram Android UX + Rope gap analysis → ranked epics. Research does **not** merge product code. PO picks the next epic from their brief.
- **REV-01 remains independent** of every implementer and every lead on that slice. PASS_WITH_CONCERNS may ship; FAIL blocks tag.
- Cadence unchanged: PR → REV-01 → CI green → ff-merge `main` → tag `vX.Y.Z` → GitHub Release → record on `main` → next epic. Do not leave PRs open. Do not retag published releases. No FCM. Kotlin never implements crypto. Name/logo colors stay.
- Last ship remains **v0.3.17** (product D-030). Next product slice is **0.3.18** grouped photo albums.
Consequences: Org size grows inside the existing User → PO → Lead → Specialist depth. Domain leads are the Lead layer. Research is a standing subteam under PO, not a fourth management layer and not a merge path. Overlapping extra feature agents on the same paths remain forbidden (D-006 / T-003). This decision is org law; it does not bump Android/Go/Rust versions. Org docs do not block the next product epic; Research staffing can proceed in parallel.
Alternatives considered: Keep one-string hotfixes as the cadence (rejected — user asked for larger slices). Let leads self-review ordinary UI (rejected — REV-01 stays independent of leads). Let research merge product code (rejected). Add managers of agents / extra departments (rejected — D-031 expands leads + research only). Keep org as D-030 after v0.3.17 shipped (rejected — would collide with the 0.3.17 product decision).
