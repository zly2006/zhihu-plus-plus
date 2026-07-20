package com.hrm.markdown.renderer.block

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import com.hrm.markdown.parser.ast.Table
import com.hrm.markdown.parser.ast.TableBody
import com.hrm.markdown.parser.ast.TableCell
import com.hrm.markdown.parser.ast.TableHead
import com.hrm.markdown.parser.ast.TableRow
import com.hrm.markdown.renderer.LocalMarkdownTheme
import com.hrm.markdown.renderer.inline.InlineLayoutBlockText
import com.hrm.markdown.renderer.inline.rememberInlineModel
import com.hrm.markdown.renderer.internal.core.model.TableBlockModel
import com.hrm.markdown.renderer.internal.core.model.TableCellBlockModel
import com.hrm.markdown.renderer.internal.core.model.TableRowBlockModel
import com.hrm.markdown.renderer.internal.layout.model.LayoutTableBlockModel
import com.hrm.markdown.renderer.internal.layout.table.computeAutoTableColumnWidths
import kotlin.math.roundToInt

/**
 * GFM 表格渲染器。
 *
 * 使用接近 CSS auto table layout 的列宽分配：
 * 先按单元格内容计算每列 min-content / max-content，再按视口宽度伸缩或水平滚动。
 */
@Composable
internal fun TableRenderer(
    node: Table,
    modifier: Modifier = Modifier,
) {
    val theme = LocalMarkdownTheme.current

    // 收集所有行（表头 + 表体）
    val allRows = mutableListOf<Pair<TableRow, Boolean>>() // (row, isHeader)
    val head = node.children.filterIsInstance<TableHead>().firstOrNull()
    head?.children?.filterIsInstance<TableRow>()?.forEach { allRows.add(it to true) }
    val body = node.children.filterIsInstance<TableBody>().firstOrNull()
    body?.children?.filterIsInstance<TableRow>()?.forEach { allRows.add(it to false) }
    val columnCount = node.columnAlignments.size
        .coerceAtLeast(allRows.maxOfOrNull { it.first.children.filterIsInstance<TableCell>().size } ?: 1)
        .coerceAtLeast(1)

    AutoTableViewport(modifier) { availableWidthPx ->
        // 自定义表格布局
        TableLayout(
            allRows = allRows,
            alignments = node.columnAlignments,
            columnCount = columnCount,
            availableWidthPx = availableWidthPx,
            modifier = Modifier.border(width = 1.dp, color = theme.tableBorderColor),
        )
    }
}

@Composable
private fun AutoTableViewport(
    modifier: Modifier,
    content: @Composable (availableWidthPx: Int?) -> Unit,
) {
    val density = LocalDensity.current
    BoxWithConstraints(modifier = modifier.fillMaxWidth()) {
        val viewportWidthPx = if (maxWidth.value.isFinite()) {
            with(density) { maxWidth.roundToPx() }
        } else {
            null
        }
        val minWidthModifier = viewportWidthPx?.let {
            Modifier.widthIn(min = with(density) { it.toDp() })
        } ?: Modifier
        Box(
            modifier = Modifier
                .horizontalScroll(rememberScrollState())
                .then(minWidthModifier),
        ) {
            content(viewportWidthPx)
        }
    }
}

