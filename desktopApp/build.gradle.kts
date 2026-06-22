import org.gradle.jvm.tasks.Jar
import org.jetbrains.compose.desktop.application.dsl.TargetFormat
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

val appVersionName = property("app.versionName").toString()
val desktopPackageVersion = if (appVersionName.count { it == '.' } >= 2) appVersionName else "$appVersionName.0"

plugins {
    kotlin("jvm")
    id("org.jetbrains.compose")
    kotlin("plugin.compose")
    id("org.jlleitschuh.gradle.ktlint")
}

ktlint {
    outputToConsole.set(true)
    enableExperimentalRules.set(true)
    filter {
        exclude("**/generated/**")
        exclude("**/build/**")
    }
}

kotlin {
    jvmToolchain(21)
    compilerOptions {
        jvmTarget = JvmTarget.JVM_21
    }
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
}

// Force material3 to one version across app/shared/desktop.
// 根因：KMP 元数据配置和平台配置曾解析到不同 material3 版本，导致 commonMain 使用的
// internal API 在 Android/JVM 平台编译期不可见。
configurations.configureEach {
    resolutionStrategy {
        force("org.jetbrains.compose.material3:material3:1.12.0-alpha02")
    }
}

dependencies {
    implementation(projects.shared)
    implementation(compose.desktop.currentOs)
    // JavaFX WebView 用于桌面端内嵌风控验证页面。
    // JavaFX POM 使用 ${javafx.platform} classifier，Gradle 不会自动解析。
    // 使用 resolutionStrategy 强制所有 JavaFX 模块使用平台 classifier。
    val osName = System.getProperty("os.name").lowercase()
    val osArch = System.getProperty("os.arch").lowercase()
    val fxClassifier =
        when {
            osName.contains("mac") && (osArch == "aarch64" || osArch == "arm64") -> "mac-aarch64"
            osName.contains("mac") -> "mac"
            osName.contains("win") -> "win"
            osName.contains("linux") && (osArch == "aarch64" || osArch == "arm64") -> "linux-aarch64"
            else -> "linux"
        }
    val javafxModules = listOf("javafx-base", "javafx-controls", "javafx-graphics", "javafx-web", "javafx-swing", "javafx-media")
    javafxModules.forEach { module ->
        implementation("org.openjfx:$module:21.0.2:$fxClassifier")
    }
}

fun currentDesktopNativeExcludes(): List<String> {
    val osName = System.getProperty("os.name").lowercase()
    val osArch = System.getProperty("os.arch").lowercase()
    val isArm64 = osArch == "aarch64" || osArch == "arm64"
    val sqliteTarget =
        when {
            osName.contains("mac") && isArm64 -> "osx_arm64"
            osName.contains("mac") -> "osx_x64"
            osName.contains("win") -> "windows_x64"
            osName.contains("linux") && isArm64 -> "linux_arm64"
            else -> "linux_x64"
        }
    val skikoTarget =
        when {
            osName.contains("mac") && isArm64 -> "macos-arm64"
            osName.contains("mac") -> "macos-x64"
            osName.contains("win") -> "windows-x64"
            osName.contains("linux") && isArm64 -> "linux-arm64"
            else -> "linux-x64"
        }
    val sqliteNatives =
        listOf(
            "natives/linux_arm64/**",
            "natives/linux_x64/**",
            "natives/osx_arm64/**",
            "natives/osx_x64/**",
            "natives/windows_x64/**",
        )
    val skikoNatives =
        listOf(
            "libskiko-linux-arm64.so",
            "libskiko-linux-x64.so",
            "libskiko-macos-arm64.dylib",
            "libskiko-macos-x64.dylib",
            "skiko-windows-x64.dll",
        )

    return sqliteNatives.filterNot { it == "natives/$sqliteTarget/**" } +
        skikoNatives.filterNot { it.contains(skikoTarget) }
}

tasks.withType<Jar>().configureEach {
    if (name == "packageReleaseUberJarForCurrentOS") {
        exclude(currentDesktopNativeExcludes())
    }
}

compose.desktop {
    application {
        mainClass = "com.github.zly2006.zhihu.desktop.MainKt"

        buildTypes.release.proguard {
            isEnabled.set(true)
            optimize.set(false)
            configurationFiles.from(project.file("proguard-release.pro"))
        }

        nativeDistributions {
            targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb)
            packageName = "com.github.zly2006.zhihu"
            packageVersion = desktopPackageVersion
            linux {
                iconFile.set(project.file("src/main/resources/desktop-icon.png"))
            }
        }
    }
}
