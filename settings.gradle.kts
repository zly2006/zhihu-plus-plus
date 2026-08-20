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

pluginManagement {
    repositories {
        google {
            mavenContent {
                includeGroupAndSubgroups("androidx")
                includeGroupAndSubgroups("com.android")
                includeGroupAndSubgroups("com.google")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}

buildCache {
    local {
        isEnabled = true
        directory = File(rootDir, ".gradle/build-cache")
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google {
            mavenContent {
                includeGroupAndSubgroups("androidx")
                includeGroupAndSubgroups("com.android")
                includeGroupAndSubgroups("com.google")
            }
        }
        mavenCentral()
        maven("https://www.jitpack.io")
    }
}

plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

rootProject.name = "Zhihu"
include(":app")
include(":desktopApp")
include(":macosApp")
include(":macosUiDebug")
include(":sentence_embeddings")
include(":shared")
include(":shared-local-db")
include(":markdown-parser")
include(":markdown-renderer")
include(":markdown-runtime")
include(":latex-base")
include(":latex-parser")
include(":latex-renderer")
include(":codehighlight-parser")
include(":codehighlight-render")

project(":markdown-parser").projectDir = file("third_party/markdown/markdown-parser")
project(":markdown-renderer").projectDir = file("third_party/markdown/markdown-renderer")
project(":markdown-runtime").projectDir = file("third_party/markdown/markdown-runtime")
project(":latex-base").projectDir = file("third_party/latex/latex-base")
project(":latex-parser").projectDir = file("third_party/latex/latex-parser")
project(":latex-renderer").projectDir = file("third_party/latex/latex-renderer")
project(":codehighlight-parser").projectDir = file("third_party/codehigh/codehighlight-parser")
project(":codehighlight-render").projectDir = file("third_party/codehigh/codehighlight-render")
