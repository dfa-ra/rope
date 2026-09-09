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
pub const ENVELOPE_TYPE_MEDIA: u8 = 2;
pub const ENVELOPE_TYPE_GROUP_TEXT: u8 = 3;
pub const ENVELOPE_TYPE_CALL: u8 = 4;
pub const ENVELOPE_TYPE_RECEIPT: u8 = 5;
pub const HEADER_AAD_LEN: usize = 96;
pub const MAX_CIPHERTEXT: usize = 65536;
/// Reject envelopes whose `timestamp_ms` is more than this far ahead of local time.
/// Old timestamps are accepted so offline delivery still works.
pub const MAX_FUTURE_SKEW_MS: u64 = 7 * 24 * 60 * 60 * 1000;

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

#[derive(uniffi::Record, Clone, Debug, PartialEq, Eq)]
pub struct DecryptedPayload {
    pub message_id: String,
    pub sender_id: String,
    pub timestamp_ms: u64,
    pub msg_type: u8,
    pub body: Vec<u8>,
}

pub fn known_envelope_type(msg_type: u8) -> bool {
    matches!(
        msg_type,
        ENVELOPE_TYPE_TEXT
            | ENVELOPE_TYPE_MEDIA
            | ENVELOPE_TYPE_GROUP_TEXT
            | ENVELOPE_TYPE_CALL
            | ENVELOPE_TYPE_RECEIPT
    )
}

pub fn encrypt_message(
    sender: &DeviceIdentity,
    recipient: &PublicIdentity,
    plaintext: &str,
) -> Result<EncryptedEnvelope, RopeError> {
    encrypt_typed(sender, recipient, ENVELOPE_TYPE_TEXT, &encode_text_payload(plaintext))
}

pub fn encrypt_typed(
    sender: &DeviceIdentity,
    recipient: &PublicIdentity,
    msg_type: u8,
    inner: &[u8],
) -> Result<EncryptedEnvelope, RopeError> {
    if !known_envelope_type(msg_type) {
        return Err(RopeError::InvalidEnvelope);
    }
    let (_, recipient_x) = decode_public(&recipient.blob)?;
    let message_id = Uuid::new_v4();
    let timestamp_ms = unix_ms();
    let sender_pk = *sender.verifying_key().as_bytes();
    let (recip_sign, _) = decode_public(&recipient.blob)?;
    let recipient_pk = *recip_sign.as_bytes();

    let mut header = Vec::with_capacity(124);
    header.extend_from_slice(ENVELOPE_MAGIC);
    header.extend_from_slice(&PROTOCOL_VERSION.to_le_bytes());
    header.push(msg_type);
    header.push(0);
    header.extend_from_slice(message_id.as_bytes());
    header.extend_from_slice(&timestamp_ms.to_le_bytes());
    header.extend_from_slice(&sender_pk);
    header.extend_from_slice(&recipient_pk);

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
    if parsed.meta.msg_type != ENVELOPE_TYPE_TEXT && parsed.meta.msg_type != ENVELOPE_TYPE_GROUP_TEXT {
        return Err(RopeError::InvalidEnvelope);
    }
    Ok(PlainMessage {
        message_id: parsed.meta.message_id,
        sender_id: parsed.meta.sender_id,
        text: decode_text_payload(&inner)?,
        timestamp_ms: parsed.meta.timestamp_ms,
    })
}

