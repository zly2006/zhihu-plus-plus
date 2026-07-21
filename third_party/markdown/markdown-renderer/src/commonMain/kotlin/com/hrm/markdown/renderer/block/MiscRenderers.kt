package com.hrm.markdown.renderer.block

import androidx.compose.foundation.clickable
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.hrm.markdown.parser.ast.BlankLine
import com.hrm.markdown.parser.ast.DefinitionDescription
import com.hrm.markdown.parser.ast.DefinitionList
import com.hrm.markdown.parser.ast.DefinitionTerm
import com.hrm.markdown.parser.ast.FootnoteDefinition
import com.hrm.markdown.parser.ast.HtmlBlock
import com.hrm.markdown.parser.ast.Paragraph
import com.hrm.markdown.parser.ast.Node
import com.hrm.markdown.renderer.LocalFootnoteNavigationState
import com.hrm.markdown.renderer.LocalMarkdownTheme
import com.hrm.markdown.renderer.LocalOnFootnoteBackClick
import com.hrm.markdown.renderer.MarkdownBlockChildren
import com.hrm.markdown.renderer.inline.InlineFlowText
import com.hrm.markdown.renderer.inline.rememberInlineContent

/**
 * HTML 块渲染器：以等宽字体显示原始 HTML。
 */
@Composable
internal fun HtmlBlockRenderer(
    node: HtmlBlock,
    modifier: Modifier = Modifier,
) {
    val theme = LocalMarkdownTheme.current
    Text(
        text = node.literal.trimEnd('\n'),
        modifier = modifier.fillMaxWidth(),
        style = theme.codeBlockStyle.copy(fontFamily = FontFamily.Monospace),
    )
}

/**
 * 定义列表渲染器。
 */
@Composable
internal fun DefinitionListRenderer(
    node: DefinitionList,
    modifier: Modifier = Modifier,
) {
    val theme = LocalMarkdownTheme.current

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        for (child in node.children) {
            when (child) {
                is DefinitionTerm -> {
                    val inlineResult = rememberInlineContent(
                        parent = child,
                        hostTextStyle = theme.bodyStyle.copy(fontWeight = FontWeight.Bold),
                    )
                    InlineFlowText(
                        annotated = inlineResult.annotated,
                        inlineContents = inlineResult.inlineContents,
                        style = theme.bodyStyle.copy(fontWeight = FontWeight.Bold),
                    )
                }
                is DefinitionDescription -> {
                    MarkdownBlockChildren(
                        parent = child,
                        modifier = Modifier.padding(start = 24.dp),
                    )
                }
                else -> BlockRenderer(child)
            }
        }
    }
}

/**
 * 脚注定义渲染器。
 */
@Composable
internal fun FootnoteDefinitionRenderer(
    node: FootnoteDefinition,
    modifier: Modifier = Modifier,
) {
    val theme = LocalMarkdownTheme.current
    val footnoteNavigationState = LocalFootnoteNavigationState.current
    val onFootnoteBackClick = LocalOnFootnoteBackClick.current
    val bringIntoViewRequester = remember { BringIntoViewRequester() }

    DisposableEffect(footnoteNavigationState, node.label, bringIntoViewRequester) {
        footnoteNavigationState?.registerDefinition(node.label, bringIntoViewRequester)
        onDispose {
            footnoteNavigationState?.unregisterDefinition(node.label, bringIntoViewRequester)
        }
    }

    val contentBlocks = remember(node) {
        node.children.filter { it !is BlankLine }
    }
    val firstBlock = contentBlocks.firstOrNull()
    val remainingBlocks = if (contentBlocks.size > 1) contentBlocks.drop(1) else emptyList()

    Column(
        modifier = modifier
            .fillMaxWidth()
            .bringIntoViewRequester(bringIntoViewRequester)
            .padding(top = 4.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.Top,
        ) {
            Text(
                text = "[${node.index}]",
                style = theme.bodyStyle.copy(
                    fontWeight = FontWeight.SemiBold,
                    fontSize = theme.footnoteStyle.fontSize,
                ),
            )
            Text(
                text = "↩",
                modifier = Modifier
                    .then(
                        if (onFootnoteBackClick != null) {
                            Modifier.clickable { onFootnoteBackClick(node.label) }
                        } else {
                            Modifier
                        }
                    ),
                style = theme.bodyStyle.copy(
                    color = theme.linkColor,
                    fontSize = theme.footnoteStyle.fontSize,
                ),
            )
            Box(modifier = Modifier.weight(1f)) {
                if (firstBlock != null) {
                    key(firstBlock::class, firstBlock.stableKey) {
                        FootnoteContentBlock(firstBlock)
                    }
                }
            }
        }
        if (remainingBlocks.isNotEmpty()) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(theme.blockSpacing),
            ) {
                for (child in remainingBlocks) {
                    key(child::class, child.stableKey) {
                        FootnoteContentBlock(child)
                    }
                }
            }
        }
    }
}

@Composable
private fun FootnoteContentBlock(node: Node) {
    when (node) {
        is Paragraph -> ParagraphRenderer(node, Modifier.fillMaxWidth())
        else -> BlockRenderer(
            node = node,
            renderRevision = blockRenderRevision(node),
            modifier = Modifier.fillMaxWidth(),
        )
    }
}
