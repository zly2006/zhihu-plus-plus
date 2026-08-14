package com.hrm.markdown.parser.incremental

import com.hrm.markdown.parser.LineRange
import com.hrm.markdown.parser.ast.Node
import com.hrm.markdown.parser.core.SourceText

/**
 * 脏区域追踪器。
 *
 * 根据编辑操作计算需要重新解析的行范围（脏区域）。
 *
 * 核心思路：
 * - 将编辑操作映射到行范围
 * - 向前扩展到块边界（空行或块级结构起始标记）
 * - 向后扩展到块边界
 * - 合并重叠的脏区域
 */
class DirtyRegionTracker {

    /**
     * 计算单次编辑操作影响的脏行范围。
     *
     * @param edit 编辑操作
     * @param oldSource 编辑前的源文本
     * @param newSource 编辑后的源文本
     * @param oldBlocks 旧的顶层块列表
     * @return 需要重新解析的行范围（在新源文本中的坐标）
     */
    fun computeDirtyRange(
        edit: EditOperation,
        oldSource: SourceText,
        newSource: SourceText,
        oldBlocks: List<Node>
    ): LineRange {
        val (editStartLine, editEndLineOld, editEndLineNew) = when (edit) {
            is EditOperation.Insert -> {
                val startLine = oldSource.lineAtOffset(edit.offset)
                val insertedLines = edit.text.count { it == '\n' }
                Triple(startLine, startLine, startLine + insertedLines + 1)
            }
            is EditOperation.Delete -> {
                val startLine = oldSource.lineAtOffset(edit.offset)
                val endLine = oldSource.lineAtOffset((edit.offset + edit.length - 1).coerceAtLeast(edit.offset))
                Triple(startLine, endLine + 1, startLine + 1)
            }
            is EditOperation.Replace -> {
                val startLine = oldSource.lineAtOffset(edit.offset)
                val oldEndLine = oldSource.lineAtOffset((edit.offset + edit.length - 1).coerceAtLeast(edit.offset))
                val insertedLines = edit.newText.count { it == '\n' }
                Triple(startLine, oldEndLine + 1, startLine + insertedLines + 1)
            }
            is EditOperation.Append -> {
                val startLine = if (oldSource.lineCount > 0) oldSource.lineCount - 1 else 0
                Triple(startLine, oldSource.lineCount, newSource.lineCount)
            }
        }

        // 向前扩展到块边界
        val expandedStart = expandToBlockBoundaryBackward(editStartLine, newSource)

        // 向后扩展到块边界
        val expandedEnd = expandToBlockBoundaryForward(editEndLineNew, newSource)

        return LineRange(expandedStart, expandedEnd)
    }

    /**
     * 对于 append-only 场景的优化版本。
     * 找到安全的重解析起点。
     */
    fun computeAppendDirtyRange(
        stableEndLine: Int,
        newSource: SourceText
    ): LineRange {
        if (stableEndLine <= 0) return LineRange(0, newSource.lineCount)

        val proposedStart = stableEndLine.coerceAtMost(newSource.lineCount)

        // 检查上次稳定行之前是否有明确的块分隔
        if (proposedStart > 0) {
            val prevLine = proposedStart - 1
            if (prevLine < newSource.lineCount) {
                val prevContent = newSource.lineContent(prevLine)
                if (prevContent.isBlank()) {
                    return LineRange(proposedStart, newSource.lineCount)
                }
            }
        }

        // 向前回退到块边界
        val start = expandToBlockBoundaryBackward(proposedStart, newSource)
        return LineRange(start, newSource.lineCount)
    }

    /**
     * 向前（行号减小方向）扩展到块边界。
     */
    private fun expandToBlockBoundaryBackward(line: Int, source: SourceText): Int {
        if (line <= 0) return 0

        var l = line
        while (l > 0) {
            val prevContent = source.lineContent(l - 1)
            if (prevContent.isBlank()) return l

            val trimmed = prevContent.trimStart()
            // 围栏代码块/数学块/前置元数据的开始标记
            if (trimmed.startsWith("```") || trimmed.startsWith("~~~")) return l - 1
            if (trimmed.startsWith("$$")) return l - 1
            if ((trimmed == "---" || trimmed == "+++") && l - 1 == 0) return 0

            l--
        }
        return 0
    }

    /**
     * 向后（行号增大方向）扩展到块边界。
     */
    private fun expandToBlockBoundaryForward(line: Int, source: SourceText): Int {
        if (line >= source.lineCount) return source.lineCount

        var l = line
        while (l < source.lineCount) {
            val content = source.lineContent(l)
            if (content.isBlank()) return l + 1
            l++
        }
        return source.lineCount
    }
}
