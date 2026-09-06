use thiserror::Error;

#[derive(Debug, Error, uniffi::Error)]
pub enum RopeError {
    #[error("crypto failure: {0}")]
    Crypto(String),
    #[error("invalid identity")]
    InvalidIdentity,
    #[error("invalid envelope")]
    InvalidEnvelope,
    #[error("unsupported protocol version {0}")]
    UnsupportedVersion(u16),
    #[error("invite rejected: {0}")]
    Invite(String),
    #[error("fingerprint mismatch")]
    FingerprintMismatch,
    #[error("decode error: {0}")]
    Decode(String),
}

impl RopeError {
    pub fn crypto(msg: impl Into<String>) -> Self {
        Self::Crypto(msg.into())
    }
}
