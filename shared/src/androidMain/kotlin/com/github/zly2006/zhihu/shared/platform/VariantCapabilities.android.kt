package com.github.zly2006.zhihu.shared.platform

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext

@Composable
actual fun rememberIsLiteVariant(): Boolean {
    val context = LocalContext.current
    return remember(context) { context.packageName.endsWith(".lite") }
}
