package com.hrm.markdown.parser.block.starters

import com.hrm.markdown.parser.LineRange
import com.hrm.markdown.parser.ast.SetextHeading
import com.hrm.markdown.parser.block.OpenBlock
import com.hrm.markdown.parser.core.LineCursor

/**
 * Setext 标题开启器：`heading\n===` 或 `heading\n---`。
 * 必须在段落上下文中（tip 有 paragraphContent）。
 */
internal class SetextHeadingStarter : BlockStarter {
    override val priority: Int = 100
    override val canInterruptParagraph: Boolean = true

    override fun tryStart(cursor: LineCursor, lineIdx: Int, tip: OpenBlock): OpenBlock? {
        if (tip.paragraphContent == null) return null

        val indent = cursor.advanceSpaces(3)
        if (cursor.isAtEnd) return null

        val c = cursor.peek()
        if (c != '=' && c != '-') return null

        val rest = cursor.rest()
        val stripped = rest.trimEnd()
        if (!stripped.all { it == c }) return null
        if (stripped.isEmpty()) return null

        val level = if (c == '=') 1 else 2
        val heading = SetextHeading(level)
        heading.lineRange = LineRange(tip.contentStartLine, lineIdx + 1)

        val ob = OpenBlock(heading, contentStartLine = tip.contentStartLine, lastLineIndex = lineIdx)
        ob.contentLines.addAll(tip.paragraphContent.toString().split('\n'))
        ob.starterTag = this::class.simpleName
        return ob
    }
}
