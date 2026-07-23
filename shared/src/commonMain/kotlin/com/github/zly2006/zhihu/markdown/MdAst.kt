/*
 * Zhihu++ - Free & Ad-Free Zhihu client for all platforms.
 * Copyright (C) 2024-2026, zly2006 <i@zly2006.me>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation (version 3 only).
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.github.zly2006.zhihu.markdown

import com.fleeksoft.ksoup.Ksoup
import com.fleeksoft.ksoup.nodes.Element
import com.fleeksoft.ksoup.nodes.TextNode
import com.github.zly2006.zhihu.navigation.Video
import com.github.zly2006.zhihu.navigation.resolveContent
import com.github.zly2006.zhihu.shared.util.extractImageUrl
import com.github.zly2006.zhihu.shared.util.parseSegmentTextParagraph
import com.github.zly2006.zhihu.ui.components.SegmentedText
import com.github.zly2006.zhihu.ui.components.segmentedTextStyle
import com.hrm.markdown.parser.LineRange
import com.hrm.markdown.parser.MarkdownParser
import com.hrm.markdown.parser.ast.BlockQuote
import com.hrm.markdown.parser.ast.ContainerNode
import com.hrm.markdown.parser.ast.Document
import com.hrm.markdown.parser.ast.Emphasis
import com.hrm.markdown.parser.ast.FencedCodeBlock
import com.hrm.markdown.parser.ast.Figure
import com.hrm.markdown.parser.ast.FootnoteDefinition
import com.hrm.markdown.parser.ast.FootnoteReference
import com.hrm.markdown.parser.ast.HardLineBreak
import com.hrm.markdown.parser.ast.Heading
import com.hrm.markdown.parser.ast.Highlight
import com.hrm.markdown.parser.ast.Image
import com.hrm.markdown.parser.ast.InlineCode
import com.hrm.markdown.parser.ast.InlineMath
import com.hrm.markdown.parser.ast.KeyboardInput
import com.hrm.markdown.parser.ast.Link
import com.hrm.markdown.parser.ast.ListBlock
import com.hrm.markdown.parser.ast.ListItem
import com.hrm.markdown.parser.ast.MathBlock
import com.hrm.markdown.parser.ast.NativeBlock
import com.hrm.markdown.parser.ast.Paragraph
import com.hrm.markdown.parser.ast.Strikethrough
import com.hrm.markdown.parser.ast.StrongEmphasis
import com.hrm.markdown.parser.ast.Subscript
import com.hrm.markdown.parser.ast.Superscript
import com.hrm.markdown.parser.ast.Table
import com.hrm.markdown.parser.ast.TableBody
import com.hrm.markdown.parser.ast.TableCell
import com.hrm.markdown.parser.ast.TableHead
import com.hrm.markdown.parser.ast.TableRow
import com.hrm.markdown.parser.ast.Text
import com.hrm.markdown.parser.ast.ThematicBreak
import io.ktor.http.Url
import com.fleeksoft.ksoup.nodes.Node as HtmlNode
import com.hrm.markdown.parser.ast.Node as MarkdownNode

private var parsingDocument: Document? = null
private const val ZHIHU_EQUATION_URL_PREFIX = "https://www.zhihu.com/equation?tex="

fun htmlToMdAst(
    html: String,
    noNativeBlock: Boolean = false,
): Document {
    val document = Document()
    parsingDocument = document
    Ksoup
        .parseBodyFragment(html)
        .body()
        .childNodes()
        .convertNodesToBlocks(noNativeBlock)
        .forEach(document::appendChild)
    document.footnoteDefinitions.forEach { (_, definition) ->
        document.appendChild(definition)
    }
    parsingDocument = null
    document.assignStableLineRanges()
    return document
}

fun markdownToMdAst(markdown: String): Document = MarkdownParser().parse(markdown)

private fun MarkdownNode.assignStableLineRanges(startLine: Int = 0): Int {
    lineRange = LineRange(startLine, startLine)
    var nextLine = startLine + 1
    if (this is ContainerNode) {
        children.forEach { child ->
            nextLine = child.assignStableLineRanges(nextLine)
        }
    }
    return nextLine
}

internal fun Document.previewImageUrls(): List<String> =
    collectPreviewImageUrls()
        .filter { it.isNotBlank() && !it.startsWith("data") }
        .distinct()

private fun MarkdownNode.collectPreviewImageUrls(): List<String> = when (this) {
    is Figure -> listOf(imageUrl)
    is Image -> listOf(destination)
    is ContainerNode -> children.flatMap { it.collectPreviewImageUrls() }
    else -> emptyList()
}

private fun List<HtmlNode>.convertNodesToBlocks(noNativeBlock: Boolean): List<MarkdownNode> {
    val blocks = mutableListOf<MarkdownNode>()
    var currentParagraph: Paragraph? = null

    fun paragraph(): Paragraph = currentParagraph ?: Paragraph().also {
        blocks.add(it)
        currentParagraph = it
    }

    for ((index, node) in this.withIndex()) {
        when (node) {
            is TextNode -> {
                val text = node.text().trimInlineBoundary(
                    hasPreviousInline = currentParagraph != null,
                    hasNextInline = drop(index + 1).hasNextInlineContent(),
                )
                if (text.isNotEmpty()) {
                    paragraph().appendChild(
                        Text(text),
                    )
                }
            }

            is Element -> {
                if (node.tagName().equals("img", ignoreCase = true) && node.attr("eeimg") != "2") {
                    extractEquationTex(node)?.let { formula ->
                        paragraph().appendChild(InlineMath(formula))
                        continue
                    }
                }

                val blockNode = convertElementToBlock(node, noNativeBlock)
                if (blockNode.isNotEmpty()) {
                    blocks.addAll(blockNode)
                    currentParagraph = null
                } else {
                    val inlineNodes = extractInlineNode(node)
                    if (inlineNodes.size == 1 && inlineNodes.first() is HardLineBreak) {
                        // treat single <br> as block separator
                        currentParagraph = null
                        continue
                    }
                    inlineNodes.forEach {
                        if (it is MathBlock) {
                            blocks.add(it)
                        } else {
                            paragraph().appendChild(it)
                        }
                    }
                }
            }
        }
    }

    return blocks
}

private fun String.trimInlineBoundary(
    hasPreviousInline: Boolean,
    hasNextInline: Boolean,
): String = when {
    isBlank() -> if (hasPreviousInline && hasNextInline) " " else ""
    hasPreviousInline && hasNextInline -> this
    hasPreviousInline -> trimEnd()
    hasNextInline -> trimStart()
    else -> trim()
}

private fun List<HtmlNode>.hasNextInlineContent(): Boolean {
    for (node in this) {
        if (node is Element && node.isBlockBoundary()) {
            return false
        }
        if (node.hasInlineContent()) {
            return true
        }
    }
    return false
}

private fun Element.isBlockBoundary(): Boolean = when (tagName().lowercase()) {
    "br",
    "h1",
    "h2",
    "h3",
    "h4",
    "h5",
    "h6",
    "p",
    "blockquote",
    "pre",
    "ul",
    "ol",
    "hr",
    "figure",
    "table",
    "div",
    -> true
    "img" -> attr("eeimg") == "2"
    "a" -> attr("class").contains("video-box")
    else -> false
}

private fun convertElementToBlock(
    element: Element,
    noNativeBlock: Boolean,
): List<MarkdownNode> = when (element.tagName().lowercase()) {
    "h1", "h2", "h3", "h4", "h5", "h6" -> listOf(
        Heading(level = element.tagName()[1].digitToInt()).apply {
            appendChildren(extractInlineChildren(element))
        },
    )

    "p" -> {
        fun Element.textWithOnlyWhitespace(): Boolean =
            childNodes().filterIsInstance<TextNode>().all { it.text().isBlank() }

        if (element.childNodeSize() == 0) {
            // empty paragraph
            emptyList()
        } else if (element.childrenSize() == 1 && element.textWithOnlyWhitespace() && element.child(0).tagName() == "br") {
            // single <br> as paragraph, treat it as empty to avoid extra spacing
            emptyList()
        } else if (element.childrenSize() == 1 && element.textWithOnlyWhitespace() && element.child(0).tagName() == "img" && element.child(0).attr("eeimg") == "1") {
            // eeimg == "1" is inline math
            val image = element.child(0)
            extractEquationTex(image)
                ?.let { formula -> listOf(MathBlock(formula)) }
                ?: listOfNotNull(createBlockImage(image))
        } else {
            if (!noNativeBlock && element.selectFirst("span.highlight-wrap") != null) {
                // 含有知乎的划线高亮结构，需要单独处理
                // TODO: 暂不考虑其他可能的结构，直接尝试解析整个段落为SegmentedTextParagraph
                parseSegmentTextParagraph(element)?.let { paragraph ->
                    return listOf(
                        NativeBlock {
                            SegmentedText(
                                parts = paragraph.parts,
                                style = segmentedTextStyle(),
                            )
                        },
                    )
                }
            }
            // 特殊处理<p>里面包含的MathBlock
            val list = mutableListOf<MarkdownNode>()

            fun paragraph(): Paragraph = list.lastOrNull() as? Paragraph ?: Paragraph().also { list.add(it) }
            extractInlineChildren(element).forEach {
                if (it is MathBlock) {
                    list.add(it)
                } else {
                    paragraph().appendChild(it)
                }
            }
            list
        }
    }

    "blockquote" -> listOf(
        BlockQuote().apply {
            element.childNodes().convertNodesToBlocks(noNativeBlock).forEach(::appendChild)
        },
    )

    "pre" -> listOf(createCodeBlock(element))

    "ul" -> listOf(createListBlock(element, ordered = false, noNativeBlock = noNativeBlock))

    "ol" -> listOf(createListBlock(element, ordered = true, noNativeBlock = noNativeBlock))

    "hr" -> listOf(ThematicBreak())

    "img" -> listOfNotNull(createBlockImage(element))

    "figure" -> listOfNotNull(createFigureBlock(element))

    "table" -> listOf(createTableBlock(element))

    "div" -> {
        element.childNodes().convertNodesToBlocks(noNativeBlock)
    }

    "a" -> {
        if (element.attr("class").contains("video-box")) {
            if (noNativeBlock) {
                listOfNotNull(createVideoBoxLinkBlock(element))
            } else {
                listOfNotNull(createVideoBoxBlock(element))
            }
        } else {
            emptyList()
        }
    }

    else -> emptyList()
}

private fun createCodeBlock(element: Element): FencedCodeBlock {
    val codeElement = element.selectFirst("code")
    val language =
        sequenceOf(
            parseLanguageFromClassName(codeElement?.classNames().orEmpty()),
            element.attr("lang").ifBlank { null },
        ).firstOrNull { !it.isNullOrBlank() }.orEmpty()

    return FencedCodeBlock(
        info = language,
        language = language,
        literal = codeElement?.text() ?: element.text(),
    )
}

private fun parseLanguageFromClassName(classNames: Set<String>): String? {
    val prefix = "language-"
    return classNames.firstOrNull { it.startsWith(prefix) }?.removePrefix(prefix)
}

private fun createListBlock(
    element: Element,
    ordered: Boolean,
    noNativeBlock: Boolean,
): ListBlock = ListBlock(
    ordered = ordered,
    startNumber = element.attr("start").toIntOrNull() ?: 1,
).apply {
    var precedingListItem: ListItem? = null
    element.children().forEach { childElement ->
        when (childElement.tagName().lowercase()) {
            "li" -> {
                val listItem = ListItem().apply {
                    val children = childElement.childNodes().convertNodesToBlocks(noNativeBlock)
                    if (children.isEmpty()) {
                        appendChild(
                            Paragraph().apply {
                                appendChildren(extractInlineChildren(childElement))
                            },
                        )
                    } else {
                        appendChildren(children)
                    }
                }
                appendChild(listItem)
                precedingListItem = listItem
            }

            "ul", "ol" -> {
                val nestedList = createListBlock(
                    childElement,
                    ordered = childElement.tagName().equals("ol", ignoreCase = true),
                    noNativeBlock = noNativeBlock,
                )
                if (precedingListItem != null) {
                    precedingListItem.appendChild(nestedList)
                } else {
                    val nestedItems = nestedList.children.toList()
                    nestedList.clearChildren()
                    appendChildren(nestedItems)
                }
            }
        }
    }
}

private fun createBlockImage(element: Element): MarkdownNode? {
    if (element.attr("eeimg") == "2") {
        extractEquationTex(element)?.let { formula ->
            return MathBlock(formula)
        }
    }

    val src = extractImageUrl(element::attr) ?: return null
    val caption = element.attr("data-caption").ifBlank { element.attr("alt") }
    return Figure(
        imageUrl = src,
        caption = caption,
    )
}

private fun createFigureBlock(element: Element): MarkdownNode? {
    element.selectFirst("img")?.let { image ->
        val src = extractImageUrl(image::attr) ?: return@let null
        val caption = element.selectFirst("figcaption")?.text()?.ifBlank { null } ?: ""
        return Figure(
            imageUrl = src,
            caption = caption,
            imageWidth = image.attr("width").toIntOrNull(),
            imageHeight = image.attr("height").toIntOrNull(),
        )
    }

    if (element.classNames().any { it.contains("highlight") }) {
        return createCodeBlock(element)
    }

    val inlines = extractInlineChildren(element)
    return inlines.takeIf { it.isNotEmpty() }?.let {
        Paragraph().apply { appendChildren(it) }
    }
}

private fun createVideoBoxBlock(element: Element): MarkdownNode? {
    val href = element.attr("href")
    val videoId = href.takeIf { it.isNotBlank() }?.let { destination ->
        val resolved = resolveContent(destination)
        if (resolved is Video) resolved.id else null
    } ?: element.attr("data-lens-id").toLongOrNull() ?: return null
    val thumbnailUrl = element.selectFirst("img")?.let { image ->
        extractImageUrl(image::attr)
    }

    return NativeBlock {
        RenderVideoBox(
            videoId = videoId,
            thumbnailUrl = thumbnailUrl,
        )
    }
}

private fun createVideoBoxLinkBlock(element: Element): MarkdownNode? {
    val href = element.attr("href").ifBlank {
        element
            .attr("data-lens-id")
            .toLongOrNull()
            ?.let { "https://www.zhihu.com/video/$it" }
            .orEmpty()
    }
    if (href.isBlank()) return null
    return Paragraph().apply {
        appendChild(
            Link(destination = normalizeLinkDestination(href)).apply {
                appendChild(Text("视频"))
            },
        )
    }
}

private fun createTableBlock(element: Element): Table = Table().apply {
    val directRows = element.select("> tr")
    val headRows = element.select("> thead > tr")
    val bodyRows = element.select("> tbody > tr")

    if (headRows.isNotEmpty()) {
        appendChild(
            TableHead().apply {
                headRows.forEach { appendChild(createTableRow(it, isHeader = true)) }
            },
        )
    }

    val rowsForBody = when {
        bodyRows.isNotEmpty() -> bodyRows
        headRows.isEmpty() -> directRows
        else -> emptyList()
    }
    if (rowsForBody.isNotEmpty()) {
        appendChild(
            TableBody().apply {
                rowsForBody.forEach { appendChild(createTableRow(it, isHeader = false)) }
            },
        )
    }

    val referenceRow = headRows.firstOrNull() ?: rowsForBody.firstOrNull()
    columnAlignments = referenceRow
        ?.select("> th, > td")
        ?.map { it.toAlignment() }
        .orEmpty()
}

private fun createTableRow(
    row: Element,
    isHeader: Boolean,
): TableRow = TableRow().apply {
    row.select("> th, > td").forEach { cell ->
        appendChild(
            TableCell(
                alignment = cell.toAlignment(),
                isHeader = isHeader || cell.tagName().equals("th", ignoreCase = true),
            ).apply {
                appendChildren(extractInlineChildren(cell))
            },
        )
    }
}

private fun Element.toAlignment(): Table.Alignment = when (attr("align").lowercase()) {
    "left" -> Table.Alignment.LEFT
    "center" -> Table.Alignment.CENTER
    "right" -> Table.Alignment.RIGHT
    else -> Table.Alignment.NONE
}

private fun extractInlineChildren(element: Element): List<MarkdownNode> {
    val childNodes = element.childNodes()
    return childNodes.flatMapIndexed { index, child ->
        if (child is TextNode && child.text().isBlank()) {
            if (childNodes.take(index).any { it.hasInlineContent() } && childNodes.drop(index + 1).any { it.hasInlineContent() }) {
                listOf(Text(" "))
            } else {
                emptyList()
            }
        } else {
            extractInlineNode(child)
        }
    }
}

private fun HtmlNode.hasInlineContent(): Boolean = when (this) {
    is TextNode -> text().isNotBlank()
    is Element -> when (tagName().lowercase()) {
        "br" -> false
        "img" -> extractEquationTex(this) != null || extractImageUrl(this::attr) != null
        else -> childNodes().any { it.hasInlineContent() } || text().isNotBlank()
    }
    else -> false
}

private fun extractEquationTex(imgElement: Element): String? = extractImageUrl(imgElement::attr)
    ?.takeIf { it.startsWith(ZHIHU_EQUATION_URL_PREFIX) }
    ?.let { Url(it).parameters["tex"].orEmpty() }
    ?.takeIf { it.isNotBlank() }

/**
 * 将一个 HTML 节点转换为 Markdown 内联节点列表
 *
 * > 注意：由于知乎的bug，MathBlock在<p>里面。
 */
