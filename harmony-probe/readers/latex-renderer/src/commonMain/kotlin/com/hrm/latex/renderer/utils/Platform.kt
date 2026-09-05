package com.hrm.latex.renderer.utils

// Compatibility extension of the upstream MIT-licensed platform enum (huarangmeng, 2026).
enum class PlatformType { ANDROID, IOS, JVM, JS, WASM, OHOS }

expect fun getCurrentPlatform(): PlatformType

fun isMobilePlatform(): Boolean = getCurrentPlatform() in setOf(PlatformType.ANDROID, PlatformType.IOS, PlatformType.OHOS)
