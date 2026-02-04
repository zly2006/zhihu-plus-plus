/*
Apk Recompress and Sign Tool
Reduce ~40% APK size without compression loss
Copyright (C) 2025, zly2006

This program is free software: you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with this program.  If not, see <https://www.gnu.org/licenses/>.
*/
use std::env;
use std::fs::File;
use std::io::{BufReader, BufWriter, Read, Write};
use std::process::{exit, Command};
use zip::{ZipArchive, ZipWriter, CompressionMethod};
use dotenv::dotenv;

struct Config {
    input_apk: String,
    output_apk: String,
    keystore_path: String,
    keystore_password: String,
    key_alias: String,
    key_password: String,
    apksigner_path: String,
}

impl Config {
    fn from_args() -> Result<Config, Box<dyn std::error::Error>> {
        // 加载.env文件
        dotenv().ok();

        let args: Vec<String> = env::args().collect();

        if args.len() < 2 {
            print_usage();
            exit(1);
        }

        let mut config = Config {
            input_apk: String::new(),
            output_apk: String::new(),
            keystore_path: env::var("KEYSTORE_PATH").unwrap_or_default(),
            keystore_password: env::var("KEYSTORE_PASSWORD").unwrap_or_default(),
            key_alias: env::var("KEY_ALIAS").unwrap_or_default(),
            key_password: env::var("KEY_PASSWORD").unwrap_or_default(),
            apksigner_path: env::var("APKSIGNER_PATH").unwrap_or_else(|_| "apksigner".to_string()),
        };

        let mut i = 1;
        while i < args.len() {
            match args[i].as_str() {
                "-i" | "--input" => {
                    i += 1;
                    if i >= args.len() {
                        eprintln!("错误: --input 参数需要指定APK文件路径");
                        exit(1);
                    }
                    config.input_apk = args[i].clone();
                }
                "-o" | "--output" => {
                    i += 1;
                    if i >= args.len() {
                        eprintln!("错误: --output 参数需要指定输出APK文件路径");
                        exit(1);
                    }
                    config.output_apk = args[i].clone();
                }
                "-k" | "--keystore" => {
                    i += 1;
                    if i >= args.len() {
                        eprintln!("错误: --keystore 参数需要指定keystore文件路径");
                        exit(1);
                    }
                    config.keystore_path = args[i].clone();
                }
                "--ks-pass" => {
                    i += 1;
                    if i >= args.len() {
                        eprintln!("错误: --ks-pass 参数需要指定keystore密码");
                        exit(1);
                    }
                    config.keystore_password = args[i].clone();
                }
                "--alias" => {
                    i += 1;
                    if i >= args.len() {
                        eprintln!("错误: --alias 参数需要指定key别名");
                        exit(1);
                    }
                    config.key_alias = args[i].clone();
                }
                "--key-pass" => {
                    i += 1;
                    if i >= args.len() {
                        eprintln!("错误: --key-pass 参数需要指定key密码");
                        exit(1);
                    }
                    config.key_password = args[i].clone();
                }
                "--apksigner" => {
                    i += 1;
                    if i >= args.len() {
                        eprintln!("错误: --apksigner 参数需要指定apksigner工具路径");
                        exit(1);
                    }
                    config.apksigner_path = args[i].clone();
                }
                "-h" | "--help" => {
                    print_usage();
                    exit(0);
                }
                _ => {
                    if config.input_apk.is_empty() {
                        config.input_apk = args[i].clone();
                    } else {
                        eprintln!("错误: 未知参数 {}", args[i]);
                        print_usage();
                        exit(1);
                    }
                }
            }
            i += 1;
        }

        // 验证必需的参数
        if config.input_apk.is_empty() {
            eprintln!("错误: 必须指定输入APK文件");
            print_usage();
            exit(1);
        }

        if config.output_apk.is_empty() {
            // 如果没有指定输出文件，自动生成
            let input_stem = std::path::Path::new(&config.input_apk)
                .file_stem()
                .unwrap_or(std::ffi::OsStr::new("output"))
                .to_string_lossy();
            config.output_apk = format!("{}_recompressed.apk", input_stem);
        }

        if config.keystore_path.is_empty() {
            eprintln!("错误: 必须指定keystore文件路径 (--keystore)");
            print_usage();
            exit(1);
        }

        if config.keystore_password.is_empty() {
            eprintln!("错误: 必须指定keystore密码 (--ks-pass)");
            print_usage();
            exit(1);
        }

        if config.key_alias.is_empty() {
            eprintln!("错误: 必须指定key别名 (--alias)");
            print_usage();
            exit(1);
        }

        if config.key_password.is_empty() {
            eprintln!("错误: 必须指定key密码 (--key-pass)");
            print_usage();
            exit(1);
        }

        Ok(config)
    }
}

