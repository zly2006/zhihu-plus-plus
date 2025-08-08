@file:Suppress("UnstableApiUsage")

pluginManagement {
    repositories {
        maven {
            url = uri("https://mirrors.cloud.tencent.com/nexus/repository/maven-public/")
            isAllowInsecureProtocol = true
        }
        mavenCentral()

        // 阿里云 Google Maven 代理
        maven {
            url = uri("https://mirrors.cloud.tencent.com/repository/google")
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }

        gradlePluginPortal()

        maven {
            url = uri("https://jitpack.io")
        }
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        maven {
            url = uri("https://mirrors.cloud.tencent.com/nexus/repository/maven-public/")
            isAllowInsecureProtocol = true
        }
        mavenCentral()
        // 替代 google()
        maven {
            url = uri("https://mirrors.cloud.tencent.com/repository/google")
        }

    }
}

rootProject.name = "Zhihu"
include(":app")
