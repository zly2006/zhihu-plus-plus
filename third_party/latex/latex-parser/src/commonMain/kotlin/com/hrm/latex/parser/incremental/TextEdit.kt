/*
 * Copyright (c) 2026 huarangmeng
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package com.hrm.latex.parser.incremental

/**
 * 描述一次文本编辑操作（参考 tree-sitter 的 TSInputEdit）
 *
 * 表示旧文本中 [startOffset, oldEndOffset) 区域被替换为新内容，
 * 替换后该区域变为 [startOffset, newEndOffset)。
 *
 * 示例：
 * - 在位置 5 插入 3 个字符：TextEdit(5, 5, 8)
 * - 删除位置 5~8 的字符：TextEdit(5, 8, 5)
 * - 替换位置 5~8 为 2 个字符：TextEdit(5, 8, 7)
 *
 * @param startOffset 编辑开始位置（在旧文本中的偏移）
 * @param oldEndOffset 编辑结束位置（在旧文本中的偏移）
 * @param newEndOffset 编辑结束位置（在新文本中的偏移）
 */
data class TextEdit(
    val startOffset: Int,
    val oldEndOffset: Int,
    val newEndOffset: Int
) {
    /** 旧文本中被删除/替换的长度 */
    val oldLength: Int get() = oldEndOffset - startOffset

    /** 新文本中插入/替换的长度 */
    val newLength: Int get() = newEndOffset - startOffset

    /** 偏移量变化（新长度 - 旧长度） */
    val delta: Int get() = newLength - oldLength

    /** 是否为纯插入操作 */
    val isInsertion: Boolean get() = oldLength == 0

    /** 是否为纯删除操作 */
    val isDeletion: Boolean get() = newLength == 0

    companion object {
        /**
         * 从"旧文本是新文本的前缀"（追加场景）计算 edit
         */
        fun fromAppend(oldLength: Int, newLength: Int): TextEdit {
            return TextEdit(oldLength, oldLength, newLength)
        }

        /**
         * 从两个字符串的差异自动计算 edit
         * 找到最长公共前缀和后缀，中间部分即为编辑区域
         */
        fun diff(oldText: String, newText: String): TextEdit {
            if (oldText == newText) return TextEdit(0, 0, 0)
            if (oldText.isEmpty()) return TextEdit(0, 0, newText.length)
            if (newText.isEmpty()) return TextEdit(0, oldText.length, 0)

            // 找公共前缀长度
            var prefixLen = 0
            val minLen = minOf(oldText.length, newText.length)
            while (prefixLen < minLen && oldText[prefixLen] == newText[prefixLen]) {
                prefixLen++
            }

            // 找公共后缀长度（不与前缀重叠）
            var suffixLen = 0
            val maxSuffix = minLen - prefixLen
            while (suffixLen < maxSuffix
                && oldText[oldText.length - 1 - suffixLen] == newText[newText.length - 1 - suffixLen]
            ) {
                suffixLen++
            }

            return TextEdit(
                startOffset = prefixLen,
                oldEndOffset = oldText.length - suffixLen,
                newEndOffset = newText.length - suffixLen
            )
        }
    }
}
