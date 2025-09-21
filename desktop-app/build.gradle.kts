plugins {
    kotlin("jvm")
    kotlin("plugin.serialization")
    application
}

dependencies {
    // 依赖本地推荐库
    implementation(project(":local-recommendation-lib"))

    // Kotlin标准库
    implementation("org.jetbrains.kotlin:kotlin-stdlib")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3")

    // 命令行参数解析
    implementation("org.jetbrains.kotlinx:kotlinx-cli:0.3.6")

    // 日志
    implementation("ch.qos.logback:logback-classic:1.4.11")

    // 测试
    testImplementation("org.jetbrains.kotlin:kotlin-test")
}

application {
    mainClass.set("com.github.zly2006.zhihu.desktop.MainKt")
}

kotlin {
    jvmToolchain(17)
}
