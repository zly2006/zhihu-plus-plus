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
        mavenLocal {
            content {
                includeGroupAndSubgroups("io.github.zly2006")
                includeGroupAndSubgroups("io.github.huarangmeng")
            }
        }
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
include(":sentence_embeddings")
include(":shared")
include(":shared-local-db")
include(":markdown-parser")
include(":markdown-renderer")
include(":markdown-runtime")

project(":markdown-parser").projectDir = file("third_party/markdown/markdown-parser")
project(":markdown-renderer").projectDir = file("third_party/markdown/markdown-renderer")
project(":markdown-runtime").projectDir = file("third_party/markdown/markdown-runtime")