@Composable
private fun TableLayout(
    allRows: List<Pair<TableRow, Boolean>>,
    alignments: List<Table.Alignment>,
    columnCount: Int,
    availableWidthPx: Int?,
    modifier: Modifier = Modifier,
) {
    val theme = LocalMarkdownTheme.current

    // 将所有单元格作为 content 传入 Layout
    Layout(
        modifier = modifier,
        content = {
            for ((row, isHeader) in allRows) {
                val cells = row.children.filterIsInstance<TableCell>()
                for (colIndex in 0 until columnCount) {
                    val cell = cells.getOrNull(colIndex)
                    val alignment = alignments.getOrElse(colIndex) { Table.Alignment.NONE }
                    TableCellRenderer(
                        cell = cell,
                        alignment = alignment,
                        isHeader = isHeader,
                        modifier = Modifier
                            .border(0.5.dp, theme.tableBorderColor)
                            .let {
                                if (isHeader) it.background(theme.tableHeaderBackground) else it
                            }
                            .padding(theme.tableCellPadding),
                    )
                }
            }
        },
    ) { measurables, constraints ->
        val rowCount = allRows.size
        if (rowCount == 0 || columnCount == 0) {
            return@Layout layout(0, 0) {}
        }

        val minContentWidths = MutableList(columnCount) { 0f }
        val maxContentWidths = MutableList(columnCount) { 0f }
        for (index in measurables.indices) {
            val colIdx = index % columnCount
            val minIntrinsicWidth = measurables[index].minIntrinsicWidth(Constraints.Infinity)
            val maxIntrinsicWidth = measurables[index].maxIntrinsicWidth(Constraints.Infinity)
            minContentWidths[colIdx] = maxOf(minContentWidths[colIdx], minIntrinsicWidth.toFloat())
            maxContentWidths[colIdx] = maxOf(maxContentWidths[colIdx], maxIntrinsicWidth.toFloat())
        }
        val columnWidths = computeColumnWidthsPx(minContentWidths, maxContentWidths, availableWidthPx)

        val placeables = Array(measurables.size) { index ->
            val colIdx = index % columnCount
            val fixedWidth = columnWidths[colIdx]
            measurables[index].measure(
                Constraints(
                    minWidth = fixedWidth,
                    maxWidth = fixedWidth,
                )
            )
        }

        // 计算每行高度
        val rowHeights = IntArray(rowCount)
        for (rowIdx in 0 until rowCount) {
            for (colIdx in 0 until columnCount) {
                val placeable = placeables[rowIdx * columnCount + colIdx]
                rowHeights[rowIdx] = maxOf(rowHeights[rowIdx], placeable.height)
            }
        }

        val totalWidth = columnWidths.sum()
        val totalHeight = rowHeights.sum()

        layout(totalWidth, totalHeight) {
            var y = 0
            for (rowIdx in 0 until rowCount) {
                var x = 0
                for (colIdx in 0 until columnCount) {
                    val placeable = placeables[rowIdx * columnCount + colIdx]
                    placeable.placeRelative(x, y)
                    x += columnWidths[colIdx]
                }
                y += rowHeights[rowIdx]
            }
        }
    }
}

@Composable
private fun TableCellRenderer(
    cell: TableCell?,
    alignment: Table.Alignment,
    isHeader: Boolean,
    modifier: Modifier = Modifier,
) {
    val theme = LocalMarkdownTheme.current

    val textAlign = when (alignment) {
        Table.Alignment.LEFT -> TextAlign.Start
        Table.Alignment.CENTER -> TextAlign.Center
        Table.Alignment.RIGHT -> TextAlign.End
        Table.Alignment.NONE -> TextAlign.Start
    }

    val style = if (isHeader) {
        theme.bodyStyle.copy(fontWeight = FontWeight.Bold, textAlign = textAlign)
    } else {
        theme.bodyStyle.copy(textAlign = textAlign)
    }

    if (cell == null) {
        Box(modifier = modifier)
        return
    }

    val inlineModel = rememberInlineModel(cell)
    Box(modifier = modifier, contentAlignment = Alignment.CenterStart) {
        InlineLayoutBlockText(
            model = inlineModel,
            style = style,
        )
    }
}

@Composable
internal fun RenderTableBlockModel(
    model: TableBlockModel,
    modifier: Modifier = Modifier,
) {
    val theme = LocalMarkdownTheme.current
    val columnCount = model.columnAlignments.size
        .coerceAtLeast(model.rows.maxOfOrNull { it.cells.size } ?: 1)
        .coerceAtLeast(1)
    AutoTableViewport(modifier) { availableWidthPx ->
        TableBlockModelLayout(
            rows = model.rows,
            alignments = model.columnAlignments,
            columnCount = columnCount,
            availableWidthPx = availableWidthPx,
            modifier = Modifier.border(width = 1.dp, color = theme.tableBorderColor),
        )
    }
}

@Composable
internal fun RenderTableLayoutBlockModel(
    model: LayoutTableBlockModel,
    modifier: Modifier = Modifier,
) {
    val theme = LocalMarkdownTheme.current
    AutoTableViewport(modifier) { availableWidthPx ->
        TableBlockModelLayout(
            rows = model.block.rows,
            alignments = model.block.columnAlignments,
            columnCount = model.columnWidths.size
                .coerceAtLeast(model.block.rows.maxOfOrNull { it.cells.size } ?: 1)
                .coerceAtLeast(1),
            availableWidthPx = availableWidthPx,
            modifier = Modifier.border(width = 1.dp, color = theme.tableBorderColor),
        )
    }
}

