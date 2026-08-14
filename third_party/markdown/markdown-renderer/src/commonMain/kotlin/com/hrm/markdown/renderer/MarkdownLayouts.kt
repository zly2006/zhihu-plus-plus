package com.hrm.markdown.renderer

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.SubcomposeLayout
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
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
import com.hrm.markdown.renderer.selection.PersistentSelectionContainer
import com.hrm.markdown.renderer.selection.PersistentSelectionGroup
import com.hrm.markdown.renderer.selection.PersistentSelectionScope
import kotlin.math.ceil

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
                    PersistentSelectionContainer(documentKey = LocalRendererDocument.current) {
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
    )
}

@Composable
internal fun MarkdownBlockColumn(
    blocks: List<Node>,
    modifier: Modifier = Modifier,
) {
    DeferredMarkdownColumn(
        blocks = blocks,
        modifier = modifier,
    ) { _, node ->
        BlockRenderer(
            node = node,
            renderRevision = blockRenderRevision(node),
        )
    }
}

@Composable
internal fun <T : Node> DeferredMarkdownColumn(
    blocks: List<T>,
    modifier: Modifier = Modifier,
    blockSpacing: Dp? = null,
    prefetchViewports: Float? = null,
    estimateHeightDp: (T, Float, MarkdownTheme) -> Float = ::estimateMarkdownBlockHeightDp,
    blockContent: @Composable (Int, T) -> Unit,
) {
    val groupKey = blocks.firstOrNull()?.parent ?: blocks
    PersistentSelectionGroup(groupKey) { groupOrder ->
        DeferredMarkdownBlockLayout(
            blocks = blocks,
            groupOrder = groupOrder,
            modifier = modifier,
            blockSpacing = blockSpacing,
            prefetchViewports = prefetchViewports,
            estimateHeightDp = estimateHeightDp,
            blockContent = blockContent,
        )
    }
}

