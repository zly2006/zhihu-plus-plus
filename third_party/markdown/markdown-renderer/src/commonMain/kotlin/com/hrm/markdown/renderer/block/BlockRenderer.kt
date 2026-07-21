package com.hrm.markdown.renderer.block

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.unit.dp
import com.hrm.markdown.parser.ast.*
import com.hrm.markdown.renderer.MarkdownBlockChildren
import com.hrm.markdown.renderer.LocalMarkdownDirectiveRegistry
import com.hrm.markdown.renderer.LocalMarkdownTheme
import com.hrm.markdown.renderer.LocalRendererDocument
import com.hrm.markdown.runtime.DirectiveBlockRenderScope

private fun List<IntRange>.flattenLineNumbers(): Set<Int> = buildSet {
    for (range in this@flattenLineNumbers) {
        addAll(range)
    }
}

/**
 * 块级节点分发器。
 * 根据节点类型分发到对应的块级渲染器。
 */
@Composable
internal fun BlockRenderer(
    node: Node,
    renderRevision: Long = 0L,
    modifier: Modifier = Modifier,
) {
    val directiveRegistry = LocalMarkdownDirectiveRegistry.current
    if (node is MathBlock) {
        MathBlockRenderer(
            latex = node.literal,
            modifier = modifier,
        )
        return
    }

    key(renderRevision) {
        when (node) {
            is Heading -> HeadingRenderer(node, modifier)
            is SetextHeading -> SetextHeadingRenderer(node, modifier)
            is Paragraph -> ParagraphRenderer(node, modifier)
            is ThematicBreak -> ThematicBreakRenderer(modifier)
            is FencedCodeBlock -> {
                FencedCodeBlockRenderer(
                    text = node.literal,
                    language = node.language,
                    title = node.attributes.pairs["title"],
                    showLineNumbers = node.showLineNumbers,
                    startLine = node.startLineNumber,
                    highlightedLines = node.highlightLines.flattenLineNumbers(),
                    modifier = modifier,
                )
            }
            is IndentedCodeBlock -> {
                IndentedCodeBlockRenderer(
                    text = node.literal,
                    modifier = modifier,
                )
            }
            is BlockQuote -> BlockQuoteRenderer(node, modifier)
            is ListBlock -> ListBlockRenderer(node, modifier)
            is HtmlBlock -> HtmlBlockRenderer(node, modifier)
            is Table -> TableRenderer(node, modifier)
            is Admonition -> AdmonitionRenderer(node, modifier)
            is CustomContainer -> CustomContainerRenderer(node, modifier)
            is DiagramBlock -> DiagramBlockRenderer(node, modifier)
            is ColumnsLayout -> ColumnsLayoutRenderer(node, modifier)
            is DefinitionList -> DefinitionListRenderer(node, modifier)
            is FootnoteDefinition -> FootnoteDefinitionRenderer(node, modifier)
            is TocPlaceholder -> TocPlaceholderRenderer(node, modifier)
            is PageBreak -> PageBreakRenderer(modifier)
            is DirectiveBlock -> {
                val renderer = directiveRegistry.findBlockDirectiveRenderer(node.tagName)
                if (renderer != null) {
                    renderer(
                        DirectiveBlockRenderScope(
                            tagName = node.tagName,
                            args = node.args,
                            node = node,
                            content = if (node.children.isNotEmpty()) {
                                { MarkdownBlockChildren(parent = node) }
                            } else null,
                        )
                    )
                } else {
                    DirectiveBlockRenderer(node, modifier)
                }
            }
            is TabBlock -> TabBlockRenderer(node, modifier)
            is BibliographyDefinition -> BibliographyDefinitionRenderer(node, modifier)
            is Figure -> FigureRenderer(node, modifier)
            is NativeBlock -> node.content()
            is FrontMatter -> { /* FrontMatter 通常不渲染 */ }
            is LinkReferenceDefinition -> { /* 引用定义不直接渲染 */ }
            is AbbreviationDefinition -> { /* 缩写定义不直接渲染 */ }
            is BlankLine -> { /* 空行不渲染 */ }
            else -> {
                // 未知块级节点，尝试渲染子节点
                if (node is ContainerNode) {
                    for (child in node.children) {
                        BlockRenderer(child)
                    }
                }
            }
        }
    }
}

