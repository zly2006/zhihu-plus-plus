package com.hrm.markdown.parser.core

/**
 * 高效的行文本扫描游标。
 * 处理制表符到 4 空格制表位的展开。
 */
class LineCursor(
    val line: String,
    private var pos: Int = 0,
    private var column: Int = 0
) {
    // remaining virtual spaces from a partially consumed tab
    private var partialTabSpaces: Int = 0

    /** 当前在字符串中的位置。 */
    val position: Int get() = pos

    /** 当前列号（考虑制表符展开）。 */
    val currentColumn: Int get() = column

    /** 剩余字符数。 */
    val remaining: Int get() = line.length - pos

    /** 是否已到达末尾。 */
    val isAtEnd: Boolean get() = pos >= line.length && partialTabSpaces == 0

    /** 查看当前字符但不前进。 */
    fun peek(): Char {
        if (partialTabSpaces > 0) return ' '
        return if (pos < line.length) line[pos] else '\u0000'
    }

    /** 查看从当前位置偏移处的字符。 */
    fun peek(offset: Int): Char {
        if (offset == 0) return peek()
        // for offsets beyond partial tab, look into the actual string
        val adjustedOffset = if (partialTabSpaces > 0) {
            if (offset < partialTabSpaces) return ' '
            offset - partialTabSpaces
        } else {
            offset
        }
        val idx = pos + adjustedOffset
        return if (idx in line.indices) line[idx] else '\u0000'
    }

    /** 前进一个字符并返回该字符。 */
    fun advance(): Char {
        if (partialTabSpaces > 0) {
            partialTabSpaces--
            column++
            return ' '
        }
        if (pos >= line.length) return '\u0000'
        val c = line[pos++]
        column = if (c == '\t') {
            ((column / 4) + 1) * 4
        } else {
            column + 1
        }
        return c
    }

    /** 前进 n 个字符。 */
    fun advance(n: Int) {
        repeat(n) { advance() }
    }

    /**
     * 跳过最多 [max] 个空格/制表符，展开制表符。
     * 返回消耗的列数。
     */
    fun advanceSpaces(max: Int = Int.MAX_VALUE): Int {
        var consumed = 0
        while (!isAtEnd && consumed < max) {
            if (partialTabSpaces > 0) {
                // consume remaining virtual spaces from a partial tab
                val take = minOf(partialTabSpaces, max - consumed)
                partialTabSpaces -= take
                column += take
                consumed += take
                continue
            }
            val c = peek()
            if (c == ' ') {
                advance()
                consumed++
            } else if (c == '\t') {
                val tabWidth = 4 - (column % 4)
                if (consumed + tabWidth <= max) {
                    // fully consume this tab
                    advance()
                    consumed += tabWidth
                } else {
                    // partially consume this tab
                    val take = max - consumed
                    // advance pos past the tab character
                    pos++
                    column += take
                    partialTabSpaces = tabWidth - take
                    consumed += take
                }
            } else {
                break
            }
        }
        return consumed
    }

    /** 获取从当前位置到行尾的剩余内容。 */
    fun rest(): String {
        val prefix = if (partialTabSpaces > 0) " ".repeat(partialTabSpaces) else ""
        val suffix = if (pos < line.length) line.substring(pos) else ""
        return prefix + suffix
    }

    /** 获取从当前位置到行尾的剩余内容（去除首尾空白）。 */
    fun restTrimmed(): String = rest().trim()

    /** 检查剩余内容是否为空白（仅包含空格/制表符）。 */
    fun restIsBlank(): Boolean {
        // partial tab spaces are whitespace, so they don't prevent blank
        for (i in pos until line.length) {
            if (line[i] != ' ' && line[i] != '\t') return false
        }
        return true
    }

    /** 创建当前状态的快照。 */
    fun snapshot(): Snapshot = Snapshot(pos, column, partialTabSpaces)

    /** 恢复到之前的状态。 */
    fun restore(snapshot: Snapshot) {
        pos = snapshot.pos
        column = snapshot.column
        partialTabSpaces = snapshot.partialTabSpaces
    }

    data class Snapshot(val pos: Int, val column: Int, val partialTabSpaces: Int = 0)
}
