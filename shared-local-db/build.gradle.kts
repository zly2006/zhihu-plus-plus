import org.jetbrains.kotlin.gradle.dsl.JvmTarget
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
        exclude { it.file.absolutePath.contains("/build/generated/") }
    }
}

// ktlint 的 filter/exclude 拦不住 KSP 产物：Room 生成的 *_Impl.kt 是通过 source set 加进来的，
// 而上面基于 "/build/generated/" 的路径判断在 Windows 上永远不成立（absolutePath 用反斜杠）。
// 和 shared 模块一样，直接把格式化任务的源限定成手写代码，并关掉对应的报告任务。
tasks.withType<KtLintFormatTask>().configureEach {
    exclude("**/generated/**")
    exclude("**/ksp/**")
}

tasks
    .withType<GenerateReportsTask>()
    .matching { it.name in setOf("ktlintAndroidMainSourceSetFormat", "ktlintJvmMainSourceSetFormat") }
    .configureEach {
        enabled = false
    }

mapOf(
    "runKtlintFormatOverAndroidMainSourceSet" to "src/androidMain/kotlin",
    "runKtlintFormatOverJvmMainSourceSet" to "src/jvmMain/kotlin",
    "runKtlintFormatOverCommonMainSourceSet" to "src/commonMain/kotlin",
    "runKtlintFormatOverNativeMainSourceSet" to "src/nativeMain/kotlin",
).forEach { (taskName, sourcePath) ->
    tasks.withType<KtLintFormatTask>().matching { it.name == taskName }.configureEach {
        setSource(
            fileTree(sourcePath) {
                include("**/*.kt")
            },
        )
    }
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
