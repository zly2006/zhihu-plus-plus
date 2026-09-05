rootProject.name = "zhihu_harmony_probe"

pluginManagement {
    repositories {
        maven("https://maven.eazytec-cloud.com/nexus/repository/maven-public/")
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositories {
        maven("https://maven.eazytec-cloud.com/nexus/repository/maven-public/")
    }
}

include(":probe")
include(":reader-checks")
include(":ktor-url-shim")

listOf(
    "markdown-parser", "markdown-runtime", "markdown-renderer",
    "latex-base", "latex-parser", "latex-renderer",
    "codehighlight-parser", "codehighlight-render",
).forEach { module ->
    include(":$module")
    project(":$module").projectDir = file("readers/$module")
    project(":$module").buildFileName = "../../reader-module.gradle.kts"
}
