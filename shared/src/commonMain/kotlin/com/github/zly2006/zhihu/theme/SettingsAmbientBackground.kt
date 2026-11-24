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

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import kotlin.math.max

/**
 * Liquid Glass 设置与账号页的柔和多色环境背景。
 *
 * 背景绘制在页面内容之前，独立于玻璃模糊管线：分组设置卡片保持不透明以保证可读性，
 * 顶部/底部玻璃控件通过 Scaffold 录制的 backdrop 采样该多色底板。
 */
@Composable
fun Modifier.settingsAmbientBackground(): Modifier {
    val dark = ThemeManager.isDarkTheme()
    val base = if (dark) Color(0xFF15151A) else Color(0xFFF6F5F8)
    val iceBlue = if (dark) Color(0xFF20364C) else Color(0xFFDCEEFF)
    val lavender = if (dark) Color(0xFF33253F) else Color(0xFFECE1F7)
    val peach = if (dark) Color(0xFF422A28) else Color(0xFFF9DED5)

    return drawWithCache {
        val canvasRadius = max(size.width, size.height).coerceAtLeast(1f)
        val iceBlueBrush = Brush.radialGradient(
            colorStops = arrayOf(0f to iceBlue, 1f to Color.Transparent),
            center = Offset(size.width * 0.12f, size.height * 0.08f),
            radius = canvasRadius * 0.78f,
        )
        val lavenderBrush = Brush.radialGradient(
            colorStops = arrayOf(0f to lavender, 1f to Color.Transparent),
            center = Offset(size.width * 0.94f, size.height * 0.36f),
            radius = canvasRadius * 0.72f,
        )
        val peachBrush = Brush.radialGradient(
            colorStops = arrayOf(0f to peach, 1f to Color.Transparent),
            center = Offset(size.width * 0.52f, size.height * 0.98f),
            radius = canvasRadius * 0.56f,
        )
        onDrawBehind {
            drawRect(base)
            drawRect(iceBlueBrush)
            drawRect(lavenderBrush)
            drawRect(peachBrush)
        }
    }
}
