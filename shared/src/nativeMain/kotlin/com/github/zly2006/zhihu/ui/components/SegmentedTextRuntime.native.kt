package com.github.zly2006.zhihu.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember

// TODO: iOS 分段文本运行时
@Composable
actual fun rememberSegmentedTextRuntime(): SegmentedTextRuntime = remember {
    SegmentedTextRuntime(
        copyText = { _, _ -> },
        toggleSegmentLike = { error("Segment like not available on iOS") },
    )
}
