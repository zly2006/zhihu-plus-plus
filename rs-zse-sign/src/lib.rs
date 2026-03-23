use jni::objects::{JClass, JString};
use jni::sys::jstring;
use jni::JNIEnv;

pub mod decrypt;

pub(crate) const ZK: [u32; 32] = [
    1170614578, 1024848638, 1413669199, 3951632832, 3528873006, 2921909214, 4151847688, 3997739139,
    1933479194, 3323781115, 3888513386, 460404854, 3747539722, 2403641034, 2615871395, 2119585428,
    2265697227, 2035090028, 2773447226, 4289380121, 4217216195, 2200601443, 3051914490, 1579901135,
    1321810770, 456816404, 2903323407, 4065664991, 330002838, 3506006750, 363569021, 2347096187,
];

pub(crate) const ZB: [u8; 256] = [
    20, 223, 245, 7, 248, 2, 194, 209, 87, 6, 227, 253, 240, 128, 222, 91, 237, 9, 125, 157, 230,
    93, 252, 205, 90, 79, 144, 199, 159, 197, 186, 167, 39, 37, 156, 198, 38, 42, 43, 168, 217,
    153, 15, 103, 80, 189, 71, 191, 97, 84, 247, 95, 36, 69, 14, 35, 12, 171, 28, 114, 178, 148,
    86, 182, 32, 83, 158, 109, 22, 255, 94, 238, 151, 85, 77, 124, 254, 18, 4, 26, 123, 176, 232,
    193, 131, 172, 143, 142, 150, 30, 10, 146, 162, 62, 224, 218, 196, 229, 1, 192, 213, 27, 110,
    56, 231, 180, 138, 107, 242, 187, 54, 120, 19, 44, 117, 228, 215, 203, 53, 239, 251, 127, 81,
    11, 133, 96, 204, 132, 41, 115, 73, 55, 249, 147, 102, 48, 122, 145, 106, 118, 74, 190, 29, 16,
    174, 5, 177, 129, 63, 113, 99, 31, 161, 76, 246, 34, 211, 13, 60, 68, 207, 160, 65, 111, 82,
    165, 67, 169, 225, 57, 112, 244, 155, 51, 236, 200, 233, 58, 61, 47, 100, 137, 185, 64, 17, 70,
    234, 163, 219, 108, 170, 166, 59, 149, 52, 105, 24, 212, 78, 173, 45, 0, 116, 226, 119, 136,
    206, 135, 175, 195, 25, 92, 121, 208, 126, 139, 3, 75, 141, 21, 130, 98, 241, 40, 154, 66, 184,
    49, 181, 46, 243, 88, 101, 183, 8, 23, 72, 188, 104, 179, 210, 134, 250, 201, 164, 89, 216,
    202, 220, 50, 221, 152, 140, 33, 235, 214,
];

pub(crate) const ALPHABET: &str =
    "6fpLRqJO8M/c3jnYxFkUVC4ZIG12SiH=5v0mXDazWBTsuw7QetbKdoPyAl+hN9rgE";
pub(crate) const KEY16: [u8; 16] = *b"059053f7d15e01d7";

fn encode_uri_component(input: &str) -> Vec<u8> {
    fn is_unescaped(b: u8) -> bool {
        matches!(b,
            b'A'..=b'Z' | b'a'..=b'z' | b'0'..=b'9' |
            b'-' | b'_' | b'.' | b'!' | b'~' | b'*' | b'\'' | b'(' | b')'
        )
    }

    let mut out = Vec::with_capacity(input.len() * 3);
    for &b in input.as_bytes() {
        if is_unescaped(b) {
            out.push(b);
        } else {
            out.push(b'%');
            out.push(b"0123456789ABCDEF"[(b >> 4) as usize]);
            out.push(b"0123456789ABCDEF"[(b & 0x0F) as usize]);
        }
    }
    out
}

#[inline]
pub(crate) fn read_u32_be(b: &[u8], off: usize) -> u32 {
    u32::from_be_bytes([b[off], b[off + 1], b[off + 2], b[off + 3]])
}

#[inline]
pub(crate) fn write_u32_be(v: u32, out: &mut [u8], off: usize) {
    out[off..off + 4].copy_from_slice(&v.to_be_bytes());
}

