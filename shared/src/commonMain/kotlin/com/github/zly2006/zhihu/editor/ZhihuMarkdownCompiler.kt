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

import io.ktor.http.Url
import io.ktor.http.encodeURLParameter

/**
 * 把 Markdown 编译为“知乎回答编辑器可接受”的 HTML。
 *
 * 对齐 zhihu_obsidian 的核心行为：
 * - 代码块：<pre lang="...">...</pre>
 * - 表格：<table data-draft-node="block" data-draft-type="table" ...>
 * - 公式：<img eeimg="1/2" src="https://www.zhihu.com/equation?tex=...">
 * - 脚注：<sup data-draft-type="reference" ...>
 * - 卡片链接：[title](url "card") => <a data-draft-type="link-card" ...>
 * - 图片：会尽量输出知乎编辑器可识别的 <img data-original-src ...> 结构；
 *   若图片 URL 非 zhimg 且 publisher 支持，会自动下载并上传到知乎图床再替换。
 */
suspend fun compileMarkdownToZhihuAnswerHtml(
    markdown: String,
    publisher: ZhihuAnswerPublisher,
): String {
    val normalized = markdown.replace("\r\n", "\n")
    val (contentWithoutFootnotes, footnotes) = extractFootnoteDefinitions(normalized)
    val footnoteOrder = linkedMapOf<String, Int>()

    fun footnoteNumber(id: String): Int =
        footnoteOrder.getOrPut(id) { footnoteOrder.size + 1 }

    val lines = contentWithoutFootnotes.split('\n')
    val out = StringBuilder()
    var i = 0

    fun appendParagraph(html: String) {
        val trimmed = html.trim()
        if (trimmed.isNotEmpty()) {
            out.append("<p>").append(trimmed).append("</p>\n")
        }
    }

    while (i < lines.size) {
        val line = lines[i]
        if (line.isBlank()) {
            i++
            continue
        }

        if (line.startsWith("```")) {
            val lang = line.removePrefix("```").trim()
            val code = StringBuilder()
            i++
            while (i < lines.size && !lines[i].startsWith("```")) {
                code.append(lines[i]).append('\n')
                i++
            }
            if (i < lines.size) i++
            val escaped = escapeHtml(code.toString().trimEnd())
            out.append("<pre")
            if (lang.isNotEmpty()) {
                out.append(" lang=\"").append(escapeHtmlAttribute(lang)).append("\"")
            }
            out.append(">").append(escaped).append("</pre>\n")
            continue
        }

        if (line.trim() == "$$") {
            val tex = StringBuilder()
            i++
            while (i < lines.size && lines[i].trim() != "$$") {
                tex.append(lines[i]).append('\n')
                i++
            }
            if (i < lines.size) i++
            val encoded = tex.toString().trim().encodeURLParameter()
            out
                .append("""<p><img eeimg="2" src="https://www.zhihu.com/equation?tex=$encoded" alt="${escapeHtmlAttribute(tex.toString().trim())}"/></p>""")
                .append("\n")
            continue
        }

        if (line.startsWith("#")) {
            val depth = line.takeWhile { it == '#' }.length
            val text = line.drop(depth).trim()
            val (tag, asStrongParagraph) = when (depth) {
                1 -> "h2" to false
                2 -> "h3" to false
                else -> "" to true
            }
            if (asStrongParagraph) {
                appendParagraph("<strong>${compileInline(text, publisher, footnotes, ::footnoteNumber)}</strong>")
            } else {
                out
                    .append("<")
                    .append(tag)
                    .append(">")
                    .append(compileInline(text, publisher, footnotes, ::footnoteNumber))
                    .append("</")
                    .append(tag)
                    .append(">\n")
            }
            i++
            continue
        }

        if (line.startsWith(">")) {
            val quoteLines = mutableListOf<String>()
            while (i < lines.size && lines[i].startsWith(">")) {
                quoteLines.add(lines[i].removePrefix(">").trimStart())
                i++
            }
            val calloutMatch = Regex("^\\[!([^\\]]+)]\\s*(.*)$").find(quoteLines.firstOrNull().orEmpty())
            if (calloutMatch != null) {
                val title = calloutMatch.groupValues[1].trim()
                val firstRest = calloutMatch.groupValues[2]
                val restLines = buildList {
                    add(firstRest)
                    addAll(quoteLines.drop(1))
                }.filter { it.isNotBlank() }
                val calloutBodySb = StringBuilder()
                for (index in restLines.indices) {
                    if (index != 0) calloutBodySb.append("<br/>")
                    calloutBodySb.append(compileInline(restLines[index], publisher, footnotes, ::footnoteNumber))
                }
                val body = calloutBodySb.toString()
                appendParagraph("<strong>${escapeHtml(title)}</strong>${if (body.isNotEmpty()) "<br/>$body" else ""}")
            } else {
                val quoteBodySb = StringBuilder()
                for (index in quoteLines.indices) {
                    if (index != 0) quoteBodySb.append("<br/>")
                    quoteBodySb.append(compileInline(quoteLines[index], publisher, footnotes, ::footnoteNumber))
                }
                val body = quoteBodySb.toString()
                out.append("<blockquote>").append(body).append("</blockquote>\n")
            }
            continue
        }

        if (looksLikeTableHeader(line) && i + 1 < lines.size && looksLikeTableSeparator(lines[i + 1])) {
            val header = parseTableRow(line)
            val rows = mutableListOf<List<String>>()
            i += 2
            while (i < lines.size && lines[i].trim().startsWith("|")) {
                rows.add(parseTableRow(lines[i]))
                i++
            }
            out.append("""<table data-draft-node="block" data-draft-type="table" data-size="normal"><tbody>""")
            out.append("<tr>")
            for (cell in header) {
                out
                    .append("<td>")
                    .append(compileInline(cell, publisher, footnotes, ::footnoteNumber))
                    .append("</td>")
            }
            out.append("</tr>")
            for (row in rows) {
                out.append("<tr>")
                for (cell in row) {
                    out
                        .append("<td>")
                        .append(compileInline(cell, publisher, footnotes, ::footnoteNumber))
                        .append("</td>")
                }
                out.append("</tr>")
            }
            out.append("</tbody></table>\n")
            continue
        }

        if (line.trimStart().startsWith("- ") || line.trimStart().startsWith("* ")) {
            val items = mutableListOf<String>()
            while (i < lines.size && (lines[i].trimStart().startsWith("- ") || lines[i].trimStart().startsWith("* "))) {
                items.add(lines[i].trimStart().drop(2))
                i++
            }
            out.append("<ul>")
            for (item in items) {
                out
                    .append("<li>")
                    .append(compileInline(item, publisher, footnotes, ::footnoteNumber))
                    .append("</li>")
            }
            out.append("</ul>\n")
            continue
        }

        val olMatch = Regex("^\\s*(\\d+)\\.\\s+").find(line)
        if (olMatch != null) {
            val items = mutableListOf<String>()
            while (i < lines.size && Regex("^\\s*\\d+\\.\\s+").containsMatchIn(lines[i])) {
                items.add(lines[i].replaceFirst(Regex("^\\s*\\d+\\.\\s+"), ""))
                i++
            }
            out.append("<ol>")
            for (item in items) {
                out
                    .append("<li>")
                    .append(compileInline(item, publisher, footnotes, ::footnoteNumber))
                    .append("</li>")
            }
            out.append("</ol>\n")
            continue
        }

        val paragraphLines = mutableListOf<String>()
        while (i < lines.size &&
            lines[i].isNotBlank() &&
            !lines[i].startsWith("```") &&
            !lines[i].startsWith(">") &&
            !lines[i].startsWith("#") &&
            !(looksLikeTableHeader(lines[i]) && i + 1 < lines.size && looksLikeTableSeparator(lines[i + 1])) &&
            !lines[i].trimStart().startsWith("- ") &&
            !lines[i].trimStart().startsWith("* ") &&
            !Regex("^\\s*\\d+\\.\\s+").containsMatchIn(lines[i])
        ) {
            paragraphLines.add(lines[i])
            i++
        }

        val rawParagraph = paragraphLines.joinToString("\n").trim()
        val inline = compileInline(rawParagraph, publisher, footnotes, ::footnoteNumber)
        val cardOnly = Regex("""^\s*\[([^\]]+)]\((\S+)\s+"card"\)\s*$""").matchEntire(rawParagraph)
        if (cardOnly != null) {
            val title = cardOnly.groupValues[1]
            val url = cardOnly.groupValues[2]
            out.append(buildLinkCardHtml(title, url)).append("\n")
        } else {
            appendParagraph(inline.replace("\n", "<br/>"))
        }
    }

    return out.toString().trim()
}

