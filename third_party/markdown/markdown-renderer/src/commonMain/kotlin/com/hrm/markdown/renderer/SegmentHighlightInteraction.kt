package com.hrm.markdown.renderer

import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.PointerEvent
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerId
import androidx.compose.ui.input.pointer.changedToDownIgnoreConsumed
import androidx.compose.ui.input.pointer.changedToUpIgnoreConsumed
import androidx.compose.ui.node.CompositionLocalConsumerModifierNode
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.node.PointerInputModifierNode
import androidx.compose.ui.node.currentValueOf
import androidx.compose.ui.platform.LocalViewConfiguration
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.unit.IntSize
import com.hrm.markdown.parser.ast.ContainerNode
import com.hrm.markdown.parser.ast.Node
import com.hrm.markdown.parser.ast.SegmentHighlight
import com.hrm.markdown.renderer.inline.SEGMENT_HIGHLIGHT_ANNOTATION_TAG

/**
 * 在实际绘制划线的文字上观察短按，不消费事件，让外层选择容器继续处理长按、拖动和选择手柄。
 *
 * 可见文字覆盖在纯文本选择层之上。普通 `pointerInput` 会独占 sibling hit test，使下层收不到长按；
 * 这里通过 Compose 的公开 sibling-sharing pointer node 同时保留真实文字命中和原生选择手势。
 * 父层双击手势会消费抬手事件，因此按原始 pressed 状态识别抬手，同时仍以时长、位移和多指取消短按。
 */
internal fun Modifier.segmentHighlightTaps(
    annotated: AnnotatedString,
    highlights: Map<String, SegmentHighlight>,
    textLayoutResult: () -> TextLayoutResult?,
    onClick: ((SegmentHighlight) -> Unit)?,
): Modifier {
    if (onClick == null || highlights.isEmpty()) return this
    return this.then(
        SegmentHighlightTapElement(
            annotated = annotated,
            highlights = highlights,
            textLayoutResult = textLayoutResult,
            onClick = onClick,
        ),
    )
}

private data class SegmentHighlightTapElement(
    val annotated: AnnotatedString,
    val highlights: Map<String, SegmentHighlight>,
    val textLayoutResult: () -> TextLayoutResult?,
    val onClick: (SegmentHighlight) -> Unit,
) : ModifierNodeElement<SegmentHighlightTapNode>() {
    override fun create() = SegmentHighlightTapNode(annotated, highlights, textLayoutResult, onClick)

    override fun update(node: SegmentHighlightTapNode) {
        node.update(annotated, highlights, textLayoutResult, onClick)
    }
}

private class SegmentHighlightTapNode(
    private var annotated: AnnotatedString,
    private var highlights: Map<String, SegmentHighlight>,
    private var textLayoutResult: () -> TextLayoutResult?,
    private var onClick: (SegmentHighlight) -> Unit,
) : Modifier.Node(),
    PointerInputModifierNode,
    CompositionLocalConsumerModifierNode {
    private var pointerId: PointerId? = null
    private var downPosition = Offset.Unspecified
    private var downUptimeMillis = 0L

    fun update(
        annotated: AnnotatedString,
        highlights: Map<String, SegmentHighlight>,
        textLayoutResult: () -> TextLayoutResult?,
        onClick: (SegmentHighlight) -> Unit,
    ) {
        if (this.annotated != annotated || this.highlights != highlights) {
            onCancelPointerInput()
        }
        this.annotated = annotated
        this.highlights = highlights
        this.textLayoutResult = textLayoutResult
        this.onClick = onClick
    }

    override fun sharePointerInputWithSiblings() = true

    override fun onPointerEvent(
        pointerEvent: PointerEvent,
        pass: PointerEventPass,
        bounds: IntSize,
    ) {
        if (pass != PointerEventPass.Final) return

        val trackedPointerId = pointerId
        if (trackedPointerId == null) {
            val down = pointerEvent.changes.firstOrNull { it.changedToDownIgnoreConsumed() } ?: return
            if (pointerEvent.changes.any { it.id != down.id && it.pressed }) return
            pointerId = down.id
            downPosition = down.position
            downUptimeMillis = down.uptimeMillis
            return
        }

        val change = pointerEvent.changes.firstOrNull { it.id == trackedPointerId }
        if (
            change == null ||
            pointerEvent.changes.any { it.id != trackedPointerId && it.pressed } ||
            (change.position - downPosition).getDistance() > currentValueOf(LocalViewConfiguration).touchSlop
        ) {
            onCancelPointerInput()
            return
        }
        if (change.changedToUpIgnoreConsumed()) {
            val tapDuration = change.uptimeMillis - downUptimeMillis
            if (tapDuration < currentValueOf(LocalViewConfiguration).longPressTimeoutMillis) {
                val layout = textLayoutResult()
                if (layout != null && annotated.isNotEmpty()) {
                    val offset = layout
                        .getOffsetForPosition(change.position)
                        .coerceIn(0, annotated.lastIndex)
                    val key = annotated
                        .getStringAnnotations(SEGMENT_HIGHLIGHT_ANNOTATION_TAG, offset, offset + 1)
                        .firstOrNull()
                        ?.item
                    key?.let(highlights::get)?.let(onClick)
                }
            }
            onCancelPointerInput()
        } else if (!change.pressed) {
            onCancelPointerInput()
        }
    }

    override fun onCancelPointerInput() {
        pointerId = null
        downPosition = Offset.Unspecified
        downUptimeMillis = 0L
    }
}

internal fun Node.segmentHighlightsByKey(): Map<String, SegmentHighlight> = when (this) {
    is SegmentHighlight -> mapOf(interactionKey to this)
    is ContainerNode -> children
        .flatMap { it.segmentHighlightsByKey().values }
        .associateBy(SegmentHighlight::interactionKey)
    else -> emptyMap()
}
