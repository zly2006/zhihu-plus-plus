package com.hrm.markdown.renderer.internal.layout.engine

import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import com.hrm.markdown.parser.ast.Table
import com.hrm.markdown.renderer.internal.core.model.TableBlockModel
import com.hrm.markdown.renderer.internal.layout.inline.computeMaxIntrinsicWidthPx
import com.hrm.markdown.renderer.internal.layout.inline.computeMinIntrinsicWidthPx
import com.hrm.markdown.renderer.internal.layout.table.computeAutoTableColumnWidths

internal fun tableColumnCount(block: TableBlockModel): Int =
    block.columnAlignments.size.coerceAtLeast(block.rows.maxOfOrNull { it.cells.size } ?: 1)
        .coerceAtLeast(1)

internal fun LayoutEnvironment.tableCellTextStyle(
    alignment: Table.Alignment,
    isHeader: Boolean,
): TextStyle {
    val textAlign = when (alignment) {
        Table.Alignment.LEFT -> TextAlign.Start
        Table.Alignment.CENTER -> TextAlign.Center
        Table.Alignment.RIGHT -> TextAlign.End
        Table.Alignment.NONE -> TextAlign.Start
    }
    return if (isHeader) {
        markdownTheme.bodyStyle.copy(fontWeight = FontWeight.Bold, textAlign = textAlign)
    } else {
        markdownTheme.bodyStyle.copy(textAlign = textAlign)
    }
}

internal fun LayoutEnvironment.computeTableColumnWidths(
    block: TableBlockModel,
    availableWidthPx: Float?,
): List<Float> {
    val columnCount = tableColumnCount(block)
    val cellPadding = with(density) { markdownTheme.tableCellPadding.toPx() }
    val horizontalPadding = cellPadding * 2f
    val minWidths = MutableList(columnCount) { horizontalPadding }
    val maxWidths = MutableList(columnCount) { horizontalPadding }

    block.rows.forEach { row ->
        row.cells.forEachIndexed { colIndex, cell ->
            if (colIndex >= columnCount) return@forEachIndexed
            val alignment = block.columnAlignments.getOrElse(colIndex) { Table.Alignment.NONE }
            val style = tableCellTextStyle(alignment, row.isHeader)
            val flowInput = inlineLayoutRuntime.renderResult(
                model = cell.inline,
                style = style,
                epoch = inlineLayoutEpoch,
                theme = markdownTheme,
                directiveRegistry = compileEnvironment.directiveRegistry,
                onLinkClick = onLinkClick,
                onFootnoteClick = onFootnoteClick,
                latexMeasurer = latexMeasurer,
                density = density,
                textMeasurer = textMeasurer,
                codeTheme = codeTheme,
            ).flowInput
            val minWidth = computeMinIntrinsicWidthPx(flowInput, style, textMeasurer).toFloat() + horizontalPadding
            val maxWidth = computeMaxIntrinsicWidthPx(flowInput, style, textMeasurer).toFloat() + horizontalPadding
            minWidths[colIndex] = maxOf(minWidths[colIndex], minWidth)
            maxWidths[colIndex] = maxOf(maxWidths[colIndex], maxWidth)
        }
    }

    return computeAutoTableColumnWidths(
        minContentWidths = minWidths,
        maxContentWidths = maxWidths,
        availableWidth = availableWidthPx,
    )
}
