import org.jetbrains.kotlin.gradle.tasks.KotlinCompilationTask

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
}

// Recompile the exact sources used by Android, without consuming their non-OHOS binaries.
val upstreamGroup = if (name.startsWith("codehighlight")) "io.github.huarangmeng" else "io.github.zly2006"
val upstreamVersion = when {
    name.startsWith("markdown") -> "0.0.1-alpha.11"
    name.startsWith("latex") -> "1.4.6-zly"
    else -> "1.1.1"
}
val upstreamSources by configurations.creating {
    isTransitive = false
}
dependencies {
    upstreamSources("$upstreamGroup:$name:$upstreamVersion:sources@jar")
}
val unpackSources = tasks.register<Sync>("unpackUpstreamSources") {
    from(provider { zipTree(upstreamSources.singleFile) })
    into(layout.buildDirectory.dir("upstream"))
    include("commonMain/com/**")
    if (project.name == "latex-renderer") {
        include("iosMain/com/hrm/latex/renderer/export/**")
        include("iosMain/com/hrm/latex/renderer/utils/GlyphBoundsProvider.ios.kt")
        exclude("commonMain/com/hrm/latex/renderer/utils/Platform.kt")
    }
    if (project.name == "markdown-renderer") {
        exclude("commonMain/com/hrm/markdown/renderer/MarkdownImage.kt")
    }
}
tasks.withType<KotlinCompilationTask<*>>().configureEach { dependsOn(unpackSources) }

kotlin {
    if (project.name == "markdown-parser") jvm()
    ohosArm64()
    ohosX64()
    sourceSets {
        commonMain {
            kotlin.srcDir(layout.buildDirectory.dir("upstream/commonMain"))
            dependencies {
                implementation(compose.runtime)
                implementation(libs.kotlinx.coroutines.core)
                if (project.name.endsWith("renderer") || project.name == "codehighlight-render") {
                    implementation(compose.foundation)
                    implementation(compose.material3)
                    implementation(compose.ui)
                    implementation(compose.components.resources)
                }
                when (project.name) {
                    "latex-parser" -> api(project(":latex-base"))
                    "latex-renderer" -> api(project(":latex-parser"))
                    "codehighlight-render" -> api(project(":codehighlight-parser"))
                    "markdown-runtime" -> api(project(":markdown-parser"))
                    "markdown-renderer" -> {
                        api(project(":markdown-runtime"))
                        api(project(":latex-renderer"))
                        api(project(":codehighlight-render"))
                    }
                }
            }
        }
        val ohosMain by creating {
            dependsOn(commonMain.get())
            if (project.name == "latex-renderer") {
                // These two upstream implementations use Skia only, not Foundation/iOS APIs.
                kotlin.srcDir(layout.buildDirectory.dir("upstream/iosMain"))
            }
        }
        ohosArm64Main.get().dependsOn(ohosMain)
        ohosX64Main.get().dependsOn(ohosMain)
    }
}
