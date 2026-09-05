package com.github.zly2006.zhihu.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape

/** 当前窗口的视觉风格；玻璃仅在支持真实折射的平台启用。 */
val LocalLiquidGlass = staticCompositionLocalOf { false }
val LocalGlassBackdrop = staticCompositionLocalOf<GlassBackdrop?> { null }

expect val isLiquidGlassSupported: Boolean

interface GlassBackdrop {
    val source: Modifier

    fun surface(shape: Shape, tint: Color, floating: Boolean = true): Modifier
}

@Composable
expect fun rememberGlassBackdrop(): GlassBackdrop
