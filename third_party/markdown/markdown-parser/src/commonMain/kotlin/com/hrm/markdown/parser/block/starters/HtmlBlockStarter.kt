package com.hrm.markdown.parser.block.starters

import com.hrm.markdown.parser.LineRange
import com.hrm.markdown.parser.ast.HtmlBlock
import com.hrm.markdown.parser.block.OpenBlock
import com.hrm.markdown.parser.core.LineCursor

/**
 * HTML 块开启器（CommonMark 规范中的类型 1-7）。
 */
internal class HtmlBlockStarter : BlockStarter {
    override val priority: Int = 400
    override val canInterruptParagraph: Boolean = false // 仅类型 1-6 能中断

    // canInterruptParagraph 需要根据具体类型判断，在 BlockParser 中特殊处理
    fun canInterruptParagraphForType(htmlType: Int): Boolean = htmlType in 1..6

    override fun tryStart(cursor: LineCursor, lineIdx: Int, tip: OpenBlock): OpenBlock? {
        val fullRest = cursor.rest()
        val indent = cursor.advanceSpaces(3)
        if (cursor.isAtEnd || cursor.peek() != '<') return null

        val rest = cursor.rest()
        val htmlType = detectHtmlBlockType(rest) ?: return null

        val block = HtmlBlock(htmlType = htmlType)
        block.lineRange = LineRange(lineIdx, lineIdx + 1)

        val ob = OpenBlock(block, contentStartLine = lineIdx, lastLineIndex = lineIdx)
        ob.htmlType = htmlType
        ob.starterTag = this::class.simpleName
        ob.contentLines.add(fullRest)
        return ob
    }

    companion object {
        private val HTML_TYPE1_REGEX = Regex("^<(script|pre|style|textarea)(\\s|>|$)", RegexOption.IGNORE_CASE)
        private val HTML_TYPE4_REGEX = Regex("^<![A-Z]")
        private val HTML_TYPE6_TAG_REGEX = Regex("^</?([a-zA-Z][a-zA-Z0-9-]*)(\\s|/?>|$)")
        private val HTML_TYPE7_OPEN_REGEX = Regex(
            """^<[a-zA-Z][a-zA-Z0-9-]*(?:\s+[a-zA-Z_:][a-zA-Z0-9_.:-]*(?:\s*=\s*(?:[^\s"'=<>`]+|'[^']*'|"[^"]*"))?)*\s*/?>[ \t]*$"""
        )
        private val HTML_TYPE7_CLOSE_REGEX = Regex(
            """^</[a-zA-Z][a-zA-Z0-9-]*\s*>[ \t]*$"""
        )

        private val BLOCK_TAGS = setOf(
            "address", "article", "aside", "base", "basefont", "blockquote", "body",
            "caption", "center", "col", "colgroup", "dd", "details", "dialog", "dir",
            "div", "dl", "dt", "fieldset", "figcaption", "figure", "footer", "form",
            "frame", "frameset", "h1", "h2", "h3", "h4", "h5", "h6", "head", "header",
            "hr", "html", "iframe", "legend", "li", "link", "main", "menu", "menuitem",
            "nav", "noframes", "ol", "optgroup", "option", "p", "param", "search",
            "section", "summary", "table", "tbody", "td", "template", "tfoot", "th",
            "thead", "title", "tr", "track", "ul"
        )

        fun detectHtmlBlockType(line: String): Int? {
            val lower = line.lowercase()
            if (HTML_TYPE1_REGEX.containsMatchIn(line)) return 1
            if (lower.startsWith("<!--")) return 2
            if (lower.startsWith("<?")) return 3
            if (HTML_TYPE4_REGEX.containsMatchIn(line)) return 4
            if (lower.startsWith("<![cdata[")) return 5
            val tagMatch = HTML_TYPE6_TAG_REGEX.find(line)
            if (tagMatch != null && tagMatch.groupValues[1].lowercase() in BLOCK_TAGS) return 6
            if (HTML_TYPE7_OPEN_REGEX.containsMatchIn(line) || HTML_TYPE7_CLOSE_REGEX.containsMatchIn(line)) return 7
            return null
        }
    }
}
