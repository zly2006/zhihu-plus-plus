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
val sharedMacosResources =
    project(":shared").layout.buildDirectory.dir("kotlin-multiplatform-resources/aggregated-resources/macosArm64")

listOf("Debug", "Release").forEach { buildType ->
    val buildTypeDirectory = buildType.lowercase()
    val appDirectory = layout.buildDirectory.dir("bin/macosArm64/${buildTypeDirectory}App/Zhihu++.app")

    val syncApp =
        tasks.register<Sync>("sync${buildType}MacosApp") {
            dependsOn(
                "link${buildType}ExecutableMacosArm64",
                ":app:prepareLibraryDefinitionsLiteDebug",
                ":shared:macosArm64AggregateResources",
            )
            into(appDirectory)
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
            from(layout.buildDirectory.file("bin/macosArm64/${buildTypeDirectory}Executable/ZhihuPlusPlus.kexe")) {
                into("Contents/MacOS")
                rename("ZhihuPlusPlus\\.kexe", "ZhihuPlusPlus")
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
    val validateAndSignApp =
        tasks.register<ValidateAndSignMacosApp>("validateAndSign${buildType}MacosApp") {
            dependsOn(syncApp)
            appBundle.set(appDirectory)
            composeResources.set(sharedMacosResources)
            requiredBundleFiles.set(
                listOf(
                    "Contents/Info.plist",
                    "Contents/MacOS/ZhihuPlusPlus",
                    "Contents/Resources/aboutlibraries.json",
                    "Contents/Resources/misc/emoji_mapping.json",
                ),
            )
        }
    tasks.register("package${buildType}MacosApp") {
        dependsOn(validateAndSignApp)
    }
}
