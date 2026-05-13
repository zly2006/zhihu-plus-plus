/*
 * Zhihu++ - Free & Ad-Free Zhihu client for Android.
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

import androidx.core.net.toUri
import com.github.zly2006.zhihu.navigation.Video
import com.github.zly2006.zhihu.navigation.resolveContent
import com.github.zly2006.zhihu.util.extractImageUrl
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
import org.jsoup.Jsoup
import org.jsoup.nodes.Element
import org.jsoup.nodes.TextNode
import com.hrm.markdown.parser.ast.Node as MarkdownNode
import org.jsoup.nodes.Node as HtmlNode

private var parsingDocument: Document? = null
private const val ZHIHU_EQUATION_URL_PREFIX = "https://www.zhihu.com/equation?tex="

fun htmlToMdAst(html: String): Document {
    val document = Document()
    parsingDocument = document
    Jsoup
        .parse(html)
        .body()
        .childNodes()
        .appendBlocksTo(document)
    document.footnoteDefinitions.forEach { (_, definition) ->
        document.appendChild(definition)
    }
    parsingDocument = null
    return document
}

private fun List<HtmlNode>.appendBlocksTo(parent: ContainerNode) {
    convertNodesToBlocks().forEach(parent::appendChild)
}

private fun List<HtmlNode>.convertNodesToBlocks(): List<MarkdownNode> {
    val blocks = mutableListOf<MarkdownNode>()
    var currentParagraph: Paragraph? = null

    fun paragraph(): Paragraph = currentParagraph ?: Paragraph().also {
        blocks.add(it)
        currentParagraph = it
    }

    for (node in this) {
        when (node) {
            is TextNode -> {
                val text = node.text().trim()
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

                val blockNode = convertElementToBlock(node)
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

private fun convertElementToBlock(element: Element): List<MarkdownNode> = when (element.tagName().lowercase()) {
    "h1", "h2", "h3", "h4", "h5", "h6" -> listOf(
        Heading(level = element.tagName()[1].digitToInt()).apply {
            appendChildren(extractInlineChildren(element))
        },
    )

    "p" -> {
        if (element.childNodeSize() == 0) {
            // empty paragraph
            emptyList()
        } else if (element.childNodeSize() == 1 && element.childrenSize() == 1 && element.child(0).tagName() == "br") {
            // single <br> as paragraph, treat it as empty to avoid extra spacing
            emptyList()
        } else {
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
            element.childNodes().appendBlocksTo(this)
        },
    )

    "pre" -> listOf(createCodeBlock(element))

    "ul" -> listOf(createListBlock(element, ordered = false))

    "ol" -> listOf(createListBlock(element, ordered = true))

    "hr" -> listOf(ThematicBreak())

    "img" -> listOfNotNull(createBlockImage(element))

    "figure" -> listOfNotNull(createFigureBlock(element))

    "table" -> listOf(createTableBlock(element))

    "div" -> {
        element.childNodes().convertNodesToBlocks()
    }

    "a" -> {
        if (element.attr("class").contains("video-box")) {
            listOfNotNull(createVideoBoxBlock(element))
        } else {
            emptyList()
        }
    }

    else -> emptyList()
}

private fun createCodeBlock(element: Element): FencedCodeBlock {
    val codeElement = element.selectFirst("code")
    val language = codeElement
        ?.classNames()
        ?.firstOrNull { it.startsWith("language-") }
        ?.removePrefix("language-")
        .orEmpty()

    return FencedCodeBlock(
        info = language,
        language = language,
        literal = codeElement?.text() ?: element.text(),
    )
}

private fun createListBlock(
    element: Element,
    ordered: Boolean,
): ListBlock = ListBlock(
    ordered = ordered,
    startNumber = element.attr("start").toIntOrNull() ?: 1,
).apply {
    element.select("> li").forEach { listItemElement ->
        appendChild(
            ListItem().apply {
                val children = listItemElement.childNodes().convertNodesToBlocks()
                if (children.isEmpty()) {
                    appendChild(
                        Paragraph().apply {
                            appendChildren(extractInlineChildren(listItemElement))
                        },
                    )
                } else {
                    appendChildren(children)
                }
            },
        )
    }
}

private fun createBlockImage(element: Element): MarkdownNode? {
    if (element.attr("eeimg") == "2") {
        extractEquationTex(element)?.let { formula ->
            return MathBlock(formula)
        }
    }

    val src = extractImageUrl(element) ?: return null
    val caption = element.attr("alt")
    return Figure(
        imageUrl = src,
        caption = caption,
    )
}

private fun createFigureBlock(element: Element): MarkdownNode? {
    element.selectFirst("img")?.let { image ->
        val src = extractImageUrl(image) ?: return@let null
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
        extractImageUrl(image)
    }

    return NativeBlock {
        RenderVideoBox(
            videoId = videoId,
            thumbnailUrl = thumbnailUrl,
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

private fun extractInlineChildren(element: Element): List<MarkdownNode> = element.childNodes().flatMap(::extractInlineNode)

private fun extractEquationTex(imgElement: Element): String? = extractImageUrl(imgElement)
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
            val destination = if (href.contains("link.zhihu.com")) {
                href.toUri().getQueryParameter("target") ?: href
            } else {
                href
            }
            listOf(
                Link(destination = destination).apply {
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
                extractImageUrl(node)
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

private fun ContainerNode.appendChildren(children: List<MarkdownNode>) {
    children.forEach(::appendChild)
}
