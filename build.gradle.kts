// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    id("com.android.application") version "9.4.0" apply false
    id("com.android.library") version "9.4.0" apply false
    id("com.android.kotlin.multiplatform.library") version "9.4.0" apply false
    id("org.jetbrains.compose") version "1.11.1" apply false
    id("com.mikepenz.aboutlibraries.plugin.android") version "15.0.0" apply false
    kotlin("jvm") version "2.4.0" apply false
    kotlin("multiplatform") version "2.4.0" apply false
    kotlin("plugin.compose") version "2.4.0" apply false
    kotlin("plugin.serialization") version "2.4.0" apply false
    id("com.google.devtools.ksp") version "2.3.9" apply false
    id("org.jlleitschuh.gradle.ktlint") version "14.2.0" apply false
}

// 临时 pin（上游 org.tiqian snapshot 处于半套状态，修复后删除）：
// 上游 tiqian-cjk/tiqian-math 的 "Publish Tiqian Math Maven snapshots" 于 2026-09-13
// 两次 dispatch 均被取消（runs 34736481810、34737396432），只发布了半套产物：
// math-engine-jvm 已更新到 build 3，而 math-jvm-skia-jvm 仍是 2026-08-29 build 2。
// 旧 skia 调用的 MathLayoutEngine(face, List, MacroExpansionLimits, provider, ...)
// 构造器已在新 engine 中删除（改为 MathResourceLimits），导致 desktop 打包 ProGuard
// 报 1 个 unresolved reference（org.tiqian.math.font.skia.SkiaMathFormulaCapabilityKt）。
// 上游重新发布完整 snapshot 后删除此处 force，恢复跟随最新 snapshot。
allprojects {
    configurations.configureEach {
        resolutionStrategy {
            force("org.tiqian:math-engine-jvm:0.1.0-20260829.021300-2")
        }
    }
}
