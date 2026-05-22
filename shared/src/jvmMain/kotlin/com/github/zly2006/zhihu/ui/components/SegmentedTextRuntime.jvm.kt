package com.github.zly2006.zhihu.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.github.zly2006.zhihu.shared.data.SegmentInfoMeta
import com.github.zly2006.zhihu.shared.util.SegmentHighlightSpan
import java.awt.Toolkit
import java.awt.datatransfer.StringSelection

@Composable
actual fun rememberSegmentedTextRuntime(): SegmentedTextRuntime = remember {
    SegmentedTextRuntime(
        copyText = { _, text ->
            runCatching {
                Toolkit.getDefaultToolkit().systemClipboard.setContents(StringSelection(text), null)
            }
        },
        toggleSegmentLike = ::toggleSegmentLikeLocally,
    )
}

private fun toggleSegmentLikeLocally(highlight: SegmentHighlightSpan): SegmentInfoMeta =
    if (highlight.meta.isLike) {
        highlight.meta.copy(
            isLike = false,
            likeCount = (highlight.meta.likeCount - 1).coerceAtLeast(0),
        )
    } else {
        highlight.meta.copy(
            isLike = true,
            likeCount = highlight.meta.likeCount + 1,
        )
    }
