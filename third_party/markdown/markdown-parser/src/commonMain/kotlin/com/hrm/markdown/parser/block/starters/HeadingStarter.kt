package com.hrm.markdown.parser.block.starters

import com.hrm.markdown.parser.LineRange
import com.hrm.markdown.parser.ast.Heading
import com.hrm.markdown.parser.block.OpenBlock
import com.hrm.markdown.parser.core.LineCursor

/**
 * ATX 标题开启器：`# heading` 到 `###### heading`。
 */
internal class HeadingStarter : BlockStarter {
    override val priority: Int = 110
    override val canInterruptParagraph: Boolean = true

    override fun tryStart(cursor: LineCursor, lineIdx: Int, tip: OpenBlock): OpenBlock? {
        val indent = cursor.advanceSpaces(3)
        if (cursor.isAtEnd || cursor.peek() != '#') return null

        var level = 0
        while (!cursor.isAtEnd && cursor.peek() == '#') {
            cursor.advance()
            level++
        }
        if (level > 6) return null
        if (!cursor.isAtEnd && cursor.peek() != ' ' && cursor.peek() != '\t') {
            if (!cursor.isAtEnd) return null
        }

        if (!cursor.isAtEnd && (cursor.peek() == ' ' || cursor.peek() == '\t')) {
            cursor.advance()
        }

        var content = cursor.rest().trimEnd()
        val customId = extractCustomId(content)
        if (customId != null) {
            content = content.replace(CUSTOM_ID_STRIP_REGEX, "").trimEnd()
        }
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
        ob.starterTag = this::class.simpleName
        return ob
    }

    companion object {
        internal val CUSTOM_ID_REGEX = Regex("\\{#([^\\}]+)\\}\\s*$")
        internal val CUSTOM_ID_STRIP_REGEX = Regex("\\s*\\{#[^\\}]+\\}\\s*$")

        fun extractCustomId(content: String): String? {
            val match = CUSTOM_ID_REGEX.find(content) ?: return null
            return match.groupValues[1]
        }
    }
}
