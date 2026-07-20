package com.hrm.markdown.parser.core

import com.hrm.markdown.parser.LineRange

/**
 * 高性能源文本容器。
 * 对输入进行预处理：规范化行尾符、替换 NUL 字符、
 * 并构建行偏移索引以实现 O(1) 的行查找。
 *
 * Tree-sitter 风格支持：提供 [contentHash] 方法，
 * 用于计算指定行范围的内容哈希，支持增量节点复用。
 */
class SourceText private constructor(
    val content: String,
    private val lineOffsets: IntArray
) {
    val length: Int get() = content.length
    val lineCount: Int get() = lineOffsets.size

    /**
     * 获取指定行的起始偏移量（基于 0 的行索引）。
     */
    fun lineStart(line: Int): Int {
        require(line in 0 until lineCount) { "Line $line out of range [0, $lineCount)" }
        return lineOffsets[line]
    }

    /**
     * 获取指定行的结束偏移量（不包含，包括换行符）。
     */
    fun lineEnd(line: Int): Int {
        require(line in 0 until lineCount) { "Line $line out of range [0, $lineCount)" }
        return if (line + 1 < lineCount) lineOffsets[line + 1] else content.length
    }

    /**
     * 获取指定行的内容（不包含末尾换行符）。
     */
    fun lineContent(line: Int): String {
        val start = lineStart(line)
        var end = lineEnd(line)
        if (end > start && content[end - 1] == '\n') end--
        return content.substring(start, end)
    }

    /**
     * 获取范围 [startLine, endLine) 内的多行内容。
     */
    fun linesContent(startLine: Int, endLine: Int): List<String> {
        val result = ArrayList<String>(endLine - startLine)
        for (i in startLine until endLine) {
            result.add(lineContent(i))
        }
        return result
    }

    /**
     * 使用二分查找确定给定偏移量所在的行。
     */
    fun lineAtOffset(offset: Int): Int {
        var lo = 0
        var hi = lineOffsets.size - 1
        while (lo <= hi) {
            val mid = (lo + hi) ushr 1
            if (lineOffsets[mid] <= offset) {
                lo = mid + 1
            } else {
                hi = mid - 1
            }
        }
        return (lo - 1).coerceAtLeast(0)
    }

    /**
     * 获取偏移量在其所在行内的列号。
     */
    fun columnAtOffset(offset: Int): Int {
        val line = lineAtOffset(offset)
        return offset - lineOffsets[line]
    }

    operator fun get(index: Int): Char = content[index]

    fun substring(startOffset: Int, endOffset: Int): String =
        content.substring(startOffset, endOffset)

    /**
     * 计算 [range] 行范围内源文本的内容哈希。
     * 使用 FNV-1a 哈希算法，用于增量解析时快速判断内容是否变化。
     */
    fun contentHash(range: LineRange): Long {
        val startOff = lineStart(range.startLine.coerceIn(0, lineCount - 1))
        val endOff = lineEnd((range.endLine - 1).coerceIn(0, lineCount - 1))
        var hash = -3750763034362895579L // FNV offset basis
        for (i in startOff until endOff.coerceAtMost(content.length)) {
            hash = hash xor content[i].code.toLong()
            hash *= 1099511628211L // FNV prime
        }
        return hash
    }

    companion object {
        /**
         * 从原始输入创建 SourceText。
         * 规范化行尾符并替换 NUL 字符。
         */
        fun of(input: String): SourceText {
            val normalized = normalize(input)
            return SourceText(normalized, computeLineOffsets(normalized))
        }

        /**
         * 对当前源文本应用编辑操作，返回新的 SourceText。
         * 这是一个便捷方法，内部通过修改文本内容后重新创建 SourceText。
         */
        fun applyEdit(
            current: SourceText,
            offset: Int,
            deleteLength: Int,
            insertText: String
        ): SourceText = applyEditFast(current, offset, deleteLength, insertText)

        /**
         * 增量应用编辑：仅对插入文本做规范化，并在保留旧 lineOffsets 的基础上局部更新行偏移，
         * 避免每次编辑都对完整文本重扫一遍换行符。
         *
         * 适合 [com.hrm.markdown.parser.incremental.IncrementalEngine.applyEdit] 这类高频调用路径。
         */
        fun applyEditFast(
            current: SourceText,
            offset: Int,
            deleteLength: Int,
            insertText: String
        ): SourceText {
            val oldContent = current.content
            require(offset in 0..oldContent.length) { "offset $offset out of [0, ${oldContent.length}]" }
            require(deleteLength >= 0 && offset + deleteLength <= oldContent.length) {
                "delete range [$offset, ${offset + deleteLength}) out of [0, ${oldContent.length}]"
            }

            val normalizedInsert = if (insertText.isEmpty()) "" else normalize(insertText)
            val newContent = buildString(oldContent.length - deleteLength + normalizedInsert.length) {
                append(oldContent, 0, offset)
                append(normalizedInsert)
                append(oldContent, offset + deleteLength, oldContent.length)
            }

            val oldOffsets = current.lineOffsets
            // 保留行 0..firstAffected（这些行的起始 ≤ offset，新内容下仍然有效）。
            val firstAffectedLine = current.lineAtOffset(offset)
            val keepHead = firstAffectedLine + 1

            // 编辑区域结束所在的旧行；其后的行需要平移 delta 后保留。
            val deleteEnd = offset + deleteLength
            val lineAfterDelete = current.lineAtOffset(deleteEnd) + 1
            val tailCount = oldOffsets.size - lineAfterDelete

            // 统计新增换行符数量。
            var insertedNewlines = 0
            for (i in normalizedInsert.indices) {
                if (normalizedInsert[i] == '\n') insertedNewlines++
            }

            val newSize = keepHead + insertedNewlines + tailCount
            val newOffsets = IntArray(newSize)
            for (i in 0 until keepHead) newOffsets[i] = oldOffsets[i]
            var idx = keepHead
            for (i in normalizedInsert.indices) {
                if (normalizedInsert[i] == '\n') {
                    newOffsets[idx++] = offset + i + 1
                }
            }
            val delta = normalizedInsert.length - deleteLength
            for (i in 0 until tailCount) {
                newOffsets[idx++] = oldOffsets[lineAfterDelete + i] + delta
            }

            return SourceText(newContent, newOffsets)
        }

        internal fun normalize(input: String): String {
            // 行尾符：\r\n -> \n，\r -> \n；NUL（U+0000）-> U+FFFD。
            // 仅当输入需要规范化时才走 buildString 拷贝路径。
            var needs = false
            for (i in input.indices) {
                val c = input[i]
                if (c == '\r' || c == '\u0000') { needs = true; break }
            }
            if (!needs) return input
            return buildString(input.length) {
                var i = 0
                while (i < input.length) {
                    val c = input[i]
                    when {
                        c == '\r' -> {
                            append('\n')
                            if (i + 1 < input.length && input[i + 1] == '\n') i++
                        }
                        c == '\u0000' -> append('\uFFFD')
                        else -> append(c)
                    }
                    i++
                }
            }
        }

        private fun computeLineOffsets(normalized: String): IntArray {
            val estimatedLines = (normalized.length / 40).coerceAtLeast(16)
            var offsets = IntArray(estimatedLines)
            offsets[0] = 0
            var lineCount = 1
            for (i in normalized.indices) {
                if (normalized[i] == '\n') {
                    if (lineCount >= offsets.size) {
                        offsets = offsets.copyOf(offsets.size * 2)
                    }
                    offsets[lineCount++] = i + 1
                }
            }
            return if (lineCount == offsets.size) offsets else offsets.copyOf(lineCount)
        }
    }
}