internal fun blockRenderRevision(node: Node): Long = when (node) {
    is Paragraph -> revisionHash(
        node.lineRange.endLine.toLong(),
        node.contentHash,
        (node.rawContent?.length ?: 0).toLong(),
    )
    is Heading -> revisionHash(
        node.level.toLong(),
        node.lineRange.endLine.toLong(),
        node.contentHash,
        (node.rawContent?.length ?: 0).toLong(),
    )
    is SetextHeading -> revisionHash(
        node.level.toLong(),
        node.lineRange.endLine.toLong(),
        node.contentHash,
        (node.rawContent?.length ?: 0).toLong(),
    )
    is FencedCodeBlock -> revisionHash(
        node.lineRange.endLine.toLong(),
        node.contentHash,
        node.literal.length.toLong(),
    )
    is IndentedCodeBlock -> revisionHash(
        node.lineRange.endLine.toLong(),
        node.contentHash,
        node.literal.length.toLong(),
    )
    is MathBlock -> revisionHash(
        node.lineRange.endLine.toLong(),
        node.literal.hashCode().toLong(),
        node.literal.length.toLong(),
    )
    is BlockQuote -> revisionHash(
        node.lineRange.endLine.toLong(),
        node.contentHash,
        node.childCount().toLong(),
        childRenderRevision(node),
    )
    is ListItem -> revisionHash(
        node.lineRange.endLine.toLong(),
        node.contentHash,
        node.childCount().toLong(),
        childRenderRevision(node),
    )
    is ListBlock -> revisionHash(
        node.lineRange.endLine.toLong(),
        node.contentHash,
        node.childCount().toLong(),
        childRenderRevision(node),
    )
    is Table -> revisionHash(
        node.lineRange.endLine.toLong(),
        node.contentHash,
        node.childCount().toLong(),
        childRenderRevision(node),
    )
    is CustomContainer -> revisionHash(
        node.lineRange.endLine.toLong(),
        node.contentHash,
        node.childCount().toLong(),
        childRenderRevision(node),
    )
    else -> revisionHash(node.lineRange.endLine.toLong(), node.contentHash)
}

private fun revisionHash(a: Long, b: Long): Long =
    mixRevision(mixRevision(REVISION_OFFSET_BASIS, a), b)

private fun revisionHash(a: Long, b: Long, c: Long): Long =
    mixRevision(revisionHash(a, b), c)

private fun revisionHash(a: Long, b: Long, c: Long, d: Long): Long =
    mixRevision(revisionHash(a, b, c), d)

private fun childRenderRevision(node: ContainerNode): Long {
    var acc = REVISION_OFFSET_BASIS
    for (child in node.children) {
        acc = mixRevision(acc, blockRenderRevision(child))
    }
    return acc
}

private fun mixRevision(acc: Long, value: Long): Long = (acc xor value) * REVISION_FNV_PRIME

private const val REVISION_OFFSET_BASIS = -3750763034362895579L
private const val REVISION_FNV_PRIME = 1099511628211L

/**
 * TOC 占位符渲染器：渲染自动生成的目录。
 *
 * 支持高级配置：
 * - `minDepth`/`maxDepth`：过滤标题层级范围
 * - `excludeIds`：排除指定 ID 的标题
 * - `order`：排序方式（asc/desc）
 */
@Composable
internal fun TocPlaceholderRenderer(
    node: TocPlaceholder,
    modifier: Modifier = Modifier,
) {
    val theme = LocalMarkdownTheme.current
    val document = LocalRendererDocument.current

    // 收集 + 过滤 + 排序结果，避免每次重组都全量遍历 AST
    val headings = remember(document, node.minDepth, node.maxDepth, node.excludeIds, node.order) {
        var list = collectHeadings(document).filter { it.level in node.minDepth..node.maxDepth }
        if (node.excludeIds.isNotEmpty()) {
            list = list.filter { it.id == null || it.id !in node.excludeIds }
        }
        if (node.order == "desc") list = list.reversed()
        list
    }
    if (headings.isEmpty()) return

    Column(
        modifier = modifier.fillMaxWidth().padding(vertical = 4.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Text(
            text = "Table of Contents",
            style = theme.headingStyles.getOrElse(2) { theme.bodyStyle },
            modifier = Modifier.padding(bottom = 4.dp),
        )
        for ((text, level, _) in headings) {
            val adjustedLevel = (level - node.minDepth).coerceAtLeast(0)
            Text(
                text = "${"  ".repeat(adjustedLevel)}• $text",
                style = theme.bodyStyle.copy(
                    color = theme.linkColor,
                    fontStyle = FontStyle.Normal,
                ),
                modifier = Modifier.padding(start = adjustedLevel.dp * 12),
            )
        }
    }
}

private data class HeadingInfo(val text: String, val level: Int, val id: String?)

private fun collectHeadings(document: Document): List<HeadingInfo> {
    val result = mutableListOf<HeadingInfo>()
    for (child in document.children) {
        collectHeadingsRecursive(child, result)
    }
    return result
}

private fun collectHeadingsRecursive(node: Node, result: MutableList<HeadingInfo>) {
    when (node) {
        is Heading -> {
            val text = node.children.joinToString("") { extractText(it) }
            result.add(HeadingInfo(text, node.level, node.id))
        }
        is SetextHeading -> {
            val text = node.children.joinToString("") { extractText(it) }
            result.add(HeadingInfo(text, node.level, node.id))
        }
        is ContainerNode -> {
            for (child in node.children) {
                collectHeadingsRecursive(child, result)
            }
        }
        else -> {}
    }
}

private fun extractText(node: Node): String = when (node) {
    is com.hrm.markdown.parser.ast.Text -> node.literal
    is InlineCode -> node.literal
    is EscapedChar -> node.literal
    is HtmlEntity -> node.resolved.ifEmpty { node.literal }
    is Emoji -> node.literal
    is ContainerNode -> node.children.joinToString("") { extractText(it) }
    else -> ""
}
