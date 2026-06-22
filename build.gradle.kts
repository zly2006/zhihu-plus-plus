// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    id("com.android.application") version "9.2.1" apply false
    id("com.android.library") version "9.2.1" apply false
    id("com.android.kotlin.multiplatform.library") version "9.2.1" apply false
    id("org.jetbrains.compose") version "1.11.1" apply false
    id("com.mikepenz.aboutlibraries.plugin.android") version "15.0.0" apply false
    kotlin("jvm") version "2.4.0" apply false
    kotlin("multiplatform") version "2.4.0" apply false
    kotlin("plugin.compose") version "2.4.0" apply false
    kotlin("plugin.serialization") version "2.4.0" apply false
    id("com.google.devtools.ksp") version "2.3.9" apply false
    id("org.jlleitschuh.gradle.ktlint") version "14.2.0" apply false
}
