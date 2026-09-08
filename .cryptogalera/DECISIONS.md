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
| D-001 | **superseded by D-005 A** | Was: documented product is `main` MVP 0.1.0 until D-005 |
| D-005 | **accepted (A)** | Promote shipped Stage-2 line: merge tag `v0.2.15` (`9694e80`) into `main`. Keep existing GitHub Releases. Do not retag. |
| D-006 | accepted | One owner, one branch; no overlapping new Stage-2 feature agents |
| D-007 | accepted | Wrap Rope; do not replace; do not delete unique existing work |
| D-008 | accepted | `cryptogalera/RUNTIME.md` + `.cryptogalera/` PO memory; Workers never spawn |
| D-009 | accepted | ADOPTED_EXISTING_WORKSPACE; continue from this checkout; `.cursor/agents/` reusable roles only |

**D-005 A (2026-09-08):** User asked to put current releases on `main`, verify they match published artifacts, and tidy extra branches without losing work. Integration tip is GitHub Latest tag `v0.2.15` = `origin/cursor/release-0215-872f` = `9694e804691bfb32b9a2ecc9e1f9e7e2cf48f0a5`. Path is merge (not fast-forward): `main` has 6 CryptoGalera commits; tag has 92 Stage-2 commits from merge-base `30ebaa7`.

**CG-017 (2026-09-08):** PR #4 merged to `main`. PRs #2/#3 left open until this promotion lands.
