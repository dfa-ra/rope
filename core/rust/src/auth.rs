use crate::identity::DeviceIdentity;
use crate::util::sha256;
use base64::engine::general_purpose::URL_SAFE_NO_PAD;
use base64::Engine;

pub fn auth_signing_string(method: &str, path: &str, timestamp: u64, body: &[u8]) -> String {
    format!(
        "rope-auth-v1\n{}\n{}\n{}\n{}",
        method.to_uppercase(),
        path,
        timestamp,
        hex::encode(sha256(body))
    )
}

pub fn make_auth_header(
    identity: &DeviceIdentity,
    method: &str,
    path: &str,
    timestamp: u64,
    body: &[u8],
) -> String {
    let msg = auth_signing_string(method, path, timestamp, body);
    let sig = identity.sign(msg.as_bytes());
    format!(
        "Rope {}.{}.{}",
        identity.device_id(),
        timestamp,
        URL_SAFE_NO_PAD.encode(sig)
    )
}

pub fn ws_signing_string(timestamp: u64) -> String {
    format!("rope-ws-v1\n{timestamp}")
}

pub fn make_ws_signature(identity: &DeviceIdentity, timestamp: u64) -> String {
    URL_SAFE_NO_PAD.encode(identity.sign(ws_signing_string(timestamp).as_bytes()))
}

pub fn unix_now() -> u64 {
    use std::time::{SystemTime, UNIX_EPOCH};
    SystemTime::now()
        .duration_since(UNIX_EPOCH)
        .map(|d| d.as_secs())
        .unwrap_or(0)
}

#[cfg(test)]
mod tests {
    use super::*;
    use crate::identity::verify_signature;

    #[test]
    fn auth_header_verifies() {
        let id = DeviceIdentity::generate();
        let ts = 1_700_000_000;
        let header = make_auth_header(&id, "POST", "/v1/invites", ts, b"{}");
        let parts: Vec<&str> = header.split([' ', '.']).collect();
        assert_eq!(parts[0], "Rope");
        assert_eq!(parts[1], id.device_id());
        let sig = URL_SAFE_NO_PAD.decode(parts[3]).unwrap();
        let msg = auth_signing_string("POST", "/v1/invites", ts, b"{}");
        assert!(verify_signature(id.verifying_key().as_bytes(), msg.as_bytes(), &sig).unwrap());
    }
}
