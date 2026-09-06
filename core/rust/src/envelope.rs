use crate::error::RopeError;
use crate::identity::{decode_public, DeviceIdentity, PublicIdentity};
use crate::util::{read_fixed, require_magic, sha256};
use chacha20poly1305::aead::{Aead, KeyInit, Payload};
use chacha20poly1305::{XChaCha20Poly1305, XNonce};
use hkdf::Hkdf;
use rand::rngs::OsRng;
use rand::RngCore;
use sha2::Sha256;
use uuid::Uuid;

pub const ENVELOPE_MAGIC: &[u8; 4] = b"ROPE";
pub const PROTOCOL_VERSION: u16 = 1;
pub const ENVELOPE_TYPE_TEXT: u8 = 1;
pub const HEADER_AAD_LEN: usize = 96;
pub const MAX_CIPHERTEXT: usize = 65536;

#[derive(uniffi::Record, Clone, Debug)]
pub struct EncryptedEnvelope {
    pub message_id: String,
    pub timestamp_ms: u64,
    pub sender_id: String,
    pub recipient_id: String,
    pub bytes: Vec<u8>,
}

#[derive(uniffi::Record, Clone, Debug)]
pub struct EnvelopeMeta {
    pub version: u16,
    pub msg_type: u8,
    pub message_id: String,
    pub timestamp_ms: u64,
    pub sender_id: String,
    pub recipient_id: String,
}

#[derive(uniffi::Record, Clone, Debug, PartialEq, Eq)]
pub struct PlainMessage {
    pub message_id: String,
    pub sender_id: String,
    pub text: String,
    pub timestamp_ms: u64,
}

pub fn encrypt_message(
    sender: &DeviceIdentity,
    recipient: &PublicIdentity,
    plaintext: &str,
) -> Result<EncryptedEnvelope, RopeError> {
    let (_, recipient_x) = decode_public(&recipient.blob)?;
    let message_id = Uuid::new_v4();
    let timestamp_ms = unix_ms();
    let sender_pk = *sender.verifying_key().as_bytes();
    let (recip_sign, _) = decode_public(&recipient.blob)?;
    let recipient_pk = *recip_sign.as_bytes();

    let mut header = Vec::with_capacity(124);
    header.extend_from_slice(ENVELOPE_MAGIC);
    header.extend_from_slice(&PROTOCOL_VERSION.to_le_bytes());
    header.push(ENVELOPE_TYPE_TEXT);
    header.push(0);
    header.extend_from_slice(message_id.as_bytes());
    header.extend_from_slice(&timestamp_ms.to_le_bytes());
    header.extend_from_slice(&sender_pk);
    header.extend_from_slice(&recipient_pk);

    let inner = encode_text_payload(plaintext);
    let key = derive_key(sender.ecdh_secret().diffie_hellman(&recipient_x).as_bytes(), message_id.as_bytes())?;
    let cipher = XChaCha20Poly1305::new((&key).into());
    let mut nonce_bytes = [0u8; 24];
    OsRng.fill_bytes(&mut nonce_bytes);
    let nonce = XNonce::from_slice(&nonce_bytes);
    let aad = &header[..HEADER_AAD_LEN];
    let ciphertext = cipher
        .encrypt(
            nonce,
            Payload {
                msg: &inner,
                aad,
            },
        )
        .map_err(|_| RopeError::crypto("encrypt"))?;
    if ciphertext.len() > MAX_CIPHERTEXT {
        return Err(RopeError::crypto("plaintext too large"));
    }

    header.extend_from_slice(&nonce_bytes);
    header.extend_from_slice(&(ciphertext.len() as u32).to_le_bytes());
    header.extend_from_slice(&ciphertext);
    let signature = sender.sign(&header);
    header.extend_from_slice(&signature);

    Ok(EncryptedEnvelope {
        message_id: message_id.as_hyphenated().to_string(),
        timestamp_ms,
        sender_id: hex::encode(sender_pk),
        recipient_id: hex::encode(recipient_pk),
        bytes: header,
    })
}

