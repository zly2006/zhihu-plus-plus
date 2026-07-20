package com.hrm.markdown.parser.block.starters

import com.hrm.markdown.parser.LineRange
import com.hrm.markdown.parser.ast.ListItem
import com.hrm.markdown.parser.block.ListItemMeta
import com.hrm.markdown.parser.block.OpenBlock
import com.hrm.markdown.parser.core.LineCursor

/**
 * 列表项开启器。
 * 支持无序列表（`-`, `*`, `+`）和有序列表（`1.`, `1)`）。
 * 包含任务列表检测 (`[ ]`, `[x]`)。
 */
internal class ListItemStarter : BlockStarter {
    override val priority: Int = 500
    override val canInterruptParagraph: Boolean = true

    override fun tryStart(cursor: LineCursor, lineIdx: Int, tip: OpenBlock): OpenBlock? {
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

        if (!cursor.isAtEnd && cursor.peek() != ' ' && cursor.peek() != '\t') return null

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
                    cursor.advanceSpaces(1)
                }
                contentIndent = indent + markerWidth + 1
            } else if (!cursor.isAtEnd && (cursor.peek() == ' ' || cursor.peek() == '\t')) {
                // more than 4 columns of whitespace after marker -> indented code content; use minimum indent
                cursor.restore(preSpaceSnap)
                cursor.advanceSpaces(1)
                contentIndent = indent + markerWidth + 1
            } else {
                contentIndent = indent + markerWidth + if (postMarker == 0) 1 else postMarker
            }
        }

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
                                if (!cursor.isAtEnd) cursor.advance()
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

        val ob = OpenBlock(listItem, contentStartLine = lineIdx, lastLineIndex = lineIdx)
        ob.listItemMeta = ListItemMeta(ordered, bulletChar, startNumber, delimiter)
        ob.starterTag = this::class.simpleName
        return ob
    }
}
