@file:OptIn(ExperimentalEncodingApi::class)

import buildlogic.gitHash
import org.gradle.api.tasks.testing.Test
import org.gradle.jvm.toolchain.JavaLanguageVersion
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

plugins {
    id("com.android.application")
    id("com.mikepenz.aboutlibraries.plugin.android")
    kotlin("android")
    kotlin("plugin.serialization")
    kotlin("plugin.compose")
    id("kotlin-parcelize")
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

ksp {
    // Fix cache invalidation by stabilizing inputs
    arg("room.schemaLocation", "$projectDir/schemas")
    arg("room.incremental", "true")
    // Stabilize logLevel to prevent cache invalidation
    arg("logging.level", "WARN")
}

android {
    namespace = "com.github.zly2006.zhihu"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.github.zly2006.zhplus"
        minSdk = 27
        targetSdk = 35
        versionCode = property("app.versionCode").toString().toIntOrNull() ?: 1
        versionName = property("app.versionName").toString()

        testInstrumentationRunner = "com.github.zly2006.zhihu.ZhihuInstrumentedTestRunner"
    }

    flavorDimensions += "version"
    productFlavors {
        create("full") {
            dimension = "version"
            buildConfigField("boolean", "IS_LITE", "false")
        }
        create("lite") {
            dimension = "version"
            buildConfigField("boolean", "IS_LITE", "true")
            applicationIdSuffix = ".lite"
//            versionNameSuffix = "-lite"
        }
    }

    androidResources {
        @Suppress("UnstableApiUsage")
        localeFilters += listOf("en", "zh")
    }

    sourceSets.getByName("androidTest").assets.srcDir(layout.buildDirectory.dir("generated/androidTestSecrets"))

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

    buildTypes {
        val gitHash = gitHash(rootProject.projectDir)
        debug {
            buildConfigField("String", "GIT_HASH", "\"$gitHash\"")
        }
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            buildConfigField("String", "GIT_HASH", "\"$gitHash\"")
            if (System.getenv("signingKey") != null) {
                signingConfig = signingConfigs["env"]
            }
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlin {
        jvmToolchain(17)
    }
    buildFeatures {
        viewBinding = true
        buildConfig = true
        compose = true
    }

    packaging {
        resources {
            excludes +=
                listOf(
                    "META-INF/DEPENDENCIES",
//                    "META-INF/*.version",
                    "META-INF/**/LICENSE",
                    "META-INF/**/LICENSE.txt",
                    "META-INF/proguard/*",
                    "**.kotlin_module",
                    "kotlin-tooling-metadata.json",
                    "DebugProbesKt.bin",
//                    "META-INF/*.kotlin_module",
                )
        }
    }

    androidComponents {
        beforeVariants(selector().all()) { variantBuilder ->
            val flavorName = variantBuilder.flavorName
            if (variantBuilder.buildType == "release") {
                val minify =
                    when (flavorName) {
                        "lite" -> true
                        else -> false
                    }
                variantBuilder.isMinifyEnabled = minify
                variantBuilder.shrinkResources = minify
            }
        }
    }
}

val generatedAndroidTestSecretsDir = layout.buildDirectory.dir("generated/androidTestSecrets")

val prepareAndroidTestSecretAccount by tasks.registering {
    val secretAccountFile = rootProject.file(".secret/account.json")
    outputs.dir(generatedAndroidTestSecretsDir)
    doLast {
        val outputDir = generatedAndroidTestSecretsDir.get().asFile
        delete(outputDir)
        if (secretAccountFile.exists()) {
            copy {
                from(secretAccountFile)
                into(outputDir.resolve("secret"))
                rename { "account.json" }
            }
        }
    }
}

tasks
    .matching {
        it.name.startsWith("merge") && it.name.contains("AndroidTestAssets")
    }.configureEach {
        dependsOn(prepareAndroidTestSecretAccount)
    }

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
    compilerOptions.freeCompilerArgs.add("-Xdebug")
}

tasks.withType<Test>().configureEach {
    javaLauncher.set(
        javaToolchains.launcherFor {
            languageVersion.set(JavaLanguageVersion.of(21))
        },
    )
}