private fun extractInlineNode(node: HtmlNode): List<MarkdownNode> = when (node) {
    is TextNode -> {
        val text = node.text()
        if (text.isBlank()) {
            emptyList()
        } else {
            listOf(
                Text(text),
            )
        }
    }

    is Element -> when (node.tagName().lowercase()) {
        "strong", "b" -> listOf(StrongEmphasis().apply { appendChildren(extractInlineChildren(node)) })

        "em", "i" -> listOf(Emphasis().apply { appendChildren(extractInlineChildren(node)) })

        "del", "s", "strike" -> listOf(Strikethrough().apply { appendChildren(extractInlineChildren(node)) })

        "mark" -> listOf(Highlight().apply { appendChildren(extractInlineChildren(node)) })

        "span" -> extractInlineChildren(node)

        "sub" -> listOf(Subscript().apply { appendChildren(extractInlineChildren(node)) })

        "sup" -> {
            if (node.attr("data-draft-type") == "reference") {
                val index = node.attr("data-numero").toInt()
                parsingDocument!!.footnoteDefinitions[node.attr("data-numero")] = FootnoteDefinition(index.toString(), index).apply {
                    appendChild(
                        Paragraph().apply {
                            appendChild(Text(node.attr("data-text")))
                            val url = node.attr("data-url")
                            if (url.isNotBlank()) {
                                appendChild(
                                    Link(destination = url).apply {
                                        appendChild(Text(url))
                                    },
                                )
                            }
                        },
                    )
                }
                listOf(FootnoteReference(index.toString(), index))
            } else {
                listOf(Superscript().apply { appendChildren(extractInlineChildren(node)) })
            }
        }

        "kbd" -> listOf(KeyboardInput(node.text()))

        "code" -> listOf(InlineCode(node.text()))

        "a" -> {
            val href = node.attr("href")
            listOf(
                Link(destination = normalizeLinkDestination(href)).apply {
                    appendChildren(
                        extractInlineChildren(node).ifEmpty {
                            listOf(
                                Text(node.text()),
                            )
                        },
                    )
                },
            )
        }

        "br" -> listOf(HardLineBreak())

        "img" -> {
            val formula = extractEquationTex(node)
            if (formula != null) {
                if (node.attr("eeimg") == "2") {
                    listOf(MathBlock(formula))
                } else {
                    listOf(InlineMath(formula))
                }
            } else {
                extractImageUrl(node::attr)
                    ?.let { url ->
                        listOf(
                            Image(
                                destination = url,
                                title = node.attr("title").ifBlank { null },
                                imageWidth = node.attr("width").toIntOrNull(),
                                imageHeight = node.attr("height").toIntOrNull(),
                            ).apply {
                                node.attr("alt").takeIf { it.isNotBlank() }?.let {
                                    appendChild(
                                        Text(it),
                                    )
                                }
                            },
                        )
                    }.orEmpty()
            }
        }

        else -> {
            val children = extractInlineChildren(node)
            if (children.isNotEmpty()) {
                children
            } else {
                node
                    .text()
                    .takeIf { it.isNotBlank() }
                    ?.let {
                        listOf(
                            Text(it),
                        )
                    }.orEmpty()
            }
        }
    }

    else -> emptyList()
}

