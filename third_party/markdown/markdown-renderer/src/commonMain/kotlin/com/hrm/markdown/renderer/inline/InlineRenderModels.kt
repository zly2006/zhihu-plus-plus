package com.hrm.markdown.renderer.inline

import androidx.compose.runtime.Composable
import androidx.compose.ui.text.AnnotatedString
import com.hrm.markdown.renderer.internal.core.identity.RenderIdentity
import com.hrm.markdown.renderer.internal.core.model.InlineWidgetModel
import com.hrm.markdown.renderer.internal.layout.inline.InlineFlowInput
import com.hrm.markdown.renderer.internal.layout.inline.InlinePlaceholderLayoutSpec
import kotlin.jvm.JvmInline

internal const val INLINE_PLACEHOLDER_TAG = "markdown-inline-placeholder"
internal const val INLINE_PLACEHOLDER_CHAR = '\uFFFC'

@JvmInline
internal value class InlinePlaceholderId(
    val value: Long,
) {
    companion object {
        fun from(widget: InlineWidgetModel): InlinePlaceholderId = from(widget.identity)

        fun from(identity: RenderIdentity): InlinePlaceholderId = InlinePlaceholderId(identity.stableId)

        fun fromAnnotation(annotation: String): InlinePlaceholderId? = annotation.toLongOrNull()?.let(::InlinePlaceholderId)
    }

    override fun toString(): String = value.toString()
}

internal data class InlineWidgetPaintPayload(
    val alternateText: String,
    val placeholder: InlinePlaceholderLayoutSpec,
    val content: @Composable () -> Unit,
)

internal data class InlinePlaceholderAnnotationRange(
    val id: InlinePlaceholderId,
    val start: Int,
    val end: Int,
)

internal data class InlineRenderResult(
    val annotated: AnnotatedString,
    val paintPayloads: Map<InlinePlaceholderId, InlineWidgetPaintPayload>,
    val flowInput: InlineFlowInput,
)

internal fun inlineWidgetPaintPayload(
    alternateText: String,
    widthPx: Float,
    heightPx: Float,
    content: @Composable () -> Unit,
): InlineWidgetPaintPayload {
    return InlineWidgetPaintPayload(
        alternateText = alternateText,
        placeholder = InlinePlaceholderLayoutSpec(
            alternateText = alternateText,
            widthPx = widthPx,
            heightPx = heightPx,
        ),
        content = content,
    )
}

internal fun AnnotatedString.Builder.appendInlinePlaceholder(id: InlinePlaceholderId) {
    pushStringAnnotation(tag = INLINE_PLACEHOLDER_TAG, annotation = id.toString())
    append(INLINE_PLACEHOLDER_CHAR)
    pop()
}

internal fun AnnotatedString.getInlinePlaceholderRanges(
    start: Int = 0,
    end: Int = text.length,
): List<InlinePlaceholderAnnotationRange> {
    return getStringAnnotations(tag = INLINE_PLACEHOLDER_TAG, start = start, end = end)
        .mapNotNull { annotation ->
            InlinePlaceholderId.fromAnnotation(annotation.item)?.let { id ->
                InlinePlaceholderAnnotationRange(
                    id = id,
                    start = annotation.start,
                    end = annotation.end,
                )
            }
        }
}

internal fun AnnotatedString.getInlinePlaceholderIds(
    start: Int = 0,
    end: Int = text.length,
): List<InlinePlaceholderId> {
    return getInlinePlaceholderRanges(start = start, end = end).map { range -> range.id }
}
