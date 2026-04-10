// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    id("com.android.application") version "8.13.2" apply false
    id("com.android.library") version "8.13.2" apply false
    kotlin("android") version "2.3.10" apply false
    kotlin("plugin.compose") version "2.3.10" apply false
    kotlin("plugin.serialization") version "2.3.10" apply false
    id("com.google.devtools.ksp") version "2.3.2" apply false
    id("org.jlleitschuh.gradle.ktlint") version "14.2.0" apply false
}
