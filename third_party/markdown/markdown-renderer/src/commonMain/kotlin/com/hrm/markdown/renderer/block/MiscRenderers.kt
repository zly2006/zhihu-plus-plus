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
import androidx.compose.ui.layout.FirstBaseline
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
import com.hrm.markdown.renderer.inline.InlineLayoutBlockText
import com.hrm.markdown.renderer.inline.rememberInlineModel
import com.hrm.markdown.renderer.internal.core.model.DefinitionDescriptionBlockModel
import com.hrm.markdown.renderer.internal.core.model.DefinitionListBlockModel
import com.hrm.markdown.renderer.internal.core.model.DefinitionTermBlockModel
import com.hrm.markdown.renderer.internal.core.model.FootnoteDefinitionBlockModel
import com.hrm.markdown.renderer.internal.core.model.HtmlBlockModel
import com.hrm.markdown.renderer.internal.core.model.InternalRenderBlockModel
import com.hrm.markdown.renderer.internal.layout.model.LayoutTocBlockModel
import com.hrm.markdown.renderer.internal.layout.model.LayoutDefinitionDescriptionGroup
import com.hrm.markdown.renderer.internal.layout.model.LayoutDefinitionListBlockModel
import com.hrm.markdown.renderer.internal.layout.model.LayoutDefinitionTermGroup
import com.hrm.markdown.renderer.internal.layout.model.InternalLayoutBlockModel
import com.hrm.markdown.renderer.internal.layout.model.LayoutFootnoteBlockModel

/**
 * HTML 块渲染器：以等宽字体显示原始 HTML。
 */
@Composable
internal fun HtmlBlockRenderer(
    node: HtmlBlock,
    modifier: Modifier = Modifier,
) {
    RenderHtmlBlockModel(
        model = HtmlBlockModel(
            identity = com.hrm.markdown.renderer.internal.core.identity.RenderIdentity(
                stableId = node.stableKey.toLong(),
                contentRevision = node.contentHash,
                layoutRevision = node.contentHash,
                paintRevision = 0L,
            ),
            html = node.literal,
        ),
        modifier = modifier,
    )
}

@Composable
internal fun RenderHtmlBlockModel(
    model: HtmlBlockModel,
    modifier: Modifier = Modifier,
) {
    val theme = LocalMarkdownTheme.current
    Text(
        text = model.html.trimEnd('\n'),
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
                    val inlineModel = rememberInlineModel(child)
                    InlineLayoutBlockText(
                        model = inlineModel,
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

@Composable
internal fun RenderDefinitionListBlockModel(
    model: DefinitionListBlockModel,
    renderChildren: @Composable (List<InternalRenderBlockModel>) -> Unit,
    modifier: Modifier = Modifier,
) {
    val theme = LocalMarkdownTheme.current
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        for (item in model.items) {
            when (item) {
                is DefinitionTermBlockModel -> {
                    InlineLayoutBlockText(
                        model = item.inline,
                        style = theme.bodyStyle.copy(fontWeight = FontWeight.Bold),
                    )
                }

                is DefinitionDescriptionBlockModel -> {
                    Column(
                        modifier = Modifier.padding(start = 24.dp),
                        verticalArrangement = Arrangement.spacedBy(theme.blockSpacing),
                    ) {
                        renderChildren(item.children)
                    }
                }
            }
        }
    }
}

@Composable
internal fun RenderDefinitionListLayoutBlockModel(
    model: LayoutDefinitionListBlockModel,
    renderChildren: @Composable (List<InternalLayoutBlockModel>) -> Unit,
    modifier: Modifier = Modifier,
) {
    val theme = LocalMarkdownTheme.current
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        for (item in model.items) {
            when (item) {
                is LayoutDefinitionTermGroup -> {
                    InlineLayoutBlockText(
                        model = item.item.inline,
                        style = theme.bodyStyle.copy(fontWeight = FontWeight.Bold),
                    )
                }

                is LayoutDefinitionDescriptionGroup -> {
                    Column(
                        modifier = Modifier.padding(start = 24.dp),
                        verticalArrangement = Arrangement.spacedBy(theme.blockSpacing),
                    ) {
                        renderChildren(item.children)
                    }
                }
            }
        }
    }
}

@Composable
internal fun RenderTocLayoutBlockModel(
    model: LayoutTocBlockModel,
    modifier: Modifier = Modifier,
) {
    val theme = LocalMarkdownTheme.current
    val onLinkClick = com.hrm.markdown.renderer.LocalOnLinkClick.current
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        model.entries.forEach { item ->
            Text(
                text = item.entry.text,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = ((item.entry.level - 1).coerceAtLeast(0) * 16).dp)
                    .then(
                        if (!item.entry.id.isNullOrBlank() && onLinkClick != null) {
                            Modifier.clickable { onLinkClick("#${item.entry.id}") }
                        } else {
                            Modifier
                        }
                    ),
                style = theme.bodyStyle,
                color = theme.linkColor,
            )
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
    val contentBlocks = remember(node) {
        node.children.filter { it !is BlankLine }
    }
    RenderFootnoteDefinitionBlockModel(
        model = FootnoteDefinitionBlockModel(
            identity = com.hrm.markdown.renderer.internal.core.identity.RenderIdentity(
                stableId = node.stableKey.toLong(),
                contentRevision = node.contentHash,
                layoutRevision = node.contentHash,
                paintRevision = 0L,
            ),
            label = node.label,
            index = node.index,
            children = emptyList(),
        ),
        modifier = modifier,
        renderLeadContent = {
            contentBlocks.firstOrNull()?.let { firstBlock ->
                key(firstBlock::class, firstBlock.stableKey) {
                    FootnoteContentBlock(node = firstBlock)
                }
            }
        },
        renderTrailingContent = {
            if (contentBlocks.size > 1) {
            val theme = LocalMarkdownTheme.current
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(theme.blockSpacing),
            ) {
                for (child in contentBlocks.drop(1)) {
                    key(child::class, child.stableKey) {
                        FootnoteContentBlock(child)
                    }
                }
            }
        }
        },
    )
}

