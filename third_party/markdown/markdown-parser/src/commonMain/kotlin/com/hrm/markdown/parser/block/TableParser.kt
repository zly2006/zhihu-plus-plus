package com.hrm.markdown.parser.block

import com.hrm.markdown.parser.ast.Table

/**
 * 表格解析辅助：解析表格分隔行和数据行。
 *
 * 从 BlockParser 中提取，被 BlockStarters 和 BlockParser 共同使用。
 */
internal object TableParser {

    fun parseTableDelimiterRow(line: String): List<Table.Alignment>? {
        val trimmed = line.trim().let { if (it.startsWith('|')) it.drop(1) else it }
            .let { if (it.endsWith('|')) it.dropLast(1) else it }
        if (trimmed.isBlank()) return null

        val cells = trimmed.split('|')
        if (cells.isEmpty()) return null

        val alignments = mutableListOf<Table.Alignment>()
        for (cell in cells) {
            val c = cell.trim()
            if (!c.matches(TABLE_DELIM_CELL_REGEX)) return null
            val left = c.startsWith(':')
            val right = c.endsWith(':')
            alignments.add(
                when {
                    left && right -> Table.Alignment.CENTER
                    right -> Table.Alignment.RIGHT
                    left -> Table.Alignment.LEFT
                    else -> Table.Alignment.NONE
                }
            )
        }
        return alignments
    }

    fun parseTableRow(line: String): List<String> {
        val trimmed = line.trim()
        val stripped = if (trimmed.startsWith('|')) trimmed.drop(1) else trimmed
        val end = if (stripped.endsWith('|')) stripped.dropLast(1) else stripped

        val cells = mutableListOf<String>()
        val current = StringBuilder()
        var escaped = false
        for (c in end) {
            when {
                escaped -> {
                    current.append(c)
                    escaped = false
                }
                c == '\\' -> {
                    current.append(c)
                    escaped = true
                }
                c == '|' -> {
                    cells.add(current.toString().trim())
                    current.clear()
                }
                else -> current.append(c)
            }
        }
        cells.add(current.toString().trim())
        return cells
    }

    /** 表格分隔行单元格校验 */
    private val TABLE_DELIM_CELL_REGEX = Regex(":?-+:?")
}
