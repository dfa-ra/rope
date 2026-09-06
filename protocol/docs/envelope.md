# Envelope format (protocol v1)

Binary, little-endian. The Go relay parses the header and verifies the sender signature. It does not decrypt `ciphertext`.

```
offset  size  field
0       4     magic = 0x52 0x4F 0x50 0x45 ("ROPE")
4       2     version = 1
6       1     type (1 = text)
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

Inner plaintext for type=1:

```
1 byte  payload_version = 1
4 bytes text_utf8_len (little-endian u32)
N bytes UTF-8 text
```

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
