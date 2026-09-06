use crate::error::RopeError;
use crate::util::{read_fixed, require_magic};
use ed25519_dalek::{Signature, Signer, SigningKey, Verifier, VerifyingKey};
use rand::rngs::OsRng;
use x25519_dalek::{PublicKey as XPublic, StaticSecret};
pub const IDENTITY_MAGIC: &[u8; 4] = b"ROPI";
pub const PUBLIC_MAGIC: &[u8; 4] = b"ROPP";
pub const IDENTITY_VERSION: u16 = 1;

#[derive(uniffi::Record, Clone, Debug, PartialEq, Eq)]
pub struct PublicIdentity {
    pub device_id: String,
    pub blob: Vec<u8>,
}

pub struct DeviceIdentity {
    sign: SigningKey,
    ecdh: StaticSecret,
}

impl DeviceIdentity {
    pub fn generate() -> Self {
        let sign = SigningKey::generate(&mut OsRng);
        let ecdh = StaticSecret::random_from_rng(OsRng);
        Self { sign, ecdh }
    }

    pub fn from_bytes(bytes: &[u8]) -> Result<Self, RopeError> {
        if bytes.len() != 4 + 2 + 32 + 32 {
            return Err(RopeError::InvalidIdentity);
        }
        let mut rest = bytes;
        require_magic(&mut rest, IDENTITY_MAGIC, RopeError::InvalidIdentity)?;
        let version = u16::from_le_bytes(read_fixed::<2>(&mut rest)?);
        if version != IDENTITY_VERSION {
            return Err(RopeError::UnsupportedVersion(version));
        }
        let seed = read_fixed::<32>(&mut rest)?;
        let scalar = read_fixed::<32>(&mut rest)?;
        let sign = SigningKey::from_bytes(&seed);
        let ecdh = StaticSecret::from(scalar);
        Ok(Self { sign, ecdh })
    }

    pub fn to_bytes(&self) -> Vec<u8> {
        let mut out = Vec::with_capacity(70);
        out.extend_from_slice(IDENTITY_MAGIC);
        out.extend_from_slice(&IDENTITY_VERSION.to_le_bytes());
        out.extend_from_slice(self.sign.as_bytes());
        out.extend_from_slice(self.ecdh.as_bytes());
        out
    }

    pub fn verifying_key(&self) -> VerifyingKey {
        self.sign.verifying_key()
    }

    pub fn ecdh_public(&self) -> XPublic {
        XPublic::from(&self.ecdh)
    }

    pub fn ecdh_secret(&self) -> &StaticSecret {
        &self.ecdh
    }

    pub fn device_id(&self) -> String {
        hex::encode(self.verifying_key().as_bytes())
    }

    pub fn public_identity(&self) -> PublicIdentity {
        PublicIdentity {
            device_id: self.device_id(),
            blob: encode_public(self.verifying_key(), self.ecdh_public()),
        }
    }

    pub fn sign(&self, data: &[u8]) -> Vec<u8> {
        self.sign.sign(data).to_bytes().to_vec()
    }
}

pub fn encode_public(sign: VerifyingKey, ecdh: XPublic) -> Vec<u8> {
    let mut out = Vec::with_capacity(70);
    out.extend_from_slice(PUBLIC_MAGIC);
    out.extend_from_slice(&IDENTITY_VERSION.to_le_bytes());
    out.extend_from_slice(sign.as_bytes());
    out.extend_from_slice(ecdh.as_bytes());
    out
}

pub fn decode_public(bytes: &[u8]) -> Result<(VerifyingKey, XPublic), RopeError> {
    if bytes.len() != 70 {
        return Err(RopeError::InvalidIdentity);
    }
    let mut rest = bytes;
    require_magic(&mut rest, PUBLIC_MAGIC, RopeError::InvalidIdentity)?;
    let version = u16::from_le_bytes(read_fixed::<2>(&mut rest)?);
    if version != IDENTITY_VERSION {
        return Err(RopeError::UnsupportedVersion(version));
    }
    let sign_bytes = read_fixed::<32>(&mut rest)?;
    let ecdh_bytes = read_fixed::<32>(&mut rest)?;
    let sign = VerifyingKey::from_bytes(&sign_bytes).map_err(|_| RopeError::InvalidIdentity)?;
    let ecdh = XPublic::from(ecdh_bytes);
    Ok((sign, ecdh))
}

pub fn device_id_from_public(bytes: &[u8]) -> Result<String, RopeError> {
    let (sign, _) = decode_public(bytes)?;
    Ok(hex::encode(sign.as_bytes()))
}

pub fn verify_signature(public_key: &[u8], data: &[u8], signature: &[u8]) -> Result<bool, RopeError> {
    if public_key.len() != 32 || signature.len() != 64 {
        return Ok(false);
    }
    let vk = VerifyingKey::from_bytes(public_key.try_into().unwrap())
        .map_err(|_| RopeError::InvalidIdentity)?;
    let sig = Signature::from_bytes(signature.try_into().unwrap());
    Ok(vk.verify(data, &sig).is_ok())
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn identity_roundtrip() {
        let id = DeviceIdentity::generate();
        let bytes = id.to_bytes();
        let restored = DeviceIdentity::from_bytes(&bytes).unwrap();
        assert_eq!(id.device_id(), restored.device_id());
        assert_eq!(id.public_identity().blob, restored.public_identity().blob);
    }

    #[test]
    fn rejects_bad_magic() {
        let mut bytes = DeviceIdentity::generate().to_bytes();
        bytes[0] = b'X';
        assert!(matches!(
            DeviceIdentity::from_bytes(&bytes),
            Err(RopeError::InvalidIdentity)
        ));
    }
}
