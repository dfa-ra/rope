# Invite and deep link

## URL

```
rope://join?v=1&host=<hostname-or-ip>&port=8443&sid=<server_id>&fp=<tls-fingerprint-hex>&tok=<token>
```

Optional: `name` (UTF-8 display hint, URL-encoded).

QR codes encode this URL as text.

## Client checks before bootstrap

1. `v` must be `1` (or missing, treated as 1).
2. Connect with TLS pinning to `fp` (SHA-256 of the server certificate DER, lowercase hex).
3. `GET /v1/info` must return the same `server_id` (`sid`) and fingerprint.
4. On any mismatch: stop and show a fingerprint warning. Do not send the token.
5. `POST /v1/bootstrap` with `tok`. The server hashes the token and accepts it once before expiry.

Owner first-run uses the installer `setup_token` the same way, without a QR.