fn print_usage() {
    println!("APK重新压缩和签名工具");
    println!();
    println!("用法:");
    println!("  apk-recompress [选项] <输入APK文件>");
    println!();
    println!("选项:");
    println!("  -i, --input <文件>        输入APK文件路径");
    println!("  -o, --output <文件>       输出APK文件路径 (可选，默认为输入文件名_recompressed.apk)");
    println!("  -k, --keystore <文件>     keystore文件路径 (必需)");
    println!("  --ks-pass <密码>          keystore密码 (必需)");
    println!("  --alias <别名>            key别名 (必需)");
    println!("  --key-pass <密码>         key密码 (必需)");
    println!("  --apksigner <路径>        apksigner工具路径 (可选，默认从PATH查找)");
    println!("  -h, --help                显示此帮助信息");
    println!();
    println!("环境变量配置 (.env文件支持):");
    println!("  KEYSTORE_PATH             keystore文件路径");
    println!("  KEYSTORE_PASSWORD         keystore密码");
    println!("  KEY_ALIAS                 key别名");
    println!("  KEY_PASSWORD              key密码");
    println!("  APKSIGNER_PATH            apksigner工具路径");
    println!();
    println!("示例:");
    println!("  # 使用命令行参数");
    println!("  apk-recompress -i test.apk -k debug.keystore --ks-pass android --alias androiddebugkey --key-pass android");
    println!();
    println!("  # 使用.env文件配置");
    println!("  echo 'KEYSTORE_PATH=/path/to/release.jks' > .env");
    println!("  echo 'KEYSTORE_PASSWORD=mypass' >> .env");
    println!("  echo 'KEY_ALIAS=mykey' >> .env");
    println!("  echo 'KEY_PASSWORD=mypass' >> .env");
    println!("  apk-recompress test.apk");
}

fn main() -> Result<(), Box<dyn std::error::Error>> {
    let config = Config::from_args()?;

    println!("开始重新压缩和签名 APK 文件: {}", config.input_apk);
    println!("输出文件: {}", config.output_apk);

    // 第一步：重新压缩APK
    recompress_apk(&config.input_apk, &config.output_apk)?;

    // 第二步：使用apksigner重新签名APK
    sign_apk_with_apksigner(&config)?;

    println!("APK重新压缩和签名完成！");

    Ok(())
}

fn recompress_apk(input_path: &str, output_path: &str) -> Result<(), Box<dyn std::error::Error>> {
    println!("步骤1: 重新压缩APK文件");

    // 打开原始APK文件
    let input_file = File::open(input_path)?;
    let reader = BufReader::new(input_file);
    let mut archive = ZipArchive::new(reader)?;

    // 创建新的压缩文件
    let output_file = File::create(output_path)?;
    let writer = BufWriter::new(output_file);
    let mut zip_writer = ZipWriter::new(writer);

    println!("正在处理 {} 个文件...", archive.len());

    // 遍历原始APK中的所有文件，跳过旧的签名文件
    for i in 0..archive.len() {
        let mut file = archive.by_index(i)?;
        let file_name = file.name().to_string();

        // 跳过META-INF中的签名相关文件
        if file_name.starts_with("META-INF/") &&
            (file_name.ends_with(".SF") ||
                file_name.ends_with(".RSA") ||
                file_name.ends_with(".DSA") ||
                file_name.ends_with(".MF")) {
            println!("跳过签名文件: {}", file_name);
            continue;
        }

        // 读取文件内容
        let mut buffer = Vec::new();
        file.read_to_end(&mut buffer)?;

        // 根据文件类型选择压缩策略
        let options: zip::write::FileOptions<'_, ()> = if should_store_uncompressed(&file_name) {
            // 某些文件需要保持未压缩状态
            zip::write::FileOptions::default()
                .compression_method(CompressionMethod::Stored)
        } else {
            // 其他文件使用最高压缩等级
            zip::write::FileOptions::default()
                .compression_method(CompressionMethod::Deflated)
                .compression_level(Some(9)) // 最高压缩等级
        };

        // 写入到新的压缩文件
        zip_writer.start_file::<&String, ()>(&file_name, options)?;
        zip_writer.write_all(&buffer)?;
    }

    // 完成压缩
    zip_writer.finish()?;

    // 显示文件大小对比
    let original_size = std::fs::metadata(input_path)?.len();
    let compressed_size = std::fs::metadata(output_path)?.len();

    println!("原始文件大小: {} 字节", original_size);
    println!("压缩后文件大小: {} 字节", compressed_size);

    if compressed_size < original_size {
        let savings = ((original_size - compressed_size) as f64 / original_size as f64) * 100.0;
        println!("节省空间: {:.1}%", savings);
    }

    Ok(())
}

