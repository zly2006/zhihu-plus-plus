package com.hrm.markdown.renderer.block

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.Layout
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
import com.hrm.markdown.renderer.LocalOnLinkClick
import com.hrm.markdown.renderer.inline.InlineFlowText
import com.hrm.markdown.renderer.inline.rememberInlineContent

/**
 * GFM 表格渲染器。
 *
 * 使用自定义 Layout 实现列宽对齐：所有行中同一列使用该列最大内容宽度。
 * 始终支持水平滚动。
 */
@Composable
internal fun TableRenderer(
    node: Table,
    modifier: Modifier = Modifier,
) {
    val theme = LocalMarkdownTheme.current
    val columnCount = node.columnAlignments.size.coerceAtLeast(1)

    // 收集所有行（表头 + 表体）
    val allRows = mutableListOf<Pair<TableRow, Boolean>>() // (row, isHeader)
    val head = node.children.filterIsInstance<TableHead>().firstOrNull()
    head?.children?.filterIsInstance<TableRow>()?.forEach { allRows.add(it to true) }
    val body = node.children.filterIsInstance<TableBody>().firstOrNull()
    body?.children?.filterIsInstance<TableRow>()?.forEach { allRows.add(it to false) }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
    ) {
        // 自定义表格布局
        TableLayout(
            allRows = allRows,
            alignments = node.columnAlignments,
            columnCount = columnCount,
            modifier = Modifier.border(width = 1.dp, color = theme.tableBorderColor),
        )
    }
}

@Composable
private fun TableLayout(
    allRows: List<Pair<TableRow, Boolean>>,
    alignments: List<Table.Alignment>,
    columnCount: Int,
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

        // 用 intrinsic width 计算每列最大宽度（不消耗 measure 次数）
        val columnWidths = IntArray(columnCount)
        for (index in measurables.indices) {
            val colIdx = index % columnCount
            val intrinsicWidth = measurables[index].maxIntrinsicWidth(Constraints.Infinity)
            columnWidths[colIdx] = maxOf(columnWidths[colIdx], intrinsicWidth)
        }

        // 用确定的列宽测量每个单元格（只 measure 一次）
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
    val onLinkClick = LocalOnLinkClick.current

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

    val inlineResult = rememberInlineContent(
        parent = cell,
        onLinkClick = onLinkClick,
        hostTextStyle = style,
    )
    Box(modifier = modifier, contentAlignment = Alignment.CenterStart) {
        InlineFlowText(
            annotated = inlineResult.annotated,
            inlineContents = inlineResult.inlineContents,
            style = style,
            maxLines = 1,
        )
    }
}
