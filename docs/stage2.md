# Stage 2 — media, calls, groups

Server stays a transport: encrypted envelopes, encrypted object blobs, group membership + epoch, live call signaling. It never sees plaintext, object keys, or call media.

## Order shipped

1. Protocol/core: envelope types 1–5, `encrypt_typed` / `decrypt_typed`, chunked object crypto (`ROCH`)
2. Encrypted object store: `POST/GET /v1/objects`, 25 MB, 7-day TTL, 512 MB quota
3. Photos, files, voice notes on Android (hold-to-record)
4. Local notifications + WSS reconnect (no FCM — the VPS is private)
5. 1-to-1 audio: WebRTC (DTLS-SRTP) over existing `type=call` WSS. Events `offer`/`answer`/`ice` carry SDP. The same VPS runs coturn; `GET /v1/info` advertises `ice_servers` (STUN + TURN/TURNS on that host). Media stays DTLS-SRTP — TURN only relays ciphertext.
6. Groups: REST membership, pairwise `group_send`, epoch bump on add/remove
7. Group attachments use the same media payload with `group_id`
8. Admin health cards (objects, groups, online, quota)
9. Metrics: [stage2-metrics.md](stage2-metrics.md)

## What the server stores

| Table | Contents |
| --- | --- |
| `objects` | object_id, uploader, size, sha256 of ciphertext, expiry |
| `chat_groups` | name, created_by, epoch |
| `group_members` | device_id, added_at, removed_at |

Object bytes live under `$data_dir/objects/`. Ciphertext only.

## Client crypto

Kotlin never derives keys. UniFFI:

- `encryptObject` / `decryptObject` — object key stays inside the E2EE media payload (`key_b64`)
- `encryptTyped(type=2|3)` — media metadata and group text
- `encryptMessage` — unchanged v1 DM text

## Groups

Not MLS. Each group message is a fan-out of pairwise envelopes. The server checks membership, then runs the normal mailbox path. A membership change increments `epoch` so clients can rotate later.

## Calls

WSS events: `ring`, `accept`, `reject`, `hangup`, plus WebRTC `offer`, `answer`, `ice`. Offline callee → `not_found` (no mailbox for rings — a missed call must not sit on disk). Incoming UI is full-screen. Media is DTLS-SRTP. When UDP hole punching fails (typical in Russia), ICE uses TURN on the same VPS — usually TURNS/TLS on 443 (`turns:HOST:443?transport=tcp`) plus TURN on 3478. The relay forwards encrypted media only; it does not decrypt voice. The call overlay shows `WebRTC · через сервер` when the selected pair is a relay, otherwise `WebRTC · DTLS-SRTP`.

`install.sh` and in-app «Обновить ядро» install/enable coturn next to `rope-server`. HMAC `turn_secret` is generated per VPS and stored in `/etc/rope/config.json` (mode 640). Clients cache `ice_servers` on the profile after `/v1/info`. Public Google/Cloudflare STUN is a last-resort fallback only when the VPS did not advertise ICE.
