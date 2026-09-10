use crate::error::RopeError;
use chacha20poly1305::aead::{Aead, KeyInit, Payload};
use chacha20poly1305::{XChaCha20Poly1305, XNonce};
use hkdf::Hkdf;
use rand::rngs::OsRng;
use rand::RngCore;
use sha2::Sha256;

pub const MAGIC: &[u8; 4] = b"ROBK";
pub const VERSION: u16 = 1;
pub const SALT_LEN: usize = 16;
pub const INFO: &[u8] = b"rope-chat-backup-v1";
pub const MIN_PASSWORD: usize = 12;
const CHUNK: usize = 256 * 1024;
const HEADER: usize = 4 + 2 + SALT_LEN + 4 + 4 + 8;

pub fn seal_chat_backup(password: &str, plaintext: &[u8]) -> Result<Vec<u8>, RopeError> {
    if password.is_empty() {
        return Err(RopeError::crypto("empty password"));
    }
    if password.chars().count() < MIN_PASSWORD {
        return Err(RopeError::crypto("password too short"));
    }
    let mut salt = [0u8; SALT_LEN];
    OsRng.fill_bytes(&mut salt);
    let key = backup_key(password.as_bytes(), &salt)?;
    let chunks = if plaintext.is_empty() {
        1
    } else {
        (plaintext.len() + CHUNK - 1) / CHUNK
    };
    let mut out = Vec::with_capacity(HEADER + plaintext.len() + chunks * 40);
    out.extend_from_slice(MAGIC);
    out.extend_from_slice(&VERSION.to_le_bytes());
    out.extend_from_slice(&salt);
    out.extend_from_slice(&(CHUNK as u32).to_le_bytes());
    out.extend_from_slice(&(chunks as u32).to_le_bytes());
    out.extend_from_slice(&(plaintext.len() as u64).to_le_bytes());
    for i in 0..chunks {
        let start = i * CHUNK;
        let end = (start + CHUNK).min(plaintext.len());
        let slice = if start < plaintext.len() {
            &plaintext[start..end]
        } else {
            &[]
        };
        let ck = chunk_key(&key, i as u32)?;
        let cipher = XChaCha20Poly1305::new((&ck).into());
        let mut nonce = [0u8; 24];
        OsRng.fill_bytes(&mut nonce);
        let ct = cipher
            .encrypt(XNonce::from_slice(&nonce), Payload { msg: slice, aad: &[] })
            .map_err(|_| RopeError::crypto("backup encrypt"))?;
        out.extend_from_slice(&nonce);
        out.extend_from_slice(&(ct.len() as u32).to_le_bytes());
        out.extend_from_slice(&ct);
    }
    Ok(out)
}

pub fn open_chat_backup(password: &str, blob: &[u8]) -> Result<Vec<u8>, RopeError> {
    if password.is_empty() {
        return Err(RopeError::crypto("empty password"));
    }
    if blob.len() < HEADER || &blob[0..4] != MAGIC {
        return Err(RopeError::crypto("not robk"));
    }
    let version = u16::from_le_bytes(blob[4..6].try_into().unwrap());
    if version != VERSION {
        return Err(RopeError::UnsupportedVersion(version));
    }
    let salt = &blob[6..6 + SALT_LEN];
    let chunk = u32::from_le_bytes(blob[22..26].try_into().unwrap()) as usize;
    let count = u32::from_le_bytes(blob[26..30].try_into().unwrap()) as usize;
    let total = u64::from_le_bytes(blob[30..38].try_into().unwrap()) as usize;
    if chunk == 0 || count == 0 {
        return Err(RopeError::crypto("bad backup header"));
    }
    let key = backup_key(password.as_bytes(), salt)?;
    let mut rest = &blob[HEADER..];
    let mut out = Vec::with_capacity(total);
    for i in 0..count {
        if rest.len() < 28 {
            return Err(RopeError::crypto("truncated backup"));
        }
        let nonce = &rest[..24];
        let ct_len = u32::from_le_bytes(rest[24..28].try_into().unwrap()) as usize;
        rest = &rest[28..];
        if rest.len() < ct_len {
            return Err(RopeError::crypto("truncated backup chunk"));
        }
        let ct = &rest[..ct_len];
        rest = &rest[ct_len..];
        let ck = chunk_key(&key, i as u32)?;
        let cipher = XChaCha20Poly1305::new((&ck).into());
        let part = cipher
            .decrypt(XNonce::from_slice(nonce), Payload { msg: ct, aad: &[] })
            .map_err(|_| RopeError::crypto("backup decrypt"))?;
        out.extend_from_slice(&part);
    }
    if !rest.is_empty() {
        return Err(RopeError::crypto("trailing backup bytes"));
    }
    if out.len() != total {
        return Err(RopeError::crypto("backup length mismatch"));
    }
    Ok(out)
}

fn backup_key(password: &[u8], salt: &[u8]) -> Result<[u8; 32], RopeError> {
    let hk = Hkdf::<Sha256>::new(Some(salt), password);
    let mut key = [0u8; 32];
    hk.expand(INFO, &mut key)
        .map_err(|_| RopeError::crypto("hkdf backup"))?;
    Ok(key)
}

fn chunk_key(master: &[u8; 32], index: u32) -> Result<[u8; 32], RopeError> {
    let hk = Hkdf::<Sha256>::new(Some(&index.to_le_bytes()), master);
    let mut key = [0u8; 32];
    hk.expand(b"rope-obj-v1", &mut key)
        .map_err(|_| RopeError::crypto("hkdf backup chunk"))?;
    Ok(key)
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn roundtrip_and_wrong_password() {
        let plain = b"{\"v\":1,\"kind\":\"chat_takeout\"}".to_vec();
        let blob = seal_chat_backup("twelve chars!", &plain).unwrap();
        assert_eq!(&blob[0..4], MAGIC);
        assert_ne!(&blob[0..4], b"ROCH");
        assert_ne!(&blob[0..4], b"RODB");
        assert_eq!(open_chat_backup("twelve chars!", &blob).unwrap(), plain);
        assert!(open_chat_backup("twelve chars?", &blob).is_err());
    }

    #[test]
    fn rejects_empty_and_short_password() {
        let err = seal_chat_backup("", b"x").unwrap_err();
        match err {
            RopeError::Crypto(msg) => assert_eq!(msg, "empty password"),
            other => panic!("{other:?}"),
        }
        assert!(seal_chat_backup("short", b"x").is_err());
        assert!(open_chat_backup("", b"ROBK").is_err());
    }

    #[test]
    fn tamper_fails() {
        let mut blob = seal_chat_backup("twelve chars!", b"secret-jsonl").unwrap();
        let i = blob.len() - 1;
        blob[i] ^= 0x01;
        assert!(open_chat_backup("twelve chars!", &blob).is_err());
    }

    #[test]
    fn empty_plaintext_roundtrips() {
        let blob = seal_chat_backup("twelve chars!", &[]).unwrap();
        assert!(open_chat_backup("twelve chars!", &blob).unwrap().is_empty());
    }
}
