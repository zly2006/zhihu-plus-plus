import org.jetbrains.kotlin.gradle.dsl.JvmTarget

// P3 验证：CPF Room3 的 JVM 线（上游 androidx.room3 3.0.0-alpha01 + 上游 sqlite 2.7.0-alpha01）。
//
// 重要：CPF 的 room3 OH 变体（room3-runtime-ohosarm64 3.0.0-alpha01-0.3.0）把 RoomOpenDelegate 与
// sqlite API 改成了非 suspend 形态，但配套的 room3-compiler 未发布（上游 alpha01~3.0.2 生成 suspend
// 代码，均无法在 OH 变体上编译，见 P3-VALIDATION.md）。因此在选型结论落地前，Room3 仅参与宿主机
// JVM 验证与格式兼容性实验，不进入 OHOS 编译。
plugins {
    alias(libs.plugins.kotlinJvm)
    alias(libs.plugins.ksp)
}

kotlin {
    compilerOptions {
        jvmTarget = JvmTarget.JVM_17
        freeCompilerArgs.add("-opt-in=kotlin.time.ExperimentalTime")
    }
}

dependencies {
    api(libs.room3.runtime)
    api(libs.sqlite.bundled.jvm.upstream)
    api(libs.kotlinx.coroutines.core)
    // 编译器版本可用 -Proom3CompilerVersion=x 覆盖；P3 期间逐一试探过 alpha02~3.0.2，均与 OH 变体不匹配。
    ksp("androidx.room3:room3-compiler:3.0.0-alpha01")
}

ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
    arg("room.incremental", "true")
}
