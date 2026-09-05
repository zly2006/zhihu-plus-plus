import org.jetbrains.kotlin.gradle.dsl.JvmTarget

// P3 验证：CPF SQLDelight OH 变体（app.cash.sqldelight 2.2.1-1.0.0：runtime / native-driver /
// coroutines-extensions / sqlite-3-30-dialect）与同一 fork 版本线的 JVM 变体共用同一份 .sq schema。
plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.sqldelight)
}

sqldelight {
    databases {
        create("ContentFilterDb") {
            packageName.set("com.github.zly2006.zhihu.harmonyprobe.db.sqldelight")
            dialect("app.cash.sqldelight:sqlite-3-30-dialect:2.2.1-1.0.0")
            schemaOutputDirectory.set(file("schemas"))
        }
    }
}

kotlin {
    jvm {
        compilerOptions { jvmTarget = JvmTarget.JVM_17 }
    }
    ohosArm64()

    sourceSets {
        val jvmMain by getting {
            dependencies {
                api(libs.sqldelight.runtime.jvm)
                api(libs.sqldelight.coroutines.extensions.jvm)
                api(libs.kotlinx.coroutines.core)
                implementation(libs.sqldelight.sqlite.driver)
            }
        }
        val ohosArm64Main by getting {
            dependencies {
                api(libs.sqldelight.runtime.ohosarm64)
                api(libs.sqldelight.coroutines.extensions.ohosarm64)
                api(libs.kotlinx.coroutines.core)
                implementation(libs.sqldelight.native.driver.ohosarm64)
            }
        }
    }
}