@Composable
private fun <T : Node> DeferredMarkdownBlockLayout(
    blocks: List<T>,
    groupOrder: List<Int>,
    modifier: Modifier,
    blockSpacing: Dp?,
    prefetchViewports: Float?,
    estimateHeightDp: (T, Float, MarkdownTheme) -> Float,
    blockContent: @Composable (Int, T) -> Unit,
) {
    val theme = LocalMarkdownTheme.current
    val footnoteNavigationState = LocalFootnoteNavigationState.current
    val density = LocalDensity.current
    val viewportHeightPx = LocalWindowInfo.current.containerSize.height.toFloat()
    val deferredBlockStates = rememberSaveableStateHolder()
    val measuredHeightsPx = remember { mutableStateMapOf<String, Int>() }
    val blockKeys = remember(blocks) { blocks.map(Node::stableKey) }
    val estimatedHeightsByWidth = remember(blocks, theme, density.density, density.fontScale) {
        mutableMapOf<Int, List<Int>>()
    }
    val effectivePrefetchViewports = prefetchViewports ?: remember(blocks) {
        if (blocks.any(Node::containsBlockMath)) 1.5f else 0.5f
    }
    var topInWindowPx by remember { mutableFloatStateOf(0f) }

    // A Column containing one deferred Box per top-level block still composes and measures hundreds of
    // coordinate/constraint nodes before drawing the first frame. The issue #495 fixture stayed near
    // 400 ms even though only a few BlockRenderers were materialized. Keep the complete AST and total
    // document geometry, but only subcompose blocks intersecting the viewport plus its prefetch margin.
    // Restoring eager rich-content layout is not an alternative: the same fixture took about 3.4 s and
    // repeated layouts exhausted the roughly 200 MB instrument-test heap.
    SubcomposeLayout(
        modifier = modifier
            .fillMaxWidth()
            .onGloballyPositioned { coordinates ->
                val newTop = coordinates.positionInWindow().y
                if (topInWindowPx != newTop) topInWindowPx = newTop
            },
    ) { constraints ->
        val spacingPx = with(density) { (blockSpacing ?: theme.blockSpacing).roundToPx() }
        val estimatedHeightsPx = estimatedHeightsByWidth.getOrPut(constraints.maxWidth) {
            val widthDp = with(density) { constraints.maxWidth.toDp().value }
            blocks.map { node ->
                with(density) { estimateHeightDp(node, widthDp, theme).dp.roundToPx() }
            }
        }
        val blockHeights = blocks.indices.map { index ->
            measuredHeightsPx[blockKeys[index]]
                ?: estimatedHeightsPx[index]
        }
        val blockTops = IntArray(blocks.size)
        var totalHeight = 0
        blockHeights.forEachIndexed { index, height ->
            blockTops[index] = totalHeight
            totalHeight += height
            if (index != blocks.lastIndex) totalHeight += spacingPx
        }
        // LaTeX parsing and layout are prepared off the UI thread. Formula documents keep one and
        // a half viewports ahead: one viewport raised the same stress corpus' forward-scroll peak
        // from about 95 ms to 151 ms. Plain content uses half a viewport so first layout does not
        // eagerly compose two and a half screens that provide no asynchronous preparation benefit.
        val prefetchPx = viewportHeightPx * effectivePrefetchViewports
        val visibleTop = -topInWindowPx - prefetchPx
        val visibleBottom = viewportHeightPx - topInWindowPx + prefetchPx
        val childConstraints = constraints.copy(minHeight = 0, maxHeight = Constraints.Infinity)
        val placeables = buildList {
            blocks.forEachIndexed { index, node ->
                val top = blockTops[index]
                val bottom = top + blockHeights[index]
                val requestedByNavigation = footnoteNavigationState?.let { navigationState ->
                    (node is FootnoteDefinition && navigationState.isDefinitionRequested(node.label)) ||
                        node.hasRequestedFootnoteReference(navigationState)
                } == true
                if (bottom >= visibleTop && top <= visibleBottom || requestedByNavigation) {
                    val blockKey = blockKeys[index]
                    val placeable = subcompose(blockKey) {
                        deferredBlockStates.SaveableStateProvider(blockKey) {
                            PersistentSelectionScope(
                                scopeKey = blockKey,
                                documentOrder = groupOrder + index,
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .onSizeChanged { size ->
                                            if (size.height > 0 && measuredHeightsPx[blockKey] != size.height) {
                                                measuredHeightsPx[blockKey] = size.height
                                            }
                                        },
                                ) {
                                    blockContent(index, node)
                                }
                            }
                        }
                    }.single().measure(childConstraints)
                    add(top to placeable)
                }
            }
        }
        layout(
            width = constraints.maxWidth.coerceIn(constraints.minWidth, constraints.maxWidth),
            height = totalHeight.coerceIn(constraints.minHeight, constraints.maxHeight),
        ) {
            placeables.forEach { (top, placeable) -> placeable.placeRelative(0, top) }
        }
    }
}

private fun Node.containsBlockMath(): Boolean = when (this) {
    is MathBlock -> true
    is ContainerNode -> children.any(Node::containsBlockMath)
    else -> false
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
            val widthUnits = node.estimatedInlineWidthDp(fontSize, safeWidth)
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

internal fun Node.estimatedInlineWidthDp(
    fontSize: Float,
    availableWidth: Float,
): Float = when (this) {
    is Text ->
        literal.sumOf { character ->
            when {
                character.code > 0x7f -> 1.0
                character.isWhitespace() -> 0.35
                character in "ilI.,'`:;!|" -> 0.3
                character in "MW@#%&" -> 0.9
                else -> 0.6
            }
        }.toFloat() * fontSize
    is InlineMath -> (literal.length * 0.7f).coerceIn(1.5f, 20f) * fontSize * 1.125f
    is InlineCode -> literal.length.coerceAtLeast(1) * fontSize * 0.65f
    is Image, is HardLineBreak, is SoftLineBreak -> availableWidth
    is ContainerNode -> children.sumOf { it.estimatedInlineWidthDp(fontSize, availableWidth).toDouble() }.toFloat()
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
