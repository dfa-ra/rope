# Organization — Rope repository

This is the org for **this repo as it is**: default branch `main` matches GitHub Latest `v0.2.15` (D-005 A) plus the CryptoGalera wrap. Do not spawn overlapping chrome/calls/landing agents. Unique unshipped work on `cursor/telegram-chrome-872f` is not the staffing baseline.

Operating model: [company.md](company.md). Agent rules: [AGENTS.md](../AGENTS.md).

## Current org chart

```
User
 └── Founder / Product Owner  (D-015 unchanged; picks next epic from Research)
      ├── Research subteam (standing; ranked Telegram-gap epics; no product merges)
      ├── Android UI lead  ← domain lead; spawns AND-n; integrates Android on the epic branch
      │    └── AND-n subordinates (files/tests they own; never spawn)
      ├── Go relay lead    ← seated if server/API/storage; spawns GO-n
      │    └── GO-n subordinates
      ├── Rust core lead   ← seated if crypto/protocol/UniFFI; spawns CORE-n
      │    └── CORE-n subordinates
      └── REV-01           ← independent of every implementer and every lead on that slice
```

Default unit of work is an **epic slice** (D-031): a user-visible Telegram gap that may span Android + Go + Rust. Tiny copy nits are a hotfix after FAIL / PASS_WITH_CONCERNS leftover, not the sprint cadence.

**Seated every cycle:** Product Owner, standing Research, REV-01. **Seated per epic:** the domain leads the gap needs. Specialists (`AND-n` / `GO-n` / `CORE-n`) are created per task and released when the task ends.

**Not seated unless the task requires them:** Design (visual redesign only). No other departments.

Leads integrate on **one** feature branch. Leads do **not** self-review. PASS_WITH_CONCERNS may ship; FAIL blocks tag.

## File ownership

| Path | Owner | Notes |
| --- | --- | --- |
| `apps/android/**` | Android UI lead | Kotlin UI, HTTPS/WSS client, QR, SSH provision UI. Must not implement cryptography. May spawn `AND-n`. |
| `core/rust/**` | Rust core lead | Identity, keys, envelope, encrypt/sign/verify, invite + fingerprint checks. UniFFI API. May spawn `CORE-n`. |
| `server/go/**` | Go relay lead | REST, WSS, members/devices, invites, encrypted mailbox, SQLite. Never sees plaintext. May spawn `GO-n`. |
| `protocol/**` | Engineering Lead | Wire format. Any change is cross-team. Coordinate before coding. |
| `deployment/**` | Go / deployment | `install.sh`, systemd, TLS material on the VPS. |
| `scripts/**` | Engineering Lead | UniFFI Kotlin bindings and Android native `.so` builds. Touches Android + Rust. |
| `docs/**` | PO + Engineering Lead | Product, architecture, threat model, company, tasks, decisions. |
| `.github/**`, `Makefile` | Engineering Lead | CI and release workflows. |

Shared surfaces: protocol docs, UniFFI API, invite URL, REST/WSS. The specialist who “needs a field” does not edit three trees alone — the named integration owner sequences the work.

Do not change another row’s tree without the integration owner. Process/docs tasks do not edit `apps/`, `core/`, `server/`, `deployment/`, `protocol/`, `scripts/`, or CI.

## How a typical Rope change is staffed

Staff from the **smallest** row that covers the work. Do not add roles “for completeness.”

### Android-only UI epic

Examples: a Telegram-gap that is entirely in `apps/android` (search, badges, composer chrome). No protocol, no crypto.

- **Staff:** Android UI lead, plus `AND-n` subordinates for files/tests they own if needed.
- **Design:** only if the brief is a visual redesign, not a tweak.
- **Review:** **REV-01**, independent of the Android UI lead. PASS_WITH_CONCERNS may ship; FAIL blocks tag.
- **Tests:** `apps/android` unit tests for touched logic; do not skip if ViewModel/repository code changed.
- **Forbidden:** staffing a one-string copy nit as the sprint cadence; a second agent for “polish”; a parallel Telegram-UI rewrite; lead self-review.

### Client + relay behavior, same protocol version

Examples: better error handling on mailbox fetch, installer flag plumbing, a local-only store field.

- **Staff:** Android UI lead and/or Go relay lead as the gap needs, plus at most one subordinate per **disjoint** tree.
- **Integrator:** the domain lead the PO names. One branch.
- **Review:** **REV-01**; not either lead.

### Protocol change

Examples: new REST field, envelope header, invite URL query, WSS message type, protocol version bump.

- **Staff:** PO names an integration owner + Android UI / Go relay / Rust core leads as needed. **Not** three uncoordinated agents.
- **Order:** write the protocol doc change → get the integrator (and PO if it is user-visible or version-breaking) → implement in lockstep → update [architecture.md](architecture.md) if the data flow changed.
- **Review:** **REV-01** (reviewer ≠ every implementer and ≠ every lead). Threat model if guarantees shift.
- **Forbidden:** implementing only one side and leaving the rest “for later agents.”

### Crypto / identity / envelope change

Examples: new primitive, key wrap, signature payload, fingerprint check, anything in `core/rust` that handles keys or ciphertext.

- **Staff:** Rust core lead + `CORE-n` as needed. **Separate** REV-01 (and SEC-01 when threat-model claims move).
- **Escalate:** PO before changing working crypto. User if it changes a threat-model guarantee or fundamental requirement.
- **Git:** minimal diff; do not rewrite working crypto as a cleanup. Run `cargo test` in `core/rust` plus any Go/Android tests that verify the handshake.
- **Docs:** [threat-model.md](threat-model.md) and protocol docs must match the code. Log the decision in [decisions.md](decisions.md).
- **Forbidden:** “improve” crypto without a brief; port a ratchet/X3DH because it is industry-standard (out of MVP unless PO/user says otherwise).

### Deployment / CI / release plumbing

- **Staff:** Go relay lead or Engineering Lead as integrator.
- **Review:** **REV-01**; security review if TLS, `setup_token`, or signing secrets change.
- **Forbidden:** a release agent on every UI tweak; treating a version bump as an epic.

### Standing research (Telegram-gap scan)

Examples: what Telegram Android ships that Rope still lacks; ranked next epic.

- **Staff:** standing **Research subteam**. Internet + Telegram Android UX + Rope gap analysis. Time-box. Return a **ranked epic brief**, not a literature dump.
- **Then:** PO picks the next epic; implementation is a new task with domain leads. Research does **not** merge product code (D-031).

## Accountability

| Question | Answer |
| --- | --- |
| Who is accountable for this task? | The domain lead named in the brief (PO names the integrator) |
| Who may spawn agents? | That lead, after a plan, and only per [AGENTS.md](../AGENTS.md). Workers never spawn. |
| Who lands the branch? | Integration owner (the domain lead the PO names) |
| Who reviews? | **REV-01**, independent of every implementer and every lead on the slice |
| Who ranks the next epic? | Standing Research; PO picks |
| Who changes scope? | User, via PO — nobody else |
