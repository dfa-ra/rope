use crate::envelope::http_dev_fingerprint;
use crate::error::RopeError;

#[derive(uniffi::Record, Clone, Debug, PartialEq, Eq)]
pub struct InviteLink {
    pub version: u16,
    pub host: String,
    pub port: u16,
    pub server_id: String,
    pub fingerprint: String,
    pub token: String,
    pub display_name: Option<String>,
}

pub fn build_invite_url(link: InviteLink) -> Result<String, RopeError> {
    if link.host.is_empty() || link.token.is_empty() || link.fingerprint.is_empty() {
        return Err(RopeError::Invite("missing fields".into()));
    }
    reject_control(&link.host)?;
    reject_control(&link.server_id)?;
    reject_control(&link.fingerprint)?;
    reject_control(&link.token)?;
    if let Some(name) = &link.display_name {
        reject_control(name)?;
    }
    let mut url = format!(
        "rope://join?v={}&host={}&port={}&sid={}&fp={}&tok={}",
        link.version,
        encode_query(&link.host),
        link.port,
        encode_query(&link.server_id),
        encode_query(&link.fingerprint.to_lowercase()),
        encode_query(&link.token)
    );
    if let Some(name) = &link.display_name {
        if !name.is_empty() {
            url.push_str("&name=");
            url.push_str(&encode_query(name));
        }
    }
    Ok(url)
}

pub fn parse_invite_url(url: &str) -> Result<InviteLink, RopeError> {
    let rest = url
        .strip_prefix("rope://join")
        .ok_or_else(|| RopeError::Invite("scheme must be rope://join".into()))?;
    let query = rest.strip_prefix('?').unwrap_or("");
    if query.is_empty() {
        return Err(RopeError::Invite("missing query".into()));
    }
    let mut version = 1u16;
    let mut host = None;
    let mut port = None;
    let mut server_id = None;
    let mut fingerprint = None;
    let mut token = None;
    let mut display_name = None;
    for part in query.split('&') {
        let Some((k, v)) = part.split_once('=') else {
            continue;
        };
        let value = decode_query(v)?;
        match k {
            "v" => {
                version = value
                    .parse()
                    .map_err(|_| RopeError::Invite("bad version".into()))?;
            }
            "host" => host = Some(value),
            "port" => {
                port = Some(
                    value
                        .parse()
                        .map_err(|_| RopeError::Invite("bad port".into()))?,
                )
            }
            "sid" => server_id = Some(value),
            "fp" => fingerprint = Some(value.to_lowercase()),
            "tok" => token = Some(value),
            "name" => display_name = Some(value),
            _ => {}
        }
    }
    if version != 1 {
        return Err(RopeError::UnsupportedVersion(version));
    }
    Ok(InviteLink {
        version,
        host: host.ok_or_else(|| RopeError::Invite("missing host".into()))?,
        port: port.ok_or_else(|| RopeError::Invite("missing port".into()))?,
        server_id: server_id.ok_or_else(|| RopeError::Invite("missing sid".into()))?,
        fingerprint: fingerprint.ok_or_else(|| RopeError::Invite("missing fp".into()))?,
        token: token.ok_or_else(|| RopeError::Invite("missing tok".into()))?,
        display_name,
    })
}

pub fn verify_invite_against_server(
    link: &InviteLink,
    server_id: &str,
    presented_fingerprint: &str,
) -> Result<(), RopeError> {
    if link.server_id != server_id {
        return Err(RopeError::Invite("server id mismatch".into()));
    }
    if link.fingerprint.to_lowercase() != presented_fingerprint.to_lowercase() {
        return Err(RopeError::FingerprintMismatch);
    }
    Ok(())
}

pub fn fingerprint_from_cert_der(der: Vec<u8>) -> String {
    crate::envelope::cert_fingerprint_hex(&der)
}

pub fn dev_http_fingerprint() -> String {
    http_dev_fingerprint()
}

