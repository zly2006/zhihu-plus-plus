package com.hrm.markdown.renderer

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextLayoutResult
import com.hrm.markdown.parser.ast.ContainerNode
import com.hrm.markdown.parser.ast.Node
import com.hrm.markdown.parser.ast.SegmentHighlight
import com.hrm.markdown.renderer.inline.SEGMENT_HIGHLIGHT_ANNOTATION_TAG
import kotlinx.coroutines.withTimeoutOrNull

/**
 * 只在 Final pass 观察短按，不消费事件，让同一个文字层继续处理长按、拖动和选择手柄。
 */
internal fun Modifier.segmentHighlightTaps(
    annotated: AnnotatedString,
    highlights: Map<String, SegmentHighlight>,
    textLayoutResult: () -> TextLayoutResult?,
    onClick: ((SegmentHighlight) -> Unit)?,
): Modifier {
    if (onClick == null || highlights.isEmpty()) return this
    return pointerInput(annotated, highlights, onClick) {
        awaitEachGesture {
            awaitFirstDown(
                requireUnconsumed = false,
                pass = PointerEventPass.Final,
            )
            val up = withTimeoutOrNull(viewConfiguration.longPressTimeoutMillis) {
                waitForUpOrCancellation(pass = PointerEventPass.Final)
            } ?: return@awaitEachGesture
            val layout = textLayoutResult() ?: return@awaitEachGesture
            if (annotated.isEmpty()) return@awaitEachGesture
            val offset = layout
                .getOffsetForPosition(up.position)
                .coerceIn(0, annotated.lastIndex)
            val key = annotated
                .getStringAnnotations(SEGMENT_HIGHLIGHT_ANNOTATION_TAG, offset, offset + 1)
                .firstOrNull()
                ?.item
                ?: return@awaitEachGesture
            highlights[key]?.let(onClick)
        }
    }
}

internal fun Node.segmentHighlightsByKey(): Map<String, SegmentHighlight> = when (this) {
    is SegmentHighlight -> mapOf(interactionKey to this)
    is ContainerNode -> children
        .flatMap { it.segmentHighlightsByKey().values }
        .associateBy(SegmentHighlight::interactionKey)
    else -> emptyMap()
}
