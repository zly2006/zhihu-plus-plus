import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jlleitschuh.gradle.ktlint.tasks.GenerateReportsTask
import org.jlleitschuh.gradle.ktlint.tasks.KtLintFormatTask

plugins {
    kotlin("multiplatform")
    kotlin("plugin.compose")
    kotlin("plugin.serialization")
    id("com.android.kotlin.multiplatform.library")
    id("com.google.devtools.ksp")
    id("org.jetbrains.compose")
    id("org.jlleitschuh.gradle.ktlint")
}

// Force material3 to 1.10.0-alpha05 across all configurations.
// 根因：material-kolor 在 commonMain 用 strictly 约束强制 1.10.0-alpha05，
// 但该约束仅作用于 KMP 元数据配置，不会传播到 jvmMain/androidMain 平台配置。
// 平台配置仍然从 Compose 插件解析到 1.9.0，导致 commonMain 代码（如 MyModalBottomSheet.kt）
// 编译时用 1.10.0-alpha05 的 API，而平台编译时看到的是 1.9.0，产生 HIDDEN/invisible 编译错误。
configurations.configureEach {
    resolutionStrategy {
        force("org.jetbrains.compose.material3:material3:1.10.0-alpha05")
    }
}


ktlint {
    android.set(true)
    outputToConsole.set(true)
    enableExperimentalRules.set(true)
    filter {
        exclude("**/generated/**")
        exclude("**/build/**")
        exclude("build/generated/**")
        exclude("**/build/generated/ksp/**")
        exclude("**/ksp/**")
        exclude { it.file.absolutePath.contains("/build/generated/") }
    }
}

tasks.withType<KtLintFormatTask>().configureEach {
    exclude("**/generated/**")
    exclude("build/generated/**")
    exclude("**/build/generated/ksp/**")
    exclude("**/ksp/**")
    exclude { it.file.absolutePath.contains("/build/generated/") }
}

tasks
    .withType<GenerateReportsTask>()
    .matching { it.name in setOf("ktlintAndroidMainSourceSetFormat", "ktlintJvmMainSourceSetFormat") }
    .configureEach {
        enabled = false
    }

mapOf(
    "runKtlintFormatOverAndroidMainSourceSet" to "src/androidMain/kotlin",
    "runKtlintFormatOverJvmMainSourceSet" to "src/jvmMain/kotlin",
    "runKtlintFormatOverCommonMainSourceSet" to "src/commonMain/kotlin",
    "runKtlintFormatOverJvmTestSourceSet" to "src/jvmTest/kotlin",
).forEach { (taskName, sourcePath) ->
    tasks.withType<KtLintFormatTask>().matching { it.name == taskName }.configureEach {
        setSource(
            fileTree(sourcePath) {
                include("**/*.kt")
            },
        )
    }
}

ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
}

kotlin {
    compilerOptions {
        freeCompilerArgs.add("-Xexpect-actual-classes")
    }

    androidLibrary {
        namespace = "com.github.zly2006.zhihu.shared"
        compileSdk = 36
        minSdk = 27

        compilerOptions {
            jvmTarget = JvmTarget.JVM_17
        }
        androidResources {
            enable = true
        }
    }
    jvm {
        compilerOptions {
            jvmTarget = JvmTarget.JVM_17
        }
    }

    listOf(
        iosArm64(),
        iosSimulatorArm64(),
    ).forEach { iosTarget ->
        iosTarget.binaries.framework {
            baseName = "Shared"
            isStatic = true
        }
    }

    sourceSets {
        commonMain.dependencies {
            implementation(compose.runtime)
            implementation(compose.foundation)
            implementation(compose.material3)
            implementation(compose.materialIconsExtended)
            implementation(compose.ui)
            implementation(compose.components.resources)
            implementation("io.coil-kt.coil3:coil-compose:3.4.0")
            implementation("org.jetbrains.androidx.navigation:navigation-compose:2.9.2")
            implementation("org.jetbrains.androidx.lifecycle:lifecycle-runtime-compose:2.10.0")
            implementation("org.jetbrains.androidx.lifecycle:lifecycle-viewmodel-compose:2.10.0")
            implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.11.0")
            implementation("io.ktor:ktor-client-core:3.5.0")
            implementation("io.ktor:ktor-client-content-negotiation:3.5.0")
            implementation("io.ktor:ktor-serialization-kotlinx-json:3.5.0")
            implementation("com.materialkolor:material-kolor:4.1.1")
            implementation("com.fleeksoft.ksoup:ksoup:0.2.6")
            implementation("io.github.zly2006:latex-renderer:1.4.4-zly")
            implementation("io.github.zly2006:markdown-parser:0.0.1-alpha.10")
            implementation("io.github.zly2006:markdown-renderer:0.0.1-alpha.10")
            implementation("org.jetbrains.kotlinx:kotlinx-datetime:0.7.1")
            implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.9.0")
            implementation("androidx.room:room-runtime:2.8.4")
            implementation("androidx.sqlite:sqlite-bundled:2.6.1")
            implementation("com.mikepenz:aboutlibraries-compose-m3:14.0.1")
        }
        commonTest.dependencies {
            implementation(kotlin("test"))
            implementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.11.0")
            implementation("io.ktor:ktor-client-mock:3.5.0")
        }
        androidMain.dependencies {
            implementation("androidx.activity:activity-compose:1.13.0")
            implementation("androidx.browser:browser:1.8.0")
            implementation("androidx.core:core-ktx:1.18.0")
            implementation("androidx.lifecycle:lifecycle-livedata-ktx:2.10.0")
            implementation("androidx.preference:preference:1.2.1")
            implementation("androidx.webkit:webkit:1.16.0")
            implementation("com.journeyapps:zxing-android-embedded:4.3.0")
            implementation("com.google.zxing:core:3.5.3")
            implementation("io.coil-kt.coil3:coil-gif:3.4.0")
            implementation("io.coil-kt.coil3:coil-network-ktor3-android:3.4.0")
            implementation("me.saket.telephoto:zoomable-image-coil3:0.19.0")
            implementation("org.jsoup:jsoup:1.22.1")
        }
        jvmMain.dependencies {
            implementation(compose.desktop.currentOs)
            implementation("com.google.zxing:core:3.5.3")
            implementation("io.ktor:ktor-client-cio:3.5.0")
            implementation("org.jetbrains.kotlinx:kotlinx-coroutines-swing:1.11.0")
        }
        jvmTest.dependencies {
            implementation("org.jsoup:jsoup:1.22.1")
        }
    }
}

dependencies {
    add("kspAndroid", "androidx.room:room-compiler:2.8.4")
    add("kspJvm", "androidx.room:room-compiler:2.8.4")
}
