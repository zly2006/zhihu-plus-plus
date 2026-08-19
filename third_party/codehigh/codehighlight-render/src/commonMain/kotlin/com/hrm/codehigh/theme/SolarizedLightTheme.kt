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
 * Solarized Light 亮色主题，基于 Solarized Light 配色方案。
 */
object SolarizedLightTheme : CodeTheme {
    override val background: Color = Color(0xFFFDF6E3)
    override val isDark: Boolean = false

    override fun colorFor(type: TokenType): Color = when (type) {
        TokenType.KEYWORD -> Color(0xFF859900) // 绿色
        TokenType.STRING -> Color(0xFF2AA198) // 青色
        TokenType.NUMBER -> Color(0xFFD33682) // 洋红
        TokenType.COMMENT -> Color(0xFF93A1A1) // 浅灰
        TokenType.OPERATOR -> Color(0xFF657B83) // 深灰
        TokenType.PUNCTUATION -> Color(0xFF657B83) // 深灰
        TokenType.IDENTIFIER -> Color(0xFF657B83) // 深灰
        TokenType.TYPE -> Color(0xFFCB4B16) // 橙红
        TokenType.FUNCTION -> Color(0xFF268BD2) // 蓝色
        TokenType.VARIABLE -> Color(0xFF657B83) // 深灰
        TokenType.CONSTANT -> Color(0xFFD33682) // 洋红
        TokenType.ANNOTATION -> Color(0xFFCB4B16) // 橙红
        TokenType.DECORATOR -> Color(0xFFCB4B16) // 橙红
        TokenType.BUILTIN -> Color(0xFF268BD2) // 蓝色
        TokenType.PLAIN -> Color(0xFF657B83) // 深灰
    }
}
