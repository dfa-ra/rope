# Architecture (pointer)

PO-owned. Do not duplicate the product architecture.

**Source of truth for system design:** [docs/architecture.md](../docs/architecture.md)

**Wire contracts:** [docs/api.md](../docs/api.md) → `protocol/docs/` on the chosen trunk (`v0.2.15` / `main` after CG-004).

**Reconstructed correction (D-010):** client local store is `SQLiteOpenHelper` in `LocalStore.kt`, not Android Room, despite the architecture doc saying Room.

**Trunk (D-005 A):** product trees `apps/`, `core/`, `server/`, `deployment/`, `protocol/` must match tag `v0.2.15` after the promotion merge. CryptoGalera wrap stays beside them.
