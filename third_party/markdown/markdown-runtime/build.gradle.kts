import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    kotlin("multiplatform")
    kotlin("plugin.compose")
    id("com.android.kotlin.multiplatform.library")
}

kotlin {
    androidLibrary {
        namespace = "com.hrm.markdown.runtime"
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
            baseName = "MarkdownRuntime"
            isStatic = true
        }
    }

    sourceSets {
        commonMain.dependencies {
            api(project(":markdown-parser"))
            implementation("org.jetbrains.compose.runtime:runtime:1.11.1")
        }
    }
}
