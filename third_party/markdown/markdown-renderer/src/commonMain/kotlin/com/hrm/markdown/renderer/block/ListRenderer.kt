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
import com.hrm.markdown.parser.ast.ListBlock
import com.hrm.markdown.parser.ast.ListItem
import com.hrm.markdown.renderer.LocalMarkdownTheme
import com.hrm.markdown.renderer.MarkdownBlockChildren
import com.hrm.markdown.renderer.internal.core.model.InternalRenderBlockModel
import com.hrm.markdown.renderer.internal.core.model.ListBlockModel
import com.hrm.markdown.renderer.internal.core.model.ListItemBlockModel
import com.hrm.markdown.renderer.internal.layout.list.OrderedListMarkerWidth
import com.hrm.markdown.renderer.internal.layout.list.TaskListMarkerWidth
import com.hrm.markdown.renderer.internal.layout.list.UnorderedListMarkerWidth
import com.hrm.markdown.renderer.internal.layout.model.InternalLayoutBlockModel
import com.hrm.markdown.renderer.internal.layout.model.LayoutListBlockModel
import com.hrm.markdown.renderer.internal.layout.model.LayoutListItemGroup

/**
 * 列表渲染器（有序/无序列表）。
 */
@Composable
internal fun ListBlockRenderer(
    node: ListBlock,
    modifier: Modifier = Modifier,
) {
    val items = node.children.filterIsInstance<ListItem>()
    RenderListContainer(
        ordered = node.ordered,
        startNumber = node.startNumber,
        tight = node.tight,
        modifier = modifier,
        items = items.mapIndexed { index, item ->
            item.stableKey.toLong() to {
                ListItemRenderer(
                    node = item,
                    ordered = node.ordered,
                    index = node.startNumber + index,
                )
            }
        },
    )
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
                    modifier = Modifier.size(TaskListMarkerWidth).align(Alignment.Top),
                    colors = CheckboxDefaults.colors(
                        checkedColor = theme.taskCheckedColor,
                        uncheckedColor = theme.taskUncheckedColor,
                    ),
                )
            }
            ordered -> {
                Text(
                    text = "${index}.",
                    modifier = Modifier.width(OrderedListMarkerWidth).align(Alignment.Top),
                    style = theme.bodyStyle,
                    color = theme.listBulletColor,
                )
            }
            else -> {
                Text(
                    text = "•",
                    modifier = Modifier.width(UnorderedListMarkerWidth).align(Alignment.Top),
                    style = theme.bodyStyle,
                    color = theme.listBulletColor,
                )
            }
        }

        // 内容列
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(theme.blockSpacing),
        ) {
            MarkdownBlockChildren(node)
        }
    }
}

@Composable
internal fun RenderListBlockModel(
    model: ListBlockModel,
    renderChildren: @Composable (List<InternalRenderBlockModel>) -> Unit,
    modifier: Modifier = Modifier,
) {
    RenderListContainer(
        ordered = model.ordered,
        startNumber = model.startNumber,
        tight = model.tight,
        modifier = modifier,
        items = model.items.mapIndexed { index, item ->
            item.identity.stableId to {
                RenderListItemBlockModel(
                    model = item,
                    ordered = model.ordered,
                    index = model.startNumber + index,
                    renderChildren = renderChildren,
                )
            }
        },
    )
}

@Composable
internal fun RenderListLayoutBlockModel(
    model: LayoutListBlockModel,
    renderChildren: @Composable (List<InternalLayoutBlockModel>) -> Unit,
    modifier: Modifier = Modifier,
) {
    RenderListContainer(
        ordered = model.block.ordered,
        startNumber = model.block.startNumber,
        tight = model.block.tight,
        modifier = modifier,
        items = model.items.map { item ->
            item.identity.stableId to {
                RenderListItemLayoutGroup(
                    item = item,
                    renderChildren = renderChildren,
                )
            }
        },
    )
}

@Composable
private fun RenderListItemBlockModel(
    model: ListItemBlockModel,
    ordered: Boolean,
    index: Int,
    renderChildren: @Composable (List<InternalRenderBlockModel>) -> Unit,
    modifier: Modifier = Modifier,
) {
    val theme = LocalMarkdownTheme.current
    Row(
        modifier = modifier.fillMaxWidth().padding(start = theme.listIndent),
        horizontalArrangement = Arrangement.Start,
    ) {
        when {
            model.taskListItem -> {
                Checkbox(
                    checked = model.checked,
                    onCheckedChange = null,
                    modifier = Modifier.size(TaskListMarkerWidth).align(Alignment.Top),
                    colors = CheckboxDefaults.colors(
                        checkedColor = theme.taskCheckedColor,
                        uncheckedColor = theme.taskUncheckedColor,
                    ),
                )
            }

            ordered -> {
                Text(
                    text = "${index}.",
                    modifier = Modifier.width(OrderedListMarkerWidth).align(Alignment.Top),
                    style = theme.bodyStyle,
                    color = theme.listBulletColor,
                )
            }

            else -> {
                Text(
                    text = "•",
                    modifier = Modifier.width(UnorderedListMarkerWidth).align(Alignment.Top),
                    style = theme.bodyStyle,
                    color = theme.listBulletColor,
                )
            }
        }

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(theme.blockSpacing),
        ) {
            renderChildren(model.children)
        }
    }
}

@Composable
private fun RenderListItemLayoutGroup(
    item: LayoutListItemGroup,
    renderChildren: @Composable (List<InternalLayoutBlockModel>) -> Unit,
    modifier: Modifier = Modifier,
) {
    val theme = LocalMarkdownTheme.current
    Row(
        modifier = modifier.fillMaxWidth().padding(start = theme.listIndent),
        horizontalArrangement = Arrangement.Start,
    ) {
        when {
            item.taskListItem -> {
                Checkbox(
                    checked = item.checked,
                    onCheckedChange = null,
                    modifier = Modifier.size(TaskListMarkerWidth).align(Alignment.Top),
                    colors = CheckboxDefaults.colors(
                        checkedColor = theme.taskCheckedColor,
                        uncheckedColor = theme.taskUncheckedColor,
                    ),
                )
            }

            item.markerText.endsWith(".") -> {
                Text(
                    text = item.markerText,
                    modifier = Modifier.width(OrderedListMarkerWidth).align(Alignment.Top),
                    style = theme.bodyStyle,
                    color = theme.listBulletColor,
                )
            }

            else -> {
                Text(
                    text = item.markerText,
                    modifier = Modifier.width(UnorderedListMarkerWidth).align(Alignment.Top),
                    style = theme.bodyStyle,
                    color = theme.listBulletColor,
                )
            }
        }

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(theme.blockSpacing),
        ) {
            renderChildren(item.children)
        }
    }
}

@Composable
private fun RenderListContainer(
    ordered: Boolean,
    startNumber: Int,
    tight: Boolean,
    modifier: Modifier = Modifier,
    items: List<Pair<Long, @Composable () -> Unit>>,
) {
    val theme = LocalMarkdownTheme.current
    val spacing = if (tight) 2.dp else theme.blockSpacing
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(spacing),
    ) {
        items.forEach { (stableId, content) ->
            key(stableId, ordered, startNumber) {
                content()
            }
        }
    }
}
