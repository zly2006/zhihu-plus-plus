package com.hrm.markdown.renderer.block

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.hrm.markdown.parser.ast.BlankLine
import com.hrm.markdown.parser.ast.ListBlock
import com.hrm.markdown.parser.ast.ListItem
import com.hrm.markdown.parser.ast.Paragraph
import com.hrm.markdown.renderer.LocalMarkdownTheme
import com.hrm.markdown.renderer.MarkdownBlockChildren

/**
 * 列表渲染器（有序/无序列表）。
 */
@Composable
internal fun ListBlockRenderer(
    node: ListBlock,
    modifier: Modifier = Modifier,
) {
    val theme = LocalMarkdownTheme.current
    val spacing = if (node.tight) 2.dp else theme.blockSpacing

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(spacing),
    ) {
        val items = node.children.filterIsInstance<ListItem>()
        items.forEachIndexed { index, item ->
            key(item.stableKey, blockRenderRevision(item)) {
                ListItemRenderer(
                    node = item,
                    ordered = node.ordered,
                    index = node.startNumber + index,
                )
            }
        }
    }
}

@Composable
private fun ListItemRenderer(
    node: ListItem,
    ordered: Boolean,
    index: Int,
    modifier: Modifier = Modifier,
) {
    val theme = LocalMarkdownTheme.current

    Row(
        modifier = modifier.fillMaxWidth().padding(start = theme.listIndent),
        horizontalArrangement = Arrangement.Start,
    ) {
        // Marker 列
        when {
            node.taskListItem -> {
                Checkbox(
                    checked = node.checked,
                    onCheckedChange = null,
                    modifier = Modifier.size(20.dp).align(Alignment.Top),
                    colors = CheckboxDefaults.colors(
                        checkedColor = theme.taskCheckedColor,
                        uncheckedColor = theme.taskUncheckedColor,
                    ),
                )
            }
            ordered -> {
                Text(
                    text = "${index}.",
                    modifier = Modifier.width(24.dp).align(Alignment.Top),
                    style = theme.bodyStyle,
                    color = theme.listBulletColor,
                )
            }
            else -> {
                Text(
                    text = "•",
                    modifier = Modifier.width(16.dp).align(Alignment.Top),
                    style = theme.bodyStyle,
                    color = theme.listBulletColor,
                )
            }
        }

        // 内容列
        Box(modifier = Modifier.weight(1f)) {
            MarkdownBlockChildren(node)
        }
    }
}
