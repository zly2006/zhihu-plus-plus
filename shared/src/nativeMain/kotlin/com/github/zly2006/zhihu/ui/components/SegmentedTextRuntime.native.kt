package com.github.zly2006.zhihu.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember

@Composable
actual fun rememberSegmentedTextRuntime(): SegmentedTextRuntime = remember {
    SegmentedTextRuntime(
        toggleSegmentLike = { error("Segment like not available on iOS") },
    )
} // TODO: iOS 分段文本运行时
