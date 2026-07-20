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
import com.hrm.markdown.renderer.internal.core.model.ColumnBlockModel
import com.hrm.markdown.renderer.internal.core.model.ColumnsLayoutBlockModel
import com.hrm.markdown.renderer.internal.core.model.InternalRenderBlockModel
import com.hrm.markdown.renderer.internal.layout.model.InternalLayoutBlockModel
import com.hrm.markdown.renderer.internal.layout.model.LayoutColumnsBlockModel

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
    RenderColumnsRow(
        modifier = modifier,
        columns = columns.map { column ->
            column.width to {
                MarkdownBlockChildren(
                    parent = column,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
    )
}

@Composable
internal fun RenderColumnsLayoutBlockModel(
    model: ColumnsLayoutBlockModel,
    renderChildren: @Composable (List<InternalRenderBlockModel>) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (model.columns.isEmpty()) return
    RenderColumnsRow(
        modifier = modifier,
        columns = model.columns.map { column ->
            column.width to {
                renderChildren(column.children)
            }
        },
    )
}

@Composable
internal fun RenderColumnsLayoutGroupModel(
    model: LayoutColumnsBlockModel,
    renderChildren: @Composable (List<InternalLayoutBlockModel>) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (model.columns.isEmpty()) return
    RenderColumnsRow(
        modifier = modifier,
        columns = model.columns.map { column ->
            column.width to {
                renderChildren(column.children)
            }
        },
    )
}

@Composable
private fun RenderColumnsRow(
    columns: List<Pair<String, @Composable () -> Unit>>,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        for ((width, content) in columns) {
            val weight = parseWeight(width, columns.size)
            androidx.compose.foundation.layout.Box(
                modifier = Modifier.weight(weight),
            ) {
                content()
            }
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