private data class FootnoteDefinition(
    val text: String,
    val url: String?,
)

private fun extractFootnoteDefinitions(markdown: String): Pair<String, Map<String, FootnoteDefinition>> {
    val lines = markdown.split('\n')
    val outLines = mutableListOf<String>()
    val definitions = linkedMapOf<String, StringBuilder>()
    var i = 0
    while (i < lines.size) {
        val line = lines[i]
        val match = Regex("^\\[\\^([^\\]]+)]:(.*)$").find(line)
        if (match != null) {
            val id = match.groupValues[1].trim().uppercase()
            val rest = match.groupValues[2].trimStart()
            val buf = StringBuilder()
            buf.append(rest)
            i++
            while (i < lines.size && (lines[i].startsWith("  ") || lines[i].startsWith("\t"))) {
                buf.append('\n').append(lines[i].trimStart())
                i++
            }
            definitions[id] = buf
            continue
        }
        outLines.add(line)
        i++
    }

    val footnotes = definitions.mapValues { (_, buf) ->
        val raw = buf.toString().trim()
        val link = Regex("\\[([^\\]]+)]\\(([^)\\s]+)\\)").find(raw)
        val url = link?.groupValues?.get(2)
        FootnoteDefinition(
            text = raw
                .replace(Regex("\\[([^\\]]+)]\\(([^)]+)\\)")) { it.groupValues[1] }
                .replace(Regex("[*_`]+"), "")
                .trim(),
            url = url,
        )
    }
    return outLines.joinToString("\n") to footnotes
}

