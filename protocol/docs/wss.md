# WebSocket protocol (v1)

URL: `wss://<host>:<port>/v1/ws?device_id=<hex>&ts=<unix>&sig=<base64url>`

`sig` is Ed25519 over `rope-ws-v1\n<unix_seconds>`.

On connect the server:

1. Authenticates the device
2. Marks it online and broadcasts `{ "type": "presence", "devices": [...] }` to every live socket
3. Pushes every mailbox blob for that device as `deliver` frames
4. Sends `{ "type": "mailbox_done" }`
5. Flushes short-TTL in-memory pending `type=call` frames (RING/control only; never audio)
6. On disconnect, drops the device from the hub and broadcasts an updated `presence` list

## Client → server

```json
{ "type": "send", "envelope": "<standard base64>" }
{ "type": "ack", "message_id": "<uuid hex>" }
{ "type": "group_send", "group_id": "<uuid>", "envelopes": ["<base64>", "..."] }
{ "type": "call", "call_id": "<uuid>", "to": "<device_hex>", "event": "ring|accept|reject|hangup|offer|answer|ice|relay|audio", "payload": "" }
```

`group_send`: sender must be a current member; every envelope recipient must be a current member. Each envelope then follows the normal mailbox path.

`call` is live when `to` is on the hub. If `to` is offline, `audio` still returns `not_found` (realtime frames are not queued). Other events (`ring`, `accept`, `offer`, `answer`, `ice`, `relay`, `hangup`, `reject`, …) sit in a short-TTL (~60s) **in-memory** pending slot keyed by `call_id`; the sender gets `{ "type": "queued", "call_id": "..." }` instead of `not_found`. `hangup`/`reject` **replace** that slot (so a RING that never delivered is cancelled, and a callee who already rang still gets a clean end on reconnect). Nothing is written to SQLite — never `PutMailbox` for calls, no SDP/audio on disk. WSS `payload` for RING/control/audio is opaque to the relay. **Android applies offer/answer/ICE only from sealed call envelopes** so the VPS cannot swap DTLS fingerprints. Android also applies offer/answer/ICE only from the live peer (`CallLink.matchesCall`); a matching `call_id` from a third device is ignored. SDP/ICE with CRLF injection or `file:` / `javascript:` / `data:` schemes are dropped before `setRemoteDescription`. Clients still refuse SDP JSON larger than 16384 bytes (`VideoCallRules.fitsWss`) — refuse, do not truncate. Do **not** raise the Go cap unless a measured video SDP exceeds 16384. `ring` payload may include tiny `{"v":1,"video":true}` so the callee can show a video ring before SDP.

Connect URL still uses `?device_id=&ts=&sig=` (Ed25519 over `rope-ws-v1\n<unix>`). Moving `sig` to a header is a paired client+server cut and is not done in this slice.

Decoded `payload` is capped at 16384 bytes (`too_large` if larger). Empty payload is allowed for control events (`ring`, `accept`, `reject`, `hangup`, `relay`, …). `audio` is rate-limited per sender at ~40 frames/sec (`rate_limited`, key `ws-call-audio:<device_id>`). Signaling events (`ring`, `accept`, `reject`, `hangup`, `offer`, `answer`, `ice`, `relay`) are not throttled at that cap.

Call events:

- `ring|accept|reject|hangup` — plaintext WSS control (no SDP)
- `offer|answer|ice` — WebRTC SDP/ICE; Android sends and applies these in sealed envelopes, not as trusted WSS payloads
- `relay` — switch this live call to WSS media (ICE/WebRTC failed). Control event; payload may be empty
- `audio` — base64 UniFFI ciphertext of a PCM frame (typically < 8KiB). Live WSS fallback only; do not send on mailbox `send`

ICE servers (STUN/TURN on the same host) are advertised on authenticated REST `GET /v1/info`, not as a new WSS type.

## Server → client

```json
{ "type": "queued", "message_id": "<uuid hex>" }
{ "type": "queued", "call_id": "<uuid>" }
{ "type": "deliver", "envelope": "<standard base64>" }
{ "type": "delivered", "message_id": "<uuid hex>" }
{ "type": "mailbox_done" }
{ "type": "presence", "devices": ["hex", "..."] }
{ "type": "error", "code": "protocol|auth|not_found|too_large|rate_limited", "message": "..." }
{ "type": "call", "call_id": "...", "from": "<device_hex>", "event": "ring|relay|audio", "payload": "" }
```

## Delivery rules

1. Parse and verify the envelope (version, size, registered sender/recipient, signature).
2. Insert into `mailbox` (`delivery_state=pending`) with TTL.
3. Reply `queued` to the sender (`sent_to_server`).
4. If the recipient has a live socket, push `deliver`.
5. Recipient sends `ack` after durable local store.
6. Server deletes the mailbox row and notifies the sender with `delivered` (`delivered_to_device`).

If the recipient is offline, the blob stays until ack or TTL. Expired rows are deleted; the message is undelivered.
