fn main() {
    let args: Vec<String> = std::env::args().collect();
    let input = args.get(1).map(|s| s.as_str()).unwrap_or("");
    let now_ms: i64 = args
        .get(2)
        .and_then(|s| s.parse().ok())
        .unwrap_or_else(|| {
            std::time::SystemTime::now()
                .duration_since(std::time::UNIX_EPOCH)
                .unwrap()
                .as_millis() as i64
        });
    let out = zsesigner::encrypt_zse_v4(input, now_ms);
    print!("{}", out);
}
