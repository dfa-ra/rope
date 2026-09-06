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

```json
{ "ok": true }
```

### `GET /version`

```json
{ "server": "0.1.0", "protocol": 1 }
```

### `GET /v1/info`

```json
{
  "server_id": "hex",
  "protocol_version": 1,
  "fingerprint": "hex sha256 of TLS cert DER"
}
```

On `--allow-http` debug servers `fingerprint` is the SHA-256 of the ASCII string `rope-http-dev`.

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
  "version": "0.1.0",
  "protocol_version": 1,
  "member_count": 2,
  "mailbox_count": 0
}
```

### `POST /v1/admin/revoke-member` (owner)

```json
{ "member_id": "uuid" }
```

### `POST /v1/admin/revoke-device` (owner)

```json
{ "device_id": "hex" }
```
