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
 * GitHub Light 亮色主题，基于 GitHub 代码高亮配色方案。
 */
object GithubLightTheme : CodeTheme {
    override val background: Color = Color(0xFFFFFFFF)
    override val isDark: Boolean = false

    override fun colorFor(type: TokenType): Color = when (type) {
        TokenType.KEYWORD -> Color(0xFFCF222E) // 红色
        TokenType.STRING -> Color(0xFF0A3069) // 深蓝
        TokenType.NUMBER -> Color(0xFF0550AE) // 蓝色
        TokenType.COMMENT -> Color(0xFF6E7781) // 灰色
        TokenType.OPERATOR -> Color(0xFF24292F) // 深色
        TokenType.PUNCTUATION -> Color(0xFF24292F) // 深色
        TokenType.IDENTIFIER -> Color(0xFF24292F) // 深色
        TokenType.TYPE -> Color(0xFF953800) // 棕色
        TokenType.FUNCTION -> Color(0xFF8250DF) // 紫色
        TokenType.VARIABLE -> Color(0xFF24292F) // 深色
        TokenType.CONSTANT -> Color(0xFF0550AE) // 蓝色
        TokenType.ANNOTATION -> Color(0xFF953800) // 棕色
        TokenType.DECORATOR -> Color(0xFF953800) // 棕色
        TokenType.BUILTIN -> Color(0xFF0550AE) // 蓝色
        TokenType.PLAIN -> Color(0xFF24292F) // 深色
    }
}
