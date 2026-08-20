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

package com.hrm.latex.parser.model

/**
 * 源代码位置范围，表示 LaTeX 输入字符串中的 [start, end) 半开区间
 *
 * @param start 起始字符偏移（包含）
 * @param end 结束字符偏移（不包含）
 */
data class SourceRange(val start: Int, val end: Int) {

    /** 范围长度 */
    val length: Int get() = end - start

    /** 是否为空范围 */
    val isEmpty: Boolean get() = start >= end

    /** 判断偏移量是否在范围内 */
    fun contains(offset: Int): Boolean = offset in start until end

    /** 合并两个范围，返回覆盖两者的最小范围 */
    fun merge(other: SourceRange): SourceRange =
        SourceRange(minOf(start, other.start), maxOf(end, other.end))

    companion object {
        /** 空范围，用于无源码位置的节点 */
        val EMPTY = SourceRange(0, 0)
    }
}
