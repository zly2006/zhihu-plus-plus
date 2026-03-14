use crate::{g_transform, read_u32_be, write_u32_be, ALPHABET, KEY16, ZK};

fn decode_custom(encoded: &str) -> Result<Vec<u8>, String> {
    if !encoded.len().is_multiple_of(4) {
        return Err("invalid encoded length".to_string());
    }

    let mut reverse = [255u8; 128];
    for (i, ch) in ALPHABET.bytes().enumerate() {
        reverse[ch as usize] = i as u8;
    }

    let mut processed = Vec::with_capacity((encoded.len() / 4) * 3);
    let mut i = 0u32;
    let bytes = encoded.as_bytes();
    let mut p = 0usize;
    while p < bytes.len() {
        let a = *bytes.get(p).ok_or_else(|| "invalid chunk".to_string())?;
        let b = *bytes
            .get(p + 1)
            .ok_or_else(|| "invalid chunk".to_string())?;
        let c = *bytes
            .get(p + 2)
            .ok_or_else(|| "invalid chunk".to_string())?;
        let d = *bytes
            .get(p + 3)
            .ok_or_else(|| "invalid chunk".to_string())?;
        p += 4;

        if a >= 128 || b >= 128 || c >= 128 || d >= 128 {
            return Err("invalid alphabet char".to_string());
        }
        let ia = reverse[a as usize];
        let ib = reverse[b as usize];
        let ic = reverse[c as usize];
        let id = reverse[d as usize];
        if ia == 255 || ib == 255 || ic == 255 || id == 255 {
            return Err("invalid alphabet char".to_string());
        }

        let v = (ia as u32) | ((ib as u32) << 6) | ((ic as u32) << 12) | ((id as u32) << 18);
        for shift in [0u32, 8, 16] {
            let x = ((v >> shift) & 0xFF) as u8;
            let mask = ((58u32 >> (8 * (i % 4))) & 0xFF) as u8;
            i += 1;
            processed.push(x ^ mask);
        }
    }

    processed.reverse();
    let trim = processed.len() % 16;
    if trim > 2 || processed.len() < trim {
        return Err("invalid block alignment".to_string());
    }
    processed.truncate(processed.len() - trim);
    if processed.is_empty() || !processed.len().is_multiple_of(16) {
        return Err("invalid ciphertext length".to_string());
    }
    Ok(processed)
}

fn r_block_decrypt(input16: &[u8]) -> [u8; 16] {
    let mut tr = [0u32; 36];
    tr[0] = read_u32_be(input16, 0);
    tr[1] = read_u32_be(input16, 4);
    tr[2] = read_u32_be(input16, 8);
    tr[3] = read_u32_be(input16, 12);

    for i in 0..32 {
        let ta = g_transform(tr[i + 1] ^ tr[i + 2] ^ tr[i + 3] ^ ZK[31 - i]);
        tr[i + 4] = tr[i] ^ ta;
    }

    let mut out = [0u8; 16];
    write_u32_be(tr[35], &mut out, 0);
    write_u32_be(tr[34], &mut out, 4);
    write_u32_be(tr[33], &mut out, 8);
    write_u32_be(tr[32], &mut out, 12);
    out
}

fn pkcs7_unpad(bytes: &mut Vec<u8>) -> Result<(), String> {
    let Some(&last) = bytes.last() else {
        return Err("empty plaintext".to_string());
    };
    let pad = last as usize;
    if pad == 0 || pad > 16 || pad > bytes.len() {
        return Err("invalid pkcs7 padding".to_string());
    }
    if !bytes[bytes.len() - pad..]
        .iter()
        .all(|&b| b as usize == pad)
    {
        return Err("invalid pkcs7 padding".to_string());
    }
    bytes.truncate(bytes.len() - pad);
    Ok(())
}

fn decode_uri_component_bytes(input: &[u8]) -> Result<Vec<u8>, String> {
    fn hex_val(b: u8) -> Option<u8> {
        match b {
            b'0'..=b'9' => Some(b - b'0'),
            b'a'..=b'f' => Some(b - b'a' + 10),
            b'A'..=b'F' => Some(b - b'A' + 10),
            _ => None,
        }
    }

    let mut out = Vec::with_capacity(input.len());
    let mut i = 0usize;
    while i < input.len() {
        if input[i] == b'%' {
            if i + 2 >= input.len() {
                return Err("invalid percent encoding".to_string());
            }
            let h = hex_val(input[i + 1]).ok_or_else(|| "invalid percent encoding".to_string())?;
            let l = hex_val(input[i + 2]).ok_or_else(|| "invalid percent encoding".to_string())?;
            out.push((h << 4) | l);
            i += 3;
        } else {
            out.push(input[i]);
            i += 1;
        }
    }
    Ok(out)
}

pub fn decrypt_zse_v4(encoded: &str) -> Result<String, String> {
    let cipher = decode_custom(encoded)?;
    let mut plain = Vec::with_capacity(cipher.len());

    let first = r_block_decrypt(&cipher[..16]);
    for i in 0..16 {
        plain.push(first[i] ^ KEY16[i] ^ 42);
    }

    let mut prev_block = &cipher[..16];
    for chunk in cipher[16..].chunks(16) {
        let dec = r_block_decrypt(chunk);
        for i in 0..16 {
            plain.push(dec[i] ^ prev_block[i]);
        }
        prev_block = chunk;
    }

    pkcs7_unpad(&mut plain)?;
    if plain.len() < 2 || plain[1] != 0 {
        return Err("invalid plaintext header".to_string());
    }
    let raw = decode_uri_component_bytes(&plain[2..])?;
    String::from_utf8(raw).map_err(|_| "invalid utf-8".to_string())
}

#[cfg(test)]
mod tests {
    use crate::{encrypt_zse_v4, r_block};

    use super::decrypt_zse_v4;

    #[test]
    fn decrypt_roundtrip_ascii() {
        let input = "hello world?a=1&b=2";
        let encrypted = encrypt_zse_v4(input);
        let decrypted = decrypt_zse_v4(&encrypted).expect("decrypt should succeed");
        assert_eq!(decrypted, input);
    }

    #[test]
    fn decrypt_roundtrip_unicode() {
        let input = "你好，知乎++ Rust 解密";
        let encrypted = encrypt_zse_v4(input);
        let decrypted = decrypt_zse_v4(&encrypted).expect("decrypt should succeed");
        assert_eq!(decrypted, input);
    }

    #[test]
    fn block_decrypt_inverts_block_encrypt() {
        let input = *b"0123456789ABCDEF";
        let encrypted = r_block(&input);
        let decrypted = super::r_block_decrypt(&encrypted);
        assert_eq!(decrypted, input);
    }
}
