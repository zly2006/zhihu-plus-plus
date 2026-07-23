package com.hrm.markdown.parser.block.starters

import com.hrm.markdown.parser.LineRange
import com.hrm.markdown.parser.ast.BlockQuote
import com.hrm.markdown.parser.block.OpenBlock
import com.hrm.markdown.parser.core.LineCursor

/**
 * 块引用开启器：以 `>` 为前缀的行。
 */
internal class BlockQuoteStarter : BlockStarter {
    override val priority: Int = 410
    override val canInterruptParagraph: Boolean = true

    override fun tryStart(cursor: LineCursor, lineIdx: Int, tip: OpenBlock): OpenBlock? {
        val indent = cursor.advanceSpaces(3)
        if (cursor.isAtEnd || cursor.peek() != '>') return null
        if (cursor.peek(1) == '!') return null

        cursor.advance()
        // skip optional space (or one column of a tab) after '>'
        if (!cursor.isAtEnd && (cursor.peek() == ' ' || cursor.peek() == '\t')) {
            cursor.advanceSpaces(1)
        }

        val bq = BlockQuote()
        bq.lineRange = LineRange(lineIdx, lineIdx + 1)

        val ob = OpenBlock(bq, contentStartLine = lineIdx, lastLineIndex = lineIdx)
        ob.starterTag = this::class.simpleName
        return ob
    }
}
