import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    kotlin("multiplatform")
    kotlin("plugin.compose")
    id("com.android.kotlin.multiplatform.library")
    id("org.jetbrains.compose")
}

kotlin {
    androidLibrary {
        namespace = "com.hrm.markdown.renderer"
        compileSdk = 37
        minSdk = 27

        compilerOptions {
            jvmTarget = JvmTarget.JVM_17
        }
        optimization {
            consumerKeepRules.publish = true
            consumerKeepRules.files.add(project.file("consumer-rules.pro"))
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
            baseName = "MarkdownRenderer"
            isStatic = true
        }
    }

    sourceSets {
        commonMain.dependencies {
            api(project(":markdown-parser"))
            api(project(":markdown-runtime"))

            implementation("org.jetbrains.compose.runtime:runtime:1.11.1")
            implementation("org.jetbrains.compose.foundation:foundation:1.11.1")
            implementation("org.jetbrains.compose.material3:material3:1.10.0-alpha05")
            implementation("org.jetbrains.compose.ui:ui:1.11.1")
            implementation("org.jetbrains.compose.components:components-resources:1.11.1")

            implementation("io.github.zly2006:latex-base:0.0.1-alpha3")
            implementation("io.github.zly2006:latex-parser:0.0.1-alpha3")
            implementation("io.github.zly2006:latex-renderer:0.0.1-alpha3")
            implementation("io.github.huarangmeng:codehighlight-parser:1.1.1")
            implementation("io.github.huarangmeng:codehighlight-render:1.1.1")

            implementation("io.coil-kt.coil3:coil-compose:3.5.0")
            implementation("io.coil-kt.coil3:coil-network-ktor3:3.5.0")
        }
        androidMain.dependencies {
            implementation("io.ktor:ktor-client-android:3.5.0")
        }
        iosMain.dependencies {
            implementation("io.ktor:ktor-client-darwin:3.5.0")
        }
        jvmMain.dependencies {
            implementation("io.ktor:ktor-client-java:3.5.0")
        }
    }
}
