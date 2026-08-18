/*
 * Zhihu++ - Free & Ad-Free Zhihu client for all platforms.
 * Copyright (C) 2024-2026, zly2006 <i@zly2006.me>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation (version 3 only).
 */

package com.github.zly2006.zhihu.markdown

import com.hrm.markdown.parser.ast.Abbreviation
import com.hrm.markdown.parser.ast.AbbreviationDefinition
import com.hrm.markdown.parser.ast.Autolink
import com.hrm.markdown.parser.ast.BibliographyDefinition
import com.hrm.markdown.parser.ast.BlankLine
import com.hrm.markdown.parser.ast.BlockQuote
import com.hrm.markdown.parser.ast.CitationReference
import com.hrm.markdown.parser.ast.ContainerNode
import com.hrm.markdown.parser.ast.DirectiveInline
import com.hrm.markdown.parser.ast.Document
import com.hrm.markdown.parser.ast.Emoji
import com.hrm.markdown.parser.ast.Emphasis
import com.hrm.markdown.parser.ast.EscapedChar
import com.hrm.markdown.parser.ast.FencedCodeBlock
import com.hrm.markdown.parser.ast.Figure
import com.hrm.markdown.parser.ast.FootnoteDefinition
import com.hrm.markdown.parser.ast.FootnoteReference
import com.hrm.markdown.parser.ast.FrontMatter
import com.hrm.markdown.parser.ast.HardLineBreak
import com.hrm.markdown.parser.ast.Heading
import com.hrm.markdown.parser.ast.Highlight
import com.hrm.markdown.parser.ast.HtmlBlock
import com.hrm.markdown.parser.ast.HtmlEntity
import com.hrm.markdown.parser.ast.Image
import com.hrm.markdown.parser.ast.IndentedCodeBlock
import com.hrm.markdown.parser.ast.InlineCode
import com.hrm.markdown.parser.ast.InlineHtml
import com.hrm.markdown.parser.ast.InlineMath
import com.hrm.markdown.parser.ast.InsertedText
import com.hrm.markdown.parser.ast.KeyboardInput
import com.hrm.markdown.parser.ast.LeafNode
import com.hrm.markdown.parser.ast.Link
import com.hrm.markdown.parser.ast.LinkReferenceDefinition
import com.hrm.markdown.parser.ast.ListBlock
import com.hrm.markdown.parser.ast.ListItem
import com.hrm.markdown.parser.ast.MathBlock
import com.hrm.markdown.parser.ast.Node
import com.hrm.markdown.parser.ast.Paragraph
import com.hrm.markdown.parser.ast.RubyText
import com.hrm.markdown.parser.ast.SegmentHighlight
import com.hrm.markdown.parser.ast.SetextHeading
import com.hrm.markdown.parser.ast.SoftLineBreak
import com.hrm.markdown.parser.ast.Spoiler
import com.hrm.markdown.parser.ast.Strikethrough
import com.hrm.markdown.parser.ast.StrongEmphasis
import com.hrm.markdown.parser.ast.StyledText
import com.hrm.markdown.parser.ast.Subscript
import com.hrm.markdown.parser.ast.Superscript
import com.hrm.markdown.parser.ast.Table
import com.hrm.markdown.parser.ast.TableBody
import com.hrm.markdown.parser.ast.TableCell
import com.hrm.markdown.parser.ast.TableHead
import com.hrm.markdown.parser.ast.TableRow
import com.hrm.markdown.parser.ast.Text
import com.hrm.markdown.parser.ast.ThematicBreak
import com.hrm.markdown.parser.ast.WikiLink
import org.tiqian.markdown.MarkdownBlock
import org.tiqian.markdown.MarkdownBlockQuote
import org.tiqian.markdown.MarkdownCapabilityIssue
import org.tiqian.markdown.MarkdownCapabilityIssueKind
import org.tiqian.markdown.MarkdownCodeBlock
import org.tiqian.markdown.MarkdownCustomBlock
import org.tiqian.markdown.MarkdownFootnoteDefinition
import org.tiqian.markdown.MarkdownHeading
import org.tiqian.markdown.MarkdownHtmlBlock
import org.tiqian.markdown.MarkdownImageBlock
import org.tiqian.markdown.MarkdownList
import org.tiqian.markdown.MarkdownListItem
import org.tiqian.markdown.MarkdownMathBlock
import org.tiqian.markdown.MarkdownNodeKey
import org.tiqian.markdown.MarkdownNodeMetadata
import org.tiqian.markdown.MarkdownParagraph
import org.tiqian.markdown.MarkdownRenderDocument
import org.tiqian.markdown.MarkdownSourceSpan
import org.tiqian.markdown.MarkdownTable
import org.tiqian.markdown.MarkdownTableAlignment
import org.tiqian.markdown.MarkdownTableCell
import org.tiqian.markdown.MarkdownTableRow
import org.tiqian.markdown.MarkdownTaskState
import org.tiqian.markdown.MarkdownText
import org.tiqian.markdown.MarkdownTextMark
import org.tiqian.markdown.MarkdownTextRange
import org.tiqian.markdown.MarkdownTextSpan
import org.tiqian.markdown.MarkdownThematicBreak
import org.tiqian.markdown.MarkdownUnsupportedBlock

