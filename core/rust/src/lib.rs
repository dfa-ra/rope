uniffi::setup_scaffolding!();

mod auth;
mod envelope;
mod error;
mod group;
mod identity;
mod invite;
mod media;
mod util;

pub use envelope::{
    decrypt_typed, decrypt_with_peer, encrypt_message, encrypt_typed, parse_envelope_meta,
    DecryptedPayload, EncryptedEnvelope, EnvelopeMeta, PlainMessage, PROTOCOL_VERSION,
};
pub use error::RopeError;
pub use identity::{DeviceIdentity as NativeIdentity, PublicIdentity};
pub use invite::InviteLink;

use std::sync::Arc;

#[derive(uniffi::Object)]
pub struct DeviceIdentity {
    inner: NativeIdentity,
}

#[uniffi::export]
impl DeviceIdentity {
    #[uniffi::constructor]
    pub fn generate() -> Arc<Self> {
        Arc::new(Self {
            inner: NativeIdentity::generate(),
        })
    }

    #[uniffi::constructor]
    pub fn from_bytes(bytes: Vec<u8>) -> Result<Arc<Self>, RopeError> {
        Ok(Arc::new(Self {
            inner: NativeIdentity::from_bytes(&bytes)?,
        }))
    }

    pub fn to_bytes(&self) -> Vec<u8> {
        self.inner.to_bytes()
    }

    pub fn device_id(&self) -> String {
        self.inner.device_id()
    }

    pub fn public_identity(&self) -> PublicIdentity {
        self.inner.public_identity()
    }

    pub fn encrypt_message(
        &self,
        recipient: PublicIdentity,
        plaintext: String,
    ) -> Result<EncryptedEnvelope, RopeError> {
        encrypt_message(&self.inner, &recipient, &plaintext)
    }

    pub fn decrypt_message(
        &self,
        sender: PublicIdentity,
        envelope: Vec<u8>,
    ) -> Result<PlainMessage, RopeError> {
        decrypt_with_peer(&self.inner, &sender, &envelope)
    }

    pub fn encrypt_typed(
        &self,
        recipient: PublicIdentity,
        msg_type: u8,
        body: Vec<u8>,
    ) -> Result<EncryptedEnvelope, RopeError> {
        encrypt_typed(&self.inner, &recipient, msg_type, &body)
    }

    pub fn decrypt_typed(
        &self,
        sender: PublicIdentity,
        envelope: Vec<u8>,
    ) -> Result<DecryptedPayload, RopeError> {
        decrypt_typed(&self.inner, &sender, &envelope)
    }

    pub fn sign(&self, data: Vec<u8>) -> Vec<u8> {
        self.inner.sign(&data)
    }

    pub fn auth_header(&self, method: String, path: String, timestamp: u64, body: Vec<u8>) -> String {
        auth::make_auth_header(&self.inner, &method, &path, timestamp, &body)
    }

    pub fn ws_signature(&self, timestamp: u64) -> String {
        auth::make_ws_signature(&self.inner, timestamp)
    }
}

#[uniffi::export]
pub fn create_device_identity() -> Arc<DeviceIdentity> {
    DeviceIdentity::generate()
}

#[uniffi::export]
pub fn public_identity(identity: &DeviceIdentity) -> PublicIdentity {
    identity.public_identity()
}

#[uniffi::export]
pub fn verify(public_key_hex: String, data: Vec<u8>, signature: Vec<u8>) -> Result<bool, RopeError> {
    let pk = hex::decode(public_key_hex).map_err(|e| RopeError::Decode(e.to_string()))?;
    identity::verify_signature(&pk, &data, &signature)
}

#[uniffi::export]
pub fn parse_envelope(bytes: Vec<u8>) -> Result<EnvelopeMeta, RopeError> {
    parse_envelope_meta(&bytes)
}

#[uniffi::export]
pub fn parse_invite_url(url: String) -> Result<InviteLink, RopeError> {
    invite::parse_invite_url(&url)
}

#[uniffi::export]
pub fn build_invite_url(link: InviteLink) -> Result<String, RopeError> {
    invite::build_invite_url(link)
}

#[uniffi::export]
pub fn verify_invite(
    link: InviteLink,
    server_id: String,
    presented_fingerprint: String,
) -> Result<(), RopeError> {
    invite::verify_invite_against_server(&link, &server_id, &presented_fingerprint)
}

#[uniffi::export]
pub fn fingerprint_from_cert_der(der: Vec<u8>) -> String {
    invite::fingerprint_from_cert_der(der)
}

#[uniffi::export]
pub fn dev_http_fingerprint() -> String {
    invite::dev_http_fingerprint()
}

#[uniffi::export]
pub fn encrypt_object(plaintext: Vec<u8>) -> Result<media::EncryptedObject, RopeError> {
    media::encrypt_object(plaintext)
}

#[uniffi::export]
pub fn decrypt_object(
    key: Vec<u8>,
    ciphertext: Vec<u8>,
    expected_sha256: String,
) -> Result<Vec<u8>, RopeError> {
    media::decrypt_object(key, ciphertext, expected_sha256)
}

#[uniffi::export]
pub fn object_hash_hex(ciphertext: Vec<u8>) -> String {
    media::object_hash_hex(ciphertext)
}

#[uniffi::export]
pub fn bump_group_epoch(epoch: u32) -> u32 {
    group::bump_epoch(epoch)
}

#[uniffi::export]
pub fn initial_group_epoch() -> u32 {
    group::initial_epoch()
}

#[uniffi::export]
pub fn known_envelope_type(msg_type: u8) -> bool {
    envelope::known_envelope_type(msg_type)
}

#[uniffi::export]
pub fn protocol_version() -> u16 {
    PROTOCOL_VERSION
}

#[uniffi::export]
pub fn unix_timestamp() -> u64 {
    auth::unix_now()
}

#[uniffi::export]
pub fn public_identity_from_blob(blob: Vec<u8>) -> Result<PublicIdentity, RopeError> {
    let id = identity::device_id_from_public(&blob)?;
    Ok(PublicIdentity { device_id: id, blob })
}

#[cfg(test)]
mod api_tests {
    use super::*;

    #[test]
    fn uniffi_style_roundtrip() {
        let alice = DeviceIdentity::generate();
        let bob = DeviceIdentity::generate();
        let env = alice
            .encrypt_message(bob.public_identity(), "ping".into())
            .unwrap();
        let plain = bob
            .decrypt_message(alice.public_identity(), env.bytes)
            .unwrap();
        assert_eq!(plain.text, "ping");
    }
}
