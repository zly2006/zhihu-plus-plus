import com.vanniktech.maven.publish.SonatypeHost

// The rust-android-gradle plugin has to be declared before the Android/Kotlin plugins
// see: https://github.com/mozilla/rust-android-gradle/issues/147#issuecomment-2134688017
plugins {
    id("org.mozilla.rust-android-gradle.rust-android") version "0.9.6"
    alias(libs.plugins.android.library)
    alias(libs.plugins.jetbrains.kotlin.android)
    id("com.vanniktech.maven.publish") version "0.32.0"
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
}

cargo {
    module = "../rs-hf-tokenizer"
    libname = "hftokenizer"
    prebuiltToolchains = true
    targets = listOf("arm64")
    profile = "release"
    verbose = true
}

mavenPublishing {
    publishToMavenCentral(SonatypeHost.CENTRAL_PORTAL)
    signAllPublications()
    coordinates(
        "io.gitlab.shubham0204",
        "sentence-embeddings",
        "v6.1",
    )
    pom {
        name = "Sentence-Embeddings-Android"
        description =
            "Embeddings from sentence-transformers in Android! Supports all-MiniLM-L6-V2, bge-small-en, snowflake-arctic, model2vec models and more "
        inceptionYear = "2024"
        url = "https://github.com/shubham0204/Sentence-Embeddings-Android"
        version = "v6.1"
        licenses {
            license {
                name = "The Apache License, Version 2.0"
                url = "https://www.apache.org/licenses/LICENSE-2.0.txt"
                distribution = "https://www.apache.org/licenses/LICENSE-2.0.txt"
            }
        }
        developers {
            developer {
                id = "shubham0204"
                name = "Shubham Panchal"
                url = "https://github.com/shubham0204"
            }
        }
        scm {
            url = "https://github.com/shubham0204/Sentence-Embeddings-Android"
            connection = "scm:git:git://github.com/shubham0204/Sentence-Embeddings-Android.git"
            developerConnection = "scm:git:ssh://git@github.com/shubham0204/Sentence-Embeddings-Android.git"
        }
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation("com.microsoft.onnxruntime:onnxruntime-android:1.23.0")
}
