/*
 * Zhihu++ - Free & Ad-Free Zhihu client for all platforms.
 * Copyright (C) 2024-2026, zly2006 <i@zly2006.me>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation (version 3 only).
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.github.zly2006.zhihu.theme

import androidx.compose.runtime.staticCompositionLocalOf

const val UI_STYLE_PREFERENCE_KEY = "ui_style"

/**
 * 统一的界面风格枚举。
 *
 * - [Material3]：默认 Material 3 Expressive 风格。
 * - [Miuix]：基于 compose-miuix-ui 的类 HyperOS 风格（KMP 全平台可用）。
 * - [LiquidGlass]：基于 Android Liquid Glass 的苹果风格玻璃材质（当前仅 Android 13+ 支持）。
 */
enum class UiStyle(
    val prefValue: String,
    val displayName: String,
) {
    Material3("material", "Material 3"),
    Miuix("miuix", "Miuix"),
    LiquidGlass("liquid_glass", "Liquid Glass"),
    ;

    val isSupported: Boolean
        get() = when (this) {
            Material3, Miuix -> true
            LiquidGlass -> isLiquidGlassSupported
        }

    fun resolveEffective(): UiStyle = if (isSupported) this else Material3

    companion object {
        fun fromValueOrDefault(value: String?): UiStyle {
            if (value.isNullOrBlank()) return Material3
            return entries.firstOrNull {
                it.prefValue.equals(value, ignoreCase = true) ||
                    it.name.equals(value, ignoreCase = true)
            } ?: Material3
        }
    }
}

/** 当前窗口生效的视觉风格（已按平台能力降级）。 */
val LocalUiStyle = staticCompositionLocalOf { UiStyle.Material3 }
