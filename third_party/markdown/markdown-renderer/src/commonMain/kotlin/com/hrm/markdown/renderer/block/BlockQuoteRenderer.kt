package com.hrm.markdown.renderer.block

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import com.hrm.markdown.parser.ast.BlockQuote
import com.hrm.markdown.renderer.LocalMarkdownTheme
import com.hrm.markdown.renderer.MarkdownBlockChildren

/**
 * 块引用渲染器 (> ...)
 * 左侧绘制竖线，内部递归渲染子块。
 */
@Composable
internal fun BlockQuoteRenderer(
    node: BlockQuote,
    modifier: Modifier = Modifier,
) {
    val theme = LocalMarkdownTheme.current
    val borderColor = theme.blockQuoteBorderColor
    val borderWidthPx = theme.blockQuoteBorderWidth

    Box(
        modifier = modifier
            .fillMaxWidth()
            .drawBehind {
                val strokeWidth = borderWidthPx.toPx()
                drawLine(
                    color = borderColor,
                    start = Offset(strokeWidth / 2, 0f),
                    end = Offset(strokeWidth / 2, size.height),
                    strokeWidth = strokeWidth,
                )
            }
            .padding(start = theme.blockQuotePadding + theme.blockQuoteBorderWidth),
    ) {
        MarkdownBlockChildren(node)
    }
}