@Composable
private fun TableBlockModelLayout(
    rows: List<TableRowBlockModel>,
    alignments: List<Table.Alignment>,
    columnCount: Int,
    availableWidthPx: Int?,
    modifier: Modifier = Modifier,
) {
    val theme = LocalMarkdownTheme.current
    Layout(
        modifier = modifier,
        content = {
            for (row in rows) {
                for (colIndex in 0 until columnCount) {
                    val cell = row.cells.getOrNull(colIndex)
                    val alignment = alignments.getOrElse(colIndex) { Table.Alignment.NONE }
                    TableBlockModelCellRenderer(
                        cell = cell,
                        alignment = alignment,
                        isHeader = row.isHeader,
                        modifier = Modifier
                            .border(0.5.dp, theme.tableBorderColor)
                            .let {
                                if (row.isHeader) it.background(theme.tableHeaderBackground) else it
                            }
                            .padding(theme.tableCellPadding),
                    )
                }
            }
        },
    ) { measurables, constraints ->
        val rowCount = rows.size
        if (rowCount == 0 || columnCount == 0) {
            return@Layout layout(0, 0) {}
        }

        val minContentWidths = MutableList(columnCount) { 0f }
        val maxContentWidths = MutableList(columnCount) { 0f }
        for (index in measurables.indices) {
            val colIdx = index % columnCount
            val minIntrinsicWidth = measurables[index].minIntrinsicWidth(Constraints.Infinity)
            val maxIntrinsicWidth = measurables[index].maxIntrinsicWidth(Constraints.Infinity)
            minContentWidths[colIdx] = maxOf(minContentWidths[colIdx], minIntrinsicWidth.toFloat())
            maxContentWidths[colIdx] = maxOf(maxContentWidths[colIdx], maxIntrinsicWidth.toFloat())
        }
        val columnWidths = computeColumnWidthsPx(minContentWidths, maxContentWidths, availableWidthPx)

        val placeables = Array(measurables.size) { index ->
            val colIdx = index % columnCount
            val fixedWidth = columnWidths[colIdx]
            measurables[index].measure(
                Constraints(
                    minWidth = fixedWidth,
                    maxWidth = fixedWidth,
                )
            )
        }

        val rowHeights = IntArray(rowCount)
        for (rowIdx in 0 until rowCount) {
            for (colIdx in 0 until columnCount) {
                val placeable = placeables[rowIdx * columnCount + colIdx]
                rowHeights[rowIdx] = maxOf(rowHeights[rowIdx], placeable.height)
            }
        }

        val totalWidth = columnWidths.sum()
        val totalHeight = rowHeights.sum()
        layout(totalWidth, totalHeight) {
            var y = 0
            for (rowIdx in 0 until rowCount) {
                var x = 0
                for (colIdx in 0 until columnCount) {
                    val placeable = placeables[rowIdx * columnCount + colIdx]
                    placeable.placeRelative(x, y)
                    x += columnWidths[colIdx]
                }
                y += rowHeights[rowIdx]
            }
        }
    }
}

@Composable
private fun TableBlockModelCellRenderer(
    cell: TableCellBlockModel?,
    alignment: Table.Alignment,
    isHeader: Boolean,
    modifier: Modifier = Modifier,
) {
    val theme = LocalMarkdownTheme.current
    val textAlign = when (alignment) {
        Table.Alignment.LEFT -> TextAlign.Start
        Table.Alignment.CENTER -> TextAlign.Center
        Table.Alignment.RIGHT -> TextAlign.End
        Table.Alignment.NONE -> TextAlign.Start
    }
    val style = if (isHeader) {
        theme.bodyStyle.copy(fontWeight = FontWeight.Bold, textAlign = textAlign)
    } else {
        theme.bodyStyle.copy(textAlign = textAlign)
    }
    if (cell == null) {
        Box(modifier = modifier)
        return
    }
    Box(modifier = modifier, contentAlignment = Alignment.CenterStart) {
        InlineLayoutBlockText(
            model = cell.inline,
            style = style,
        )
    }
}

private fun computeColumnWidthsPx(
    minContentWidths: List<Float>,
    maxContentWidths: List<Float>,
    availableWidthPx: Int?,
): IntArray {
    val widths = computeAutoTableColumnWidths(
        minContentWidths = minContentWidths,
        maxContentWidths = maxContentWidths,
        availableWidth = availableWidthPx?.toFloat(),
    )
    val rounded = IntArray(widths.size) { index -> widths[index].roundToInt().coerceAtLeast(0) }
    val target = widths.sum().roundToInt()
    val delta = target - rounded.sum()
    if (delta != 0 && rounded.isNotEmpty()) {
        rounded[rounded.lastIndex] = (rounded.last() + delta).coerceAtLeast(0)
    }
    return rounded
}
