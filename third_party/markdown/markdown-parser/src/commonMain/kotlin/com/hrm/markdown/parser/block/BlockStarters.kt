package com.hrm.markdown.parser.block

import com.hrm.markdown.parser.LineRange
import com.hrm.markdown.parser.ast.*
import com.hrm.markdown.parser.core.CharacterUtils
import com.hrm.markdown.parser.core.HtmlEntities
import com.hrm.markdown.parser.core.LineCursor
import com.hrm.markdown.parser.core.SourceText

/**
 * 块级开启器：负责检测并创建新的块级节点。
 *
 * 从 BlockParser 中提取，按优先级顺序尝试各种块开启。
 * 所有方法均为 internal，仅供 BlockParser 在同一包内调用。
 */
internal class BlockStarters(
    private val source: SourceText,
) {
    /**
     * 从当前游标位置尝试开启新块。
     */
    fun tryStartBlock(cursor: LineCursor, lineIdx: Int, tip: OpenBlock): OpenBlock? {
        val snap = cursor.snapshot()

        // 文档开头的前置元数据
        if (lineIdx == 0) {
            tryStartFrontMatter(cursor, lineIdx)?.let { return it }
            cursor.restore(snap)
        }

        // Setext 标题（根据 CommonMark 规范必须在主题分隔线之前检查，
        // 因为当前面有段落内容时 Setext 标题优先级更高）
        tryStartSetextHeading(cursor, lineIdx, tip)?.let { return it }
        cursor.restore(snap)

        // ATX 标题
        tryStartAtxHeading(cursor, lineIdx)?.let { return it }
        cursor.restore(snap)

        // 表格（在段落行后检查分隔行 - 在主题分隔线之前，
        // 使得 `| A |\n| --- |` 被解析为表格而非主题分隔线）
        tryStartTable(cursor, lineIdx, tip)?.let { return it }
        cursor.restore(snap)

        // 主题分隔线（必须在列表项之前检查）
        tryStartThematicBreak(cursor, lineIdx)?.let { return it }
        cursor.restore(snap)

        // 自定义容器 (::: syntax)
        tryStartCustomContainer(cursor, lineIdx)?.let { return it }
        cursor.restore(snap)

        // 围栏代码块
        tryStartFencedCodeBlock(cursor, lineIdx)?.let { return it }
        cursor.restore(snap)

        // 数学块 ($$)
        tryStartMathBlock(cursor, lineIdx)?.let { return it }
        cursor.restore(snap)

        // HTML 块
        tryStartHtmlBlock(cursor, lineIdx)?.let { return it }
        cursor.restore(snap)

        // 块引用
        tryStartBlockQuote(cursor, lineIdx)?.let { return it }
        cursor.restore(snap)

        // 列表项
        tryStartListItem(cursor, lineIdx, tip)?.let { return it }
        cursor.restore(snap)

        // 脚注定义
        tryStartFootnoteDefinition(cursor, lineIdx)?.let { return it }
        cursor.restore(snap)

        // 定义列表
        tryStartDefinitionDescription(cursor, lineIdx, tip)?.let { return it }
        cursor.restore(snap)

        // 缩进代码块（必须在列表项检查之后）
        tryStartIndentedCodeBlock(cursor, lineIdx, tip)?.let { return it }
        cursor.restore(snap)

        return null
    }

    /**
     * 检查新块是否可以中断段落。
     */
    fun canInterruptParagraph(node: Node, cursor: LineCursor): Boolean {
        return when (node) {
            is Heading -> true
            is ThematicBreak -> true
            is BlockQuote -> true
            is FencedCodeBlock -> true
            is HtmlBlock -> node.htmlType in 1..6
            is ListItem -> true
            is IndentedCodeBlock -> false // 不能中断段落
            is Table -> true // 表格的 header 来自段落内容，属于段落转换而非打断
            is MathBlock -> true
            is DefinitionDescription -> true // 定义描述可以将段落转换为定义术语
            is CustomContainer -> true // 自定义容器可以打断段落
            else -> true
        }
    }

    // ────── 块开启器 ──────

    private fun tryStartAtxHeading(cursor: LineCursor, lineIdx: Int): OpenBlock? {
        val indent = cursor.advanceSpaces(3)
        if (cursor.isAtEnd || cursor.peek() != '#') return null

        var level = 0
        while (!cursor.isAtEnd && cursor.peek() == '#') {
            cursor.advance()
            level++
        }
        if (level > 6) return null
        if (!cursor.isAtEnd && cursor.peek() != ' ' && cursor.peek() != '\t') {
            if (!cursor.isAtEnd) return null // `#heading` without space is not a heading
        }

        // 跳过 # 后面的空格
        if (!cursor.isAtEnd && (cursor.peek() == ' ' || cursor.peek() == '\t')) {
            cursor.advance()
        }

        // 获取内容，去除可选的尾部 #
        var content = cursor.rest().trimEnd()
        val customId = extractCustomId(content)
        if (customId != null) {
            content = content.replace(CUSTOM_ID_STRIP_REGEX, "").trimEnd()
        }
        // 去除尾部 #
        if (content.endsWith('#')) {
            val strippedTrailing = content.trimEnd('#')
            if (strippedTrailing.isEmpty() || strippedTrailing.endsWith(' ') || strippedTrailing.endsWith('\t')) {
                content = strippedTrailing.trimEnd()
            }
        }

        val heading = Heading(level)
        heading.customId = customId
        heading.lineRange = LineRange(lineIdx, lineIdx + 1)

        val ob = OpenBlock(heading, contentStartLine = lineIdx, lastLineIndex = lineIdx)
        ob.contentLines.add(content)
        return ob
    }

    internal fun extractCustomId(content: String): String? {
        val match = CUSTOM_ID_REGEX.find(content) ?: return null
        return match.groupValues[1]
    }

    private fun tryStartSetextHeading(cursor: LineCursor, lineIdx: Int, tip: OpenBlock): OpenBlock? {
        if (tip.paragraphContent == null) return null

        val indent = cursor.advanceSpaces(3)
        if (cursor.isAtEnd) return null

        val c = cursor.peek()
        if (c != '=' && c != '-') return null

        // 所有剩余字符必须是同一个字符（加上尾部空格）
        val rest = cursor.rest()
        val stripped = rest.trimEnd()
        if (!stripped.all { it == c }) return null
        if (stripped.isEmpty()) return null

        val level = if (c == '=') 1 else 2
        val heading = SetextHeading(level)
        heading.lineRange = LineRange(tip.contentStartLine, lineIdx + 1)

        val ob = OpenBlock(heading, contentStartLine = tip.contentStartLine, lastLineIndex = lineIdx)
        ob.contentLines.addAll(tip.paragraphContent.toString().split('\n'))
        return ob
    }

    private fun tryStartThematicBreak(cursor: LineCursor, lineIdx: Int): OpenBlock? {
        val indent = cursor.advanceSpaces(3)
        if (cursor.isAtEnd) return null

        val c = cursor.peek()
        if (c != '-' && c != '*' && c != '_') return null

        var count = 0
        val rest = cursor.rest()
        for (ch in rest) {
            when (ch) {
                c -> count++
                ' ', '\t' -> {} // 之间允许空格
                else -> return null
            }
        }

        if (count < 3) return null

        val tb = ThematicBreak(c)
        tb.lineRange = LineRange(lineIdx, lineIdx + 1)

        val ob = OpenBlock(tb, lastLineIndex = lineIdx)
        return ob
    }

    private fun tryStartFencedCodeBlock(cursor: LineCursor, lineIdx: Int): OpenBlock? {
        val indent = cursor.advanceSpaces(3)
        if (cursor.isAtEnd) return null

        val c = cursor.peek()
        if (c != '`' && c != '~') return null

        var fenceLength = 0
        while (!cursor.isAtEnd && cursor.peek() == c) {
            cursor.advance()
            fenceLength++
        }
        if (fenceLength < 3) return null

        // 信息字符串（反引号围栏的信息中不允许包含反引号）
        val info = cursor.rest().trim()
        if (c == '`' && info.contains('`')) return null

        // 消耗掉 info string 剩余部分，避免被 addLineToTip 当作代码内容
        cursor.advance(cursor.remaining)

        val rawLang = info.split(INFO_LANG_SPLIT_REGEX).firstOrNull()?.trim() ?: ""
        val language = HtmlEntities.replaceAll(resolveBackslashEscapes(rawLang))

        val block = FencedCodeBlock(
            info = info,
            language = language,
            fenceChar = c,
            fenceLength = fenceLength,
            fenceIndent = indent
        )
        block.lineRange = LineRange(lineIdx, lineIdx + 1)

        val ob = OpenBlock(block, contentStartLine = lineIdx, lastLineIndex = lineIdx)
        ob.isFenced = true
        ob.fenceChar = c
        ob.fenceLength = fenceLength
        ob.fenceIndent = indent
        return ob
    }

    private fun tryStartIndentedCodeBlock(cursor: LineCursor, lineIdx: Int, tip: OpenBlock): OpenBlock? {
        // 缩进代码块不能中断段落
        if (tip.paragraphContent != null) return null
        // 必须有 4 个空格
        val indent = cursor.advanceSpaces()
        if (indent < 4) return null

        val block = IndentedCodeBlock()
        block.lineRange = LineRange(lineIdx, lineIdx + 1)

        val ob = OpenBlock(block, contentStartLine = lineIdx, lastLineIndex = lineIdx)
        return ob
    }

    private fun tryStartBlockQuote(cursor: LineCursor, lineIdx: Int): OpenBlock? {
        val indent = cursor.advanceSpaces(3)
        if (cursor.isAtEnd || cursor.peek() != '>') return null

        cursor.advance() // 跳过 '>'
        if (!cursor.isAtEnd && cursor.peek() == ' ') {
            cursor.advance() // 跳过可选空格
        }

        val bq = BlockQuote()
        bq.lineRange = LineRange(lineIdx, lineIdx + 1)

        val ob = OpenBlock(bq, contentStartLine = lineIdx, lastLineIndex = lineIdx)
        return ob
    }

    fun tryStartListItem(cursor: LineCursor, lineIdx: Int, tip: OpenBlock): OpenBlock? {
        val snap = cursor.snapshot()
        val indent = cursor.advanceSpaces(3)
        if (cursor.isAtEnd) return null

        val c = cursor.peek()
        var ordered = false
        var bulletChar = c
        var startNumber = 1
        var delimiter = '.'
        var markerWidth = 0

        when (c) {
            '-', '*', '+' -> {
                cursor.advance()
                markerWidth = 1
            }
            in '0'..'9' -> {
                ordered = true
                val numBuilder = StringBuilder()
                while (!cursor.isAtEnd && cursor.peek() in '0'..'9' && numBuilder.length < 9) {
                    numBuilder.append(cursor.advance())
                }
                if (cursor.isAtEnd) return null
                val d = cursor.peek()
                if (d != '.' && d != ')') return null
                delimiter = d
                cursor.advance()
                startNumber = numBuilder.toString().toIntOrNull() ?: return null
                markerWidth = numBuilder.length + 1
            }
            else -> return null
        }

        // 必须后跟空格/制表符或行尾
        if (!cursor.isAtEnd && cursor.peek() != ' ' && cursor.peek() != '\t') return null

        // 消耗标记后的一个空格（或使用行尾标记）
        val contentIndent: Int
        if (cursor.isAtEnd) {
            contentIndent = indent + markerWidth + 1
        } else {
            val preSpaceSnap = cursor.snapshot()
            val postMarker = cursor.advanceSpaces(4)
            if (cursor.isAtEnd || cursor.restIsBlank()) {
                // blank line after marker: use minimum indent
                cursor.restore(preSpaceSnap)
                if (!cursor.isAtEnd && (cursor.peek() == ' ' || cursor.peek() == '\t')) {
                    cursor.advance()
                }
                contentIndent = indent + markerWidth + 1
            } else if (postMarker == 4 && !cursor.isAtEnd && (cursor.peek() == ' ' || cursor.peek() == '\t')) {
                // more than 4 spaces after marker -> indented code block; use minimum indent
                cursor.restore(preSpaceSnap)
                cursor.advance()
                contentIndent = indent + markerWidth + 1
            } else {
                contentIndent = indent + markerWidth + if (postMarker == 0) 1 else postMarker
            }
        }

        // 检查任务列表
        var isTask = false
        var checked = false
        if (!cursor.isAtEnd) {
            val taskSnap = cursor.snapshot()
            if (cursor.peek() == '[') {
                cursor.advance()
                if (!cursor.isAtEnd) {
                    val mark = cursor.peek()
                    if (mark == ' ' || mark == 'x' || mark == 'X') {
                        cursor.advance()
                        if (!cursor.isAtEnd && cursor.peek() == ']') {
                            cursor.advance()
                            if (cursor.isAtEnd || cursor.peek() == ' ' || cursor.peek() == '\t') {
                                isTask = true
                                checked = mark == 'x' || mark == 'X'
                                if (!cursor.isAtEnd) cursor.advance() // 跳过 ] 后的空格
                            } else {
                                cursor.restore(taskSnap)
                            }
                        } else {
                            cursor.restore(taskSnap)
                        }
                    } else {
                        cursor.restore(taskSnap)
                    }
                } else {
                    cursor.restore(taskSnap)
                }
            }
        }

        val listItem = ListItem(
            markerIndent = indent,
            contentIndent = contentIndent
        )
        listItem.taskListItem = isTask
        listItem.checked = checked
        listItem.lineRange = LineRange(lineIdx, lineIdx + 1)

        return listItem to ListItemMeta(ordered, bulletChar, startNumber, delimiter)
    }

    /**
     * tryStartListItem 的返回类型辅助，用于传递列表元数据。
     * 重载版本，返回 OpenBlock。实际的列表创建逻辑仍在 BlockParser 中。
     */
    private infix fun ListItem.to(meta: ListItemMeta): OpenBlock {
        val ob = OpenBlock(this, contentStartLine = this.lineRange.startLine, lastLineIndex = this.lineRange.startLine)
        ob.listItemMeta = meta
        return ob
    }

    private fun tryStartHtmlBlock(cursor: LineCursor, lineIdx: Int): OpenBlock? {
        val indent = cursor.advanceSpaces(3)
        if (cursor.isAtEnd || cursor.peek() != '<') return null

        val rest = cursor.rest()
        val htmlType = detectHtmlBlockType(rest) ?: return null

        val block = HtmlBlock(htmlType = htmlType)
        block.lineRange = LineRange(lineIdx, lineIdx + 1)

        val ob = OpenBlock(block, contentStartLine = lineIdx, lastLineIndex = lineIdx)
        ob.htmlType = htmlType
        return ob
    }

    private fun tryStartTable(cursor: LineCursor, lineIdx: Int, tip: OpenBlock): OpenBlock? {
        if (tip.paragraphContent == null) return null
        val headerLine = tip.paragraphContent.toString().trim()
        if (!headerLine.contains('|')) return null
        // 检查当前行是否为有效的分隔行
        val delimLine = cursor.rest().trim()
        val alignments = TableParser.parseTableDelimiterRow(delimLine) ?: return null
        val headerCells = TableParser.parseTableRow(headerLine)
        if (headerCells.isEmpty()) return null

        val table = Table()
        table.columnAlignments = alignments
        table.lineRange = LineRange(tip.contentStartLine, lineIdx + 1)

        val head = TableHead()
        val headerRow = TableRow()
        // 列数以分隔行为准：多余截断，不足补空
        val colCount = alignments.size
        for (i in 0 until colCount) {
            val cellContent = headerCells.getOrElse(i) { "" }
            val align = alignments[i]
            val cell = TableCell(alignment = align, isHeader = true)
            cell.lineRange = LineRange(tip.contentStartLine, tip.contentStartLine + 1)
            cell.rawContent = cellContent
            headerRow.appendChild(cell)
        }
        head.appendChild(headerRow)
        table.appendChild(head)

        val body = TableBody()
        table.appendChild(body)

        val ob = OpenBlock(table, contentStartLine = tip.contentStartLine, lastLineIndex = lineIdx)
        ob.contentLines.clear()
        return ob
    }

    private fun tryStartMathBlock(cursor: LineCursor, lineIdx: Int): OpenBlock? {
        val indent = cursor.advanceSpaces(3)
        if (cursor.remaining < 2) return null
        if (cursor.peek() != '$' || cursor.peek(1) != '$') return null

        cursor.advance()
        cursor.advance()
        val rest = cursor.rest().trim()
        // 如果同一行内找到闭合 $$，则为单行数学块，闭合后的内容保留为后续段落。
        val closingIndex = rest.indexOf("$$")
        if (closingIndex > 0) {
            val content = rest.substring(0, closingIndex)
            val trailing = rest.substring(closingIndex + 2)
            val block = MathBlock(literal = content)
            block.lineRange = LineRange(lineIdx, lineIdx + 1)
            val ob = OpenBlock(block, contentStartLine = lineIdx, lastLineIndex = lineIdx)
            ob.trailingContent = trailing
            return ob
        }

        val block = MathBlock()
        block.lineRange = LineRange(lineIdx, lineIdx + 1)
        val ob = OpenBlock(block, contentStartLine = lineIdx, lastLineIndex = lineIdx)
        return ob
    }

    private fun tryStartFootnoteDefinition(cursor: LineCursor, lineIdx: Int): OpenBlock? {
        val snap = cursor.snapshot()
        val indent = cursor.advanceSpaces(3)
        if (cursor.isAtEnd || cursor.peek() != '[') return null
        cursor.advance()
        if (cursor.isAtEnd || cursor.peek() != '^') return null
        cursor.advance()

        val label = StringBuilder()
        while (!cursor.isAtEnd && cursor.peek() != ']') {
            label.append(cursor.advance())
        }
        if (cursor.isAtEnd || label.isEmpty()) return null
        cursor.advance() // 跳过 ']'
        if (cursor.isAtEnd || cursor.peek() != ':') return null
        cursor.advance() // 跳过 ':'

        // 跳过可选空格
        if (!cursor.isAtEnd && cursor.peek() == ' ') cursor.advance()

        val fd = FootnoteDefinition(label = label.toString())
        fd.lineRange = LineRange(lineIdx, lineIdx + 1)

        val ob = OpenBlock(fd, contentStartLine = lineIdx, lastLineIndex = lineIdx)
        return ob
    }

    private fun tryStartDefinitionDescription(cursor: LineCursor, lineIdx: Int, tip: OpenBlock): OpenBlock? {
        // 定义描述行必须紧跟段落（作为术语）或已存在的定义列表
        if (tip.paragraphContent == null && tip.node !is DefinitionList) return null

        val snap = cursor.snapshot()
        val indent = cursor.advanceSpaces(3)
        if (cursor.isAtEnd || cursor.peek() != ':') {
            cursor.restore(snap)
            return null
        }
        cursor.advance() // 跳过 ':'
        // 冒号后必须跟空格或 tab
        if (cursor.isAtEnd || (cursor.peek() != ' ' && cursor.peek() != '\t')) {
            cursor.restore(snap)
            return null
        }
        cursor.advance() // 跳过空格

        val desc = DefinitionDescription()
        desc.lineRange = LineRange(lineIdx, lineIdx + 1)

        val ob = OpenBlock(desc, contentStartLine = lineIdx, lastLineIndex = lineIdx)
        return ob
    }

    private fun tryStartFrontMatter(cursor: LineCursor, lineIdx: Int): OpenBlock? {
        val rest = cursor.rest().trim()
        val format = when {
            rest == "---" -> "yaml"
            rest == "+++" -> "toml"
            else -> return null
        }

        val block = FrontMatter(format = format)
        block.lineRange = LineRange(lineIdx, lineIdx + 1)
        val ob = OpenBlock(block, contentStartLine = lineIdx, lastLineIndex = lineIdx)
        return ob
    }

    private fun tryStartCustomContainer(cursor: LineCursor, lineIdx: Int): OpenBlock? {
        val indent = cursor.advanceSpaces(3)
        if (cursor.isAtEnd) return null

        if (cursor.peek() != ':') return null

        // 计算连续冒号数
        var colonCount = 0
        val snap = cursor.snapshot()
        while (!cursor.isAtEnd && cursor.peek() == ':') {
            cursor.advance()
            colonCount++
        }
        if (colonCount < 3) return null

        // 冒号后面可以跟：类型名、属性 {.class #id}、标题 "..."
        val rest = cursor.rest().trim()
        cursor.advance(cursor.remaining) // 消耗剩余内容

        // 解析类型、标题和属性
        var type = ""
        var title = ""
        var cssClasses = emptyList<String>()
        var cssId: String? = null

        if (rest.isNotEmpty()) {
            val parsed = parseContainerInfo(rest)
            type = parsed.type
            title = parsed.title
            cssClasses = parsed.cssClasses
            cssId = parsed.cssId
        }

        val block = CustomContainer(
            type = type,
            title = title,
            cssClasses = cssClasses,
            cssId = cssId,
        )
        block.lineRange = LineRange(lineIdx, lineIdx + 1)

        val ob = OpenBlock(block, contentStartLine = lineIdx, lastLineIndex = lineIdx)
        ob.fenceChar = ':'
        ob.fenceLength = colonCount
        ob.fenceIndent = indent
        return ob
    }

    // ────── HTML 块类型检测 ──────

    internal fun detectHtmlBlockType(line: String): Int? {
        val lower = line.lowercase()
        // 类型 1：<script>、<pre>、<style>、<textarea>
        if (HTML_TYPE1_REGEX.containsMatchIn(line)) return 1
        // 类型 2：<!-- 注释
        if (lower.startsWith("<!--")) return 2
        // 类型 3：<? 处理指令
        if (lower.startsWith("<?")) return 3
        // 类型 4：<!声明
        if (HTML_TYPE4_REGEX.containsMatchIn(line)) return 4
        // 类型 5：CDATA
        if (lower.startsWith("<![cdata[")) return 5
        // 类型 6：已知块级标签
        val tagMatch = HTML_TYPE6_TAG_REGEX.find(line)
        if (tagMatch != null && tagMatch.groupValues[1].lowercase() in BLOCK_TAGS) return 6
        if (HTML_TYPE7_OPEN_REGEX.containsMatchIn(line) || HTML_TYPE7_CLOSE_REGEX.containsMatchIn(line)) return 7
        return null
    }

    // ────── 容器信息解析 ──────

    private data class ContainerInfo(
        val type: String,
        val title: String,
        val cssClasses: List<String>,
        val cssId: String?,
    )

    private fun parseContainerInfo(info: String): ContainerInfo {
        var remaining = info
        var type = ""
        var title = ""
        val cssClasses = mutableListOf<String>()
        var cssId: String? = null

        // 提取属性块 {.class #id key=value}
        val attrMatch = CONTAINER_ATTR_REGEX.find(remaining)
        if (attrMatch != null) {
            val attrContent = attrMatch.groupValues[1]
            // 解析 .class
            CONTAINER_CLASS_REGEX.findAll(attrContent).forEach {
                cssClasses.add(it.groupValues[1])
            }
            // 解析 #id
            CONTAINER_ID_REGEX.find(attrContent)?.let {
                cssId = it.groupValues[1]
            }
            remaining = remaining.removeRange(attrMatch.range).trim()
        }

        // 提取标题 "..." 或 '...'
        val titleMatch = CONTAINER_TITLE_REGEX.find(remaining)
        if (titleMatch != null) {
            title = titleMatch.groupValues[1].ifEmpty { titleMatch.groupValues[2] }
            remaining = remaining.removeRange(titleMatch.range).trim()
        }

        // 剩余部分是类型名
        type = remaining.trim().split(INFO_LANG_SPLIT_REGEX).firstOrNull() ?: ""

        return ContainerInfo(type, title, cssClasses, cssId)
    }

    private fun resolveBackslashEscapes(s: String): String {
        val sb = StringBuilder(s.length)
        var i = 0
        while (i < s.length) {
            if (s[i] == '\\' && i + 1 < s.length && CharacterUtils.isAsciiPunctuation(s[i + 1])) {
                sb.append(s[i + 1])
                i += 2
            } else {
                sb.append(s[i])
                i++
            }
        }
        return sb.toString()
    }

    companion object {
        internal val CUSTOM_ID_REGEX = Regex("\\{#([^\\}]+)\\}\\s*$")
        internal val CUSTOM_ID_STRIP_REGEX = Regex("\\s*\\{#[^\\}]+\\}\\s*$")

        /** 围栏代码块信息字符串中的语言提取 */
        private val INFO_LANG_SPLIT_REGEX = Regex("\\s+")

        /** 自定义容器属性块：{.class #id key=value} */
        private val CONTAINER_ATTR_REGEX = Regex("\\{([^}]+)\\}")
        /** 自定义容器 CSS class：.classname */
        private val CONTAINER_CLASS_REGEX = Regex("\\.([a-zA-Z][a-zA-Z0-9_-]*)")
        /** 自定义容器 CSS ID：#idname */
        private val CONTAINER_ID_REGEX = Regex("#([a-zA-Z][a-zA-Z0-9_-]*)")
        /** 自定义容器标题提取（双引号或单引号） */
        private val CONTAINER_TITLE_REGEX = Regex("\"([^\"]*)\"|'([^']*)'")

        /** HTML 块类型检测（类型 1-7） */
        private val HTML_TYPE1_REGEX = Regex("^<(script|pre|style|textarea)(\\s|>|$)", RegexOption.IGNORE_CASE)
        private val HTML_TYPE4_REGEX = Regex("^<![A-Z]")
        private val HTML_TYPE6_TAG_REGEX = Regex("^</?([a-zA-Z][a-zA-Z0-9-]*)(\\s|/?>|$)")
        // type 7: complete open tag or closing tag, followed by only optional whitespace
        private val HTML_TYPE7_OPEN_REGEX = Regex(
            """^<[a-zA-Z][a-zA-Z0-9-]*(?:\s+[a-zA-Z_:][a-zA-Z0-9_.:-]*(?:\s*=\s*(?:[^\s"'=<>`]+|'[^']*'|"[^"]*"))?)*\s*/?>[ \t]*$"""
        )
        private val HTML_TYPE7_CLOSE_REGEX = Regex(
            """^</[a-zA-Z][a-zA-Z0-9-]*\s*>[ \t]*$"""
        )

        /** HTML 块类型 6 的已知块级标签集合 */
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
    }
}

/**
 * 列表项的元数据，用于 BlockParser 中创建/匹配 ListBlock。
 */
data class ListItemMeta(
    val ordered: Boolean,
    val bulletChar: Char,
    val startNumber: Int,
    val delimiter: Char,
)
