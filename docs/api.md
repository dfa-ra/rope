# API source of truth

This file is a **pointer**, not a second API.

The wire contract for the **current trunk** (`main` MVP, protocol v1) is:

| Topic | File |
| --- | --- |
| REST | [protocol/docs/rest.md](../protocol/docs/rest.md) |
| WebSocket | [protocol/docs/wss.md](../protocol/docs/wss.md) |
| Envelope | [protocol/docs/envelope.md](../protocol/docs/envelope.md) |
| Invite / deep link | [protocol/docs/invite.md](../protocol/docs/invite.md) |

Do not duplicate those specs here. If a prompt, agent, or Stage-2 branch disagrees with the files above, the current-trunk `protocol/docs` win.

Stage-2 protocol (expanded envelope types, objects, groups, call signaling) exists on other branches and tags (`v0.2.x`, PRs #2 / #3). That protocol is **not** source of truth until the user accepts [D-005](decisions.md#d-005-stage-2-parallel-line-escalated). Mixed-version clients and servers are unsupported until a chosen trunk is documented.
