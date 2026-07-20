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
import com.hrm.markdown.renderer.internal.core.model.FigureBlockModel
import com.hrm.markdown.renderer.internal.layout.model.LayoutFigureBlockModel

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
    RenderFigureBlockModel(
        model = FigureBlockModel(
            identity = com.hrm.markdown.renderer.internal.core.identity.RenderIdentity(
                stableId = node.stableKey.toLong(),
                contentRevision = node.contentHash,
                layoutRevision = node.contentHash,
                paintRevision = 0L,
            ),
            imageUrl = node.imageUrl,
            caption = node.caption,
            imageWidth = node.imageWidth,
            imageHeight = node.imageHeight,
            attributes = node.attributes,
        ),
        modifier = modifier,
    )
}

@Composable
internal fun RenderFigureBlockModel(
    model: FigureBlockModel,
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
            url = model.imageUrl,
            altText = model.caption,
            title = model.caption,
            width = model.imageWidth,
            height = model.imageHeight,
            attributes = model.attributes,
        )
        if (customRenderer != null) {
            customRenderer(imageData, Modifier)
        } else {
            DefaultMarkdownImage(data = imageData)
        }

        // 渲染 figcaption（标题）
        if (model.caption.isNotEmpty()) {
            BasicText(
                text = model.caption,
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

@Composable
internal fun RenderFigureLayoutBlockModel(
    model: LayoutFigureBlockModel,
    modifier: Modifier = Modifier,
) {
    RenderFigureBlockModel(
        model = model.block,
        modifier = modifier,
    )
}