val ktor = "3.5.0"
val coil = "3.4.0"
val aboutLibraries = "14.0.1"
val composeVersion = "1.11.0"
val lifecycleVersion = "2.10.0"
dependencies {
    implementation("androidx.preference:preference:1.2.1")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.11.0")
    implementation("io.ktor:ktor-client-core-jvm:$ktor")
    implementation("io.ktor:ktor-client-android:$ktor")
    implementation("io.ktor:ktor-client-content-negotiation-jvm:$ktor")
    implementation("io.ktor:ktor-serialization-kotlinx-json:$ktor")
    implementation("org.jetbrains.kotlinx:kotlinx-datetime:0.7.1")
    //noinspection GradleDependency
    implementation("androidx.browser:browser:1.8.0")

    implementation("io.github.zly2006:markdown-parser-android:0.0.1-alpha.8")
    implementation("io.github.zly2006:markdown-renderer-android:0.0.1-alpha.8")
    implementation("io.github.zly2006:latex-renderer-android:1.4.4-zly")

    implementation("io.coil-kt.coil3:coil-compose:$coil")
    implementation("io.coil-kt.coil3:coil-network-ktor3-android:$coil")
    implementation("io.coil-kt.coil3:coil-gif:$coil")
    implementation("io.coil-kt.coil3:coil-svg:$coil")
    implementation("me.saket.telephoto:zoomable-image-coil3:0.19.0")

    implementation("com.google.android.material:material:1.13.0")
    implementation("com.materialkolor:material-kolor:4.1.1")

    implementation("org.jsoup:jsoup:1.22.1")
    implementation("androidx.legacy:legacy-support-v4:1.0.0")

    // ZXing for QR code scanning
    implementation("com.journeyapps:zxing-android-embedded:4.3.0")

    implementation("androidx.core:core-ktx:1.18.0")
    implementation("com.google.android.material:material:1.14.0")
    // Lifecycle (JetBrains KMP versions)
    implementation("org.jetbrains.androidx.lifecycle:lifecycle-runtime-compose:$lifecycleVersion")
    implementation("org.jetbrains.androidx.lifecycle:lifecycle-viewmodel-compose:$lifecycleVersion")
    // LiveData is Android-specific, keep androidx
    implementation("androidx.lifecycle:lifecycle-livedata-ktx:$lifecycleVersion")
    // Navigation (JetBrains KMP version)
    //noinspection GradleDependency
    implementation("org.jetbrains.androidx.navigation:navigation-compose:2.9.2")
    implementation("androidx.webkit:webkit:1.16.0")
    implementation("androidx.activity:activity-compose:1.13.0")
    // Compose (core from JetBrains KMP)
    implementation("org.jetbrains.compose.runtime:runtime:$composeVersion")
    implementation("org.jetbrains.compose.foundation:foundation:$composeVersion")
    implementation("org.jetbrains.compose.material3:material3:1.11.0-alpha07")
    implementation("org.jetbrains.compose.ui:ui:$composeVersion")
    implementation("org.jetbrains.compose.ui:ui-graphics:$composeVersion")
    implementation("org.jetbrains.compose.animation:animation:$composeVersion")
    implementation("org.jetbrains.compose.animation:animation-core:$composeVersion")
    // Compose (AndroidX — icons, tooling, test not available from JetBrains yet)
    implementation(platform("androidx.compose:compose-bom:2026.05.00"))
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("com.mikepenz:aboutlibraries-compose-m3:$aboutLibraries")
    implementation("androidx.room:room-common-jvm:2.8.4")
    implementation("androidx.room:room-runtime-android:2.8.4")
    annotationProcessor("androidx.room:room-compiler:2.8.4")
    ksp("androidx.room:room-compiler:2.8.4")
    "fullImplementation"(project(":sentence_embeddings"))
    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")
    androidTestImplementation(platform("androidx.compose:compose-bom:2026.05.00"))
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")

    // HanLP for Chinese NLP
    "fullImplementation"("com.hankcs:hanlp:portable-1.8.4")

    testImplementation("junit:junit:4.13.2")
    testImplementation("io.ktor:ktor-client-cio:$ktor")
    testImplementation("io.ktor:ktor-client-content-negotiation:$ktor")
    testImplementation("io.ktor:ktor-serialization-kotlinx-json:$ktor")
    //noinspection GradleDependency
    androidTestImplementation("androidx.test.ext:junit:1.2.1")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.7.0")
    androidTestImplementation("io.ktor:ktor-client-mock:$ktor")
}