/// 判断文件是否应该保持未压缩状态
fn should_store_uncompressed(file_name: &str) -> bool {
    // Native库文件必须保持未压缩
    if file_name.ends_with(".so") {
        return true;
    }

    // 资源文件
    if file_name == "resources.arsc" {
        return true;
    }

    // 某些音频/视频文件已经压缩过了
    let already_compressed_extensions = [
        ".jpg", ".jpeg", ".png", ".gif", ".webp",
        ".mp3", ".mp4", ".avi", ".mkv", ".webm",
        ".ogg", ".wav", ".m4a",
        ".zip", ".jar", ".apk",
    ];

    for ext in &already_compressed_extensions {
        if file_name.to_lowercase().ends_with(ext) {
            return true;
        }
    }

    // DEX文件保持适度压缩（不完全未压缩，但也不用最高压缩）
    if file_name.ends_with(".dex") {
        return false; // 这些文件仍然可以压缩，但我们在调用处会使用中等压缩
    }

    false
}

fn sign_apk_with_apksigner(config: &Config) -> Result<(), Box<dyn std::error::Error>> {
    println!("步骤2: 使用apksigner重新签名APK文件");
    if !std::path::Path::new(&config.keystore_path).exists() {
        println!("keystore不存在！");
        exit(1);
    }

    // 使用apksigner签名APK
    println!("正在使用apksigner签名APK...");
    let output = Command::new(&config.apksigner_path)
        .arg("sign")
        .arg("--ks")
        .arg(&config.keystore_path)
        .arg("--ks-pass")
        .arg(&format!("pass:{}", config.keystore_password))
        .arg("--ks-key-alias")
        .arg(&config.key_alias)
        .arg("--key-pass")
        .arg(&format!("pass:{}", config.key_password))
        .arg("--v1-signing-enabled")
        .arg("true")
        .arg("--v2-signing-enabled")
        .arg("true")
        .arg(&config.output_apk)
        .output();

    match output {
        Ok(result) => {
            if result.status.success() {
                println!("APK签名成功！");

                // 验证签名
                verify_apk_signature(&config.output_apk, &config.apksigner_path)?;
            } else {
                let error_msg = String::from_utf8_lossy(&result.stderr);
                eprintln!("apksigner签名失败: {}", error_msg);
                return Err(format!("签名失败: {}", error_msg).into());
            }
        }
        Err(e) => {
            eprintln!("无法执行apksigner命令: {}", e);
            eprintln!("请确保Android SDK已安装并且apksigner在PATH中");
            return Err(e.into());
        }
    }

    Ok(())
}

fn verify_apk_signature(apk_path: &str, apksigner_path: &str) -> Result<(), Box<dyn std::error::Error>> {
    println!("正在验证APK签名...");

    let output = Command::new(apksigner_path)
        .arg("verify")
        .arg("--verbose")
        .arg(apk_path)
        .output();

    match output {
        Ok(result) => {
            if result.status.success() {
                let stdout = String::from_utf8_lossy(&result.stdout);
                println!("APK签名验证成功！");
                println!("验证结果:\n{}", stdout);
            } else {
                let error_msg = String::from_utf8_lossy(&result.stderr);
                eprintln!("APK签名验证失败: {}", error_msg);
            }
        }
        Err(e) => {
            eprintln!("无法执行apksigner verify命令: {}", e);
        }
    }

    Ok(())
}