private fun normalizeLinkDestination(href: String): String =
    if (href.contains("link.zhihu.com")) {
        runCatching { Url(href).parameters["target"] }.getOrNull()?.takeIf { it.isNotBlank() } ?: href
    } else {
        href
    }

private fun ContainerNode.appendChildren(children: List<MarkdownNode>) {
    children.forEach(::appendChild)
}

/**
 * 把知乎回答的 HTML（DataHolder.Answer.content / editableContent）转换成 Markdown，
 * 用于编辑已有回答时回填到编辑框。
 *
 * 转换链路：
 * - HTML -> MdAst：复用 [htmlToMdAst]，尽可能按知乎 HTML 的实际结构映射到 [Document]
 * - MdAst -> Markdown：复用 [Document.toMarkdown]，按项目约定输出可读性优先的 Markdown
 *
 */
fun zhihuHtmlToMarkdown(html: String): String = htmlToMdAst(html).toMarkdown().trim()

/**
 * 将 [Document] 序列化为 Markdown 文本。
 *
 * 当前支持的主要节点：
 * - 标题：`#` ~ `######`
 * - 段落：空行分隔
 * - 引用：`>` 前缀
 * - 代码块：```lang
 * - 列表：`-` 与 `1.`，子块简单缩进
 * - 分隔线：`---`
 * - 公式：`$...$` / `$$...$$`
 * - 图片/Figure：`![alt](url)`
 * - 表格：pipe table（首行表头 + 分隔行）
 * - 脚注：`[^n]` 与 `[^n]: ...`
 */
