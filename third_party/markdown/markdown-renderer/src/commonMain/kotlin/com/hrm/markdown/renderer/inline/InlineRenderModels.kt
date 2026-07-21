package com.hrm.markdown.renderer.inline

import androidx.compose.foundation.text.InlineTextContent
import androidx.compose.ui.text.AnnotatedString

internal const val INLINE_PLACEHOLDER_TAG = "markdown-inline-placeholder"
internal const val INLINE_PLACEHOLDER_CHAR = '\uFFFC'

internal data class InlineContentEntry(
    val alternateText: String,
    val inlineTextContent: InlineTextContent,
)

internal data class InlineContentResult(
    val annotated: AnnotatedString,
    val inlineContents: Map<String, InlineContentEntry>,
)

internal fun AnnotatedString.Builder.appendInlinePlaceholder(id: String) {
    pushStringAnnotation(tag = INLINE_PLACEHOLDER_TAG, annotation = id)
    append(INLINE_PLACEHOLDER_CHAR)
    pop()
}
