package com.github.zly2006.zhihu.shared.platform

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo

@Composable
actual fun rememberScreenSizeDp(): ScreenSizeDp {
    val density = LocalDensity.current
    val containerSize = LocalWindowInfo.current.containerSize
    return with(density) {
        ScreenSizeDp(
            width = containerSize.width.toDp().value,
            height = containerSize.height.toDp().value,
        )
    }
}
