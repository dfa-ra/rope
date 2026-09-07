# WebSocket protocol (v1)

URL: `wss://<host>:<port>/v1/ws?device_id=<hex>&ts=<unix>&sig=<base64url>`

`sig` is Ed25519 over `rope-ws-v1\n<unix_seconds>`.

On connect the server:

1. Authenticates the device
2. Marks it online and broadcasts `{ "type": "presence", "devices": [...] }` to every live socket
3. Pushes every mailbox blob for that device as `deliver` frames
4. Sends `{ "type": "mailbox_done" }`
5. On disconnect, drops the device from the hub and broadcasts an updated `presence` list

## Client → server

```json
{ "type": "send", "envelope": "<standard base64>" }
{ "type": "ack", "message_id": "<uuid hex>" }
{ "type": "group_send", "group_id": "<uuid>", "envelopes": ["<base64>", "..."] }
{ "type": "call", "call_id": "<uuid>", "to": "<device_hex>", "event": "ring|accept|reject|hangup|offer|answer|ice", "payload": "" }
```

`group_send`: sender must be a current member; every envelope recipient must be a current member. Each envelope then follows the normal mailbox path.

`call` is live-only. If `to` is offline the sender gets `not_found`. The server does not store SDP.

## Server → client

```json
{ "type": "queued", "message_id": "<uuid hex>" }
{ "type": "deliver", "envelope": "<standard base64>" }
{ "type": "delivered", "message_id": "<uuid hex>" }
{ "type": "mailbox_done" }
{ "type": "presence", "devices": ["hex", "..."] }
{ "type": "error", "code": "protocol|auth|not_found|too_large|rate_limited", "message": "..." }
{ "type": "call", "call_id": "...", "from": "<device_hex>", "event": "ring", "payload": "" }
```

## Delivery rules

1. Parse and verify the envelope (version, size, registered sender/recipient, signature).
2. Insert into `mailbox` (`delivery_state=pending`) with TTL.
3. Reply `queued` to the sender (`sent_to_server`).
4. If the recipient has a live socket, push `deliver`.
5. Recipient sends `ack` after durable local store.
6. Server deletes the mailbox row and notifies the sender with `delivered` (`delivered_to_device`).

If the recipient is offline, the blob stays until ack or TTL. Expired rows are deleted; the message is undelivered.
