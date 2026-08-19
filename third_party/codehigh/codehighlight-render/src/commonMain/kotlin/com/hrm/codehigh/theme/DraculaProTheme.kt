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

package com.hrm.codehigh.theme

import androidx.compose.ui.graphics.Color
import com.hrm.codehigh.ast.TokenType

/**
 * Dracula Pro 暗色主题，基于 Dracula Pro 配色方案。
 */
object DraculaProTheme : CodeTheme {
    override val background: Color = Color(0xFF22212C)
    override val isDark: Boolean = true

    override fun colorFor(type: TokenType): Color = when (type) {
        TokenType.KEYWORD -> Color(0xFFFF79C6) // 粉色
        TokenType.STRING -> Color(0xFFF1FA8C) // 黄色
        TokenType.NUMBER -> Color(0xFFBD93F9) // 紫色
        TokenType.COMMENT -> Color(0xFF6272A4) // 蓝灰
        TokenType.OPERATOR -> Color(0xFFFF79C6) // 粉色
        TokenType.PUNCTUATION -> Color(0xFFF8F8F2) // 白色
        TokenType.IDENTIFIER -> Color(0xFFF8F8F2) // 白色
        TokenType.TYPE -> Color(0xFF8BE9FD) // 青色
        TokenType.FUNCTION -> Color(0xFF50FA7B) // 绿色
        TokenType.VARIABLE -> Color(0xFFFFB86C) // 橙色
        TokenType.CONSTANT -> Color(0xFFBD93F9) // 紫色
        TokenType.ANNOTATION -> Color(0xFF8BE9FD) // 青色
        TokenType.DECORATOR -> Color(0xFF8BE9FD) // 青色
        TokenType.BUILTIN -> Color(0xFF8BE9FD) // 青色
        TokenType.PLAIN -> Color(0xFFF8F8F2) // 白色
    }
}