private suspend fun compileInline(
    text: String,
    publisher: ZhihuAnswerPublisher,
    footnotes: Map<String, FootnoteDefinition>,
    footnoteNumber: (String) -> Int,
): String {
    if (text.isEmpty()) return ""
    var working = text
    val tokens = mutableListOf<String>()

    fun addToken(html: String): String {
        val index = tokens.size
        tokens.add(html)
        return "\u0001$index\u0002"
    }

    suspend fun replaceWithTokens(regex: Regex, build: suspend (MatchResult) -> String) {
        val sb = StringBuilder()
        var lastIndex = 0
        val matches = regex.findAll(working).toList()
        if (matches.isEmpty()) return
        for (m in matches) {
            sb.append(working.substring(lastIndex, m.range.first))
            sb.append(addToken(build(m)))
            lastIndex = m.range.last + 1
        }
        sb.append(working.substring(lastIndex))
        working = sb.toString()
    }

    replaceWithTokens(Regex("""!\[([^\]]*)]\((\S+?)(?:\s+"([^"]*)")?\)""")) { m ->
        val alt = m.groupValues[1].ifBlank { "image" }
        val url = m.groupValues[2]
        val title = m.groupValues.getOrNull(3)
        val meta = parseZhimgMeta(title)

        val image = if (isZhimgUrl(url)) {
            UploadedZhihuImage(
                url = url,
                originalUrl = url,
                watermark = meta["wm"]?.toIntOrNull()?.let { it != 0 },
                watermarkUrl = meta["wmsrc"],
                rawWidth = meta["w"]?.toIntOrNull() ?: 0,
                rawHeight = meta["h"]?.toIntOrNull() ?: 0,
            )
        } else {
            publisher.uploadImageFromUrl(url)
        }
        buildZhihuImageHtml(image, alt)
    }

    replaceWithTokens(Regex("""\[\^([^\]]+)]""")) { m ->
        val id = m.groupValues[1].trim().uppercase()
        val numero = footnoteNumber(id)
        val def = footnotes[id]
        val textValue = def?.text?.takeIf { it.isNotBlank() } ?: id
        val urlValue = def?.url
        buildString {
            append("<sup")
            append(""" data-draft-node="inline"""")
            append(""" data-draft-type="reference"""")
            append(""" data-numero="$numero"""")
            append(""" data-text="${escapeHtmlAttribute(textValue)}"""")
            if (!urlValue.isNullOrBlank()) {
                append(""" data-url="${escapeHtmlAttribute(urlValue)}"""")
            }
            append(">")
            append("[$numero]")
            append("</sup>")
        }
    }

    replaceWithTokens(Regex("""\[\[([^\]]+)]]""")) { m ->
        val label = m.groupValues[1].trim()
        "<u>${escapeHtml(label)}</u>"
    }

    replaceWithTokens(Regex("""\[(.+?)]\((\S+)\s+"card"\)""")) { m ->
        val title = m.groupValues[1]
        val url = m.groupValues[2]
        buildLinkCardHtml(title, url)
    }

    replaceWithTokens(Regex("""\[(.+?)]\((\S+)\)""")) { m ->
        val label = m.groupValues[1]
        val url = m.groupValues[2]
        """<a href="${escapeHtmlAttribute(url)}">${escapeHtml(label)}</a>"""
    }

    replaceWithTokens(Regex("`([^`]+)`")) { m ->
        "<code>${escapeHtml(m.groupValues[1])}</code>"
    }

    replaceWithTokens(Regex("""\$(.+?)\$""")) { m ->
        val tex = m.groupValues[1]
        val encoded = tex.encodeURLParameter()
        """<img eeimg="1" src="https://www.zhihu.com/equation?tex=$encoded" alt="${escapeHtmlAttribute(tex)}"/>"""
    }

    working = escapeHtml(working)
        .replace(Regex("\\*\\*([^*]+)\\*\\*")) { m -> "<strong>${escapeHtml(m.groupValues[1])}</strong>" }
        .replace(Regex("\\*([^*]+)\\*")) { m -> "<em>${escapeHtml(m.groupValues[1])}</em>" }

    tokens.forEachIndexed { index, html ->
        working = working.replace("\u0001$index\u0002", html)
    }

    return working
}

private fun buildZhihuImageHtml(image: UploadedZhihuImage, alt: String): String = buildString {
    append("<img")
    append(""" src="${escapeHtmlAttribute(image.url)}"""")
    append(""" alt="${escapeHtmlAttribute(alt)}"""")
    append(""" data-caption="${escapeHtmlAttribute(alt)}"""")
    append(""" data-size="normal"""")
    append(""" data-rawwidth="${image.rawWidth}"""")
    append(""" data-rawheight="${image.rawHeight}"""")
    append(""" data-original-src="${escapeHtmlAttribute(image.originalUrl)}"""")
    image.watermark?.let { append(""" data-watermark="$it"""") }
    image.watermarkUrl?.let { append(""" data-watermark-src="${escapeHtmlAttribute(it)}"""") }
    append(""" data-private-watermark-src=""""")
    append(" />")
}

private fun buildLinkCardHtml(title: String, url: String): String = buildString {
    append("<a")
    append(""" data-draft-node="block"""")
    append(""" data-draft-type="link-card"""")
    append(""" data-draft-title="${escapeHtmlAttribute(title)}"""")
    append(""" data-draft-cover=""""")
    append(""" href="${escapeHtmlAttribute(url)}"""")
    append(">")
    append(escapeHtml(title))
    append("</a>")
}

private fun looksLikeTableHeader(line: String): Boolean = line.trim().startsWith("|") && line.contains('|')

private fun looksLikeTableSeparator(line: String): Boolean {
    val t = line.trim()
    if (!t.startsWith("|")) return false
    return t.replace("|", "").trim().all { it == '-' || it == ':' || it.isWhitespace() }
}

private fun parseTableRow(line: String): List<String> =
    line
        .trim()
        .trim('|')
        .split('|')
        .map { it.trim() }

private fun isZhimgUrl(url: String): Boolean {
    val u = runCatching { Url(url) }.getOrNull() ?: return false
    return u.host.endsWith("zhimg.com")
}

private fun parseZhimgMeta(title: String?): Map<String, String> {
    val text = title?.trim().orEmpty()
    if (!text.startsWith("zhimg:", ignoreCase = true)) return emptyMap()
    return text
        .removePrefix("zhimg:")
        .split(';')
        .mapNotNull { part ->
            val (k, v) = part.split('=', limit = 2).takeIf { it.size == 2 } ?: return@mapNotNull null
            k.trim() to v.trim()
        }.toMap()
}

private fun escapeHtml(text: String): String =
    text
        .replace("&", "&amp;")
        .replace("<", "&lt;")
        .replace(">", "&gt;")
        .replace("\"", "&quot;")
        .replace("'", "&#39;")

private fun escapeHtmlAttribute(text: String): String = escapeHtml(text)
