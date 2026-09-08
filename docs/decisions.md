# Decisions

Durable decisions for Rope. Format: [company.md](company.md). Do not log reversible local choices.

Status: `accepted` = in force · `proposed` = needs user or PO · `superseded` = historical.

---

### D-001: Documented product is `main` MVP

Date: 2026-09-08
Status: accepted (operational; user may override via D-005)
Decider: Product Owner
Context: README and `main` describe a 1-to-1 text MVP (`0.1.0`). Cloud agents shipped GitHub Releases `v0.2.x` from side branches that never merged to `main`. Agents were treating tags and PR titles as product.
Decision: Until the user picks a trunk in D-005, the product contract is [`main` + product.md + README](product.md). Off-`main` features are not in scope. A Release tag does not change scope.
Consequences: New work branches from `main`. No “catch up to 0.2.15” agents. Existing Stage-2 PRs stay open but are not the staffing baseline.
Alternatives considered: Treat `v0.2.15` as canonical immediately (rejected — changes scope without the user). Ignore Releases (rejected — users may already have those APKs; the split must be visible).

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
Status: proposed (user decision)
Decider: User (Product Owner will execute the choice)
Context: README lists calls, files, groups, web/desktop, and polished Telegram UI as **not in MVP**. Unmerged PRs #2 and #3 and tags `v0.2.0`–`v0.2.15` implement much of that anyway. `main` is still `0.1.0`. Protocol on Stage-2 expands envelope types, objects, groups, and call signaling. Nested PRs: PR #2 ⊂ PR #3 ⊂ `v0.2.15`. Tag `v0.2.15` is not the same git tip as PR #3 (the tag is ahead).
Decision needed — pick **one** trunk:

| Option | Meaning |
| --- | --- |
| **A. Promote Stage-2** | Fast-forward `main` to a single named integration tip (likely tag `v0.2.15`, which is ahead of PR #3), rewrite product.md / protocol / threat model to match, close nested PRs |
| **B. Keep MVP** | `main` stays 1-to-1 text. Treat `v0.2.x` as experimental. Stop publishing tags from side branches. Close or freeze Stage-2 PRs |
| **C. Subset** | User names which Stage-2 pieces (e.g. UI chrome only, or calls only) may merge, with a new threat model and protocol bump |

Recommendation: **Do not spawn more Stage-2 agents until this is answered.** If the user wants a messenger that already has calls/groups/Telegram chrome, choose A and then staff one integration owner. If the user wants the original private 1-to-1 product, choose B.
Consequences: Until answered, D-001 holds. Protocol forks will break mixed-version installs if Stage-2 binaries are used against MVP servers without a migration story.
Alternatives considered: Keep both lines forever (rejected — that is the current failure mode).

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
