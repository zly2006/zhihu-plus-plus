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
): List<Color> = List(steps) { i ->
    val linearFraction = i.toFloat() / (steps - 1)
    val fraction = if (reverse) linearFraction else 1f - linearFraction
    val alpha = easing.transform(fraction) * maxAlpha
    color.copy(alpha = alpha)
}