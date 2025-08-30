// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    id("com.android.application") version "8.11.0" apply false
    kotlin("android") version "2.1.21" apply false
    kotlin("plugin.compose") version "2.1.21" apply false
    kotlin("plugin.serialization") version "2.1.21" apply false
    id("com.google.devtools.ksp") version "2.1.21-2.0.2" apply false
    id("org.jlleitschuh.gradle.ktlint") version "13.0.0" apply false
}
