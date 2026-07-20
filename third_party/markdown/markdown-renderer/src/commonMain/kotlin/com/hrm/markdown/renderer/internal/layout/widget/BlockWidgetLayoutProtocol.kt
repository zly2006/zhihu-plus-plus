package com.hrm.markdown.renderer.internal.layout.widget

import com.hrm.markdown.renderer.internal.core.model.BlockWidgetModel
import com.hrm.markdown.renderer.internal.core.model.CodeBlockWidgetModel
import com.hrm.markdown.renderer.internal.core.model.DiagramBlockWidgetModel
import com.hrm.markdown.renderer.internal.core.model.MathBlockWidgetModel
import com.hrm.markdown.renderer.internal.layout.engine.LayoutEnvironment
import com.hrm.markdown.renderer.internal.layout.model.BlockWidgetMeasurement
import kotlin.math.ceil
import kotlin.math.max

internal fun measureBlockWidget(
    widget: BlockWidgetModel,
    viewportWidthPx: Float,
    environment: LayoutEnvironment,
): BlockWidgetMeasurement {
    return when (widget) {
        is CodeBlockWidgetModel -> measureCodeWidget(widget, viewportWidthPx)
        is MathBlockWidgetModel -> measureMathWidget(widget, viewportWidthPx)
        is DiagramBlockWidgetModel -> measureDiagramWidget(widget, viewportWidthPx, environment)
    }
}

private fun measureCodeWidget(
    widget: CodeBlockWidgetModel,
    viewportWidthPx: Float,
): BlockWidgetMeasurement {
    val lineCount = widget.code.lineSequence().count().coerceAtLeast(1)
    val longestLine = widget.code.lineSequence().maxOfOrNull { it.length } ?: 0
    val charWidthPx = 8f
    val titleHeightPx = if (widget.title.isNullOrBlank()) 0f else 28f
    val horizontalPaddingPx = 24f
    val contentWidthPx = max(viewportWidthPx, longestLine * charWidthPx + horizontalPaddingPx)
    return BlockWidgetMeasurement(
        widthPx = contentWidthPx,
        heightPx = titleHeightPx + lineCount * 22f + 24f,
        scrollableHorizontally = contentWidthPx > viewportWidthPx,
    )
}

private fun measureMathWidget(
    widget: MathBlockWidgetModel,
    viewportWidthPx: Float,
): BlockWidgetMeasurement {
    val length = widget.latex.trim().length.coerceAtLeast(1)
    val wrappedLines = ceil((length * 10f) / viewportWidthPx.coerceAtLeast(160f)).toInt().coerceAtLeast(1)
    return BlockWidgetMeasurement(
        widthPx = viewportWidthPx,
        heightPx = 40f + wrappedLines * 28f,
    )
}

private fun measureDiagramWidget(
    widget: DiagramBlockWidgetModel,
    viewportWidthPx: Float,
    environment: LayoutEnvironment,
): BlockWidgetMeasurement {
    val cachedHeight = environment.diagramHostRegistry.cachedHeightPx(widget.hostKey)
    if (cachedHeight != null) {
        return BlockWidgetMeasurement(
            widthPx = viewportWidthPx,
            heightPx = cachedHeight,
        )
    }
    val lineCount = widget.code.lineSequence().count().coerceAtLeast(3)
    val preferredHeight = when (widget.diagramType.lowercase()) {
        "mermaid" -> 120f + lineCount * 10f
        "plantuml" -> 110f + lineCount * 9f
        else -> 100f + lineCount * 8f
    }
    return BlockWidgetMeasurement(
        widthPx = viewportWidthPx,
        heightPx = preferredHeight.coerceAtLeast(140f),
    )
}
