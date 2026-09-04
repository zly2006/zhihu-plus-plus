/*
 * Zhihu++ - Free & Ad-Free Zhihu client for Android.
 * Copyright (C) 2024-2026, zly2006 <i@zly2006.me>
 *
 * Licensed under AGPL-3.0-only.
 */

package com.github.zly2006.zhihu.theme

/**
 * UI 风格枚举。
 *
 * - [Material3]：项目原有的 Material 3 Expressive 风格。
 * - [Miuix]：基于 compose-miuix-ui 的类 HyperOS 风格。
 *
 * 在 [ThemeManager] 中持久化为字符串。切换后由顶层 [ZhihuTheme] 分流到不同的内部主题。
 */
enum class ThemeStyle {
    Material3,
    Miuix,
    ;

    companion object {
        fun fromValueOrDefault(value: String?): ThemeStyle =
            entries.find { it.name == value } ?: Material3
    }
}
