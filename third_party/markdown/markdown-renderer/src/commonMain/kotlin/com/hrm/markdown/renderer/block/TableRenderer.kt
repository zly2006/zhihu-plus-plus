package com.hrm.markdown.renderer.block

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
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
import com.hrm.markdown.renderer.DeferredMarkdownColumn
import com.hrm.markdown.renderer.estimatedInlineWidthDp
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
    val allRows = remember(node) {
        buildList {
            val head = node.children.filterIsInstance<TableHead>().firstOrNull()
            head?.children?.filterIsInstance<TableRow>()?.forEach { add(it to true) }
            val body = node.children.filterIsInstance<TableBody>().firstOrNull()
            body?.children?.filterIsInstance<TableRow>()?.forEach { add(it to false) }
        }
    }

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
    val rows = remember(allRows) { allRows.map { it.first } }
    val fontSize = theme.bodyStyle.fontSize.value.takeIf { it.isFinite() && it > 0f } ?: 16f
    val lineHeight = theme.bodyStyle.lineHeight.value.takeIf { it.isFinite() && it > 0f } ?: fontSize * 1.5f
    val horizontalPadding = theme.tableCellPadding.value * 2f
    val columnWidthsDp = remember(allRows, alignments, theme) {
        List(columnCount) { columnIndex ->
            allRows.maxOfOrNull { (row, _) ->
                row.children
                    .filterIsInstance<TableCell>()
                    .getOrNull(columnIndex)
                    ?.estimatedInlineWidthDp(fontSize, 240f)
                    ?.times(1.1f)
                    ?: 0f
            }?.plus(horizontalPadding)?.coerceAtLeast(48f) ?: 48f
        }
    }
    val estimatedRowHeightDp = lineHeight + theme.tableCellPadding.value * 2f + 1f

    DeferredMarkdownColumn(
        blocks = rows,
        modifier = modifier.width(columnWidthsDp.sum().dp),
        blockSpacing = 0.dp,
        prefetchViewports = 0.5f,
        estimateHeightDp = { _, _, _ -> estimatedRowHeightDp },
    ) { rowIndex, row ->
        TableRowRenderer(
            row = row,
            isHeader = allRows[rowIndex].second,
            alignments = alignments,
            columnWidthsDp = columnWidthsDp,
        )
    }
}

@Composable
private fun TableRowRenderer(
    row: TableRow,
    isHeader: Boolean,
    alignments: List<Table.Alignment>,
    columnWidthsDp: List<Float>,
) {
    val theme = LocalMarkdownTheme.current
    val cells = row.children.filterIsInstance<TableCell>()
    Layout(
        content = {
            columnWidthsDp.forEachIndexed { columnIndex, widthDp ->
                TableCellRenderer(
                    cell = cells.getOrNull(columnIndex),
                    alignment = alignments.getOrElse(columnIndex) { Table.Alignment.NONE },
                    isHeader = isHeader,
                    modifier = Modifier
                        .width(widthDp.dp)
                        .border(0.5.dp, theme.tableBorderColor)
                        .let {
                            if (isHeader) it.background(theme.tableHeaderBackground) else it
                        }
                        .padding(theme.tableCellPadding),
                )
            }
        },
    ) { measurables, constraints ->
        if (measurables.isEmpty()) {
            return@Layout layout(0, 0) {}
        }
        val placeables = Array(measurables.size) { index ->
            val fixedWidth = columnWidthsDp[index].dp.roundToPx()
            measurables[index].measure(
                Constraints(
                    minWidth = fixedWidth,
                    maxWidth = fixedWidth,
                )
            )
        }

        val totalWidth = placeables.sumOf { it.width }
        val rowHeight = placeables.maxOf { it.height }

        layout(totalWidth, rowHeight) {
            var x = 0
            placeables.forEach { placeable ->
                placeable.placeRelative(x, 0)
                x += placeable.width
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
