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

import org.gradle.api.tasks.Sync
import org.jetbrains.kotlin.gradle.plugin.mpp.NativeBuildType.DEBUG

plugins {
    kotlin("multiplatform")
    kotlin("plugin.compose")
    id("org.jetbrains.compose")
    id("org.jlleitschuh.gradle.ktlint")
}

ktlint {
    outputToConsole.set(true)
    enableExperimentalRules.set(true)
    filter {
        exclude("**/generated/**")
        exclude("**/build/**")
    }
}

kotlin {
    macosArm64 {
        binaries {
            all {
                freeCompilerArgs += "-Xoverride-konan-properties=ignoreXcodeVersionCheck=true"
            }
            executable(listOf(DEBUG)) {
                baseName = "ZhihuPlusPlusUiDebug"
                entryPoint = "com.github.zly2006.zhihu.macos.debug.main"
            }
        }
    }

    sourceSets {
        commonMain.dependencies {
            implementation(project(":shared"))
            implementation(compose.runtime)
            implementation(compose.ui)
            implementation("org.jetbrains.compose.ui:ui-test:1.11.1")
            implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.11.0")
        }
    }
}

val debugBundle = layout.buildDirectory.dir("bin/macosArm64/debugApp/ZhihuPlusPlusUiDebug.app")
val appVersionName = providers.gradleProperty("app.versionName").get()
val appVersionCode = providers.gradleProperty("app.versionCode").get()
val sharedMacosResources =
    project(":shared").layout.buildDirectory.dir("kotlin-multiplatform-resources/aggregated-resources/macosArm64")

tasks.register<Sync>("packageDebugMacosUiDebug") {
    dependsOn(
        "linkDebugExecutableMacosArm64",
        ":app:prepareLibraryDefinitionsLiteDebug",
        ":shared:macosArm64AggregateResources",
    )
    into(debugBundle)
    from("src/macosMain/resources/Info.plist") {
        into("Contents")
        filter { line ->
            line
                .replace("@APP_VERSION_NAME@", appVersionName)
                .replace("@APP_VERSION_CODE@", appVersionCode)
        }
    }
    from(layout.buildDirectory.file("bin/macosArm64/debugExecutable/ZhihuPlusPlusUiDebug.kexe")) {
        into("Contents/MacOS")
        rename { "ZhihuPlusPlusUiDebug" }
    }
    from(rootProject.file("misc/emoji_mapping.json")) {
        into("Contents/Resources/misc")
    }
    from(rootProject.file("misc/emojis")) {
        into("Contents/Resources/misc/emojis")
    }
    from(rootProject.file("desktopApp/src/main/resources/desktop-icon.png")) {
        into("Contents/Resources")
    }
    from(rootProject.file("app/build/generated/aboutLibraries/liteDebug/res/raw/aboutlibraries.json")) {
        into("Contents/Resources")
    }
    from(sharedMacosResources) {
        into("Contents/Resources/compose-resources")
    }
    doLast {
        val bundle = debugBundle.get().asFile
        val sourceRoot = sharedMacosResources.get().asFile
        val packagedRoot = bundle.resolve("Contents/Resources/compose-resources")
        val resourceFiles = sourceRoot.walkTopDown().filter(File::isFile).toList()
        check(resourceFiles.isNotEmpty()) {
            "macOS Compose resources were not assembled"
        }
        check(resourceFiles.all { packagedRoot.resolve(it.relativeTo(sourceRoot)).isFile }) {
            "The background UI debugger bundle is missing Compose resources"
        }
        check(bundle.resolve("Contents/Resources/aboutlibraries.json").isFile) {
            "The background UI debugger bundle is missing aboutlibraries.json"
        }
        check(bundle.resolve("Contents/Resources/misc/emoji_mapping.json").isFile) {
            "The background UI debugger bundle is missing emoji_mapping.json"
        }
        providers
            .exec {
                commandLine(
                    "/usr/bin/codesign",
                    "--force",
                    "--deep",
                    "--sign",
                    "-",
                    "--timestamp=none",
                    bundle.absolutePath,
                )
            }.result
            .get()
            .assertNormalExitValue()
    }
}

tasks.register("verifyMacosReleaseHasNoUiDebugProtocol") {
    dependsOn(":macosApp:linkReleaseExecutableMacosArm64")
    doLast {
        val releaseBinary =
            project(":macosApp")
                .layout.buildDirectory
                .file("bin/macosArm64/releaseExecutable/ZhihuPlusPlus.kexe")
                .get()
                .asFile
        check(releaseBinary.isFile) {
            "macOS release binary does not exist: $releaseBinary"
        }
        val stringsExecution =
            providers.exec {
                commandLine("/usr/bin/strings", releaseBinary.absolutePath)
            }
        val stringsOutput = stringsExecution.standardOutput
        val strings = stringsOutput.asText.get()
        check("ZHPP_BACKGROUND_UI_DEBUG_V1" !in strings) {
            "The macOS release binary contains the debug UI protocol"
        }
    }
}
