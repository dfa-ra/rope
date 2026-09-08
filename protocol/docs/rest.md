# REST API (v1)

All application endpoints except `/health`, `/version`, `/v1/info`, and `/v1/bootstrap` require device authentication.

## Auth header

```
Authorization: Rope <device_id>.<unix_seconds>.<base64url(signature)>
```

Signature is Ed25519 over the UTF-8 string:

```
rope-auth-v1\n<METHOD>\n<PATH>\n<unix_seconds>\n<hex(sha256(body))>
```

`PATH` is the path only (no query string). Timestamps older than 300 seconds are rejected.

## Endpoints

### `GET /health`

`ok` is the Go process. `turn_running` is a live TCP probe of coturn on `turn_port` (listening), not “`turn_secret` exists”. `turn_allocate_ok` is a local TURN Allocate (HMAC + XOR-RELAYED-ADDRESS). Listening without a successful Allocate still yields `turn_running: true` and a `turn_error` (typical: wrong `relay-ip` / `external-ip`, HMAC mismatch). `/health` `ok` stays true so the installer can finish.

```json
{
  "ok": true,
  "turn_running": true,
  "turn_allocate_ok": true,
  "turn_relayed_ip": "203.0.113.9",
  "turns_listening": true,
  "turn_port": 3478,
  "turns_port": 443
}
```

Without `public_host` + `turn_secret` the body is `{ "ok": true, "turn_running": false, "turn_allocate_ok": false }`. A down coturn does **not** make `/health` fail — installer still greps `"ok"`.

### `GET /version`

```json
{ "server": "0.3.3", "protocol": 1 }
```

### `GET /v1/info`

```json
{
  "server_id": "hex",
  "protocol_version": 1,
  "fingerprint": "hex sha256 of TLS cert DER",
  "public_ip": "203.0.113.9",
  "ice_servers": [
    { "urls": ["stun:vps.example:3478"], "hostname": "vps.example" },
    {
      "urls": [
        "turns:vps.example:443?transport=tcp",
        "turns:vps.example:443",
        "turn:vps.example:3478?transport=tcp",
        "turn:vps.example:3478?transport=udp",
        "turn:vps.example:3478"
      ],
      "username": "<unix_expiry>:rope",
      "credential": "base64(HMAC-SHA1(turn_secret, username))",
      "hostname": "vps.example"
    }
  ],
  "ice_ttl_seconds": 604800
}
```

On `--allow-http` debug servers `fingerprint` is the SHA-256 of the ASCII string `rope-http-dev`.

`ice_servers` is present when the VPS has coturn (`public_host` + `turn_secret` in `/etc/rope/config.json`). Guests and the owner both read this unauthenticated endpoint. Credentials are time-limited (coturn REST / HMAC-SHA1, username `<unix_expiry>:rope`, default TTL 7 days); the long-term secret never leaves the VPS. If 443 is already taken or did not bind, installer uses TURNS on 5349 (or omits `turns:`). Without TURN (local `--allow-http`) the field is omitted.

`public_ip` is the VPS IPv4 (from `public_ip` in config, a raw `public_host`, or installer `external_ip`) so the client can duplicate `turn`/`turns` URLs when DNS for `public_host` fails. `ice_servers[].hostname` is the DNS/SNI name for self-signed TURNS when the URL host is a raw IP (`public_host` DNS name, or `tls_hostname` if URLs stay on an IP). Omitted when there is no DNS name.

### `POST /v1/bootstrap`

```json
{
    "token": "setup_token or invite token",
    "display_name": "required unique login, 2-24 letters/digits/_ . -",
  "public_identity": "base64 ROPP blob",
  "device_id": "hex ed25519 pk"
}
```

Response:

```json
{
  "member_id": "uuid",
  "device_id": "hex",
  "role": "owner" | "member",
  "server_id": "hex"
}
```

### `POST /v1/invites` (owner)

```json
{ "ttl_seconds": 3600 }
```

```json
{ "invite_id": "uuid", "token": "opaque", "expires_at": "RFC3339" }
```

The database stores only `sha256(token)`.

### `GET /v1/directory` (any member)

Public identities of non-revoked devices so clients can encrypt.

```json
{
  "members": [{ "member_id": "...", "display_name": "", "role": "owner", "revoked": false }],
  "devices": [{
    "device_id": "hex",
    "member_id": "uuid",
    "public_identity": "base64",
    "last_seen": "RFC3339",
    "online": true
  }]
}
```

### `GET /v1/admin/status` (owner)

```json
{
  "server_id": "hex",
  "version": "0.3.3",
  "protocol_version": 1,
  "member_count": 2,
  "device_count": 2,
  "mailbox_count": 0,
  "object_count": 0,
  "object_bytes": 0,
  "group_count": 0,
  "listen": "0.0.0.0:8443",
  "max_object_bytes": 26214400,
  "online_devices": 1,
  "public_host": "203.0.113.9",
  "turn_port": 3478,
  "turns_port": 5349,
  "ice_enabled": true,
  "turn_running": true,
  "turn_allocate_ok": true,
  "turns_listening": true,
  "turn_listen": "0.0.0.0",
  "turn_external_ip": "203.0.113.9",
  "turn_relayed_ip": "203.0.113.9",
  "turn_error": "",
  "ice_urls": ["turns:203.0.113.9:5349?transport=tcp", "turn:203.0.113.9:3478?transport=tcp"]
}
```

### `POST /v1/objects`

Auth signs the **raw ciphertext body**. Optional `X-Rope-SHA256` must match SHA-256 of that body. Limits: 25 MiB object, 512 MiB server quota. Response:

```json
{ "object_id": "uuid", "sha256": "hex", "size": 123, "expires_at": "RFC3339" }
```

### `GET /v1/objects/{id}`

Returns `application/octet-stream` plus `X-Rope-SHA256`. Any authenticated member who knows the id can download (capability is the E2EE media payload).

### `POST /v1/groups`

```json
{ "name": "crew" }
```

```json
{ "group_id": "uuid", "name": "crew", "epoch": 1, "members": ["device_hex"] }
```

### `GET /v1/groups`

```json
{ "groups": [{ "group_id": "...", "name": "...", "epoch": 2, "members": [] }] }
```

### `POST /v1/groups/{id}/members`

```json
{ "device_id": "hex" }
```

Bumps epoch. Caller must already be a member.

### `POST /v1/groups/{id}/remove`

```json
{ "device_id": "hex" }
```

Soft-removes the member and bumps epoch. Removed devices no longer see the group.

### `POST /v1/admin/revoke-member` (owner)

```json
{ "member_id": "uuid" }
```

### `POST /v1/admin/revoke-device` (owner)

```json
{ "device_id": "hex" }
```