internal const val ZHIHU_SEGMENT_HIGHLIGHT_KIND = "zhihu.segment-highlight"

/** Converts the host-owned Markdown AST into the renderer's neutral document model. */
internal class TiqianRenderDocumentCompiler(
    private val customBlockAdapter: MarkdownCustomBlockAdapter? = null,
) {
    fun compile(document: Document, sourceMarkdown: String? = null): MarkdownRenderDocument {
        val issues = mutableListOf<MarkdownCapabilityIssue>()
        val sourceLocator = sourceMarkdown?.let(::MarkdownSourceLocator)
        val blocks = document.children.mapIndexedNotNull { index, node ->
            compileBlock(node, listOf(index), sourceMarkdown, sourceLocator, issues)
        }
        return MarkdownRenderDocument(blocks = blocks, issues = issues)
    }

    private fun compileBlock(
        node: Node,
        path: List<Int>,
        source: String?,
        sourceLocator: MarkdownSourceLocator?,
        issues: MutableList<MarkdownCapabilityIssue>,
    ): MarkdownBlock? {
        val metadata = node.metadata(path, source, sourceLocator)
        return when (node) {
            is Paragraph -> MarkdownParagraph(
                text = compileText(node, metadata.sourceSpan, sourceLocator, issues),
                metadata = metadata,
            )

            is Heading -> MarkdownHeading(
                level = node.level.coerceIn(1, 6),
                id = node.id,
                text = compileText(node, metadata.sourceSpan, sourceLocator, issues),
                metadata = metadata,
            )

            is SetextHeading -> MarkdownHeading(
                level = node.level.coerceIn(1, 6),
                id = node.id,
                text = compileText(node, metadata.sourceSpan, sourceLocator, issues),
                metadata = metadata,
            )

            is BlockQuote -> MarkdownBlockQuote(
                blocks = compileBlockChildren(node, path, source, sourceLocator, issues),
                metadata = metadata,
            )

            is ListBlock -> MarkdownList(
                ordered = node.ordered,
                startNumber = node.startNumber,
                tight = node.tight,
                items = node.children.mapIndexedNotNull { index, child ->
                    val item = child as? ListItem ?: return@mapIndexedNotNull null
                    MarkdownListItem(
                        blocks = compileBlockChildren(item, path + index, source, sourceLocator, issues),
                        task = if (!item.taskListItem) {
                            null
                        } else if (item.checked) {
                            MarkdownTaskState.Checked
                        } else {
                            MarkdownTaskState.Unchecked
                        },
                        metadata = item.metadata(path + index, source, sourceLocator),
                    )
                },
                metadata = metadata,
            )

            is FencedCodeBlock -> MarkdownCodeBlock(
                code = node.literal,
                language = node.language.ifBlank { null },
                info = node.info.ifBlank { null },
                metadata = metadata,
            )

            is IndentedCodeBlock -> MarkdownCodeBlock(
                code = node.literal,
                language = null,
                info = null,
                metadata = metadata,
            )

            is ThematicBreak -> MarkdownThematicBreak(metadata)

            is Figure -> MarkdownImageBlock(
                destination = node.imageUrl,
                description = node.caption,
                title = node.caption.ifBlank { null },
                widthPixels = node.imageWidth,
                heightPixels = node.imageHeight,
                metadata = metadata,
                attributes = node.attributes,
                caption = node.caption.takeIf(String::isNotBlank)?.let(::MarkdownText),
            )

            is MathBlock -> MarkdownMathBlock(
                expression = node.literal,
                metadata = metadata,
            )

            is HtmlBlock -> MarkdownHtmlBlock(
                html = node.literal,
                htmlType = node.htmlType,
                metadata = metadata,
            )

            is Table -> compileTable(node, path, source, sourceLocator, issues, metadata)

            is FootnoteDefinition -> MarkdownFootnoteDefinition(
                label = node.label,
                index = node.index,
                blocks = compileBlockChildren(node, path, source, sourceLocator, issues),
                metadata = metadata,
            )

            // Definitions and front matter affect the document but do not produce visible blocks.
            is LinkReferenceDefinition,
            is AbbreviationDefinition,
            is BibliographyDefinition,
            is BlankLine,
            is FrontMatter,
            -> null

            else -> {
                customBlockAdapter?.adapt(node, metadata)?.let { return it }
                issues += MarkdownCapabilityIssue(
                    kind = MarkdownCapabilityIssueKind.UnsupportedBlock,
                    nodeType = node.typeName(),
                    sourceSpan = metadata.sourceSpan,
                )
                MarkdownUnsupportedBlock(
                    nodeType = node.typeName(),
                    fallbackText = metadata.sourceMarkdown ?: node.readableText(),
                    metadata = metadata,
                )
            }
        }
    }

    private fun compileTable(
        node: Table,
        path: List<Int>,
        source: String?,
        sourceLocator: MarkdownSourceLocator?,
        issues: MutableList<MarkdownCapabilityIssue>,
        metadata: MarkdownNodeMetadata,
    ): MarkdownTable {
        val rows = buildList {
            node.children.forEachIndexed { sectionIndex, section ->
                val sectionIsHeader = section is TableHead
                if (section !is TableHead && section !is TableBody) return@forEachIndexed
                section.children.forEachIndexed { rowIndex, rowNode ->
                    val row = rowNode as? TableRow ?: return@forEachIndexed
                    val rowPath = path + sectionIndex + rowIndex
                    add(
                        MarkdownTableRow(
                            cells = row.children.mapIndexedNotNull { cellIndex, cellNode ->
                                val cell = cellNode as? TableCell ?: return@mapIndexedNotNull null
                                val cellMetadata = cell.metadata(rowPath + cellIndex, source, sourceLocator)
                                MarkdownTableCell(
                                    text = compileText(cell, cellMetadata.sourceSpan, sourceLocator, issues),
                                    alignment = cell.alignment.toRenderAlignment(),
                                    header = sectionIsHeader || cell.isHeader,
                                    metadata = cellMetadata,
                                )
                            },
                            header = sectionIsHeader,
                            metadata = row.metadata(rowPath, source, sourceLocator),
                        ),
                    )
                }
            }
        }
        return MarkdownTable(
            columnAlignments = node.columnAlignments.map(Table.Alignment::toRenderAlignment),
            rows = rows,
            metadata = metadata,
        )
    }

    private fun compileBlockChildren(
        node: ContainerNode,
        path: List<Int>,
        source: String?,
        sourceLocator: MarkdownSourceLocator?,
        issues: MutableList<MarkdownCapabilityIssue>,
    ): List<MarkdownBlock> = node.children.mapIndexedNotNull { index, child ->
        compileBlock(child, path + index, source, sourceLocator, issues)
    }

    private fun compileText(
        node: ContainerNode,
        sourceSpan: MarkdownSourceSpan,
        sourceLocator: MarkdownSourceLocator?,
        documentIssues: MutableList<MarkdownCapabilityIssue>,
    ): MarkdownText {
        val builder = MarkdownTextBuilder(documentIssues, sourceSpan, sourceLocator)
        node.children.forEach(builder::append)
        return builder.build()
    }
}

