import org.jetbrains.kotlin.gradle.dsl.JvmTarget

// P3 验证的 legacy fixture：用生产 Android/Desktop 的 Room 2.8.4 + sqlite-bundled 2.6.2 版本线，
// 直接编译 shared-local-db 的 7 个实体源文件（不复制），在宿主机上产出与生产一致的 v6 数据库文件，
// 作为「能否维持现有数据库格式」的基准。仅 JVM，不参与 OHOS 编译。
plugins {
    alias(libs.plugins.kotlinJvm)
    alias(libs.plugins.kotlinSerialization)
    alias(libs.plugins.ksp)
}

kotlin {
    compilerOptions {
        jvmTarget = JvmTarget.JVM_17
        freeCompilerArgs.add("-opt-in=kotlin.time.ExperimentalTime")
    }

    // 生产实体源文件同步到 build 目录后作为额外 srcDir 引入（include 过滤只作用于拷贝目录，
    // 不影响 src/main/kotlin 中的 fixture 源码）。
    val copyLegacyEntities = tasks.register<Sync>("copyLegacyEntities") {
        from(rootProject.file("../shared-local-db/src/commonMain/kotlin"))
        include(
            "com/github/zly2006/zhihu/viewmodel/filter/BlockedKeyword.kt",
            "com/github/zly2006/zhihu/viewmodel/filter/BlockedUser.kt",
            "com/github/zly2006/zhihu/viewmodel/filter/BlockedTopic.kt",
            "com/github/zly2006/zhihu/viewmodel/filter/BlockedContentRecord.kt",
            "com/github/zly2006/zhihu/viewmodel/filter/BlockedFeedRecord.kt",
            "com/github/zly2006/zhihu/viewmodel/filter/ContentOpenEvent.kt",
            "com/github/zly2006/zhihu/viewmodel/filter/ContentViewRecord.kt",
        )
        into(layout.buildDirectory.dir("legacyEntities"))
    }

    sourceSets {
        main {
            kotlin.srcDir(copyLegacyEntities)
        }
    }
}

dependencies {
    api(libs.room2.runtime)
    implementation(libs.sqlite2.bundled)
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.serialization.json)
    ksp(libs.room2.compiler)
}

ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
    arg("room.incremental", "true")
}
