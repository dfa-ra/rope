# Envelope format (protocol v1)

Binary, little-endian. The Go relay parses the header and verifies the sender signature. It does not decrypt `ciphertext`.

```
offset  size  field
0       4     magic = 0x52 0x4F 0x50 0x45 ("ROPE")
4       2     version = 1
6       1     type (1 = text, 2 = media, 3 = group_text, 4 = call, 5 = receipt)
7       1     reserved = 0
8       16    message_id (UUID)
24      8     timestamp_ms (unix epoch, milliseconds)
32      32    sender_id (Ed25519 public key)
64      32    recipient_id (Ed25519 public key)
96      24    nonce (XChaCha20-Poly1305)
120     4     ciphertext_len
124     N     ciphertext
124+N   64    Ed25519 signature over bytes [0, 124+N)
```

Maximum `ciphertext_len` accepted by the server: 65536.

## Device identifiers

`device_id` is the lowercase hex encoding of the 32-byte Ed25519 public key (64 hex chars). The X25519 public key is carried in the public-identity blob, not in the envelope header.

## Encryption

1. `shared = X25519(sender_static_sk, recipient_static_pk)`
2. `key = HKDF-SHA256(ikm=shared, salt=message_id, info="rope-e2ee-v1")` → 32 bytes
3. AEAD: XChaCha20-Poly1305
4. AAD: the envelope header through `recipient_id` inclusive (bytes `[0, 96)`)

Inner plaintext for type=1 (`text`):

```
1 byte  payload_version = 1
4 bytes text_utf8_len (little-endian u32)
N bytes UTF-8 text
```

That UTF-8 text may be a JSON object for replies (`t`,`r`,`rp`,`rn`) or attributed forwards (`t`,`ff`). Forwards must not masquerade as replies.

Unknown `type` values are rejected by the Rust core (`known_envelope_type`). The relay still treats the blob as opaque.

### type=2 media

UTF-8 JSON inside the AEAD (not visible to the server):

```json
{
  "kind": "voice|image|file|video",
  "object_id": "uuid from POST /v1/objects",
  "sha256": "hex of ciphertext",
  "key_b64": "32-byte object key",
  "mime": "audio/mp4",
  "name": "voice.m4a",
  "size": 12345,
  "duration_ms": 3200,
  "group_id": null,
  "album_id": null,
  "album_index": 0,
  "album_count": 1
}
```

Optional `album_id` / `album_index` / `album_count` group 2–10 photos into one album. Each photo is still its own type=2 envelope and object blob; the relay does not see album linkage. A single photo omits these keys and renders as a normal image.

`kind=video` is the same type=2 object JSON (duration in `duration_ms`). The client compresses the clip to the existing 25 MiB object cap before `encryptObject`. Videos are not album members in this slice. No new envelope type.

Optional `ff` is the attributed-forward origin display name («Переслано от …»). It is not a reply quote. Forwards must not set reply fields (`r` / `rp` / `rn`). Local Saved Messages / Избранное (`peer_id=saved:`) stays in the client LocalStore and never becomes a mailbox envelope or object on the VPS.

The object store holds only ciphertext. The object key never appears in HTTP headers.

### type=3 group_text

```json
{ "g": "group_id", "t": "text", "e": 2, "ff": "Анна" }
```

Optional `ff` is the attributed-forward origin. Reply fields `r` / `rp` / `rn` stay for real replies only.

### Encrypted objects (`ROCH`)

```
4 magic "ROCH"
2 version = 1
4 chunk_size
4 chunk_count
8 plaintext_len
then per chunk: 24 nonce + 4 ct_len + ciphertext
```

Chunk key = HKDF-SHA256(ikm=object_key, salt=chunk_index, info="rope-obj-v1"). Max plaintext 25 MiB.

## Identity blobs

Private identity (`ROPI`):

```
4 magic "ROPI"
2 version = 1
32 Ed25519 seed
32 X25519 scalar
```

Public identity (`ROPP`):

```
4 magic "ROPP"
2 version = 1
32 Ed25519 public key
32 X25519 public key
```

REST carries public identity as standard base64.
