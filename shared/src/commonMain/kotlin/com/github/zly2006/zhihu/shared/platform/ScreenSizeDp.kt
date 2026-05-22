package com.github.zly2006.zhihu.shared.platform

import androidx.compose.runtime.Composable

data class ScreenSizeDp(
    val width: Float,
    val height: Float,
)

@Composable
expect fun rememberScreenSizeDp(): ScreenSizeDp
