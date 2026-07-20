package com.hrm.markdown.renderer.internal.layout.inline

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import com.hrm.markdown.renderer.inline.InlinePlaceholderId

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
        val id: InlinePlaceholderId,
        override val widthPx: Float,
        override val heightPx: Float,
        val alternateText: String,
    ) : LineItem()
}
