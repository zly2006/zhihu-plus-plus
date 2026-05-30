package com.github.zly2006.zhihu.shared.filter

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember

@Composable
actual fun rememberContentFilterMaintenance(): ContentFilterMaintenance = remember {
    ContentFilterMaintenance(
        loadFilterStats = { null },
        cleanupOldData = { null },
        clearAllData = { null },
    )
} // TODO: iOS 内容过滤维护完整实现
