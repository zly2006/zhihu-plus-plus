package com.hrm.markdown.parser.block.starters

import com.hrm.markdown.parser.LineRange
import com.hrm.markdown.parser.ast.ThematicBreak
import com.hrm.markdown.parser.block.OpenBlock
import com.hrm.markdown.parser.core.LineCursor

/**
 * 主题分隔线开启器：`---`、`***` 或 `___`。
 */
internal class ThematicBreakStarter : BlockStarter {
    override val priority: Int = 210
    override val canInterruptParagraph: Boolean = true

    override fun tryStart(cursor: LineCursor, lineIdx: Int, tip: OpenBlock): OpenBlock? {
        val indent = cursor.advanceSpaces(3)
        if (cursor.isAtEnd) return null

        val c = cursor.peek()
        if (c != '-' && c != '*' && c != '_') return null

        var count = 0
        val rest = cursor.rest()
        for (ch in rest) {
            when (ch) {
                c -> count++
                ' ', '\t' -> {}
                else -> return null
            }
        }

        if (count < 3) return null

        val tb = ThematicBreak(c)
        tb.lineRange = LineRange(lineIdx, lineIdx + 1)

        val ob = OpenBlock(tb, lastLineIndex = lineIdx)
        ob.starterTag = this::class.simpleName
        return ob
    }
}