pub fn decrypt_with_peer(
    recipient: &DeviceIdentity,
    sender: &PublicIdentity,
    envelope: &[u8],
) -> Result<PlainMessage, RopeError> {
    let parsed = parse_envelope(envelope)?;
    if parsed.meta.version != PROTOCOL_VERSION {
        return Err(RopeError::UnsupportedVersion(parsed.meta.version));
    }
    if parsed.meta.recipient_id != recipient.device_id() {
        return Err(RopeError::InvalidEnvelope);
    }
    if parsed.meta.sender_id != sender.device_id {
        return Err(RopeError::InvalidEnvelope);
    }
    let (sender_sign, sender_x) = decode_public(&sender.blob)?;
    if !crate::identity::verify_signature(sender_sign.as_bytes(), parsed.signed_body, &parsed.signature)? {
        return Err(RopeError::InvalidEnvelope);
    }
    let msg_id = Uuid::parse_str(&parsed.meta.message_id).map_err(|_| RopeError::InvalidEnvelope)?;
    let key = derive_key(recipient.ecdh_secret().diffie_hellman(&sender_x).as_bytes(), msg_id.as_bytes())?;
    let cipher = XChaCha20Poly1305::new((&key).into());
    let nonce = XNonce::from_slice(&parsed.nonce);
    let aad = &parsed.signed_body[..HEADER_AAD_LEN];
    let inner = cipher
        .decrypt(
            nonce,
            Payload {
                msg: parsed.ciphertext,
                aad,
            },
        )
        .map_err(|_| RopeError::crypto("decrypt"))?;
    Ok(PlainMessage {
        message_id: parsed.meta.message_id,
        sender_id: parsed.meta.sender_id,
        text: decode_text_payload(&inner)?,
        timestamp_ms: parsed.meta.timestamp_ms,
    })
}

pub struct ParsedEnvelope<'a> {
    pub meta: EnvelopeMeta,
    pub nonce: [u8; 24],
    pub ciphertext: &'a [u8],
    pub signed_body: &'a [u8],
    pub signature: [u8; 64],
}

pub fn parse_envelope(bytes: &[u8]) -> Result<ParsedEnvelope<'_>, RopeError> {
    if bytes.len() < 124 + 64 {
        return Err(RopeError::InvalidEnvelope);
    }
    let mut rest = bytes;
    require_magic(&mut rest, ENVELOPE_MAGIC, RopeError::InvalidEnvelope)?;
    let version = u16::from_le_bytes(read_fixed::<2>(&mut rest)?);
    let msg_type = rest[0];
    let _reserved = rest[1];
    rest = &rest[2..];
    let id = read_fixed::<16>(&mut rest)?;
    let timestamp_ms = u64::from_le_bytes(read_fixed::<8>(&mut rest)?);
    let sender = read_fixed::<32>(&mut rest)?;
    let recipient = read_fixed::<32>(&mut rest)?;
    let nonce = read_fixed::<24>(&mut rest)?;
    let ct_len = u32::from_le_bytes(read_fixed::<4>(&mut rest)?) as usize;
    if ct_len > MAX_CIPHERTEXT || rest.len() != ct_len + 64 {
        return Err(RopeError::InvalidEnvelope);
    }
    let ciphertext = &rest[..ct_len];
    let signature = rest[ct_len..].try_into().unwrap();
    let signed_body = &bytes[..bytes.len() - 64];
    let message_id = Uuid::from_bytes(id).as_hyphenated().to_string();
    Ok(ParsedEnvelope {
        meta: EnvelopeMeta {
            version,
            msg_type,
            message_id,
            timestamp_ms,
            sender_id: hex::encode(sender),
            recipient_id: hex::encode(recipient),
        },
        nonce,
        ciphertext,
        signed_body,
        signature,
    })
}

