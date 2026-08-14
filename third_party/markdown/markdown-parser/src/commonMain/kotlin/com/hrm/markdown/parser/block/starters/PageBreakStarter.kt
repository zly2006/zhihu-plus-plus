package com.hrm.markdown.parser.block.starters

import com.hrm.markdown.parser.LineRange
import com.hrm.markdown.parser.ast.PageBreak
import com.hrm.markdown.parser.block.OpenBlock
import com.hrm.markdown.parser.core.LineCursor

/**
 * 分页符开启器：`***pagebreak***`。
 *
 * 识别独占一行的 `***pagebreak***` 标记，生成 [PageBreak] AST 节点。
 * 用于 PDF 导出/打印场景。
 */
internal class PageBreakStarter : BlockStarter {
    override val priority: Int = 205
    override val canInterruptParagraph: Boolean = true

    override fun tryStart(cursor: LineCursor, lineIdx: Int, tip: OpenBlock): OpenBlock? {
        val indent = cursor.advanceSpaces(3)
        if (cursor.isAtEnd) return null

        val rest = cursor.rest().trim()
        if (!PAGEBREAK_REGEX.matches(rest)) return null

        cursor.advance(cursor.remaining)

        val node = PageBreak()
        node.lineRange = LineRange(lineIdx, lineIdx + 1)

        return OpenBlock(node, contentStartLine = lineIdx, lastLineIndex = lineIdx).also {
            it.starterTag = this::class.simpleName
        }
    }

    companion object {
        private val PAGEBREAK_REGEX = Regex("^\\*{3}pagebreak\\*{3}$", RegexOption.IGNORE_CASE)
    }
}
