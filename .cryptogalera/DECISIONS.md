# Decisions

PO-owned. Reconstructed vs formally accepted are marked. Full prose: [docs/decisions.md](../docs/decisions.md).

## Reconstructed from existing implementation

These were already true in Rope **before** CryptoGalera. Not invented here.

| ID | Decision | Status | Evidence |
| --- | --- | --- | --- |
| D-002 | Kotlin UI → Rust UniFFI crypto → Go + SQLite relay | RECONSTRUCTED FROM EXISTING IMPLEMENTATION | `apps/android`, `core/rust`, `server/go`, README |
| D-003 | Ed25519 + X25519 + HKDF-SHA256 + XChaCha20-Poly1305; no ratchet | RECONSTRUCTED FROM EXISTING IMPLEMENTATION | `core/rust`, `docs/threat-model.md` |
| D-004 | Self-signed TLS + fingerprint pin; server never sees plaintext | RECONSTRUCTED FROM EXISTING IMPLEMENTATION | installer, `PinnedClient`, threat model |
| D-010 | Local history via Android SQLiteOpenHelper + Keystore wrap (not Room) | RECONSTRUCTED FROM EXISTING IMPLEMENTATION | `LocalStore.kt`, `IdentityVault.kt` vs architecture.md claiming Room |

## CryptoGalera formal

| ID | Status | Decision |
| --- | --- | --- |
| D-001 | accepted | Documented product is this-tree MVP 0.1.0 until D-005 |
| D-005 | **proposed (user)** | Trunk A promote Stage-2 / B keep MVP / C subset |
| D-006 | accepted | One owner, one branch; no overlapping Stage-2 agents |
| D-007 | accepted | Wrap Rope; do not replace; do not delete existing work |
| D-008 | accepted | `cryptogalera/RUNTIME.md` + `.cryptogalera/` PO memory; Workers never spawn |
| D-009 | accepted | ADOPTED_EXISTING_WORKSPACE; continue from this checkout; `.cursor/agents/` reusable roles only |

D-009 does **not** unfreeze product coding and does **not** choose D-005.
