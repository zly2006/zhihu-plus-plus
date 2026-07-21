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

/**
 * 内容标签页渲染器（MkDocs Material 风格）。
 */
@Composable
internal fun TabBlockRenderer(
    node: TabBlock,
    modifier: Modifier = Modifier,
) {
    val theme = LocalMarkdownTheme.current
    val tabItems = remember(node) { node.children.filterIsInstance<TabItem>() }
    if (tabItems.isEmpty()) return

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
            tabItems.forEachIndexed { index, tabItem ->
                val isSelected = index == selectedIndex
                Box(
                    modifier = Modifier
                        .clickable { selectedIndex = index }
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                ) {
                    Text(
                        text = tabItem.title,
                        style = theme.bodyStyle.copy(
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            color = if (isSelected) theme.linkColor else Color.Gray,
                        ),
                    )
                }
            }
        }

        // 选中的标签内容
        val selectedTab = tabItems.getOrNull(selectedIndex)
        if (selectedTab != null) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
            ) {
                MarkdownBlockChildren(
                    parent = selectedTab,
                )
            }
        }
    }
}
