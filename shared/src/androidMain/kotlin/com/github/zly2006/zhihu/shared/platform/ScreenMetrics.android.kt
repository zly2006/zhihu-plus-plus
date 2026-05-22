package com.github.zly2006.zhihu.shared.platform

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalConfiguration

@Composable
actual fun rememberScreenSizeDp(): ScreenSizeDp {
    val configuration = LocalConfiguration.current
    return ScreenSizeDp(
        width = configuration.screenWidthDp.toFloat(),
        height = configuration.screenHeightDp.toFloat(),
    )
}
