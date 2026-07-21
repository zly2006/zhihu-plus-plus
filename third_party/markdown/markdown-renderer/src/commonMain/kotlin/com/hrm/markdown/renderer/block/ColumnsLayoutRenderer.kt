package com.hrm.markdown.renderer.block

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.hrm.markdown.parser.ast.ColumnItem
import com.hrm.markdown.parser.ast.ColumnsLayout
import com.hrm.markdown.renderer.MarkdownBlockChildren

/**
 * 多列布局渲染器：将 [ColumnsLayout] 渲染为水平排列的多列结构。
 *
 * 每个 [ColumnItem] 按指定宽度（或平均分配）排列。
 */
@Composable
internal fun ColumnsLayoutRenderer(
    node: ColumnsLayout,
    modifier: Modifier = Modifier,
) {
    val columns = node.children.filterIsInstance<ColumnItem>()
    if (columns.isEmpty()) return

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        for (column in columns) {
            val weight = parseWeight(column.width, columns.size)
            MarkdownBlockChildren(
                parent = column,
                modifier = Modifier.weight(weight),
            )
        }
    }
}

/**
 * 将列宽字符串解析为 Row weight。
 *
 * - `"50%"` → 0.5f
 * - `"33.3%"` → 0.333f
 * - 空字符串 → 平均分配 `1f / columnCount`
 */
@Suppress("UNUSED_PARAMETER")
private fun parseWeight(width: String, columnCount: Int): Float {
    if (width.isBlank()) return 1f

    val percentMatch = PERCENT_REGEX.find(width)
    if (percentMatch != null) {
        val percent = percentMatch.groupValues[1].toFloatOrNull()
        if (percent != null && percent > 0) {
            return (percent / 100f).coerceIn(0.05f, 1f)
        }
    }

    return 1f
}

private val PERCENT_REGEX = Regex("""^(\d+(?:\.\d+)?)%$""")
