# Architecture (pointer)

PO-owned. Do not duplicate the product architecture.

**Source of truth for system design:** [docs/architecture.md](../docs/architecture.md)

**Wire contracts:** [docs/api.md](../docs/api.md) → `protocol/docs/`

**Reconstructed correction (D-010):** client local store is `SQLiteOpenHelper` in `LocalStore.kt`, not Android Room, despite the architecture doc saying Room.

**This checkout:** protocol v1, text envelopes only. Stage-2 protocol on other branches is not SoT until D-005.
