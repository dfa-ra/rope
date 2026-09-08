# API source of truth

This file is a **pointer**, not a second API.

The wire contract for the **current trunk** (`main` after D-005 A, matching tag `v0.2.15`) is:

| Topic | File |
| --- | --- |
| REST | [protocol/docs/rest.md](../protocol/docs/rest.md) |
| WebSocket | [protocol/docs/wss.md](../protocol/docs/wss.md) |
| Envelope | [protocol/docs/envelope.md](../protocol/docs/envelope.md) |
| Invite / deep link | [protocol/docs/invite.md](../protocol/docs/invite.md) |

Do not duplicate those specs here. If a prompt or agent disagrees with the files above, the current-trunk `protocol/docs` win.

Those files are the Stage-2 protocol (objects, groups, call signaling) as shipped in `v0.2.15`. Mixed-version MVP `0.1.0` clients against this server are unsupported ([D-005](decisions.md#d-005-stage-2-parallel-line-escalated) A).
