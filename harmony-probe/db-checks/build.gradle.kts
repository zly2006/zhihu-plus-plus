import org.jetbrains.kotlin.gradle.dsl.JvmTarget

// P3 双栈对比的宿主机验证入口：同一批测试场景分别跑 CPF Room3 与 CPF SQLDelight（JVM 变体），
// 并用 db-legacy-room2 产出的生产 Room 2.8.4 数据库文件做格式兼容性实验。
plugins {
    alias(libs.plugins.kotlinJvm)
}

kotlin {
    compilerOptions { jvmTarget = JvmTarget.JVM_17 }
}

dependencies {
    implementation(project(":db-room3"))
    implementation(project(":db-sqldelight"))
    implementation(project(":db-legacy-room2"))
    implementation(libs.sqldelight.sqlite.driver)
    implementation(libs.kotlinx.coroutines.core)
    testImplementation(kotlin("test"))
    testImplementation(libs.kotlinx.coroutines.core)
}
