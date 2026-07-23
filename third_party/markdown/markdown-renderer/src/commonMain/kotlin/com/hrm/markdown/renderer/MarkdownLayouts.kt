package com.hrm.markdown.renderer

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.unit.dp
import com.hrm.markdown.parser.ast.AbbreviationDefinition
import com.hrm.markdown.parser.ast.BlankLine
import com.hrm.markdown.parser.ast.ContainerNode
import com.hrm.markdown.parser.ast.FencedCodeBlock
import com.hrm.markdown.parser.ast.Figure
import com.hrm.markdown.parser.ast.FootnoteDefinition
import com.hrm.markdown.parser.ast.FootnoteReference
import com.hrm.markdown.parser.ast.FrontMatter
import com.hrm.markdown.parser.ast.HardLineBreak
import com.hrm.markdown.parser.ast.Heading
import com.hrm.markdown.parser.ast.Image
import com.hrm.markdown.parser.ast.InlineCode
import com.hrm.markdown.parser.ast.InlineMath
import com.hrm.markdown.parser.ast.LinkReferenceDefinition
import com.hrm.markdown.parser.ast.MathBlock
import com.hrm.markdown.parser.ast.Node
import com.hrm.markdown.parser.ast.Paragraph
import com.hrm.markdown.parser.ast.SetextHeading
import com.hrm.markdown.parser.ast.SoftLineBreak
import com.hrm.markdown.parser.ast.Text
import com.hrm.markdown.parser.ast.ThematicBreak
import com.hrm.markdown.renderer.block.BlockRenderer
import com.hrm.markdown.renderer.block.blockRenderRevision
import kotlin.math.ceil

@Composable
internal fun MarkdownDocumentLayout(
    renderMode: MarkdownRenderMode,
    renderState: MarkdownBlockRenderState,
    modifier: Modifier,
    enableScroll: Boolean,
    scrollState: ScrollState,
    lazyListState: LazyListState,
    deferOffscreenBlocks: Boolean,
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
                MarkdownBlockColumn(
                    blocks = renderState.renderBlocks,
                    deferOffscreenBlocks = deferOffscreenBlocks,
                )
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
    MarkdownBlockColumn(
        blocks = blockNodes,
        modifier = modifier,
        deferOffscreenBlocks = false,
    )
}

@Composable
internal fun MarkdownBlockColumn(
    blocks: List<Node>,
    modifier: Modifier = Modifier,
    deferOffscreenBlocks: Boolean,
) {
    val theme = LocalMarkdownTheme.current
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(theme.blockSpacing),
    ) {
        MarkdownBlockItems(blocks, deferOffscreenBlocks)
    }
}

@Composable
private fun MarkdownBlockItems(
    blocks: List<Node>,
    deferOffscreenBlocks: Boolean,
) {
    for (node in blocks) {
        key(node::class, node.stableKey) {
            if (deferOffscreenBlocks) {
                DeferredMarkdownBlock(node)
            } else {
                BlockRenderer(
                    node = node,
                    renderRevision = blockRenderRevision(node),
                )
            }
        }
    }
}

@Composable
private fun DeferredMarkdownBlock(node: Node) {
    val theme = LocalMarkdownTheme.current
    val footnoteNavigationState = LocalFootnoteNavigationState.current
    val density = LocalDensity.current
    val viewportHeightPx = LocalWindowInfo.current.containerSize.height.toFloat()
    var materialized by remember(node) { mutableStateOf(false) }
    var measuredHeightDp by remember(node) { mutableStateOf<Float?>(null) }
    val requestedByNavigation =
        footnoteNavigationState?.let { navigationState ->
            (node is FootnoteDefinition && navigationState.isDefinitionRequested(node.label)) ||
                node.hasRequestedFootnoteReference(navigationState)
        } == true
    val renderBlock = materialized || requestedByNavigation
    BoxWithConstraints(
        modifier = Modifier
            .fillMaxWidth()
            .onGloballyPositioned { coordinates ->
                if (viewportHeightPx > 0f) {
                    val top = coordinates.positionInWindow().y
                    val bottom = top + coordinates.size.height
                    val prefetchPx = viewportHeightPx * 0.5f
                    val nearViewport = bottom >= -prefetchPx && top <= viewportHeightPx + prefetchPx
                    if (renderBlock && coordinates.size.height > 0) {
                        measuredHeightDp = with(density) { coordinates.size.height.toDp().value }
                    }
                    if (materialized != nearViewport) {
                        materialized = nearViewport
                    }
                }
            },
    ) {
        if (renderBlock) {
            BlockRenderer(
                node = node,
                renderRevision = blockRenderRevision(node),
            )
        } else {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(
                        (measuredHeightDp ?: estimateMarkdownBlockHeightDp(node, maxWidth.value, theme)).dp,
                    ),
            )
        }
    }
}