fun Document.toMarkdown(): String {
    val out = StringBuilder()
    for (child in children) {
        child.appendMarkdownBlock(out, orderedIndex = null)
    }
    return out.toString().trimEnd()
}

private fun MarkdownNode.appendMarkdownBlock(
    out: StringBuilder,
    orderedIndex: Int?,
) {
    when (this) {
        is Heading -> {
            val level = level.coerceIn(1, 6)
            out.append("#".repeat(level)).append(" ")
            appendMarkdownInline(out)
            out.append("\n\n")
        }

        is Paragraph -> {
            appendMarkdownInline(out)
            out.append("\n\n")
        }

        is BlockQuote -> {
            val content = buildString {
                for (child in children) {
                    child.appendMarkdownBlock(this, orderedIndex = null)
                }
            }.trimEnd()
            val lines = content.lines()
            for (line in lines) {
                if (line.isBlank()) {
                    out.append(">\n")
                } else {
                    out.append("> ").append(line).append("\n")
                }
            }
            out.append("\n")
        }

        is FencedCodeBlock -> {
            val lang = language.takeIf { it.isNotBlank() }
            out.append("```")
            if (lang != null) out.append(lang)
            out.append("\n")
            out.append(literal.trimEnd())
            out.append("\n```\n\n")
        }

        is ListBlock -> {
            for ((i, item) in children.filterIsInstance<ListItem>().withIndex()) {
                val prefix =
                    if (ordered) {
                        "${startNumber + i}. "
                    } else {
                        "- "
                    }
                val itemContent = buildString {
                    item.appendMarkdownListItem(this)
                }.trimEnd()
                val lines = itemContent.lines()
                if (lines.isEmpty() || lines.all { it.isBlank() }) {
                    out.append(prefix.trimEnd()).append("\n")
                    continue
                }
                out.append(prefix).append(lines.first()).append("\n")
                for (line in lines.drop(1)) {
                    if (line.isBlank()) {
                        out.append("\n")
                    } else {
                        out.append("  ").append(line).append("\n")
                    }
                }
            }
            out.append("\n")
        }

        is ThematicBreak -> out.append("---\n\n")

        is MathBlock -> {
            val tex = literal.trim()
            out.append("$$").append(tex).append("$$\n\n")
        }

        is Figure -> {
            val alt = caption.takeIf { it.isNotBlank() }.orEmpty()
            out
                .append("![")
                .append(alt)
                .append("](")
                .append(imageUrl)
                .append(")\n\n")
        }

        is Table -> {
            appendMarkdownTable(out)
            out.append("\n")
        }

        is FootnoteDefinition -> {
            out.append("[^").append(index).append("]: ")
            val content =
                buildString {
                    this@appendMarkdownBlock.appendMarkdownInline(this)
                }.trim()
            out.append(content).append("\n\n")
        }

        else -> {
            if (this is ContainerNode) {
                for (child in children) {
                    child.appendMarkdownBlock(out, orderedIndex = orderedIndex)
                }
            }
        }
    }
}

