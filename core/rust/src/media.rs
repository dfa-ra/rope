use crate::error::RopeError;
use crate::util::sha256;
use chacha20poly1305::aead::{Aead, KeyInit, Payload};
use chacha20poly1305::{XChaCha20Poly1305, XNonce};
use hkdf::Hkdf;
use rand::rngs::OsRng;
use rand::RngCore;
use sha2::Sha256;
use uuid::Uuid;

pub const OBJECT_MAGIC: &[u8; 4] = b"ROCH";
pub const OBJECT_VERSION: u16 = 1;
pub const DEFAULT_CHUNK: usize = 256 * 1024;
pub const MAX_OBJECT_PLAIN: usize = 25 * 1024 * 1024;

#[derive(uniffi::Record, Clone, Debug)]
pub struct EncryptedObject {
    pub object_id: String,
    pub key: Vec<u8>,
    pub sha256: String,
    pub ciphertext: Vec<u8>,
    pub plain_len: u64,
}

pub fn encrypt_object(plaintext: Vec<u8>) -> Result<EncryptedObject, RopeError> {
    if plaintext.len() > MAX_OBJECT_PLAIN {
        return Err(RopeError::crypto("object too large"));
    }
    let mut key = [0u8; 32];
    OsRng.fill_bytes(&mut key);
    let ciphertext = seal_chunked(&key, &plaintext, DEFAULT_CHUNK)?;
    Ok(EncryptedObject {
        object_id: Uuid::new_v4().as_hyphenated().to_string(),
        key: key.to_vec(),
        sha256: hex::encode(sha256(&ciphertext)),
        ciphertext,
        plain_len: plaintext.len() as u64,
    })
}

pub fn decrypt_object(key: Vec<u8>, ciphertext: Vec<u8>, expected_sha256: String) -> Result<Vec<u8>, RopeError> {
    if hex::encode(sha256(&ciphertext)) != expected_sha256.to_lowercase() {
        return Err(RopeError::crypto("object hash mismatch"));
    }
    if key.len() != 32 {
        return Err(RopeError::crypto("bad object key"));
    }
    let mut k = [0u8; 32];
    k.copy_from_slice(&key);
    open_chunked(&k, &ciphertext)
}

pub fn object_hash_hex(ciphertext: Vec<u8>) -> String {
    hex::encode(sha256(&ciphertext))
}

fn seal_chunked(key: &[u8; 32], plain: &[u8], chunk: usize) -> Result<Vec<u8>, RopeError> {
    let chunks = if plain.is_empty() { 1 } else { (plain.len() + chunk - 1) / chunk };
    let mut out = Vec::with_capacity(16 + plain.len() + chunks * 40);
    out.extend_from_slice(OBJECT_MAGIC);
    out.extend_from_slice(&OBJECT_VERSION.to_le_bytes());
    out.extend_from_slice(&(chunk as u32).to_le_bytes());
    out.extend_from_slice(&(chunks as u32).to_le_bytes());
    out.extend_from_slice(&(plain.len() as u64).to_le_bytes());
    for i in 0..chunks {
        let start = i * chunk;
        let end = (start + chunk).min(plain.len());
        let slice = if start < plain.len() { &plain[start..end] } else { &[] };
        let ck = chunk_key(key, i as u32)?;
        let cipher = XChaCha20Poly1305::new((&ck).into());
        let mut nonce = [0u8; 24];
        OsRng.fill_bytes(&mut nonce);
        let ct = cipher
            .encrypt(XNonce::from_slice(&nonce), Payload { msg: slice, aad: &[] })
            .map_err(|_| RopeError::crypto("object encrypt"))?;
        out.extend_from_slice(&nonce);
        out.extend_from_slice(&(ct.len() as u32).to_le_bytes());
        out.extend_from_slice(&ct);
    }
    Ok(out)
}

fn open_chunked(key: &[u8; 32], blob: &[u8]) -> Result<Vec<u8>, RopeError> {
    if blob.len() < 22 || &blob[0..4] != OBJECT_MAGIC {
        return Err(RopeError::InvalidEnvelope);
    }
    let version = u16::from_le_bytes(blob[4..6].try_into().unwrap());
    if version != OBJECT_VERSION {
        return Err(RopeError::UnsupportedVersion(version));
    }
    let chunk = u32::from_le_bytes(blob[6..10].try_into().unwrap()) as usize;
    let count = u32::from_le_bytes(blob[10..14].try_into().unwrap()) as usize;
    let total = u64::from_le_bytes(blob[14..22].try_into().unwrap()) as usize;
    if chunk == 0 || count == 0 || total > MAX_OBJECT_PLAIN {
        return Err(RopeError::crypto("bad object header"));
    }
    let mut rest = &blob[22..];
    let mut out = Vec::with_capacity(total);
    for i in 0..count {
        if rest.len() < 28 {
            return Err(RopeError::crypto("truncated object"));
        }
        let nonce = &rest[..24];
        let ct_len = u32::from_le_bytes(rest[24..28].try_into().unwrap()) as usize;
        rest = &rest[28..];
        if rest.len() < ct_len {
            return Err(RopeError::crypto("truncated object chunk"));
        }
        let ct = &rest[..ct_len];
        rest = &rest[ct_len..];
        let ck = chunk_key(key, i as u32)?;
        let cipher = XChaCha20Poly1305::new((&ck).into());
        let part = cipher
            .decrypt(XNonce::from_slice(nonce), Payload { msg: ct, aad: &[] })
            .map_err(|_| RopeError::crypto("object decrypt"))?;
        out.extend_from_slice(&part);
    }
    if out.len() != total {
        return Err(RopeError::crypto("object length mismatch"));
    }
    Ok(out)
}

fn chunk_key(master: &[u8; 32], index: u32) -> Result<[u8; 32], RopeError> {
    let hk = Hkdf::<Sha256>::new(Some(&index.to_le_bytes()), master);
    let mut key = [0u8; 32];
    hk.expand(b"rope-obj-v1", &mut key)
        .map_err(|_| RopeError::crypto("hkdf object"))?;
    Ok(key)
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn object_roundtrip_empty_and_large() {
        for size in [0usize, 1, 1000, DEFAULT_CHUNK + 17] {
            let plain = vec![0x5A; size];
            let enc = encrypt_object(plain.clone()).unwrap();
            let dec = decrypt_object(enc.key, enc.ciphertext, enc.sha256).unwrap();
            assert_eq!(dec, plain);
        }
    }

    #[test]
    fn object_rejects_bad_hash() {
        let enc = encrypt_object(b"abc".to_vec()).unwrap();
        assert!(decrypt_object(enc.key, enc.ciphertext, "00".repeat(32)).is_err());
    }
}