@Composable
internal fun RenderFootnoteDefinitionBlockModel(
    model: FootnoteDefinitionBlockModel,
    modifier: Modifier = Modifier,
    renderLeadContent: @Composable () -> Unit,
    renderTrailingContent: @Composable () -> Unit,
) {
    val theme = LocalMarkdownTheme.current
    val footnoteNavigationState = LocalFootnoteNavigationState.current
    val onFootnoteBackClick = LocalOnFootnoteBackClick.current
    val bringIntoViewRequester = remember { BringIntoViewRequester() }

    DisposableEffect(footnoteNavigationState, model.label, bringIntoViewRequester) {
        footnoteNavigationState?.registerDefinition(model.label, bringIntoViewRequester)
        onDispose {
            footnoteNavigationState?.unregisterDefinition(model.label, bringIntoViewRequester)
        }
    }

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
                text = "[${model.index}]",
                modifier = Modifier.alignByBaseline(),
                style = theme.bodyStyle.copy(
                    fontWeight = FontWeight.SemiBold,
                    fontSize = theme.footnoteStyle.fontSize,
                ),
            )
            Text(
                text = "↩",
                modifier = Modifier
                    .alignByBaseline()
                    .then(
                        if (onFootnoteBackClick != null) {
                            Modifier.clickable { onFootnoteBackClick(model.label) }
                        } else {
                            Modifier
                        }
                    ),
                style = theme.bodyStyle.copy(
                    color = theme.linkColor,
                    fontSize = theme.footnoteStyle.fontSize,
                ),
            )
            Box(
                modifier = Modifier
                    .weight(1f)
                    .alignBy(FirstBaseline),
            ) {
                renderLeadContent()
            }
        }
        renderTrailingContent()
    }
}

@Composable
internal fun RenderFootnoteLayoutBlockModel(
    model: LayoutFootnoteBlockModel,
    renderLeadChild: @Composable (InternalLayoutBlockModel?) -> Unit,
    renderTrailingChildren: @Composable (List<InternalLayoutBlockModel>) -> Unit,
    modifier: Modifier = Modifier,
) {
    RenderFootnoteDefinitionBlockModel(
        model = model.block,
        modifier = modifier,
        renderLeadContent = {
            renderLeadChild(model.leadChild)
        },
        renderTrailingContent = {
            if (model.trailingChildren.isNotEmpty()) {
            renderTrailingChildren(model.trailingChildren)
        }
        },
    )
}

@Composable
private fun FootnoteContentBlock(
    node: Node,
    modifier: Modifier = Modifier,
) {
    when (node) {
        is Paragraph -> ParagraphRenderer(node, modifier.fillMaxWidth())
        else -> BlockRenderer(node = node, modifier = modifier.fillMaxWidth())
    }
}
