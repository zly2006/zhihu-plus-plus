package com.hrm.markdown.parser.block.starters

import com.hrm.markdown.parser.LineRange
import com.hrm.markdown.parser.ast.MathBlock
import com.hrm.markdown.parser.block.OpenBlock
import com.hrm.markdown.parser.core.LineCursor

/**
 * 数学公式块开启器：`$$...$$`。
 *
 * 公式编号（`\tag{N}`）、环境自动编号（equation/align 等）、引用（`\ref`/`\eqref`）
 * 均由 LaTeX 渲染库原生处理，parser 层保留原始 LaTeX 文本不做编号提取。
 */
internal class MathBlockStarter : BlockStarter {
    override val priority: Int = 320
    override val canInterruptParagraph: Boolean = true

    override fun tryStart(cursor: LineCursor, lineIdx: Int, tip: OpenBlock): OpenBlock? {
        val indent = cursor.advanceSpaces(3)
        if (cursor.remaining < 2) return null
        if (cursor.peek() != '$' || cursor.peek(1) != '$') return null

        cursor.advance()
        cursor.advance()
        val rest = cursor.rest().trim()
        val closingIndex = rest.indexOf("$$")
        if (closingIndex > 0) {
            val content = rest.substring(0, closingIndex)
            val trailing = rest.substring(closingIndex + 2)
            val block = MathBlock(literal = content)
            block.lineRange = LineRange(lineIdx, lineIdx + 1)
            val ob = OpenBlock(block, contentStartLine = lineIdx, lastLineIndex = lineIdx)
            ob.trailingContent = trailing
            ob.starterTag = this::class.simpleName
            return ob
        }

        val block = MathBlock()
        block.lineRange = LineRange(lineIdx, lineIdx + 1)
        val ob = OpenBlock(block, contentStartLine = lineIdx, lastLineIndex = lineIdx)
        ob.starterTag = this::class.simpleName
        return ob
    }
}
