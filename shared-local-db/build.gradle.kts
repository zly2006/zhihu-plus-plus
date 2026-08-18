import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jlleitschuh.gradle.ktlint.tasks.BaseKtLintCheckTask
import org.jlleitschuh.gradle.ktlint.tasks.GenerateReportsTask
import org.jlleitschuh.gradle.ktlint.tasks.KtLintFormatTask

plugins {
    kotlin("multiplatform")
    kotlin("plugin.serialization")
    id("com.android.kotlin.multiplatform.library")
    id("com.google.devtools.ksp")
    id("org.jlleitschuh.gradle.ktlint")
}

ktlint {
    android.set(true)
    outputToConsole.set(true)
    enableExperimentalRules.set(true)
    filter {
        exclude("**/generated/**")
        exclude("**/build/**")
        exclude("build/generated/**")
        exclude("**/build/generated/ksp/**")
        exclude("**/ksp/**")
        exclude { it.file.invariantSeparatorsPath.contains("/build/generated/") }
    }
}

// ktlint 的 filter{} 只作用于插件自己的 PatternFilterable，拦不住通过 source set 加进来的
// Room KSP 产物（*_Impl.kt）。注意用 invariantSeparatorsPath：absolutePath 在 Windows 上是反斜杠，
// "/build/generated/" 永远匹配不到，于是同一份配置在 Linux CI 上碰巧通过、在 Windows 上报几千条。
tasks.withType<BaseKtLintCheckTask>().configureEach {
    exclude { it.file.invariantSeparatorsPath.contains("/build/generated/") }
}

// format 侧的 worker 任务连 exclude 都拦不住（源由插件按 source set 直接注入），
// 只能把源改写成手写代码目录；对应的报告任务随之关闭，否则它仍会读旧产物报错。
mapOf(
    "AndroidMainSourceSet" to "src/androidMain/kotlin",
    "JvmMainSourceSet" to "src/jvmMain/kotlin",
    "CommonMainSourceSet" to "src/commonMain/kotlin",
    "NativeMainSourceSet" to "src/nativeMain/kotlin",
).forEach { (sourceSet, sourcePath) ->
    tasks.withType<KtLintFormatTask>().matching { it.name == "runKtlintFormatOver$sourceSet" }.configureEach {
        setSource(
            fileTree(sourcePath) {
                include("**/*.kt")
            },
        )
    }
}

tasks
    .withType<GenerateReportsTask>()
    .matching { it.name.endsWith("SourceSetFormat") }
    .configureEach {
        enabled = false
    }

ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
    arg("room.incremental", "true")
}

kotlin {
    compilerOptions {
        freeCompilerArgs.add("-Xexpect-actual-classes")
    }

    androidLibrary {
        namespace = "com.github.zly2006.zhihu.shared.localdb"
        compileSdk = 37
        minSdk = 27

        compilerOptions {
            jvmTarget = JvmTarget.JVM_17
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
            baseName = "SharedLocalDb"
            isStatic = true
        }
    }

    sourceSets {
        commonMain.dependencies {
            api("androidx.room:room-runtime:2.8.4")
            api("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.11.0")
            implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.11.0")
        }
        commonTest.dependencies {
            implementation(kotlin("test"))
        }
        jvmMain.dependencies {
            implementation("androidx.sqlite:sqlite-bundled:2.6.2")
        }
    }
}

dependencies {
    add("kspAndroid", "androidx.room:room-compiler:2.8.4")
    add("kspJvm", "androidx.room:room-compiler:2.8.4")
}
