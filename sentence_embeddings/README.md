# Sentence Embeddings Android Library

This module builds an Android AAR library that includes Rust-based HuggingFace tokenizer functionality.

## Prerequisites

### Local Development
- Android Studio Arctic Fox or later
- Android NDK 29.0.14206865 (installed via SDK Manager)
- Rust toolchain (https://rustup.rs/)
- cargo-ndk: `cargo install cargo-ndk`

### Android Rust Targets
Install the required Rust targets for Android:
```bash
rustup target add aarch64-linux-android
rustup target add armv7-linux-androideabi
rustup target add i686-linux-android
rustup target add x86_64-linux-android
```

## Building

### Using Gradle (Recommended)
The easiest way to build the AAR is through Gradle:

```bash
# From project root
./gradlew :sentence_embeddings:assembleRelease
```

The AAR file will be generated at:
```
sentence_embeddings/build/outputs/aar/sentence_embeddings-release.aar
```

### Using Shell Script (Alternative)
You can also build the Rust library manually:

```bash
cd rs-hf-tokenizer
./build-android.sh
```

This will create `jniLibs/` directory with the compiled native libraries for all Android architectures.

## CI/CD

The GitHub Actions workflow automatically:
1. Sets up Rust toolchain
2. Installs cargo-ndk
3. Adds Android Rust targets
4. Installs Android NDK
5. Builds the AAR with `./gradlew :sentence_embeddings:assembleRelease`

## Architecture Support

The library is built for the following Android architectures:
- arm64-v8a (64-bit ARM)
- armeabi-v7a (32-bit ARM)
- x86 (32-bit x86)
- x86_64 (64-bit x86)

## Integration

Add to your app's `build.gradle.kts`:

```kotlin
dependencies {
    implementation(project(":sentence_embeddings"))
}
```

## Troubleshooting

### NDK not found
Make sure `ANDROID_NDK_HOME` is set:
```bash
export ANDROID_NDK_HOME=$ANDROID_HOME/ndk/29.0.14206865
```

### Rust targets not installed
Run the target installation command from Prerequisites section.

### cargo-ndk not found
Install it with:
```bash
cargo install cargo-ndk
```

## License

See LICENSE file in the project root.