private fun ListItem.appendMarkdownListItem(out: StringBuilder) {
    val childBlocks = children
    if (childBlocks.size == 1 && childBlocks.single() is Paragraph) {
        (childBlocks.single() as Paragraph).appendMarkdownInline(out)
        return
    }
    for (child in childBlocks) {
        child.appendMarkdownBlock(out, orderedIndex = null)
    }
}

private fun MarkdownNode.appendMarkdownInline(out: StringBuilder) {
    when (this) {
        is Text -> out.append(literal)
        is StrongEmphasis -> out.append("**").also { children.forEach { it.appendMarkdownInline(out) } }.append("**")
        is Emphasis -> out.append("*").also { children.forEach { it.appendMarkdownInline(out) } }.append("*")
        is Strikethrough -> out.append("~~").also { children.forEach { it.appendMarkdownInline(out) } }.append("~~")
        is Highlight -> children.forEach { it.appendMarkdownInline(out) }
        is Subscript -> out.append("<sub>").also { children.forEach { it.appendMarkdownInline(out) } }.append("</sub>")
        is Superscript -> out.append("<sup>").also { children.forEach { it.appendMarkdownInline(out) } }.append("</sup>")
        is InlineCode -> out.append("`").append(literal).append("`")
        is KeyboardInput -> out.append("<kbd>").append(literal).append("</kbd>")
        is HardLineBreak -> out.append("\n")
        is InlineMath -> out.append("$").append(literal.trim()).append("$")
        is FootnoteReference -> out.append("[^").append(index).append("]")
        is Link -> {
            val text = buildString { children.forEach { it.appendMarkdownInline(this) } }.ifBlank { destination }
            out
                .append("[")
                .append(text)
                .append("](")
                .append(destination)
                .append(")")
        }

        is Image -> {
            val alt = children.filterIsInstance<Text>().joinToString(separator = "") { it.literal }.ifBlank { "" }
            out
                .append("![")
                .append(alt)
                .append("](")
                .append(destination)
                .append(")")
        }

        else -> if (this is ContainerNode) children.forEach { it.appendMarkdownInline(out) }
    }
}

