/// Membership epoch for group key rotation. Server stores the integer;
/// clients treat a bump as "new cryptographic generation".
pub fn bump_epoch(epoch: u32) -> u32 {
    epoch.saturating_add(1)
}

pub fn initial_epoch() -> u32 {
    1
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn epoch_monotonic() {
        assert_eq!(initial_epoch(), 1);
        assert_eq!(bump_epoch(1), 2);
        assert_eq!(bump_epoch(u32::MAX), u32::MAX);
    }
}
