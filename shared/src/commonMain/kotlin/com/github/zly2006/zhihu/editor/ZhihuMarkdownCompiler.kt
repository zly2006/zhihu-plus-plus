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

package com.github.zly2006.zhihu.editor

import com.hrm.markdown.parser.MarkdownParser
import com.hrm.markdown.parser.ast.BlockQuote
import com.hrm.markdown.parser.ast.ContainerNode
import com.hrm.markdown.parser.ast.Document
import com.hrm.markdown.parser.ast.Emphasis
import com.hrm.markdown.parser.ast.FencedCodeBlock
import com.hrm.markdown.parser.ast.Figure
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
import io.ktor.http.encodeURLParameter
import com.hrm.markdown.parser.ast.Node as MarkdownNode

// 将 TeX 进行 URL 编码，用于拼接知乎公式图片链接的 tex 参数。
private fun encodeZhihuEquationTex(tex: String): String =
    tex.encodeURLParameter(spaceToPlus = false)

// HTML attribute 转义
private fun escapeHtmlAttribute(value: String): String =
    value
        .replace("&", "&amp;")
        .replace("\"", "&quot;")
        .replace("<", "&lt;")
        .replace(">", "&gt;")

private fun escapeHtmlText(value: String): String =
    value
        .replace("&", "&amp;")
        .replace("<", "&lt;")
        .replace(">", "&gt;")