private fun Table.appendMarkdownTable(out: StringBuilder) {
    val head = children.filterIsInstance<TableHead>().singleOrNull()
    val body = children.filterIsInstance<TableBody>().singleOrNull()
    val allRows =
        buildList {
            head?.children?.filterIsInstance<TableRow>()?.let(::addAll)
            body?.children?.filterIsInstance<TableRow>()?.let(::addAll)
            if (head == null && body == null) addAll(children.filterIsInstance<TableRow>())
        }
    if (allRows.isEmpty()) return
    val headerRow = allRows.first()
    val headerCells = headerRow.children.filterIsInstance<TableCell>()
    if (headerCells.isEmpty()) return
    out.append("| ")
    headerCells.forEach { cell ->
        out.append(cell.inlineTextForTable()).append(" | ")
    }
    out.append("\n| ")
    headerCells.forEach { _ ->
        out.append("--- | ")
    }
    out.append("\n")
    for (row in allRows.drop(1)) {
        val cells = row.children.filterIsInstance<TableCell>()
        if (cells.isEmpty()) continue
        out.append("| ")
        cells.forEach { cell ->
            out.append(cell.inlineTextForTable()).append(" | ")
        }
        out.append("\n")
    }
}

private fun TableCell.inlineTextForTable(): String =
    buildString { this@inlineTextForTable.appendMarkdownInline(this) }
        .replace(Regex("[\n\r]+"), " ")
        .trim()
