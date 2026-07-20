package com.hrm.markdown.renderer.internal.compose

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.Placeable
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Constraints
import com.hrm.markdown.renderer.internal.layout.inline.LayoutInlineRunPlacement
import com.hrm.markdown.renderer.internal.layout.inline.runPlacements
import com.hrm.markdown.renderer.internal.layout.inline.textMeasurementStyle
import com.hrm.markdown.renderer.internal.layout.model.LayoutInlineBlockModel
import com.hrm.markdown.renderer.internal.layout.model.LayoutTextRun
import com.hrm.markdown.renderer.internal.layout.model.LayoutWidgetRun
import kotlin.math.ceil

@Composable
internal fun PaintInlineLayoutContent(
    block: LayoutInlineBlockModel,
    modifier: Modifier = Modifier,
) {
    val textMeasurer = rememberTextMeasurer()
    val placements = remember(block.lines, block.frame) {
        block.runPlacements()
    }
    val paintItems = remember(placements, block.style, textMeasurer) {
        buildInlinePaintItems(
            block = block,
            placements = placements,
            style = block.style,
            textMeasurer = textMeasurer,
        )
    }
    Layout(
        modifier = modifier,
        content = {
            for (item in paintItems) {
                when (item) {
                    is InlineTextPaintItem -> key(item.stableKey) {
                        BasicText(
                            text = item.text,
                            modifier = Modifier.clipToBounds(),
                            style = block.style,
                            maxLines = 1,
                            softWrap = false,
                        )
                    }

                    is InlineWidgetPaintItem -> key(item.run.identity.stableId) {
                        val run = item.run
                        val payload = block.inlinePayloads[run.id]
                        Box {
                            payload?.content?.invoke()
                        }
                    }
                }
            }
        }
    ) { measurables, constraints ->
        val placeables = ArrayList<Placeable>(measurables.size)
        for (index in paintItems.indices) {
            val item = paintItems[index]
            placeables += measurables[index].measure(
                if (item.width == 0 || item.height == 0) {
                    Constraints.fixed(0, 0)
                } else {
                    Constraints.fixed(item.width, item.height)
                }
            )
        }

        val desiredWidth = if (constraints.hasBoundedWidth) {
            constraints.maxWidth
        } else {
            ceil(block.frame.width).toInt().coerceAtLeast(constraints.minWidth)
        }
        val desiredHeight = ceil(block.contentFrame.height).toInt()
            .coerceAtLeast(constraints.minHeight)
            .let { height ->
                if (constraints.hasBoundedHeight) height.coerceAtMost(constraints.maxHeight) else height
            }

        layout(desiredWidth, desiredHeight) {
            for (index in paintItems.indices) {
                val item = paintItems[index]
                placeables[index].placeRelative(item.x, item.y)
            }
        }
    }
}

private sealed interface InlinePaintItem {
    val x: Int
    val y: Int
    val width: Int
    val height: Int
}

private data class InlineTextPaintItem(
    val placements: List<LayoutInlineRunPlacement>,
    val text: AnnotatedString,
    override val x: Int,
    override val y: Int,
    override val width: Int,
    override val height: Int,
    val measuredWidth: Int,
    val measuredHeight: Int,
) : InlinePaintItem {
    val stableKey: Long = placements.fold(0L) { acc, placement ->
        acc * 31 + placement.run.identity.stableId
    }
}

private data class InlineWidgetPaintItem(
    val placement: LayoutInlineRunPlacement,
) : InlinePaintItem {
    val run: LayoutWidgetRun = placement.run as LayoutWidgetRun
    override val x: Int get() = placement.x
    override val y: Int get() = placement.y
    override val width: Int get() = placement.width
    override val height: Int get() = placement.height
}

private fun buildInlinePaintItems(
    block: LayoutInlineBlockModel,
    placements: List<LayoutInlineRunPlacement>,
    style: TextStyle,
    textMeasurer: androidx.compose.ui.text.TextMeasurer,
): List<InlinePaintItem> {
    val items = ArrayList<InlinePaintItem>()
    val textPlacements = ArrayList<LayoutInlineRunPlacement>()

    fun flushTextPlacements() {
        if (textPlacements.isEmpty()) return
        items += buildInlineTextPaintItems(
            placements = textPlacements,
            style = style,
            textMeasurer = textMeasurer,
        )
        textPlacements.clear()
    }

    for (placement in placements) {
        when (placement.run) {
            is LayoutTextRun -> {
                val previous = textPlacements.lastOrNull()
                if (previous != null && !canMergeTextPlacements(previous, placement)) {
                    flushTextPlacements()
                }
                textPlacements += placement
            }

            is LayoutWidgetRun -> {
                flushTextPlacements()
                items += InlineWidgetPaintItem(placement)
            }
        }
    }
    flushTextPlacements()
    return items
}

private fun canMergeTextPlacements(
    previous: LayoutInlineRunPlacement,
    next: LayoutInlineRunPlacement,
): Boolean {
    val previousRight = previous.x + previous.width
    return previous.lineIndex == next.lineIndex &&
        previous.runIndex + 1 == next.runIndex &&
        previous.y == next.y &&
        kotlin.math.abs(next.x - previousRight) <= 1
}

private fun buildInlineTextPaintItems(
    placements: List<LayoutInlineRunPlacement>,
    style: TextStyle,
    textMeasurer: androidx.compose.ui.text.TextMeasurer,
): List<InlineTextPaintItem> {
    val merged = buildInlineTextPaintItem(
        placements = placements,
        style = style,
        textMeasurer = textMeasurer,
    )
    if (merged.measuredWidth <= merged.width + 1 && merged.measuredHeight <= merged.height + 1) {
        return listOf(merged)
    }
    if (placements.size == 1) {
        return listOf(merged)
    }
    return placements.map { placement ->
        buildInlineTextPaintItem(
            placements = listOf(placement),
            style = style,
            textMeasurer = textMeasurer,
        )
    }
}

private fun buildInlineTextPaintItem(
    placements: List<LayoutInlineRunPlacement>,
    style: TextStyle,
    textMeasurer: androidx.compose.ui.text.TextMeasurer,
): InlineTextPaintItem {
    val first = placements.first()
    val mergedText = placements.joinText()
    var right = first.x + first.width
    var bottom = first.y + first.height
    for (i in 1 until placements.size) {
        val placement = placements[i]
        right = maxOf(right, placement.x + placement.width)
        bottom = maxOf(bottom, placement.y + placement.height)
    }
    val measured = textMeasurer.measure(
        text = mergedText,
        style = textMeasurementStyle(style),
        constraints = Constraints(maxWidth = Int.MAX_VALUE),
        maxLines = 1,
        softWrap = false,
    )
    return InlineTextPaintItem(
        placements = placements.toList(),
        text = mergedText,
        x = first.x,
        y = first.y,
        width = (right - first.x).coerceAtLeast(0),
        height = (bottom - first.y).coerceAtLeast(0),
        measuredWidth = measured.size.width,
        measuredHeight = measured.size.height,
    )
}

private fun List<LayoutInlineRunPlacement>.joinText(): AnnotatedString {
    if (size == 1) {
        return (first().run as LayoutTextRun).text
    }
    return buildAnnotatedString {
        for (placement in this@joinText) {
            append((placement.run as LayoutTextRun).text)
        }
    }
}
