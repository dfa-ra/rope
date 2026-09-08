# Organization — Rope repository

This is the org for **this repo as it is**: default branch `main` matches GitHub Latest `v0.2.15` (D-005 A) plus the CryptoGalera wrap. Do not spawn overlapping chrome/calls/landing agents. Unique unshipped work on `cursor/telegram-chrome-872f` is not the staffing baseline.

Operating model: [company.md](company.md). Agent rules: [AGENTS.md](../AGENTS.md).

## Current org chart

```
User
 └── Founder / Product Owner
      ├── Engineering Lead  ← integration owner (default)
      │    ├── (optional) Android specialist
      │    ├── (optional) Rust specialist
      │    └── (optional) Go / deployment specialist
      └── QA / Security     ← same person as Engineering Lead on ordinary work;
                              when independent review is required, reports to PO
```

**Not seated unless the task requires them:** Design (UI-shaped work only), Research Lead (unknowns dominate only).

No other departments. Specialists are created per task and released when the task ends. They are not standing teams.

## File ownership

| Path | Owner | Notes |
| --- | --- | --- |
| `apps/android/**` | Android | Kotlin UI, Room, HTTPS/WSS client, QR, SSH provision UI. Must not implement cryptography. |
| `core/rust/**` | Rust | Identity, keys, envelope, encrypt/sign/verify, invite + fingerprint checks. UniFFI API. |
| `server/go/**` | Go | REST, WSS, members/devices, invites, encrypted mailbox, SQLite. Never sees plaintext. |
| `protocol/**` | Engineering Lead | Wire format. Any change is cross-team. Coordinate before coding. |
| `deployment/**` | Go / deployment | `install.sh`, systemd, TLS material on the VPS. |
| `scripts/**` | Engineering Lead | UniFFI Kotlin bindings and Android native `.so` builds. Touches Android + Rust. |
| `docs/**` | PO + Engineering Lead | Product, architecture, threat model, company, tasks, decisions. |
| `.github/**`, `Makefile` | Engineering Lead | CI and release workflows. |

Shared surfaces: protocol docs, UniFFI API, invite URL, REST/WSS. The specialist who “needs a field” does not edit three trees alone — Engineering Lead sequences the work.

Do not change another row’s tree without the integration owner. Process/docs tasks do not edit `apps/`, `core/`, `server/`, `deployment/`, `protocol/`, `scripts/`, or CI.

## How a typical Rope change is staffed

Staff from the **smallest** row that covers the work. Do not add roles “for completeness.”

### Android-only UI tweak

Examples: copy, padding, a debug-screen label, a string resource. No protocol, no crypto, no new screens that imply new product scope.

- **Staff:** Engineering Lead does it, **or** one Android specialist.
- **Design:** only if the brief is a visual redesign, not a tweak.
- **Review:** Engineering Lead if a specialist did the work. No security agent.
- **Tests:** `apps/android` unit tests for touched logic; do not skip if ViewModel/repository code changed.
- **Forbidden:** a second agent for “polish,” a landing-page agent, a parallel Telegram-UI rewrite.

### Client + relay behavior, same protocol version

Examples: better error handling on mailbox fetch, installer flag plumbing, a local-only Room field.

- **Staff:** Engineering Lead, plus at most one specialist per **disjoint** tree if the slices are truly parallel.
- **Integrator:** Engineering Lead. One branch.
- **Review:** Engineering Lead; QA/Security if auth or TLS pinning is involved.

### Protocol change

Examples: new REST field, envelope header, invite URL query, WSS message type, protocol version bump.

- **Staff:** Engineering Lead (owner) + the trees that implement it (Android, Rust, Go as needed). **Not** three uncoordinated agents.
- **Order:** write the protocol doc change → get Engineering Lead (and PO if it is user-visible or version-breaking) → implement in lockstep → update [architecture.md](architecture.md) if the data flow changed.
- **Review:** independent review required (reviewer ≠ author). Threat model if guarantees shift.
- **Forbidden:** implementing only one side and leaving the rest “for later agents.”

### Crypto / identity / envelope change

Examples: new primitive, key wrap, signature payload, fingerprint check, anything in `core/rust` that handles keys or ciphertext.

- **Staff:** Engineering Lead + Rust specialist **or** Engineering Lead alone if small. **Separate** QA/Security reviewer (reviewer ≠ author).
- **Escalate:** PO before changing working crypto. User if it changes a threat-model guarantee or fundamental requirement.
- **Git:** minimal diff; do not rewrite working crypto as a cleanup. Run `cargo test` in `core/rust` plus any Go/Android tests that verify the handshake.
- **Docs:** [threat-model.md](threat-model.md) and protocol docs must match the code. Log the decision in [decisions.md](decisions.md).
- **Forbidden:** “improve” crypto without a brief; port a ratchet/X3DH because it is industry-standard (out of MVP unless PO/user says otherwise).

### Deployment / CI / release plumbing

- **Staff:** Engineering Lead or one Go/deployment specialist.
- **Review:** Engineering Lead; security review if TLS, `setup_token`, or signing secrets change.
- **Forbidden:** a release agent on every UI tweak.

### Unknown-heavy investigation

Examples: “why does pinning fail on this VPS image?” when the cause is not in-repo.

- **Staff:** Research Lead **or** Engineering Lead. Time-box. Return a recommendation.
- **Then:** PO decides; implementation is a new task with a normal owner. Research does not morph into a rewrite.

## Accountability

| Question | Answer |
| --- | --- |
| Who is accountable for this task? | The lead named in the brief (default: Engineering Lead) |
| Who may spawn agents? | That lead, after a plan, and only per [AGENTS.md](../AGENTS.md) |
| Who lands the branch? | Integration owner (default: Engineering Lead) |
| Who changes scope? | User, via PO — nobody else |