private fun Node.hasRequestedFootnoteReference(navigationState: FootnoteNavigationState): Boolean = when (this) {
    is FootnoteReference -> navigationState.isReferenceRequested(label)
    is ContainerNode -> children.any { it.hasRequestedFootnoteReference(navigationState) }
    else -> false
}

internal fun estimateMarkdownBlockHeightDp(
    node: Node,
    widthDp: Float,
    theme: MarkdownTheme,
): Float {
    val fontSize = theme.bodyStyle.fontSize.value.takeIf { it.isFinite() && it > 0f } ?: 16f
    val lineHeight = theme.bodyStyle.lineHeight.value.takeIf { it.isFinite() && it > 0f } ?: fontSize * 1.5f
    val safeWidth = widthDp.coerceAtLeast(fontSize * 8f)
    return when (node) {
        is BlankLine, is FrontMatter, is LinkReferenceDefinition, is AbbreviationDefinition -> 0f
        is Paragraph -> {
            val widthUnits = node.estimatedInlineWidthUnits(fontSize, safeWidth)
            val forcedLines = node.countLineBreaks()
            (ceil(widthUnits / safeWidth).toInt().coerceAtLeast(1) + forcedLines) * lineHeight
        }
        is Heading -> lineHeight * (1.9f - node.level.coerceIn(1, 6) * 0.1f)
        is SetextHeading -> lineHeight * 1.7f
        is MathBlock -> {
            val rows = 1 + node.literal.windowed(2).count { it == "\\\\" }
            val tallCommands = listOf("\\frac", "\\dfrac", "\\sqrt", "\\sum", "\\int")
                .count { it in node.literal }
            theme.codeBlockPadding.value * 2f +
                theme.mathFontSize * 1.2f * (1.4f * rows + 0.25f * tallCommands)
        }
        is Figure -> {
            val ratio = node.imageWidth
                ?.takeIf { it > 0 }
                ?.let { width -> node.imageHeight?.toFloat()?.div(width) }
                ?: 0.75f
            safeWidth * 0.8f * ratio.coerceIn(0.25f, 3f) +
                if (node.caption.isBlank()) 0f else lineHeight
        }
        is FencedCodeBlock ->
            node.literal.lineSequence().count().coerceAtLeast(1) * lineHeight + theme.codeBlockPadding.value * 2f
        is ThematicBreak -> theme.dividerThickness.value.coerceAtLeast(1f)
        is ContainerNode -> {
            val children = node.children.filterNot { it is BlankLine }
            children.sumOf { estimateMarkdownBlockHeightDp(it, safeWidth, theme).toDouble() }.toFloat() +
                theme.blockSpacing.value * (children.size - 1).coerceAtLeast(0)
        }
        else -> lineHeight * 1.5f
    }.coerceAtLeast(0f)
}

private fun Node.estimatedInlineWidthUnits(
    fontSize: Float,
    availableWidth: Float,
): Float = when (this) {
    is Text -> literal.sumOf { character -> if (character.code > 0x7f) 1.0 else 0.55 }.toFloat() * fontSize
    is InlineMath -> (literal.length * 0.7f).coerceIn(1.5f, 20f) * fontSize * 1.125f
    is InlineCode -> literal.length.coerceAtLeast(1) * fontSize * 0.65f
    is Image, is HardLineBreak, is SoftLineBreak -> availableWidth
    is ContainerNode -> children.sumOf { it.estimatedInlineWidthUnits(fontSize, availableWidth).toDouble() }.toFloat()
    else -> 0f
}

private fun Node.countLineBreaks(): Int = when (this) {
    is HardLineBreak, is SoftLineBreak -> 1
    is ContainerNode -> children.sumOf { it.countLineBreaks() }
    else -> 0
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
