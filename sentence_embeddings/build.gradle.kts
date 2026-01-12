plugins {
    id("com.android.library") version "8.12.3"
    kotlin("android") version "2.2.21"
}

// Used in GitHub CI to pass the path of the installed Android NDK
val envAndroidNDKPath = System.getenv("ANDROID_NDK_HOME")

android {
    namespace = "com.ml.shubham0204.sentence_embeddings"
    compileSdk = 35

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
    kotlinOptions { jvmTarget = "17" }

    androidResources {
        noCompress += "onnx"
    }
    
    sourceSets {
        getByName("main") {
            jniLibs.srcDirs("src/main/jniLibs")
        }
    }
}

// Rust build configuration
val rustProjectDir = file("../rs-hf-tokenizer")
val jniLibsDir = file("src/main/jniLibs")

// Define Android targets
val androidTargets = mapOf(
    "aarch64-linux-android" to "arm64-v8a",
    // targets below are commented out to only build for arm64-v8a
//    "armv7-linux-androideabi" to "armeabi-v7a",
//    "i686-linux-android" to "x86",
//    "x86_64-linux-android" to "x86_64"
)

// Task to build Rust library using cargo-ndk
tasks.register<Exec>("buildRustLib") {
    description = "Build Rust library for Android using cargo-ndk"
    group = "rust"
    
    workingDir = rustProjectDir
    
    // Check if cargo-ndk is available
    commandLine("sh", "-c", """
        if ! command -v cargo-ndk &> /dev/null; then
            echo "cargo-ndk not found. Installing..."
            cargo install cargo-ndk
        fi
        
        # Add Android targets if not already added
        ${androidTargets.keys.joinToString("\n") { "rustup target add $it 2>/dev/null || true" }}
        
        # Build for each target
        ${androidTargets.entries.joinToString("\n") { (rustTarget, androidAbi) ->
            """
            echo "Building for $androidAbi..."
            cargo ndk --target $androidAbi --platform 24 build --release
            mkdir -p ${jniLibsDir.absolutePath}/$androidAbi
            cp target/$rustTarget/release/libhftokenizer.so ${jniLibsDir.absolutePath}/$androidAbi/
            """
        }}
        
        echo "Rust build completed successfully!"
    """.trimIndent())
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
    implementation("androidx.core:core-ktx:1.17.0")
    implementation("androidx.appcompat:appcompat:1.7.1")
    implementation("com.microsoft.onnxruntime:onnxruntime-android:1.23.0")
}
