plugins {
    id("com.android.library")
}

// Used in GitHub CI to pass the path of the installed Android NDK
val envAndroidNDKPath = System.getenv("ANDROID_NDK_HOME")

android {
    namespace = "com.ml.shubham0204.sentence_embeddings"
    compileSdk = 37

    // Declare the ndkVersion to avoid 'NDK not installed' errors from rust-android-plugin
    // see: https://github.com/mozilla/rust-android-gradle/issues/29#issuecomment-593501017
    ndkVersion = "29.0.14206865" // Android NDK r28b
    envAndroidNDKPath?.let {
        ndkPath = it
    }

    defaultConfig {
        minSdk = 24
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        consumerProguardFiles("consumer-rules.pro")
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlin {
        jvmToolchain(17)
    }

    androidResources {
        noCompress += "onnx"
    }

    sourceSets {
        getByName("main") {
            jniLibs.directories.add("src/main/jniLibs")
        }
    }
}

// Rust build configuration
val rustProjectDir = file("../rs-hf-tokenizer")
val jniLibsDir = file("src/main/jniLibs")
val rustSrcDir = rustProjectDir.resolve("src")
val cargoToml = rustProjectDir.resolve("Cargo.toml")
val cargoLock = rustProjectDir.resolve("Cargo.lock")

// Define Android targets
val androidTargets = mapOf(
    "aarch64-linux-android" to "arm64-v8a",
    // targets below are commented out to only build for arm64-v8a
//    "armv7-linux-androideabi" to "armeabi-v7a",
//    "i686-linux-android" to "x86",
//    "x86_64-linux-android" to "x86_64"
)

// Task to build Rust library using cargo-ndk
// Adding explicit inputs/outputs allows Gradle to skip or remote-cache this task when nothing changes
tasks.register<Exec>("buildRustLib") {
    description = "Build Rust library for Android using cargo-ndk"
    group = "rust"

    workingDir = rustProjectDir

    inputs.dir(rustSrcDir)
    inputs.file(cargoToml)
    if (cargoLock.exists()) {
        inputs.file(cargoLock)
    }
    inputs.property("androidTargets", androidTargets.toString())
    inputs.property("ndkVersion", android.ndkVersion)

    // Note: commented out for performance
    //outputs.dir(rustProjectDir.resolve("target"))
    outputs.dir(jniLibsDir)

    val isWindows = System.getProperty("os.name").lowercase().contains("windows")

    if (isWindows) {
        // Windows: use batch commands with proper path handling
        val jniLibsPath = jniLibsDir.absolutePath.replace("\\", "/")
        val buildCommands = androidTargets.entries.joinToString(" && ") { (rustTarget, androidAbi) ->
            listOf(
                "echo Building for $androidAbi...",
                "cargo ndk --target $androidAbi --platform 24 build --release",
                "if not exist \"$jniLibsPath\\$androidAbi\" mkdir \"$jniLibsPath\\$androidAbi\"",
                "copy /Y target\\$rustTarget\\release\\libhftokenizer.so \"$jniLibsPath\\$androidAbi\\\""
            ).joinToString(" && ")
        }
        commandLine(
            "cmd", "/c",
            listOf(
                "echo Rust build starting...",
                buildCommands,
                "echo Rust build completed successfully!"
            ).joinToString(" && ")
        )
    } else {
        // Unix/macOS: use shell commands
        val jniLibsPath = jniLibsDir.absolutePath
        environment("DEVELOPER_DIR", "/Library/Developer/CommandLineTools")
        val buildCommands = androidTargets.entries.joinToString("\n") { (rustTarget, androidAbi) ->
            listOf(
                "echo \"Building for $androidAbi...\"",
                "cargo ndk --target $androidAbi --platform 24 build --release",
                "mkdir -p \"$jniLibsPath/$androidAbi\"",
                "cp target/$rustTarget/release/libhftokenizer.so \"$jniLibsPath/$androidAbi/\""
            ).joinToString("\n")
        }
        val addTargetCommands = androidTargets.keys.joinToString("\n") {
            "rustup target add $it || true"
        }
        commandLine("sh", "-c", listOf(
            "set -e",
            // 强制将用户的 cargo bin 目录注入到当前 shell 的 PATH 最前面
            "export PATH=\"\$HOME/.cargo/bin:\$PATH\"",
            "# Add Android targets if not already added",
            addTargetCommands,
            "",
            "# Build for each target",
            buildCommands,
            "",
            "echo \"Rust build completed successfully!\""
        ).joinToString("\n"))
    }
}

// Clean Rust build artifacts
tasks.register<Delete>("cleanRust") {
    description = "Clean Rust build artifacts"
    group = "rust"
    delete(file("$rustProjectDir/target"))
    delete(jniLibsDir)
}

// Make clean depend on cleanRust
tasks.named("clean") {
    dependsOn("cleanRust")
}

// Make preBuild depend on buildRustLib
tasks.matching { it.name.startsWith("preBuild") }.configureEach {
    dependsOn("buildRustLib")
}

dependencies {
    implementation("androidx.core:core-ktx:1.19.0")
    implementation("androidx.appcompat:appcompat:1.7.1")
    implementation("com.microsoft.onnxruntime:onnxruntime-android:1.26.0")
}
