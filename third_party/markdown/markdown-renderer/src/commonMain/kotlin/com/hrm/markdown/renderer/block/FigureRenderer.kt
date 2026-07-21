package com.hrm.markdown.renderer.block

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hrm.markdown.parser.ast.Figure
import com.hrm.markdown.renderer.DefaultMarkdownImage
import com.hrm.markdown.renderer.LocalImageRenderer
import com.hrm.markdown.renderer.LocalMarkdownTheme
import com.hrm.markdown.renderer.MarkdownImageData

/**
 * Figure 渲染器：将 Figure 节点渲染为图片 + 标题（figcaption）。
 *
 * 居中显示图片，下方显示斜体标题文本。
 */
@Composable
internal fun FigureRenderer(
    node: Figure,
    modifier: Modifier = Modifier,
) {
    val theme = LocalMarkdownTheme.current
    val customRenderer = LocalImageRenderer.current

    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        // 渲染图片
        val imageData = MarkdownImageData(
            url = node.imageUrl,
            altText = node.caption,
            title = node.caption,
            width = node.imageWidth,
            height = node.imageHeight,
            attributes = node.attributes,
        )
        if (customRenderer != null) {
            customRenderer(imageData, Modifier)
        } else {
            DefaultMarkdownImage(data = imageData)
        }

        // 渲染 figcaption（标题）
        if (node.caption.isNotEmpty()) {
            BasicText(
                text = node.caption,
                style = theme.bodyStyle.copy(
                    fontStyle = FontStyle.Italic,
                    fontSize = theme.bodyStyle.fontSize * 0.875f,
                    textAlign = TextAlign.Center,
                    color = theme.blockQuoteTextColor,
                ),
                modifier = Modifier.padding(top = 4.dp, start = 16.dp, end = 16.dp),
            )
        }
    }
}
