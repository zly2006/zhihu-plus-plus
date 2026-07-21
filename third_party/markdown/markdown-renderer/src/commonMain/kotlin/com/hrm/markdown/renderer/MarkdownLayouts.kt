package com.hrm.markdown.renderer

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import com.hrm.markdown.parser.ast.BlankLine
import com.hrm.markdown.parser.ast.ContainerNode
import com.hrm.markdown.parser.ast.Node
import com.hrm.markdown.renderer.block.BlockRenderer
import com.hrm.markdown.renderer.block.blockRenderRevision

@Composable
internal fun MarkdownDocumentLayout(
    renderMode: MarkdownRenderMode,
    renderState: MarkdownBlockRenderState,
    modifier: Modifier,
    enableScroll: Boolean,
    scrollState: ScrollState,
    lazyListState: LazyListState,
    header: (@Composable () -> Unit)? = null,
    footer: (@Composable () -> Unit)? = null,
) {
    when (renderMode) {
        MarkdownRenderMode.LazyColumn -> {
            MarkdownBlockLazyColumn(
                blocks = renderState.blockNodes,
                lazyListState = lazyListState,
                modifier = modifier.graphicsLayer { },
                header = header,
                footer = footer,
            )
        }

        MarkdownRenderMode.SelectableColumn,
        MarkdownRenderMode.StaticColumn -> {
            val markdownBody: @Composable () -> Unit = {
                MarkdownBlockColumn(blocks = renderState.renderBlocks)
            }
            val theme = LocalMarkdownTheme.current
            Column(
                modifier = modifier
                    .then(if (enableScroll) Modifier.verticalScroll(scrollState) else Modifier)
                    .graphicsLayer { },
                verticalArrangement = Arrangement.spacedBy(theme.blockSpacing),
            ) {
                header?.invoke()
                if (renderMode == MarkdownRenderMode.SelectableColumn) {
                    SelectionContainer {
                        markdownBody()
                    }
                } else {
                    markdownBody()
                }
                footer?.invoke()
            }
        }
    }
}

@Composable
internal fun MarkdownBlockChildren(
    parent: ContainerNode,
    modifier: Modifier = Modifier,
) {
    val blockNodes = parent.children.filter { it !is BlankLine }
    MarkdownBlockColumn(blocks = blockNodes, modifier = modifier)
}

@Composable
internal fun MarkdownBlockColumn(
    blocks: List<Node>,
    modifier: Modifier = Modifier,
) {
    val theme = LocalMarkdownTheme.current
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(theme.blockSpacing),
    ) {
        MarkdownBlockItems(blocks)
    }
}

@Composable
private fun MarkdownBlockItems(blocks: List<Node>) {
    for (node in blocks) {
        key(node::class, node.stableKey) {
            BlockRenderer(
                node = node,
                renderRevision = blockRenderRevision(node),
            )
        }
    }
}

@Composable
private fun MarkdownBlockLazyColumn(
    blocks: List<Node>,
    lazyListState: LazyListState,
    modifier: Modifier = Modifier,
    header: (@Composable () -> Unit)? = null,
    footer: (@Composable () -> Unit)? = null,
) {
    val theme = LocalMarkdownTheme.current
    LazyColumn(
        state = lazyListState,
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(theme.blockSpacing),
    ) {
        if (header != null) {
            item(key = "markdown_header") {
                header()
            }
        }
        items(
            items = blocks,
            key = { it.stableKey },
        ) { node ->
            BlockRenderer(
                node = node,
                renderRevision = blockRenderRevision(node),
            )
        }
        if (footer != null) {
            item(key = "markdown_footer") {
                footer()
            }
        }
    }
}
