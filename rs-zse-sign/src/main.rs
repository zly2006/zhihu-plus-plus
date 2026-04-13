/*
 * Zhihu++ - Free & Ad-Free Zhihu client for Android.
 * Copyright (C) 2024-2026, zly2006 <i@zly2006.me>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation (version 3 only).
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

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
