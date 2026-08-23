/*
 * Zhihu++ - Free & Ad-Free Zhihu client for all platforms.
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

import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jlleitschuh.gradle.ktlint.tasks.GenerateReportsTask
import org.jlleitschuh.gradle.ktlint.tasks.KtLintFormatTask

plugins {
    kotlin("multiplatform")
    kotlin("plugin.compose")
    kotlin("plugin.serialization")
    id("com.android.kotlin.multiplatform.library")
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
        // org.tiqian 走 Central snapshot 仓（维护者手动发布的 dev 通道，正式 alpha
        // 发布后钉回固定版本）。短缓存让新发布的 snapshot 十分钟内可见。
        cacheChangingModulesFor(10, "minutes")
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
    "runKtlintFormatOverAndroidMainSourceSet" to
        listOf("src/androidMain/kotlin", "src/tiqianMarkdownMain/kotlin"),
    "runKtlintFormatOverJvmMainSourceSet" to listOf("src/jvmMain/kotlin"),
    "runKtlintFormatOverCommonMainSourceSet" to listOf("src/commonMain/kotlin"),
    "runKtlintFormatOverJvmTestSourceSet" to listOf("src/jvmTest/kotlin"),
).forEach { (taskName, sourcePaths) ->
    tasks.withType<KtLintFormatTask>().matching { it.name == taskName }.configureEach {
        setSource(
            sourcePaths.map { sourcePath ->
                fileTree(sourcePath) {
                    include("**/*.kt")
                }
            },
        )
    }
}

kotlin {
    compilerOptions {
        freeCompilerArgs.add("-Xexpect-actual-classes")
    }

    androidLibrary {
        namespace = "com.github.zly2006.zhihu.shared"
        compileSdk = 37
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
    macosArm64()

    sourceSets {
        commonMain.dependencies {
            api(project(":shared-local-db"))
            implementation(compose.runtime)
            implementation(compose.foundation)
            implementation(compose.material3)
            implementation(compose.materialIconsExtended)
            implementation(compose.ui)
            implementation(compose.components.resources)
            implementation("io.coil-kt.coil3:coil-compose:3.5.0")
            implementation("io.coil-kt.coil3:coil-network-core:3.5.0")
            implementation("org.jetbrains.androidx.navigation:navigation-compose:2.9.2")
            implementation("org.jetbrains.androidx.lifecycle:lifecycle-runtime-compose:2.10.0")
            implementation("org.jetbrains.androidx.lifecycle:lifecycle-viewmodel-compose:2.10.0")
            implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.11.0")
            implementation("io.ktor:ktor-client-core:3.5.0")
            implementation("io.ktor:ktor-client-content-negotiation:3.5.0")
            implementation("io.ktor:ktor-serialization-kotlinx-json:3.5.0")
            implementation("com.materialkolor:material-kolor:4.1.1")
            implementation("com.fleeksoft.ksoup:ksoup:0.2.6")
            implementation(project(":latex-renderer"))
            implementation(project(":markdown-parser"))
            implementation(project(":markdown-renderer"))
            implementation("org.jetbrains.kotlinx:kotlinx-datetime:0.8.0")
            implementation("org.jetbrains.kotlinx:kotlinx-io-core:0.8.1")
            implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.11.0")
            implementation("com.mikepenz:aboutlibraries-compose-m3:15.0.0")
        }
        commonTest.dependencies {
            implementation(kotlin("test"))
            implementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.11.0")
            implementation("io.ktor:ktor-client-mock:3.5.0")
        }
        androidMain {
            kotlin.srcDir("src/tiqianMarkdownMain/kotlin")
            dependencies {
                implementation("org.tiqian:markdown-compose:0.1.0-SNAPSHOT")
                implementation("org.tiqian:math-font-stix:0.1.0-SNAPSHOT")
                implementation("androidx.activity:activity-compose:1.13.0")
                implementation("androidx.browser:browser:1.10.0")
                implementation("androidx.core:core-ktx:1.19.0")
                implementation("androidx.lifecycle:lifecycle-livedata-ktx:2.11.0")
                implementation("androidx.media:media:1.7.1")
                implementation("androidx.webkit:webkit:1.16.0")
                implementation("com.journeyapps:zxing-android-embedded:4.3.0")
                implementation("com.google.zxing:core:3.5.4")
                implementation("io.coil-kt.coil3:coil-gif:3.5.0")
                implementation("io.coil-kt.coil3:coil-network-ktor3-android:3.5.0")
                implementation("io.ktor:ktor-client-android:3.5.0")
                implementation("me.saket.telephoto:zoomable-image-coil3:0.19.0")
                implementation("org.jsoup:jsoup:1.22.2")
            }
        }
        jvmMain {
            kotlin.srcDir("src/tiqianMarkdownMain/kotlin")
        }
        jvmMain.dependencies {
            implementation("org.tiqian:markdown-compose:0.1.0-SNAPSHOT")
            implementation("org.tiqian:math-font-stix:0.1.0-SNAPSHOT")
            implementation("io.coil-kt.coil3:coil-network-ktor3:3.5.0")
            implementation("androidx.sqlite:sqlite-bundled:2.6.2")
            implementation(compose.desktop.currentOs)
            implementation("com.google.zxing:core:3.5.4")
            implementation("io.ktor:ktor-client-cio:3.5.0")
            implementation("org.jetbrains.kotlinx:kotlinx-coroutines-swing:1.11.0")
            // JavaFX WebView 用于桌面端内嵌风控验证页面。
            // JavaFX 模块依赖关系：
            //   javafx-web → javafx-controls → javafx-graphics → javafx-base
            //              → javafx-media → javafx-graphics
            //   javafx-swing → javafx-graphics
            //   Platform 在 javafx-graphics 中，Scene 也在 javafx-graphics 中
            // JavaFX POM 使用 ${javafx.platform} classifier，Gradle 不会自动解析，
            // 需要显式指定平台 classifier 以获取含实际类的 jar（裸 jar 只含空 MANIFEST）。
            val osName = System.getProperty("os.name").lowercase()
            val osArch = System.getProperty("os.arch").lowercase()
            val fxClassifier =
                when {
                    osName.contains("mac") && (osArch == "aarch64" || osArch == "arm64") -> "mac-aarch64"
                    osName.contains("mac") -> "mac"
                    osName.contains("win") -> "win"
                    osName.contains("linux") && (osArch == "aarch64" || osArch == "arm64") -> "linux-aarch64"
                    else -> "linux"
                }
            // 使用 compileOnly 避免将 JavaFX 传递给依赖 shared 的其他模块
            compileOnly("org.openjfx:javafx-base:21.0.2:$fxClassifier")
            compileOnly("org.openjfx:javafx-graphics:21.0.2:$fxClassifier")
            compileOnly("org.openjfx:javafx-controls:21.0.2:$fxClassifier")
            compileOnly("org.openjfx:javafx-web:21.0.2:$fxClassifier")
            compileOnly("org.openjfx:javafx-swing:21.0.2:$fxClassifier")
        }
        macosMain.dependencies {
            implementation("io.ktor:ktor-client-darwin:3.5.0")
        }
        jvmTest.dependencies {
            implementation("org.jsoup:jsoup:1.22.2")
        }
    }
}
