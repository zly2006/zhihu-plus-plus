package com.hrm.markdown.renderer.inline

import androidx.compose.runtime.Composable
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign

internal data class InlineFlowLayout(
    val widthPx: Float,
    val heightPx: Float,
    val firstBaselinePx: Float? = null,
    val lastBaselinePx: Float? = null,
    val lines: List<InlineFlowLine>,
)

internal data class InlineFlowLine(
    val textStyle: TextStyle,
    val lineWidthPx: Float,
    val lineHeightPx: Float,
    val baselinePx: Float,
    val textAlign: TextAlign,
    val items: List<LineItem>,
)

internal sealed class LineItem {
    abstract val widthPx: Float
    abstract val heightPx: Float

    data class TextItem(
        val text: AnnotatedString,
        override val widthPx: Float,
        override val heightPx: Float,
        val baselinePx: Float,
    ) : LineItem()

    data class InlineItem(
        val id: String,
        override val widthPx: Float,
        override val heightPx: Float,
        /**
         * 行内内容自身的宽度。行内排版无法切开单个行内项，所以超宽时 [widthPx] 会被夹到可用行宽，
         * 而这里保留真实宽度，渲染侧据此判断是否需要提供横向滚动。
         */
        val naturalWidthPx: Float,
        val alternateText: String,
        val content: @Composable () -> Unit,
    ) : LineItem()
}

internal sealed class Token {
    data class Text(val annotated: AnnotatedString) : Token()
    data class Inline(val id: String) : Token()
    data object Newline : Token()
}