#[inline]
pub(crate) fn g_transform(tt: u32) -> u32 {
    let te = tt.to_be_bytes();
    let tr = [
        ZB[te[0] as usize],
        ZB[te[1] as usize],
        ZB[te[2] as usize],
        ZB[te[3] as usize],
    ];
    let ti = u32::from_be_bytes(tr);
    ti ^ ti.rotate_left(2) ^ ti.rotate_left(10) ^ ti.rotate_left(18) ^ ti.rotate_left(24)
}

pub(crate) fn r_block(input16: &[u8]) -> [u8; 16] {
    let mut tr = [0u32; 36];
    tr[0] = read_u32_be(input16, 0);
    tr[1] = read_u32_be(input16, 4);
    tr[2] = read_u32_be(input16, 8);
    tr[3] = read_u32_be(input16, 12);

    for i in 0..32 {
        let ta = g_transform(tr[i + 1] ^ tr[i + 2] ^ tr[i + 3] ^ ZK[i]);
        tr[i + 4] = tr[i] ^ ta;
    }

    let mut out = [0u8; 16];
    write_u32_be(tr[35], &mut out, 0);
    write_u32_be(tr[34], &mut out, 4);
    write_u32_be(tr[33], &mut out, 8);
    write_u32_be(tr[32], &mut out, 12);
    out
}

fn x_blocks(data: &[u8], mut iv: [u8; 16]) -> Vec<u8> {
    let mut out = Vec::with_capacity(data.len());
    for chunk in data.chunks(16) {
        let mut mixed = [0u8; 16];
        for i in 0..16 {
            mixed[i] = chunk[i] ^ iv[i];
        }
        iv = r_block(&mixed);
        out.extend_from_slice(&iv);
    }
    out
}

fn custom_encode(mut bytes: Vec<u8>) -> String {
    while bytes.len() % 3 != 0 {
        bytes.push(0);
    }

    let alphabet = ALPHABET.as_bytes();
    let mut out = String::with_capacity((bytes.len() / 3) * 4);
    let mut i: u32 = 0;
    let mut p: isize = bytes.len() as isize - 1;

    while p >= 0 {
        let mut v: u32 = 0;

        let b0 = bytes[p as usize] as u32;
        let m0 = (58u32 >> (8 * (i % 4))) & 0xFF;
        i += 1;
        v |= (b0 ^ m0) & 0xFF;

        let b1 = bytes[(p - 1) as usize] as u32;
        let m1 = (58u32 >> (8 * (i % 4))) & 0xFF;
        i += 1;
        v |= ((b1 ^ m1) & 0xFF) << 8;

        let b2 = bytes[(p - 2) as usize] as u32;
        let m2 = (58u32 >> (8 * (i % 4))) & 0xFF;
        i += 1;
        v |= ((b2 ^ m2) & 0xFF) << 16;

        out.push(alphabet[(v & 63) as usize] as char);
        out.push(alphabet[((v >> 6) & 63) as usize] as char);
        out.push(alphabet[((v >> 12) & 63) as usize] as char);
        out.push(alphabet[((v >> 18) & 63) as usize] as char);

        p -= 3;
    }

    out
}

pub fn encrypt_zse_v4(input: &str) -> String {
    let seed = 12u8; // matches zse-v4.js: Math.random = () => 0.1 → Math.floor(0.1 * 127)

    let mut plain = Vec::new();
    plain.push(seed);
    plain.push(0);
    plain.extend_from_slice(&encode_uri_component(input));

    let pad = 16 - (plain.len() % 16);
    for _ in 0..pad {
        plain.push(pad as u8);
    }

    let mut first = [0u8; 16];
    for i in 0..16 {
        first[i] = plain[i] ^ KEY16[i] ^ 42;
    }

    let c0 = r_block(&first);
    let mut cipher = Vec::with_capacity(plain.len());
    cipher.extend_from_slice(&c0);
    if plain.len() > 16 {
        cipher.extend_from_slice(&x_blocks(&plain[16..], c0));
    }

    custom_encode(cipher)
}

#[unsafe(no_mangle)]
pub extern "system" fn Java_com_github_zly2006_zhihu_util_ZseRustSigner_encrypt(
    mut env: JNIEnv,
    _class: JClass,
    input: JString,
) -> jstring {
    let input: String = env
        .get_string(&input)
        .map(|s| s.into())
        .unwrap_or_else(|_| String::new());
    let out = encrypt_zse_v4(&input);
    match env.new_string(out) {
        Ok(s) => s.into_raw(),
        Err(_) => std::ptr::null_mut(),
    }
}
