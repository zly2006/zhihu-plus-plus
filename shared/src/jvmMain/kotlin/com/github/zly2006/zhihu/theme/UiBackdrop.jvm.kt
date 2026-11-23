package com.github.zly2006.zhihu.theme

import androidx.compose.runtime.Composable

actual val isLiquidGlassSupported: Boolean = false

@Composable
internal actual fun rememberLiquidGlassBackdrop(): UiBackdrop = error("JVM 平台暂不支持 Android Liquid Glass 渲染")
