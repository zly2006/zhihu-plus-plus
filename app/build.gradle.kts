@file:OptIn(ExperimentalEncodingApi::class)

import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

plugins {
    id("com.android.application")
    kotlin("android")
    kotlin("plugin.serialization")
    kotlin("plugin.compose")
    id("kotlin-parcelize")
    id("org.ajoberstar.grgit") version "5.2.2"
    id("com.google.devtools.ksp")
    id("org.jlleitschuh.gradle.ktlint")
}

ktlint {
    android.set(true)
    outputToConsole.set(true)
    enableExperimentalRules.set(true)
    filter {
        exclude("**/generated/**")
        exclude("**/build/**")
    }
}

android {
    namespace = "com.github.zly2006.zhihu"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.github.zly2006.zhplus"
        minSdk = 28
        targetSdk = 35
        versionCode = property("app.versionCode").toString().toIntOrNull() ?: 1
        versionName = property("app.versionName").toString()

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    testOptions {
        unitTests {
            isReturnDefaultValues = true
        }
    }

    signingConfigs {
        if (System.getenv("signingKey") != null) {
            register("env") {
                storeFile =
                    file("zhihu.jks").apply {
                        writeBytes(Base64.decode(System.getenv("signingKey")))
                    }
                storePassword = System.getenv("keyStorePassword")
                keyAlias = System.getenv("keyAlias")
                keyPassword = System.getenv("keyPassword")
            }
        }
    }
    kotlinOptions {
        freeCompilerArgs += "-Xdebug"
    }
    buildTypes {
        val gitHash = grgit.head().abbreviatedId
        debug {
            buildConfigField("String", "GIT_HASH", "\"$gitHash\"")
        }
        release {
            if (System.getenv("GITHUB_ACTIONS") == null ||
                System.getenv("CI_BUILD_MINIFY").toBoolean()
            ) {
                isMinifyEnabled = true
                isShrinkResources = true
                proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            }
            buildConfigField("String", "GIT_HASH", "\"$gitHash\"")
            if (System.getenv("signingKey") != null) {
                signingConfig = signingConfigs["env"]
            }
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
    }
    buildFeatures {
        viewBinding = true
        buildConfig = true
        compose = true
    }

    packaging {
        resources {
            excludes += "META-INF/DEPENDENCIES"
        }
    }
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
    compilerOptions.freeCompilerArgs.add("-Xdebug")
}

val ktor = "3.2.2"
val voyager = "1.1.0-beta03"
dependencies {
    implementation("androidx.preference:preference:1.2.1")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.10.1")
    implementation("io.ktor:ktor-client-core-jvm:$ktor")
    implementation("io.ktor:ktor-client-apache5:$ktor")
    implementation("io.ktor:ktor-client-content-negotiation-jvm:$ktor")
    implementation("io.ktor:ktor-serialization-kotlinx-json:$ktor")
    implementation("org.jetbrains.kotlinx:kotlinx-datetime:0.6.0")
    //noinspection GradleDependency
    implementation("androidx.browser:browser:1.8.0")

    implementation("io.coil-kt.coil3:coil-compose:3.0.0")
    implementation("io.coil-kt.coil3:coil-network-ktor3-android:3.2.0")

    implementation("org.jsoup:jsoup:1.17.2")
    implementation("androidx.legacy:legacy-support-v4:1.0.0")

    // ZXing for QR code scanning
    implementation("com.journeyapps:zxing-android-embedded:4.3.0")

    implementation("androidx.core:core-ktx:1.16.0")
    implementation("com.google.android.material:material:1.12.0")
    
    // KMP-compatible Lifecycle (androidx.lifecycle already has KMP support since 2.8.0)
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.9.2")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.9.2")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.9.2")
    
    // Voyager - KMP Navigation library
    implementation("cafe.adriel.voyager:voyager-navigator:$voyager")
    implementation("cafe.adriel.voyager:voyager-screenmodel:$voyager")
    implementation("cafe.adriel.voyager:voyager-transitions:$voyager")
    implementation("cafe.adriel.voyager:voyager-androidx:$voyager")
    
    implementation("androidx.webkit:webkit:1.14.0")
    implementation("androidx.activity:activity-compose:1.10.1")
    implementation(platform("androidx.compose:compose-bom:2025.07.00"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.material:material-icons-extended")
    //noinspection GradleDependency
    implementation("androidx.compose.animation:animation:1.8.2")
    //noinspection GradleDependency
    implementation("androidx.compose.animation:animation-core:1.8.2")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.room:room-common-jvm:2.7.2")
    implementation("androidx.room:room-runtime-android:2.7.2")
    androidTestImplementation(platform("androidx.compose:compose-bom:2024.09.00"))
    annotationProcessor("androidx.room:room-compiler:2.7.2")
    ksp("androidx.room:room-compiler:2.7.2")
    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-tooling-preview")
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")
    debugImplementation("androidx.compose.ui:ui-test-manifest")

    implementation("com.github.chrisbanes:PhotoView:2.0.0") {
        exclude(group = "com.android.support")
    }

    testImplementation("junit:junit:4.13.2")
    testImplementation("io.ktor:ktor-client-cio:$ktor")
    testImplementation("io.ktor:ktor-client-content-negotiation:$ktor")
    testImplementation("io.ktor:ktor-serialization-kotlinx-json:$ktor")
    //noinspection GradleDependency
    androidTestImplementation("androidx.test.ext:junit:1.2.1")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.7.0")
}
