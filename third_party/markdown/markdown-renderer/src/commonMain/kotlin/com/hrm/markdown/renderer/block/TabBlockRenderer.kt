package com.hrm.markdown.renderer.block

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.hrm.markdown.parser.ast.TabBlock
import com.hrm.markdown.parser.ast.TabItem
import com.hrm.markdown.renderer.LocalMarkdownTheme
import com.hrm.markdown.renderer.MarkdownBlockChildren
import com.hrm.markdown.renderer.internal.core.model.InternalRenderBlockModel
import com.hrm.markdown.renderer.internal.core.model.TabBlockModel
import com.hrm.markdown.renderer.internal.layout.model.InternalLayoutBlockModel
import com.hrm.markdown.renderer.internal.layout.model.LayoutTabBlockModel

/**
 * 内容标签页渲染器（MkDocs Material 风格）。
 */
@Composable
internal fun TabBlockRenderer(
    node: TabBlock,
    modifier: Modifier = Modifier,
) {
    val tabItems = remember(node) { node.children.filterIsInstance<TabItem>() }
    if (tabItems.isEmpty()) return
    RenderTabContainer(
        titles = tabItems.map { it.title },
        modifier = modifier,
    ) { selectedIndex ->
        val selectedTab = tabItems.getOrNull(selectedIndex)
        if (selectedTab != null) {
            MarkdownBlockChildren(parent = selectedTab)
        }
    }
}

@Composable
internal fun RenderTabBlockModel(
    model: TabBlockModel,
    renderChildren: @Composable (List<InternalRenderBlockModel>) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (model.items.isEmpty()) return
    RenderTabContainer(
        titles = model.items.map { it.title },
        modifier = modifier,
    ) { selectedIndex ->
        model.items.getOrNull(selectedIndex)?.let { tab ->
            renderChildren(tab.children)
        }
    }
}

@Composable
internal fun RenderTabLayoutBlockModel(
    model: LayoutTabBlockModel,
    renderChildren: @Composable (List<InternalLayoutBlockModel>) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (model.tabs.isEmpty()) return
    RenderTabContainer(
        titles = model.tabs.map { it.title },
        modifier = modifier,
    ) { selectedIndex ->
        model.tabs.getOrNull(selectedIndex)?.let { tab ->
            renderChildren(tab.children)
        }
    }
}

@Composable
private fun RenderTabContainer(
    titles: List<String>,
    modifier: Modifier = Modifier,
    content: @Composable (selectedIndex: Int) -> Unit,
) {
    val theme = LocalMarkdownTheme.current
    var selectedIndex by remember { mutableIntStateOf(0) }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(theme.codeBlockBackground),
    ) {
        // 标签栏
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(theme.codeBlockTitleBackground)
                .padding(horizontal = 4.dp),
        ) {
            titles.forEachIndexed { index, title ->
                val isSelected = index == selectedIndex
                Box(
                    modifier = Modifier
                        .clickable { selectedIndex = index }
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                ) {
                    Text(
                        text = title,
                        style = theme.bodyStyle.copy(
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            color = if (isSelected) theme.linkColor else Color.Gray,
                        ),
                    )
                }
            }
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
        ) {
            content(selectedIndex)
        }
    }
}
