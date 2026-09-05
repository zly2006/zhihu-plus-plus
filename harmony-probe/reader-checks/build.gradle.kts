plugins {
    alias(libs.plugins.kotlinMultiplatform)
}

kotlin {
    jvm()
    sourceSets {
        commonMain {
            kotlin.srcDir("../probe/src/commonMain/kotlin")
            kotlin.include("**/P2Markdown.kt")
            dependencies { implementation(project(":markdown-parser")) }
        }
        commonTest.dependencies { implementation(kotlin("test")) }
    }
}
