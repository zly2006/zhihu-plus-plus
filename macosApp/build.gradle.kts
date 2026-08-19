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
            executable {
                baseName = "ZhihuPlusPlus"
                entryPoint = "com.github.zly2006.zhihu.macos.main"
            }
        }
    }

    sourceSets {
        commonMain.dependencies {
            implementation(project(":shared"))
            implementation(compose.runtime)
            implementation(compose.material3)
            implementation(compose.ui)
        }
        commonTest.dependencies {
            implementation(kotlin("test"))
        }
    }
}

val appVersionName = providers.gradleProperty("app.versionName").get()
val appVersionCode = providers.gradleProperty("app.versionCode").get()

listOf("Debug", "Release").forEach { buildType ->
    val buildTypeDirectory = buildType.lowercase()
    val appDirectory = layout.buildDirectory.dir("bin/macosArm64/${buildTypeDirectory}App/Zhihu++.app")

    tasks.register<Sync>("package${buildType}MacosApp") {
        dependsOn("link${buildType}ExecutableMacosArm64", ":app:prepareLibraryDefinitionsLiteDebug")
        into(appDirectory)
        from("src/macosMain/resources/Info.plist") {
            into("Contents")
            filter { line ->
                line
                    .replace("@APP_VERSION_NAME@", appVersionName)
                    .replace("@APP_VERSION_CODE@", appVersionCode)
            }
        }
        from(layout.buildDirectory.file("bin/macosArm64/${buildTypeDirectory}Executable/ZhihuPlusPlus.kexe")) {
            into("Contents/MacOS")
            rename { "ZhihuPlusPlus" }
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
        doLast {
            providers
                .exec {
                    commandLine(
                        "/usr/bin/codesign",
                        "--force",
                        "--deep",
                        "--sign",
                        "-",
                        "--timestamp=none",
                        appDirectory.get().asFile.absolutePath,
                    )
                }.result
                .get()
                .assertNormalExitValue()
            providers
                .exec {
                    commandLine(
                        "/usr/bin/codesign",
                        "--verify",
                        "--deep",
                        "--strict",
                        appDirectory.get().asFile.absolutePath,
                    )
                }.result
                .get()
                .assertNormalExitValue()
        }
    }
}