pub fn decrypt_typed(
    recipient: &DeviceIdentity,
    sender: &PublicIdentity,
    envelope: &[u8],
) -> Result<DecryptedPayload, RopeError> {
    let parsed = parse_envelope(envelope)?;
    if parsed.meta.version != PROTOCOL_VERSION {
        return Err(RopeError::UnsupportedVersion(parsed.meta.version));
    }
    if !known_envelope_type(parsed.meta.msg_type) {
        return Err(RopeError::InvalidEnvelope);
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
    if parsed.meta.msg_type == ENVELOPE_TYPE_CALL && inner.is_empty() {
        return Err(RopeError::InvalidEnvelope);
    }
    Ok(DecryptedPayload {
        message_id: parsed.meta.message_id,
        sender_id: parsed.meta.sender_id,
        timestamp_ms: parsed.meta.timestamp_ms,
        msg_type: parsed.meta.msg_type,
        body: inner,
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
    if !known_envelope_type(msg_type) {
        return Err(RopeError::InvalidEnvelope);
    }
    if timestamp_exceeds_future_skew(timestamp_ms, unix_ms()) {
        return Err(RopeError::InvalidEnvelope);
    }
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

fn timestamp_exceeds_future_skew(timestamp_ms: u64, now_ms: u64) -> bool {
    timestamp_ms > now_ms.saturating_add(MAX_FUTURE_SKEW_MS)
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

    #[test]
    fn typed_media_roundtrip_and_unknown_type() {
        let alice = DeviceIdentity::generate();
        let bob = DeviceIdentity::generate();
        let body = br#"{"kind":"voice","object_id":"x"}"#;
        let env = encrypt_typed(&alice, &bob.public_identity(), ENVELOPE_TYPE_MEDIA, body).unwrap();
        assert_eq!(parse_envelope_meta(&env.bytes).unwrap().msg_type, ENVELOPE_TYPE_MEDIA);
        let got = decrypt_typed(&bob, &alice.public_identity(), &env.bytes).unwrap();
        assert_eq!(got.msg_type, ENVELOPE_TYPE_MEDIA);
        assert_eq!(got.body, body);
        assert!(encrypt_typed(&alice, &bob.public_identity(), 99, b"x").is_err());
    }

    #[test]
    fn typed_media_album_json_roundtrip() {
        let alice = DeviceIdentity::generate();
        let bob = DeviceIdentity::generate();
        let album = br#"{"kind":"image","object_id":"o","sha256":"ab","key_b64":"k","mime":"image/jpeg","name":"a.jpg","size":1,"album_id":"alb-1","album_index":1,"album_count":3}"#;
        let env = encrypt_typed(&alice, &bob.public_identity(), ENVELOPE_TYPE_MEDIA, album).unwrap();
        let got = decrypt_typed(&bob, &alice.public_identity(), &env.bytes).unwrap();
        assert_eq!(got.msg_type, ENVELOPE_TYPE_MEDIA);
        assert_eq!(got.body, album);
        let json = std::str::from_utf8(&got.body).unwrap();
        assert!(json.contains("\"album_id\":\"alb-1\""));
        assert!(json.contains("\"album_index\":1"));
        assert!(json.contains("\"album_count\":3"));
        let singleton = br#"{"kind":"image","object_id":"o","sha256":"ab","key_b64":"k","mime":"image/jpeg","name":"a.jpg","size":1}"#;
        let env = encrypt_typed(&alice, &bob.public_identity(), ENVELOPE_TYPE_MEDIA, singleton).unwrap();
        let got = decrypt_typed(&bob, &alice.public_identity(), &env.bytes).unwrap();
        assert_eq!(got.body, singleton);
        let json = std::str::from_utf8(&got.body).unwrap();
        assert!(!json.contains("album_id"));
    }

    #[test]
    fn typed_media_video_json_roundtrip() {
        let alice = DeviceIdentity::generate();
        let bob = DeviceIdentity::generate();
        let video = br#"{"kind":"video","object_id":"o","sha256":"ab","key_b64":"k","mime":"video/mp4","name":"c.mp4","size":1,"duration_ms":3400}"#;
        let env = encrypt_typed(&alice, &bob.public_identity(), ENVELOPE_TYPE_MEDIA, video).unwrap();
        let got = decrypt_typed(&bob, &alice.public_identity(), &env.bytes).unwrap();
        assert_eq!(got.msg_type, ENVELOPE_TYPE_MEDIA);
        assert_eq!(got.body, video);
        let json = std::str::from_utf8(&got.body).unwrap();
        assert!(json.contains("\"kind\":\"video\""));
        assert!(json.contains("\"duration_ms\":3400"));
        assert!(!json.contains("album_id"));
    }

    #[test]
    fn typed_media_caption_and_video_album_json_roundtrip() {
        let alice = DeviceIdentity::generate();
        let bob = DeviceIdentity::generate();
        let captioned = br#"{"kind":"image","object_id":"o","sha256":"ab","key_b64":"k","mime":"image/jpeg","name":"a.jpg","size":1,"caption":"hi"}"#;
        let env = encrypt_typed(&alice, &bob.public_identity(), ENVELOPE_TYPE_MEDIA, captioned).unwrap();
        let got = decrypt_typed(&bob, &alice.public_identity(), &env.bytes).unwrap();
        assert_eq!(got.body, captioned);
        let json = std::str::from_utf8(&got.body).unwrap();
        assert!(json.contains("\"caption\":\"hi\""));
        let mixed = br#"{"kind":"video","object_id":"o","sha256":"ab","key_b64":"k","mime":"video/mp4","name":"c.mp4","size":1,"duration_ms":3400,"album_id":"alb-1","album_index":1,"album_count":2}"#;
        let env = encrypt_typed(&alice, &bob.public_identity(), ENVELOPE_TYPE_MEDIA, mixed).unwrap();
        let got = decrypt_typed(&bob, &alice.public_identity(), &env.bytes).unwrap();
        assert_eq!(got.body, mixed);
        let json = std::str::from_utf8(&got.body).unwrap();
        assert!(json.contains("\"kind\":\"video\""));
        assert!(json.contains("\"album_id\":\"alb-1\""));
        let empty = br#"{"kind":"image","object_id":"o","sha256":"ab","key_b64":"k","mime":"image/jpeg","name":"a.jpg","size":1}"#;
        let env = encrypt_typed(&alice, &bob.public_identity(), ENVELOPE_TYPE_MEDIA, empty).unwrap();
        let got = decrypt_typed(&bob, &alice.public_identity(), &env.bytes).unwrap();
        let json = std::str::from_utf8(&got.body).unwrap();
        assert!(!json.contains("caption"));
    }

    #[test]
    fn typed_text_forwarded_from_json_roundtrip() {
        let alice = DeviceIdentity::generate();
        let bob = DeviceIdentity::generate();
        let body = br#"{"t":"hi","ff":"Anna"}"#;
        let env = encrypt_typed(&alice, &bob.public_identity(), ENVELOPE_TYPE_TEXT, body).unwrap();
        let got = decrypt_typed(&bob, &alice.public_identity(), &env.bytes).unwrap();
        assert_eq!(got.msg_type, ENVELOPE_TYPE_TEXT);
        assert_eq!(got.body, body);
        let json = std::str::from_utf8(&got.body).unwrap();
        assert!(json.contains("\"ff\":\"Anna\""));
        assert!(!json.contains("\"rn\""));
        let media = br#"{"kind":"image","object_id":"o","sha256":"ab","key_b64":"k","mime":"image/jpeg","name":"a.jpg","size":1,"ff":"Anna"}"#;
        let env = encrypt_typed(&alice, &bob.public_identity(), ENVELOPE_TYPE_MEDIA, media).unwrap();
        let got = decrypt_typed(&bob, &alice.public_identity(), &env.bytes).unwrap();
        assert_eq!(got.body, media);
    }

    #[test]
    fn inner_json_ns_silent_flag_roundtrip() {
        let alice = DeviceIdentity::generate();
        let bob = DeviceIdentity::generate();
        let text = br#"{"t":"hi","ns":1}"#;
        let env = encrypt_typed(&alice, &bob.public_identity(), ENVELOPE_TYPE_TEXT, text).unwrap();
        let got = decrypt_typed(&bob, &alice.public_identity(), &env.bytes).unwrap();
        assert_eq!(got.msg_type, ENVELOPE_TYPE_TEXT);
        assert_eq!(got.body, text);
        let json = std::str::from_utf8(&got.body).unwrap();
        assert!(json.contains("\"ns\":1"));
        assert!(json.contains("\"t\":\"hi\""));
        let media = br#"{"kind":"image","object_id":"o","sha256":"ab","key_b64":"k","mime":"image/jpeg","name":"a.jpg","size":1,"ns":1}"#;
        let env = encrypt_typed(&alice, &bob.public_identity(), ENVELOPE_TYPE_MEDIA, media).unwrap();
        let got = decrypt_typed(&bob, &alice.public_identity(), &env.bytes).unwrap();
        assert_eq!(got.body, media);
        let group = br#"{"g":"gid","t":"hi","e":1,"ns":1}"#;
        let env = encrypt_typed(&alice, &bob.public_identity(), ENVELOPE_TYPE_GROUP_TEXT, group).unwrap();
        let got = decrypt_typed(&bob, &alice.public_identity(), &env.bytes).unwrap();
        assert_eq!(got.body, group);
        let meta = parse_envelope_meta(&env.bytes).unwrap();
        assert_eq!(meta.msg_type, ENVELOPE_TYPE_GROUP_TEXT);
    }

    #[test]
    fn v1_text_still_works_after_typed_api() {
        let alice = DeviceIdentity::generate();
        let bob = DeviceIdentity::generate();
        let env = encrypt_message(&alice, &bob.public_identity(), "compat").unwrap();
        assert_eq!(parse_envelope_meta(&env.bytes).unwrap().msg_type, ENVELOPE_TYPE_TEXT);
        assert_eq!(
            decrypt_with_peer(&bob, &alice.public_identity(), &env.bytes)
                .unwrap()
                .text,
            "compat"
        );
    }

    #[test]
    fn parse_rejects_unknown_msg_type_before_decrypt() {
        let alice = DeviceIdentity::generate();
        let bob = DeviceIdentity::generate();
        let mut env = encrypt_message(&alice, &bob.public_identity(), "x").unwrap();
        env.bytes[6] = 99;
        let err = match parse_envelope(&env.bytes) {
            Err(e) => e,
            Ok(_) => panic!("unknown msg_type must fail in parse_envelope"),
        };
        assert!(matches!(err, RopeError::InvalidEnvelope));
        let meta_err = parse_envelope_meta(&env.bytes).unwrap_err();
        assert!(matches!(meta_err, RopeError::InvalidEnvelope));
    }

    #[test]
    fn parse_rejects_timestamp_beyond_future_skew() {
        let alice = DeviceIdentity::generate();
        let bob = DeviceIdentity::generate();
        let mut env = encrypt_message(&alice, &bob.public_identity(), "x").unwrap();
        // parse_envelope samples unix_ms() again; u64::MAX cannot race the +1 boundary.
        let far = u64::MAX;
        env.bytes[24..32].copy_from_slice(&far.to_le_bytes());
        let err = match parse_envelope(&env.bytes) {
            Err(e) => e,
            Ok(_) => panic!("far-future timestamp must fail in parse_envelope"),
        };
        assert!(matches!(err, RopeError::InvalidEnvelope));
    }

    #[test]
    fn parse_allows_old_timestamp() {
        let alice = DeviceIdentity::generate();
        let bob = DeviceIdentity::generate();
        let mut env = encrypt_message(&alice, &bob.public_identity(), "x").unwrap();
        env.bytes[24..32].copy_from_slice(&0u64.to_le_bytes());
        let parsed = parse_envelope(&env.bytes).unwrap();
        assert_eq!(parsed.meta.timestamp_ms, 0);
    }

    #[test]
    fn future_skew_window_is_seven_days() {
        let now = 1_700_000_000_000u64;
        assert_eq!(MAX_FUTURE_SKEW_MS, 7 * 24 * 60 * 60 * 1000);
        assert!(!timestamp_exceeds_future_skew(now, now));
        assert!(!timestamp_exceeds_future_skew(now + MAX_FUTURE_SKEW_MS, now));
        assert!(timestamp_exceeds_future_skew(
            now + MAX_FUTURE_SKEW_MS + 1,
            now
        ));
        assert!(!timestamp_exceeds_future_skew(0, now));
    }

    #[test]
    fn decrypt_with_peer_stays_text_only() {
        let alice = DeviceIdentity::generate();
        let bob = DeviceIdentity::generate();
        let media = encrypt_typed(
            &alice,
            &bob.public_identity(),
            ENVELOPE_TYPE_MEDIA,
            br#"{"kind":"voice"}"#,
        )
        .unwrap();
        let err = decrypt_with_peer(&bob, &alice.public_identity(), &media.bytes).unwrap_err();
        assert!(matches!(err, RopeError::InvalidEnvelope));
        let typed = decrypt_typed(&bob, &alice.public_identity(), &media.bytes).unwrap();
        assert_eq!(typed.msg_type, ENVELOPE_TYPE_MEDIA);

        let call = encrypt_typed(&alice, &bob.public_identity(), ENVELOPE_TYPE_CALL, b"sdp").unwrap();
        assert!(decrypt_with_peer(&bob, &alice.public_identity(), &call.bytes).is_err());
        let got = decrypt_typed(&bob, &alice.public_identity(), &call.bytes).unwrap();
        assert_eq!(got.msg_type, ENVELOPE_TYPE_CALL);
        assert_eq!(got.body, b"sdp");
    }

    #[test]
    fn decrypt_typed_rejects_empty_call_body() {
        let alice = DeviceIdentity::generate();
        let bob = DeviceIdentity::generate();
        let env = encrypt_typed(&alice, &bob.public_identity(), ENVELOPE_TYPE_CALL, b"").unwrap();
        assert_eq!(parse_envelope_meta(&env.bytes).unwrap().msg_type, ENVELOPE_TYPE_CALL);
        let err = decrypt_typed(&bob, &alice.public_identity(), &env.bytes).unwrap_err();
        assert!(matches!(err, RopeError::InvalidEnvelope));
    }

    #[test]
    fn decrypt_typed_accepts_types_one_through_five() {
        let alice = DeviceIdentity::generate();
        let bob = DeviceIdentity::generate();
        for (ty, body) in [
            (ENVELOPE_TYPE_TEXT, encode_text_payload("hi")),
            (ENVELOPE_TYPE_MEDIA, br#"{"k":"m"}"#.to_vec()),
            (ENVELOPE_TYPE_GROUP_TEXT, encode_text_payload("g")),
            (ENVELOPE_TYPE_CALL, b"offer".to_vec()),
            (ENVELOPE_TYPE_RECEIPT, b"ack".to_vec()),
        ] {
            let env = encrypt_typed(&alice, &bob.public_identity(), ty, &body).unwrap();
            let got = decrypt_typed(&bob, &alice.public_identity(), &env.bytes).unwrap();
            assert_eq!(got.msg_type, ty);
            assert_eq!(got.body, body);
        }
    }
}
