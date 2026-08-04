package com.hrm.markdown.parser.inline

/**
 * 行内扫描器：逐字符扫描行内内容字符串。
 *
 * 从 InlineParser.kt 提取为独立文件，供 InlineParserInstance 使用。
 */
internal class InlineScanner(val text: String) {
    var pos: Int = 0

    val isAtEnd: Boolean get() = pos >= text.length

    fun peek(): Char = if (pos < text.length) text[pos] else '\u0000'
    fun peek(offset: Int): Char {
        val idx = pos + offset
        return if (idx in text.indices) text[idx] else '\u0000'
    }

    fun advance(): Char {
        if (pos >= text.length) return '\u0000'
        return text[pos++]
    }
}
