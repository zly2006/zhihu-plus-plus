package com.hrm.markdown.renderer.block

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hrm.markdown.parser.ast.DirectiveBlock
import com.hrm.markdown.renderer.LocalMarkdownTheme
import com.hrm.markdown.renderer.MarkdownBlockChildren

/**
 * 块级指令渲染器：`{% tag args %}...{% endtag %}`。
 *
 * 显示指令标签名和参数，如果有子内容则渲染子块。
 */
@Composable
internal fun DirectiveBlockRenderer(
    node: DirectiveBlock,
    modifier: Modifier = Modifier,
) {
    RenderDirectiveFallbackBlock(
        tagName = node.tagName,
        args = node.args,
        modifier = modifier,
    ) {
        if (node.children.isNotEmpty()) {
            MarkdownBlockChildren(
                parent = node,
                modifier = Modifier.padding(top = 8.dp),
            )
        }
    }
}

@Composable
internal fun RenderDirectiveFallbackBlockModel(
    tagName: String,
    args: Map<String, String>,
    modifier: Modifier = Modifier,
    renderChildren: (@Composable () -> Unit)? = null,
) {
    RenderDirectiveFallbackBlock(
        tagName = tagName,
        args = args,
        modifier = modifier,
        renderChildren = renderChildren,
    )
}

@Composable
private fun RenderDirectiveFallbackBlock(
    tagName: String,
    args: Map<String, String>,
    modifier: Modifier = Modifier,
    renderChildren: (@Composable () -> Unit)? = null,
) {
    val theme = LocalMarkdownTheme.current
    val shape = RoundedCornerShape(8.dp)
    val borderColor = theme.blockQuoteBorderColor
    val backgroundColor = theme.codeBlockBackground
    val tagColor = theme.linkColor
    val argsColor = theme.blockQuoteTextColor

    Column(
        modifier = modifier
            .fillMaxWidth()
            .border(1.dp, borderColor, shape)
            .background(backgroundColor, shape)
            .padding(12.dp),
    ) {
        Row {
            Text(
                text = "{% $tagName",
                style = theme.bodyStyle.copy(
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp,
                    color = tagColor,
                ),
            )
            if (args.isNotEmpty()) {
                val argsText = args.entries.joinToString(" ") { (k, v) ->
                    if (k.startsWith("_")) v else "$k=$v"
                }
                Text(
                    text = " $argsText",
                    style = theme.bodyStyle.copy(
                        fontFamily = FontFamily.Monospace,
                        fontSize = 14.sp,
                        color = argsColor,
                    ),
                )
            }
            Text(
                text = " %}",
                style = theme.bodyStyle.copy(
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp,
                    color = tagColor,
                ),
            )
        }
        if (renderChildren != null) {
            Column(modifier = Modifier.padding(top = 8.dp)) {
                renderChildren()
            }
        }
    }
}
