import org.jetbrains.kotlin.gradle.plugin.mpp.NativeBuildType

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.kotlinSerialization)
}

kotlin {
    listOf(
        ohosArm64(),
        ohosX64(),
    ).forEach { ohosTarget ->
        ohosTarget.binaries.sharedLib {
            baseName = "kn"
            if (buildType == NativeBuildType.RELEASE) {
                optimized = false
            }
            export(libs.compose.multiplatform.export)
            linkerOpts("-lz")

            val rendererBackend = rootProject.findProperty("rendererBackend")?.toString() ?: "skia"
            if (rendererBackend == "fusion-renderer") {
                linkerOpts(
                    "-lnative_drawing",
                    "-limage_source",
                    "-lpixelmap",
                    "-lpixelmap_ndk.z",
                    "-lnative_window",
                    "-lace_napi.z",
                    "-lhilog_ndk.z",
                    "-lhitrace_ndk.z",
                    "-luv",
                    "-lunwind",
                    "-licu",
                )
            }
        }
    }

    sourceSets {
        commonMain {
            kotlin.srcDir("../../shared/src/commonMain/kotlin")
            kotlin.include(
                "com/github/zly2006/zhihu/harmonyprobe/**",
                "com/github/zly2006/zhihu/editor/ZhihuImageUploadModels.kt",
                "com/github/zly2006/zhihu/navigation/ArticleTypeNavType.kt",
                "com/github/zly2006/zhihu/navigation/LocalNavigator.kt",
                "com/github/zly2006/zhihu/navigation/NavDestination.kt",
                "com/github/zly2006/zhihu/shared/data/OnlineHistory.kt",
                "com/github/zly2006/zhihu/shared/data/RecommendationMode.kt",
                "com/github/zly2006/zhihu/shared/data/DailyStory.kt",
                "com/github/zly2006/zhihu/shared/data/SegmentInfo.kt",
                "com/github/zly2006/zhihu/shared/nlp/NlpSupport.kt",
                "com/github/zly2006/zhihu/shared/theme/ThemeMode.kt",
                "com/github/zly2006/zhihu/shared/ui/AnswerDoubleTapAction.kt",
                "com/github/zly2006/zhihu/shared/ui/TopLevelReselectAction.kt",
                "com/github/zly2006/zhihu/shared/updater/SchematicVersion.kt",
                "com/github/zly2006/zhihu/theme/Color.kt",
                "com/github/zly2006/zhihu/theme/P1ThemeExpect.kt",
                "com/github/zly2006/zhihu/theme/ThemeManager.kt",
                "com/github/zly2006/zhihu/theme/Type.kt",
                "com/github/zly2006/zhihu/ui/components/AnswerSwitchSensitivity.kt",
                "com/github/zly2006/zhihu/util/SmoothGradient.kt",
                "com/github/zly2006/zhihu/shared/util/ZhidaSummary.kt",
                "com/github/zly2006/zhihu/shared/util/ZhihuPolicy.kt",
                "com/github/zly2006/zhihu/shared/util/ZseSigner.kt",
            )
            dependencies {
                implementation(project(":markdown-renderer"))
                implementation(compose.runtime)
                implementation(compose.foundation)
                implementation(compose.animation)
                implementation(compose.material3)
                implementation(compose.ui)
                implementation(libs.kotlinx.coroutines.core)
                implementation(libs.kotlinx.serialization.json)
                implementation(libs.atomicFu)
                implementation(libs.androidx.navigation.compose)
                implementation(libs.androidx.lifecycle.runtime.compose)
                implementation(libs.androidx.lifecycle.viewmodel.compose)
                implementation(libs.androidx.savedstate)
            }
        }

        val ohosMain = create("ohosMain") {
            dependsOn(commonMain.get())
            dependencies {
                api(libs.compose.multiplatform.export)
            }
        }
        val ohosArm64Main by getting {
            dependsOn(ohosMain)
            kotlin.srcDir("../../shared/src/commonMain/kotlin")
            kotlin.include(
                "com/github/zly2006/zhihu/harmonyprobe/**",
                "com/github/zly2006/zhihu/shared/data/ZhihuDailyClient.kt",
            )
            dependencies {
                implementation(libs.ktor.client.cio)
                implementation(libs.ktor.client.content.negotiation)
                implementation(libs.ktor.serialization.kotlinx.json)
                implementation("io.coil-kt.coil3:coil-compose:3.3.0-1.0.0")
                implementation("io.coil-kt.coil3:coil-network-ktor3:3.3.0-1.0.0")
                // P3 数据库选型：CPF SQLDelight 只有 ohosArm64 变体，只在 arm64 编译接入。
                // CPF Room3 OH 变体缺配套 compiler（见 P3-VALIDATION.md），不参与 OHOS 编译。
                implementation(project(":db-sqldelight"))
            }
        }
        val ohosX64Main by getting {
            dependsOn(ohosMain)
            dependencies {
                // CPF Ktor 没有 ohosX64 变体；用本地 io.ktor.http 垫片 klib 让共享 NavDestination.kt 通过 x64 编译。
                implementation(project(":ktor-url-shim"))
            }
        }
    }
}

// CPF 的 OHOS 变体版本带 "-OH.x" 修饰符，Gradle 语义比较可能把它排在无修饰符版本之后，
// 而 navigation/lifecycle/savedstate 的无修饰符版本没有 ohos klib，必须强制锁定 OH 变体。
configurations.all {
    resolutionStrategy.force(
        "org.jetbrains.androidx.navigation:navigation-compose:2.9.4-OH.0.1.2-37",
        "org.jetbrains.androidx.navigation:navigation-runtime:2.9.4-OH.0.1.2-37",
        "org.jetbrains.androidx.navigation:navigation-common:2.9.4-OH.0.1.2-37",
        "org.jetbrains.androidx.lifecycle:lifecycle-runtime-compose:2.9.4-OH.0.1.2-37",
        "org.jetbrains.androidx.lifecycle:lifecycle-viewmodel:2.9.4-OH.0.1.2-37",
        "org.jetbrains.androidx.lifecycle:lifecycle-viewmodel-compose:2.9.4-OH.0.1.2-37",
        "org.jetbrains.androidx.savedstate:savedstate:1.3.3-OH.0.1.2-37",
    )
}

arrayOf("debug", "release").forEach { buildType ->
    val taskSuffix = buildType.replaceFirstChar(Char::uppercaseChar)
    tasks.register<Copy>("publish${taskSuffix}BinariesToHarmonyApp") {
        group = "harmony"
        dependsOn(
            "link${taskSuffix}SharedOhosArm64",
            "link${taskSuffix}SharedOhosX64",
        )
        duplicatesStrategy = DuplicatesStrategy.INCLUDE
        into(rootProject.file("harmonyApp"))
        from("build/bin/ohosArm64/${buildType}Shared/libkn_api.h") {
            into("entry/src/main/cpp/include/arm64-v8a/")
        }
        from("build/bin/ohosArm64/${buildType}Shared/libkn.so") {
            into("entry/libs/arm64-v8a/")
        }
        from("build/bin/ohosX64/${buildType}Shared/libkn_api.h") {
            into("entry/src/main/cpp/include/x86_64/")
        }
        from("build/bin/ohosX64/${buildType}Shared/libkn.so") {
            into("entry/libs/x86_64/")
        }
    }
}
