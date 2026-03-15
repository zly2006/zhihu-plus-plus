fn main() {
    let args: Vec<String> = std::env::args().collect();
    let input = args.get(1).expect("usage: rs-zse-sign <input>\ndecrypt or encrypt zhihu signature");
    let decrypt = zsesigner::decrypt::decrypt_zse_v4(input);
    if let Ok(plain) = decrypt {
        println!("decrypted: {}", plain);
    } else {
        let out = zsesigner::encrypt_zse_v4(input);
        print!("{}", out);
    }
}