private class MarkdownTextBuilder(
    private val documentIssues: MutableList<MarkdownCapabilityIssue>,
    private val blockSourceSpan: MarkdownSourceSpan,
    private val sourceLocator: MarkdownSourceLocator?,
) {
    private val value = StringBuilder()
    private val spans = mutableListOf<MarkdownTextSpan>()
    private val issues = mutableListOf<MarkdownCapabilityIssue>()

    fun append(node: Node) {
        when (node) {
            is Text -> value.append(node.literal)
            is SoftLineBreak -> value.append(' ')
            is HardLineBreak -> value.append('\n')
            is HtmlEntity -> value.append(node.resolved.ifEmpty { node.literal })
            is EscapedChar -> value.append(node.literal)
            is Emoji -> value.append(node.unicode ?: node.literal)
            is InlineCode -> marked(node, MarkdownTextMark.InlineCode) { value.append(node.literal) }
            is Emphasis -> markedChildren(node, MarkdownTextMark.Emphasis)
            is StrongEmphasis -> markedChildren(node, MarkdownTextMark.Strong)
            is Strikethrough -> markedChildren(node, MarkdownTextMark.Strikethrough)
            is Highlight -> markedChildren(node, MarkdownTextMark.Highlight)
            is Superscript -> markedChildren(node, MarkdownTextMark.Superscript)
            is Subscript -> markedChildren(node, MarkdownTextMark.Subscript)
            is InsertedText -> markedChildren(node, MarkdownTextMark.Inserted)
            is SegmentHighlight -> marked(
                node,
                MarkdownTextMark.Custom(
                    kind = ZHIHU_SEGMENT_HIGHLIGHT_KIND,
                    attributes = node.attributes,
                ),
            ) {
                if (node.children.isEmpty()) {
                    value.append(node.text)
                } else {
                    node.children.forEach(::append)
                }
            }
            is Link -> markedChildren(node, MarkdownTextMark.Link(node.destination, node.title))
            is Autolink -> marked(node, MarkdownTextMark.Link(node.destination)) { value.append(node.literal) }
            is WikiLink -> marked(node, MarkdownTextMark.Link(node.target)) { value.append(node.literal) }
            is Abbreviation -> marked(node, MarkdownTextMark.Abbreviation(node.fullText)) {
                value.append(node.literal)
            }
            is FootnoteReference -> marked(node, MarkdownTextMark.Footnote(node.label, node.index)) {
                value.append("[${node.index}]")
            }
            is RubyText -> marked(node, MarkdownTextMark.Ruby(node.annotation)) { value.append(node.base) }
            is KeyboardInput -> marked(node, MarkdownTextMark.KeyboardInput) { value.append(node.literal) }
            is Image -> marked(
                node,
                MarkdownTextMark.InlineImage(
                    destination = node.destination,
                    description = node.children.readableText(),
                    title = node.title,
                    widthPixels = node.imageWidth,
                    heightPixels = node.imageHeight,
                    attributes = node.attributes,
                ),
            ) {
                node.children.forEach(::append)
                if (node.children.isEmpty()) value.append(node.title ?: node.destination)
            }
            is InlineMath -> marked(node, MarkdownTextMark.InlineMath(node.literal)) { value.append(node.literal) }
            is InlineHtml -> unsupported(node) { value.append(node.literal) }
            is StyledText -> unsupported(node) { node.children.forEach(::append) }
            is Spoiler -> unsupported(node) { node.children.forEach(::append) }
            is CitationReference -> unsupported(node) { value.append("[@${node.key}]") }
            is DirectiveInline -> unsupported(node) { value.append(node.literal) }
            is ContainerNode -> unsupported(node) { node.children.forEach(::append) }
            is LeafNode -> unsupported(node) { value.append(node.literal) }
        }
    }

    fun build(): MarkdownText = MarkdownText(
        value = value.toString(),
        spans = spans.toList(),
        issues = issues.toList(),
    )

    private fun markedChildren(node: ContainerNode, mark: MarkdownTextMark) {
        marked(node, mark) { node.children.forEach(::append) }
    }

    private inline fun marked(node: Node, mark: MarkdownTextMark, content: () -> Unit) {
        val start = value.length
        content()
        if (value.length > start) {
            spans += MarkdownTextSpan(MarkdownTextRange(start, value.length), mark)
        }
    }

    private inline fun unsupported(node: Node, content: () -> Unit) {
        val start = value.length
        content()
        val issue = MarkdownCapabilityIssue(
            kind = MarkdownCapabilityIssueKind.UnsupportedInline,
            nodeType = node.typeName(),
            sourceSpan = node.sourceSpan(sourceLocator, blockSourceSpan),
            textRange = MarkdownTextRange(start, value.length),
        )
        issues += issue
        documentIssues += issue
    }
}

