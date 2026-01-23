# Rust source code to expose HuggingFace Tokenizers

The [HuggingFace Tokenziers](https://github.com/huggingface/tokenizers) are authored in Rust, and can be installed as a Rust crate. We write a simple interface with JNI that helps us create a `Tokenzier` from the file `tokenizer.json` and tokenize the given text and return the `ids` and `attention_mask`.

The `ids` and `attention_mask` are returned as in the JSON format as a `jstring` (Java `String`) which is then deserialized on the Android side. This crate compiles and generates dynamic libraries (`.so`) for four architectures required by Android apps (ARM 32-bit/64-bit and x86 32-bit/64-bit).

## Setup

> [!NOTE]
> The setup was tested on Windows WSL (Debian)

Make sure you've installed the latest version of [Android NDK](https://developer.android.com/ndk/downloads). Update the paths present in `.cargo/config.toml` accordingly, pointing to the required linkers and `clang`, `clang++` compilers. With `rustup`, make sure you have added the following toolchains,

```
armv7-linux-androideabi
aarch64-linux-android
i686-linux-android
x86_64-linux-android
```

Then, execute the following commands to build the `release` versions of the library,

```bash
$> cargo build --release --target armv7-linux-androideabi
$> cargo build --release --target aarch64-linux-android
$> cargo build --release --target i686-linux-android
$> cargo build --release --target x86_64-linux-android
```