/*
 * Zhihu++ - Free & Ad-Free Zhihu client for Android.
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

package com.github.zly2006.zhihu.util

import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.ui.graphics.Color

fun smoothGradient(
    color: Color,
    maxAlpha: Float = 1f,
    steps: Int = 16,
    easing: Easing = FastOutLinearInEasing,
    reverse: Boolean = false,
): List<Color> {
    require(steps >= 2) { "steps must be at least 2" }
    return List(steps) { i ->
        val linearFraction = i.toFloat() / (steps - 1)
        val fraction = if (reverse) linearFraction else 1f - linearFraction
        val alpha = easing.transform(fraction) * maxAlpha
        color.copy(alpha = alpha)
    }
}
