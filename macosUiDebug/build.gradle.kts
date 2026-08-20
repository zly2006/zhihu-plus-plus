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

import buildlogic.ValidateAndSignMacosApp
import org.apache.tools.ant.filters.ReplaceTokens
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

val syncDebugApp =
    tasks.register<Sync>("syncDebugMacosUiDebug") {
        dependsOn(
            "linkDebugExecutableMacosArm64",
            ":app:prepareLibraryDefinitionsLiteDebug",
            ":shared:macosArm64AggregateResources",
        )
        into(debugBundle)
        from("src/macosMain/resources/Info.plist") {
            into("Contents")
            filter(
                ReplaceTokens::class,
                "tokens" to
                    mapOf(
                        "APP_VERSION_NAME" to appVersionName,
                        "APP_VERSION_CODE" to appVersionCode,
                    ),
            )
        }
        from(layout.buildDirectory.file("bin/macosArm64/debugExecutable/ZhihuPlusPlusUiDebug.kexe")) {
            into("Contents/MacOS")
            rename("ZhihuPlusPlusUiDebug\\.kexe", "ZhihuPlusPlusUiDebug")
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
    }

val validateAndSignDebugApp =
    tasks.register<ValidateAndSignMacosApp>("validateAndSignDebugMacosUiDebug") {
        dependsOn(syncDebugApp)
        appBundle.set(debugBundle)
        composeResources.set(sharedMacosResources)
        requiredBundleFiles.set(
            listOf(
                "Contents/Info.plist",
                "Contents/MacOS/ZhihuPlusPlusUiDebug",
                "Contents/Resources/aboutlibraries.json",
                "Contents/Resources/misc/emoji_mapping.json",
            ),
        )
    }

tasks.register("packageDebugMacosUiDebug") {
    dependsOn(validateAndSignDebugApp)
}

tasks.register<Exec>("verifyMacosReleaseIsolation") {
    dependsOn(":macosApp:linkReleaseExecutableMacosArm64")
    val releaseBinary =
        project(":macosApp")
            .layout.buildDirectory
            .file("bin/macosArm64/releaseExecutable/ZhihuPlusPlus.kexe")
            .get()
            .asFile
            .absolutePath
    inputs.file(releaseBinary)
    commandLine(
        "/bin/bash",
        "-euo",
        "pipefail",
        "-c",
        """
        test -f "${'$'}1"
        if /usr/bin/strings "${'$'}1" | /usr/bin/grep -Eq 'ZHPP_BACKGROUND_UI_DEBUG_V1|--smoke-test|MacosAppSmokeTest|native-unhandled-exception-(smoke-)?test'; then
          echo "macOS release binary contains debug or smoke-test controls" >&2
          exit 1
        fi
        """.trimIndent(),
        "verifyMacosReleaseIsolation",
        releaseBinary,
    )
}