internal fun interface MarkdownCustomBlockAdapter {
    /** Return null to let the compiler report and preserve this node as unsupported content. */
    fun adapt(node: Node, metadata: MarkdownNodeMetadata): MarkdownCustomBlock?
}

private fun Table.Alignment.toRenderAlignment(): MarkdownTableAlignment = when (this) {
    Table.Alignment.LEFT -> MarkdownTableAlignment.Start
    Table.Alignment.CENTER -> MarkdownTableAlignment.Center
    Table.Alignment.RIGHT -> MarkdownTableAlignment.End
    Table.Alignment.NONE -> MarkdownTableAlignment.Unspecified
}

private fun List<Node>.readableText(): String = joinToString(separator = "") { it.readableText() }

private fun Node.metadata(
    path: List<Int>,
    source: String?,
    sourceLocator: MarkdownSourceLocator?,
): MarkdownNodeMetadata {
    val span = sourceSpan(sourceLocator)
    val sourceSlice = source
        ?.takeIf {
            span.startOffset in 0..it.length && span.endOffset in span.startOffset..it.length
        }?.substring(span.startOffset, span.endOffset)
    return MarkdownNodeMetadata(
        // 上游 parser 把 Node.stableKey 从 Int 改成了路径字符串；提椠的 MarkdownNodeKey 仍取 Int，
        // 且身份相等同时含结构 path，两者组合下同文档内不同节点不会撞键。
        key = MarkdownNodeKey(parserStableKey = stableKey.hashCode(), path = path),
        sourceSpan = span,
        sourceMarkdown = sourceSlice,
    )
}

