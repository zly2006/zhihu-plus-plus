package com.hrm.markdown.renderer.block

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.hrm.markdown.parser.ast.Admonition
import com.hrm.markdown.renderer.AdmonitionStyle
import com.hrm.markdown.renderer.LocalMarkdownTheme
import com.hrm.markdown.renderer.MarkdownBlockChildren

/**
 * Admonition 渲染器 (> [!NOTE], > [!WARNING] 等)。
 */
@Composable
internal fun AdmonitionRenderer(
    node: Admonition,
    modifier: Modifier = Modifier,
) {
    val theme = LocalMarkdownTheme.current
    val style = theme.admonitionStyles[node.type.uppercase()]
        ?: theme.admonitionStyles["NOTE"]
        ?: return

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(style.backgroundColor)
            .drawBehind {
                drawLine(
                    color = style.borderColor,
                    start = Offset(0f, 0f),
                    end = Offset(0f, size.height),
                    strokeWidth = 4.dp.toPx(),
                )
            }
            .padding(start = 16.dp, top = 12.dp, end = 12.dp, bottom = 12.dp),
    ) {
        // 标题行
        Row {
            Text(
                text = style.iconText,
                modifier = Modifier.padding(end = 8.dp),
            )
            Text(
                text = node.title.ifEmpty { node.type.uppercase() },
                style = theme.bodyStyle.copy(
                    fontWeight = FontWeight.SemiBold,
                    color = style.titleColor,
                ),
            )
        }

        // 内容
        if (node.children.isNotEmpty()) {
            MarkdownBlockChildren(
                parent = node,
                modifier = Modifier.padding(top = 8.dp),
            )
        }
    }
}
