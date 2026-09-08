# Decisions (live)

PO-owned compact log. Full prose: [docs/decisions.md](../docs/decisions.md).

| ID | Status | Decision |
| --- | --- | --- |
| D-001 | accepted | Documented product is `main` MVP 0.1.0 until D-005 |
| D-002 | accepted | Stack: Kotlin UI → Rust UniFFI → Go + SQLite |
| D-003 | accepted | Ed25519 + X25519 + HKDF + XChaCha20-Poly1305; **no ratchet** in v1 |
| D-004 | accepted | Self-signed TLS + fingerprint pin; server never sees plaintext |
| D-005 | **proposed (user)** | Trunk: **A** promote Stage-2 (`v0.2.15`) / **B** keep MVP / **C** subset |
| D-006 | accepted | One owner, one branch; Stage-2 feature agents frozen |
| D-007 | accepted | CryptoGalera **wraps** Rope; do not replace; session docs preserved |
| D-008 | accepted | Cursor runtime protocol is `cryptogalera/RUNTIME.md`; company memory is `.cryptogalera/` (PO-only). Workers never spawn. Real Task-tool subagents only. |

D-008 context: user loaded the Cursor Multi-Agent Runtime Protocol. Complements constitution (who) with spawn/report (how). Does not unfreeze product work.