private fun Node.sourceSpan(
    locator: MarkdownSourceLocator?,
    fallback: MarkdownSourceSpan? = null,
): MarkdownSourceSpan {
    if (sourceRange.length > 0) {
        return MarkdownSourceSpan(
            startOffset = sourceRange.start.offset,
            endOffset = sourceRange.end.offset,
            startLine = sourceRange.start.line,
            startColumn = sourceRange.start.column,
            endLine = sourceRange.end.line,
            endColumn = sourceRange.end.column,
        )
    }
    if (locator != null && lineRange.endLine > lineRange.startLine) {
        return locator.span(lineRange.startLine, lineRange.endLine)
    }
    return fallback ?: MarkdownSourceSpan(0, 0, 0, 0, 0, 0)
}

private fun Node.typeName(): String = this::class.simpleName ?: "UnknownNode"

private fun Node.readableText(): String = when (this) {
    is HtmlEntity -> resolved.ifEmpty { literal }
    is Emoji -> unicode ?: literal
    is LeafNode -> literal
    is ContainerNode -> children.joinToString(separator = "") { it.readableText() }
}

private class MarkdownSourceLocator(
    private val source: String,
) {
    private val lineStarts: List<Int> = buildList {
        add(0)
        source.forEachIndexed { index, char ->
            if (char == '\n') add(index + 1)
        }
    }

    fun span(startLine: Int, endLineExclusive: Int): MarkdownSourceSpan {
        val safeStartLine = startLine.coerceIn(0, lineStarts.lastIndex)
        val safeLastLine = (endLineExclusive - 1).coerceIn(safeStartLine, lineStarts.lastIndex)
        val startOffset = lineStarts[safeStartLine]
        val endOffset = lineEndOffset(safeLastLine)
        return MarkdownSourceSpan(
            startOffset = startOffset,
            endOffset = endOffset,
            startLine = safeStartLine,
            startColumn = 0,
            endLine = safeLastLine,
            endColumn = endOffset - lineStarts[safeLastLine],
        )
    }

    private fun lineEndOffset(line: Int): Int {
        if (line == lineStarts.lastIndex) return source.length
        var end = lineStarts[line + 1]
        if (end > 0 && source[end - 1] == '\n') end--
        if (end > 0 && source[end - 1] == '\r') end--
        return end
    }
}
