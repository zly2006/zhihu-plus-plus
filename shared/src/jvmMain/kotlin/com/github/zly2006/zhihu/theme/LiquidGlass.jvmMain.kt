package com.github.zly2006.zhihu.theme

import androidx.compose.runtime.Composable

actual val isLiquidGlassSupported: Boolean = false

@Composable
actual fun rememberGlassBackdrop(): GlassBackdrop = error("此平台不支持 Android Liquid Glass 渲染")
