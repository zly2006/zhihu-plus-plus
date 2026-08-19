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


package com.hrm.latex.renderer.utils

/**
 * 数学字体 Unicode 转换工具
 *
 * 在无法直接加载 .ttf 字体文件时，通过映射 Unicode 数学字母块
 * 来实现 \mathbb, \mathcal, \mathfrak 等效果。
 */
object MathFontUtils {

    /**
     * 将 codePoint 转换为 UTF-16 字符串，支持非 BMP 字符 (U+10000 及以上)
     */
    private fun codePointToString(codePoint: Int): String {
        if (codePoint <= 0xFFFF) return codePoint.toChar().toString()
        val high = ((codePoint - 0x10000) shr 10) + 0xD800
        val low = ((codePoint - 0x10000) and 0x3FF) + 0xDC00
        return "${high.toChar()}${low.toChar()}"
    }

    /**
     * 转换为双线体 (Blackboard Bold) - \mathbb
     * 映射范围: A-Z (U+1D538), a-z (U+1D552), 0-9 (U+1D7D8)
     */
    fun toBlackboardBold(text: String): String {
        return text.map { char ->
            when (char) {
                in 'A'..'Z' -> {
                    // 特殊情况：C, H, N, P, Q, R, Z 在 Unicode 中不在连续块内
                    when (char) {
                        'C' -> "ℂ"
                        'H' -> "ℍ"
                        'N' -> "ℕ"
                        'P' -> "ℙ"
                        'Q' -> "ℚ"
                        'R' -> "ℝ"
                        'Z' -> "ℤ"
                        else -> codePointToString(0x1D538 + (char.code - 'A'.code))
                    }
                }
                in 'a'..'z' -> codePointToString(0x1D552 + (char.code - 'a'.code))
                in '0'..'9' -> codePointToString(0x1D7D8 + (char.code - '0'.code))
                else -> char.toString()
            }
        }.joinToString("")
    }

    /**
     * 转换为花体 (Calligraphic) - \mathcal
     * 映射范围: A-Z (U+1D49C)
     */
    fun toCalligraphic(text: String): String {
        return text.map { char ->
            when (char) {
                in 'A'..'Z' -> {
                    // 特殊情况：B, E, F, H, I, L, M, R 在 Unicode 中不在连续块内
                    when (char) {
                        'B' -> "ℬ"
                        'E' -> "ℰ"
                        'F' -> "ℱ"
                        'H' -> "ℋ"
                        'I' -> "ℐ"
                        'L' -> "ℒ"
                        'M' -> "ℳ"
                        'R' -> "ℛ"
                        else -> codePointToString(0x1D49C + (char.code - 'A'.code))
                    }
                }
                else -> char.toString()
            }
        }.joinToString("")
    }

}
