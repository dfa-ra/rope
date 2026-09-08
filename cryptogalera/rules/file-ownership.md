# File ownership

Matches [docs/organization.md](../../docs/organization.md), plus CryptoGalera runtime paths. Do not edit another row without the integration owner.

| Path | Owner | Notes |
| --- | --- | --- |
| `apps/android/**` | Android | Kotlin UI, Room, HTTPS/WSS, QR, SSH provision UI. **Must not implement cryptography.** |
| `core/rust/**` | Rust | Identity, keys, envelope, encrypt/sign/verify, invite + fingerprint checks. UniFFI API. |
| `server/go/**` | Go | REST, WSS, members/devices, invites, encrypted mailbox, SQLite. Never sees plaintext. |
| `protocol/**` | Engineering Lead | Wire format. Cross-team. Coordinate before coding. |
| `deployment/**` | Go / deployment | `install.sh`, systemd, TLS material on the VPS. |
| `scripts/**` | Engineering Lead | UniFFI Kotlin bindings and Android native `.so` builds. Touches Android + Rust. |
| `docs/**` | PO + Engineering Lead | Product, architecture, threat model, company, tasks, decisions, API pointer. |
| `.github/**`, `Makefile` | Engineering Lead | CI and release workflows. |
| `cryptogalera/CONSTITUTION.md`, `cryptogalera/README.md`, `cryptogalera/rules/**` | Process | Binding runtime law. PO is T-006 integrator, not a co-owner of these files. |
| `cryptogalera/state/**` | PO (live state) | YAML written by State Lead; Process does not create or overwrite these files. |
| `.cursor/rules/` | Process | Cursor always-on rules. Process Lead. |

Shared surfaces (protocol docs, UniFFI, invite URL, REST/WSS) are sequenced by the Engineering Lead. Process/docs tasks do not edit `apps/`, `core/`, `server/`, `deployment/`, `protocol/`, `scripts/`, `Makefile`, or `.github/`.