pub fn parse_envelope_meta(bytes: &[u8]) -> Result<EnvelopeMeta, RopeError> {
    Ok(parse_envelope(bytes)?.meta)
}

fn derive_key(shared: &[u8], salt: &[u8]) -> Result<[u8; 32], RopeError> {
    let hk = Hkdf::<Sha256>::new(Some(salt), shared);
    let mut key = [0u8; 32];
    hk.expand(b"rope-e2ee-v1", &mut key)
        .map_err(|_| RopeError::crypto("hkdf"))?;
    Ok(key)
}

fn encode_text_payload(text: &str) -> Vec<u8> {
    let raw = text.as_bytes();
    let mut out = Vec::with_capacity(5 + raw.len());
    out.push(1);
    out.extend_from_slice(&(raw.len() as u32).to_le_bytes());
    out.extend_from_slice(raw);
    out
}

fn decode_text_payload(bytes: &[u8]) -> Result<String, RopeError> {
    if bytes.len() < 5 || bytes[0] != 1 {
        return Err(RopeError::InvalidEnvelope);
    }
    let len = u32::from_le_bytes(bytes[1..5].try_into().unwrap()) as usize;
    if bytes.len() != 5 + len {
        return Err(RopeError::InvalidEnvelope);
    }
    String::from_utf8(bytes[5..].to_vec()).map_err(|_| RopeError::InvalidEnvelope)
}

fn unix_ms() -> u64 {
    use std::time::{SystemTime, UNIX_EPOCH};
    SystemTime::now()
        .duration_since(UNIX_EPOCH)
        .map(|d| d.as_millis() as u64)
        .unwrap_or(0)
}

pub fn cert_fingerprint_hex(der: &[u8]) -> String {
    hex::encode(sha256(der))
}

pub fn http_dev_fingerprint() -> String {
    hex::encode(sha256(b"rope-http-dev"))
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn encrypt_decrypt_roundtrip() {
        let alice = DeviceIdentity::generate();
        let bob = DeviceIdentity::generate();
        let env = encrypt_message(&alice, &bob.public_identity(), "hello rope").unwrap();
        let meta = parse_envelope_meta(&env.bytes).unwrap();
        assert_eq!(meta.version, 1);
        assert_eq!(meta.sender_id, alice.device_id());
        assert_eq!(meta.recipient_id, bob.device_id());
        let plain = decrypt_with_peer(&bob, &alice.public_identity(), &env.bytes).unwrap();
        assert_eq!(plain.text, "hello rope");
        assert_eq!(plain.sender_id, alice.device_id());
    }

    #[test]
    fn rejects_tampered_ciphertext() {
        let alice = DeviceIdentity::generate();
        let bob = DeviceIdentity::generate();
        let mut env = encrypt_message(&alice, &bob.public_identity(), "secret").unwrap();
        let last = env.bytes.len() - 70;
        env.bytes[last] ^= 0x01;
        assert!(decrypt_with_peer(&bob, &alice.public_identity(), &env.bytes).is_err());
    }

    #[test]
    #[test]
    fn http_dev_fingerprint_is_stable() {
        assert_eq!(
            http_dev_fingerprint(),
            "27da7f0887d3fbec0efcea4e0cf47470b0045f4150d5957b8c6bf2d2eb233c2a"
        );
    }

    #[test]
    fn rejects_wrong_version_prefix() {
        let alice = DeviceIdentity::generate();
        let bob = DeviceIdentity::generate();
        let mut env = encrypt_message(&alice, &bob.public_identity(), "x").unwrap();
        env.bytes[4] = 99;
        env.bytes[5] = 0;
        let meta = parse_envelope_meta(&env.bytes).unwrap();
        assert_eq!(meta.version, 99);
        let err = decrypt_with_peer(&bob, &alice.public_identity(), &env.bytes).unwrap_err();
        assert!(matches!(err, RopeError::UnsupportedVersion(99)));
    }
}
