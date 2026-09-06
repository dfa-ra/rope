use crate::error::RopeError;

pub fn require_magic<'a>(
    input: &mut &'a [u8],
    magic: &[u8; 4],
    err: RopeError,
) -> Result<(), RopeError> {
    if input.len() < 4 || &input[..4] != magic {
        return Err(err);
    }
    *input = &input[4..];
    Ok(())
}

pub fn read_fixed<const N: usize>(input: &mut &[u8]) -> Result<[u8; N], RopeError> {
    if input.len() < N {
        return Err(RopeError::Decode("truncated".into()));
    }
    let (head, tail) = input.split_at(N);
    *input = tail;
    Ok(head.try_into().unwrap())
}

pub fn sha256(data: &[u8]) -> [u8; 32] {
    use sha2::{Digest, Sha256};
    Sha256::digest(data).into()
}