private class ZhihuHtmlWriter(
    private val headingTopLevel: Int?,
    private val headingSecondLevel: Int?,
) {
    private val out = StringBuilder()

    fun write(document: Document): String {
        renderChildrenAsBlocks(document)
        return out.toString()
    }

    private fun renderChildrenAsBlocks(container: ContainerNode) {
        for (child in container.children) {
            renderBlock(child)
        }
    }

    private fun renderBlock(node: MarkdownNode) {
        when (node) {
            is Paragraph -> {
                out.append("<p>")
                renderChildrenAsInlines(node)
                out.append("</p>")
            }

            is Heading -> renderHeading(node)

            is BlockQuote -> {
                out.append("<blockquote>")
                renderChildrenAsBlocks(node)
                out.append("</blockquote>")
            }

            is FencedCodeBlock -> {
                val lang = node.language.takeIf { it.isNotBlank() } ?: node.info.takeIf { it.isNotBlank() }
                if (lang != null) {
                    out.append("<pre lang=\"").append(escapeHtmlAttribute(lang)).append("\">")
                } else {
                    out.append("<pre>")
                }
                out.append(node.literal.orEmpty())
                out.append("</pre>")
            }

            is ListBlock -> {
                if (node.ordered) {
                    if (node.startNumber != 1) {
                        out.append("<ol start=\"").append(node.startNumber).append("\">")
                    } else {
                        out.append("<ol>")
                    }
                } else {
                    out.append("<ul>")
                }
                for (child in node.children) {
                    if (child is ListItem) renderListItem(child) else renderBlock(child)
                }
                out.append(if (node.ordered) "</ol>" else "</ul>")
            }

            is ThematicBreak -> out.append("<hr>")

            is MathBlock -> {
                val tex = node.literal.orEmpty().trim()
                val alt = escapeHtmlAttribute(tex.replace(Regex("[\n\r]+"), " "))
                val encoded = encodeZhihuEquationTex(tex)
                out.append("<p>")
                out
                    .append("<img eeimg=\"2\" src=\"//www.zhihu.com/equation?tex=")
                    .append(encoded)
                    .append("\" alt=\"")
                    .append(alt)
                    .append("\" />")
                out.append("</p>")
            }

            is Figure -> {
                out.append("<img src=\"").append(escapeHtmlAttribute(node.imageUrl)).append("\"")
                node.imageWidth?.let { out.append(" width=\"").append(it).append("\"") }
                node.imageHeight?.let { out.append(" height=\"").append(it).append("\"") }
                val caption = node.caption.takeIf { it.isNotBlank() }
                if (caption != null) {
                    val escapedCaption = escapeHtmlAttribute(caption)
                    out.append(" data-caption=\"").append(escapedCaption).append("\"")
                }
                out.append(">")
            }

            is Table -> renderTable(node)

            is ContainerNode -> renderChildrenAsBlocks(node)

            else -> Unit
        }
    }

    private fun renderHeading(heading: Heading) {
        val level = heading.level
        when {
            headingTopLevel != null && level == headingTopLevel -> {
                out.append("<h2>")
                renderChildrenAsInlines(heading)
                out.append("</h2>")
            }

            headingSecondLevel != null && level == headingSecondLevel -> {
                out.append("<h3>")
                renderChildrenAsInlines(heading)
                out.append("</h3>")
            }

            else -> {
                out.append("<p><strong>")
                renderChildrenAsInlines(heading)
                out.append("</strong></p>")
            }
        }
    }

    private fun renderListItem(item: ListItem) {
        out.append("<li>")
        val children = item.children
        if (children.size == 1 && children.single() is Paragraph) {
            renderChildrenAsInlines(children.single() as Paragraph)
        } else {
            renderChildrenAsBlocks(item)
        }
        out.append("</li>")
    }

    private fun renderTable(table: Table) {
        out.append("<table data-draft-node=\"block\" data-draft-type=\"table\" data-size=\"normal\">")
        out.append("<tbody>")
        val head = table.children.filterIsInstance<TableHead>().singleOrNull()
        val body = table.children.filterIsInstance<TableBody>().singleOrNull()
        head
            ?.children
            ?.filterIsInstance<TableRow>()
            ?.forEach { renderTableRow(it, forceHeader = true) }
        body
            ?.children
            ?.filterIsInstance<TableRow>()
            ?.forEach { renderTableRow(it, forceHeader = false) }
        if (head == null && body == null) {
            table.children.filterIsInstance<TableRow>().forEach { renderTableRow(it, forceHeader = false) }
        }
        out.append("</tbody>")
        out.append("</table>")
    }

    private fun renderTableRow(row: TableRow, forceHeader: Boolean) {
        out.append("<tr>")
        for (cell in row.children.filterIsInstance<TableCell>()) {
            val tag = if (forceHeader || cell.isHeader) "th" else "td"
            out.append("<").append(tag).append(">")
            renderChildrenAsInlines(cell)
            out.append("</").append(tag).append(">")
        }
        out.append("</tr>")
    }

    private fun renderChildrenAsInlines(container: ContainerNode) {
        for (child in container.children) {
            renderInline(child)
        }
    }

    private fun renderInline(node: MarkdownNode) {
        when (node) {
            is Text -> out.append(escapeHtmlText(node.literal))
            is StrongEmphasis -> wrapTag("b") { renderChildrenAsInlines(node) }
            is Emphasis -> wrapTag("i") { renderChildrenAsInlines(node) }
            is Strikethrough -> wrapTag("del") { renderChildrenAsInlines(node) }
            is Highlight -> wrapTag("mark") { renderChildrenAsInlines(node) }
            is Subscript -> wrapTag("sub") { renderChildrenAsInlines(node) }
            is Superscript -> wrapTag("sup") { renderChildrenAsInlines(node) }
            is InlineCode -> wrapTag("code") { out.append(escapeHtmlText(node.literal)) }
            is KeyboardInput -> wrapTag("kbd") { out.append(escapeHtmlText(node.literal)) }
            is HardLineBreak -> out.append("<br>")

            is Link -> {
                val href = escapeHtmlAttribute(node.destination)
                out.append("<a href=\"").append(href).append("\">")
                if (node.children.isEmpty()) {
                    out.append(escapeHtmlText(node.destination))
                } else {
                    renderChildrenAsInlines(node)
                }
                out.append("</a>")
            }

            is Image -> {
                out.append("<img src=\"").append(escapeHtmlAttribute(node.destination)).append("\"")
                val alt = node.children
                    .filterIsInstance<Text>()
                    .joinToString(separator = "") { it.literal }
                    .ifBlank { null }
                if (alt != null) out.append(" alt=\"").append(escapeHtmlAttribute(alt)).append("\"")
                node.title?.takeIf { it.isNotBlank() }?.let { out.append(" title=\"").append(escapeHtmlAttribute(it)).append("\"") }
                node.imageWidth?.let { out.append(" width=\"").append(it).append("\"") }
                node.imageHeight?.let { out.append(" height=\"").append(it).append("\"") }
                out.append(" />")
            }

            is InlineMath -> {
                val tex = node.literal.orEmpty().trim()
                val alt = escapeHtmlAttribute(tex.replace(Regex("[\n\r]+"), " "))
                val encoded = encodeZhihuEquationTex(tex)
                out
                    .append("<img eeimg=\"1\" src=\"//www.zhihu.com/equation?tex=")
                    .append(encoded)
                    .append("\" alt=\"")
                    .append(alt)
                    .append("\" />")
            }

            is ContainerNode -> renderChildrenAsInlines(node)
            else -> Unit
        }
    }

    private inline fun wrapTag(tag: String, block: () -> Unit) {
        out.append("<").append(tag).append(">")
        block()
        out.append("</").append(tag).append(">")
    }
}

private fun collectHeadingLevels(document: Document): List<Int> {
    val set = LinkedHashSet<Int>()
    val stack = ArrayDeque<MarkdownNode>()
    stack.add(document)
    while (stack.isNotEmpty()) {
        val node = stack.removeLast()
        if (node is Heading) set.add(node.level)
        if (node is ContainerNode) {
            for (i in node.children.size - 1 downTo 0) stack.add(node.children[i])
        }
    }
    return set.toList().sorted()
}

@Suppress("UNUSED_PARAMETER")
suspend fun compileMdToZhihuHtml(
    markdown: String,
    publisher: ZhihuAnswerPublisher,
): String {
    val document = MarkdownParser().parse(markdown)
    val usedHeadingLevels = collectHeadingLevels(document)
    val top = usedHeadingLevels.firstOrNull()
    val second = usedHeadingLevels.drop(1).firstOrNull()
    val html =
        ZhihuHtmlWriter(
            headingTopLevel = top,
            headingSecondLevel = second,
        ).write(document)
    return html.trimEnd()
}
