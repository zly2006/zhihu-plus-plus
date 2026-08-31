/*
 * Copyright (c) 2026 huarangmeng
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    kotlin("multiplatform")
    kotlin("plugin.compose")
    id("com.android.kotlin.multiplatform.library")
    id("org.jetbrains.compose")
}

kotlin {
    androidLibrary {
        namespace = "com.hrm.codehigh.render"
        compileSdk = 37
        minSdk = 27
        compilerOptions.jvmTarget = JvmTarget.JVM_17
    }
    jvm {
        compilerOptions.jvmTarget = JvmTarget.JVM_17
    }
    iosArm64()
    iosSimulatorArm64()
    macosArm64()

    sourceSets.commonMain.dependencies {
        api(project(":codehighlight-parser"))
        implementation("org.jetbrains.compose.runtime:runtime:1.11.1")
        implementation("org.jetbrains.compose.foundation:foundation:1.11.1")
        implementation("org.jetbrains.compose.material3:material3:1.9.0")
        implementation("org.jetbrains.compose.ui:ui:1.11.1")
        implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.11.0")
    }
}
