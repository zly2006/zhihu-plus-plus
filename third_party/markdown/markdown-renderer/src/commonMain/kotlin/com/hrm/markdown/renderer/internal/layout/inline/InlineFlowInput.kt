package com.hrm.markdown.renderer.internal.layout.inline

import androidx.compose.ui.text.AnnotatedString
import com.hrm.markdown.renderer.inline.InlinePlaceholderId

internal data class InlineFlowInput(
    val segments: List<InlineFlowSegment>,
)

internal data class InlinePlaceholderLayoutSpec(
    val alternateText: String,
    val widthPx: Float,
    val heightPx: Float,
)

internal sealed interface InlineFlowSegment {
    data class TextRun(val annotated: AnnotatedString) : InlineFlowSegment
    data class InlineRun(
        val id: InlinePlaceholderId,
        val placeholder: InlinePlaceholderLayoutSpec,
    ) : InlineFlowSegment

    data object Newline : InlineFlowSegment
}