fn encode_query(s: &str) -> String {
    let mut out = String::new();
    for b in s.bytes() {
        match b {
            b'A'..=b'Z' | b'a'..=b'z' | b'0'..=b'9' | b'-' | b'_' | b'.' | b'~' => {
                out.push(b as char)
            }
            b' ' => out.push('+'),
            _ => out.push_str(&format!("%{b:02X}")),
        }
    }
    out
}

fn decode_query(s: &str) -> Result<String, RopeError> {
    let mut bytes = Vec::new();
    let raw = s.as_bytes();
    let mut i = 0;
    while i < raw.len() {
        match raw[i] {
            b'+' => {
                bytes.push(b' ');
                i += 1;
            }
            b'%' if i + 2 < raw.len() => {
                let hex = std::str::from_utf8(&raw[i + 1..i + 3])
                    .map_err(|_| RopeError::Invite("bad escape".into()))?;
                bytes.push(
                    u8::from_str_radix(hex, 16).map_err(|_| RopeError::Invite("bad escape".into()))?,
                );
                i += 3;
            }
            c => {
                bytes.push(c);
                i += 1;
            }
        }
    }
    let out = String::from_utf8(bytes).map_err(|_| RopeError::Invite("invite not utf-8".into()))?;
    reject_control(&out)?;
    Ok(out)
}

fn reject_control(s: &str) -> Result<(), RopeError> {
    if s.bytes().any(|b| b == b'\n' || b == b'\r' || b == 0) {
        return Err(RopeError::Invite("control in field".into()));
    }
    Ok(())
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn invite_roundtrip() {
        let link = InviteLink {
            version: 1,
            host: "203.0.113.10".into(),
            port: 8443,
            server_id: "abc".into(),
            fingerprint: "deadbeef".into(),
            token: "tok-1".into(),
            display_name: Some("Ada".into()),
        };
        let url = build_invite_url(link.clone()).unwrap();
        assert!(url.starts_with("rope://join?"));
        let parsed = parse_invite_url(&url).unwrap();
        assert_eq!(parsed.host, link.host);
        assert_eq!(parsed.token, link.token);
        assert_eq!(parsed.fingerprint, "deadbeef");
        verify_invite_against_server(&parsed, "abc", "DEADBEEF").unwrap();
    }

    #[test]
    fn fingerprint_mismatch() {
        let link = parse_invite_url("rope://join?v=1&host=h&port=8443&sid=s&fp=aa&tok=t").unwrap();
        let err = verify_invite_against_server(&link, "s", "bb").unwrap_err();
        assert!(matches!(err, RopeError::FingerprintMismatch));
    }

    #[test]
    fn decode_rejects_percent_encoded_lf_in_host() {
        let err = parse_invite_url(
            "rope://join?v=1&host=h%0a.evil&port=8443&sid=s&fp=aa&tok=t",
        )
        .unwrap_err();
        assert!(matches!(err, RopeError::Invite(_)));
    }

    #[test]
    fn decode_rejects_cr_nul_and_name_lf() {
        assert!(parse_invite_url(
            "rope://join?v=1&host=h&port=8443&sid=s%0d&fp=aa&tok=t"
        )
        .is_err());
        assert!(parse_invite_url(
            "rope://join?v=1&host=h&port=8443&sid=s&fp=aa&tok=t%00x"
        )
        .is_err());
        assert!(parse_invite_url(
            "rope://join?v=1&host=h&port=8443&sid=s&fp=aa&tok=t&name=Ada%0a"
        )
        .is_err());
        assert!(parse_invite_url(
            "rope://join?v=1&host=h&port=8443&sid=s&fp=aa%0A&tok=t"
        )
        .is_err());
    }

    #[test]
    fn build_rejects_lf_in_host() {
        let link = InviteLink {
            version: 1,
            host: "h\n.evil".into(),
            port: 8443,
            server_id: "s".into(),
            fingerprint: "aa".into(),
            token: "t".into(),
            display_name: None,
        };
        assert!(build_invite_url(link).is_err());
    }
}
