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
 * 代码主题接口，定义代码高亮的颜色方案。
 * 对外公开，支持自定义主题实现。
 */
interface CodeTheme {
    /** 按 Token 类型返回对应颜色 */
    fun colorFor(type: TokenType): Color

    /** 代码块背景色 */
    val background: Color

    /** 是否为暗色主题 */
    val isDark: Boolean

    val highlightedLineBackground: Color
        get() = if (isDark) Color(0x22FFFFFF) else Color(0x12000000)

    val diffAddedLineBackground: Color
        get() = if (isDark) Color(0x3340A15F) else Color(0x2234D058)

    val diffRemovedLineBackground: Color
        get() = if (isDark) Color(0x33A84C57) else Color(0x22FF6B6B)

    val diffMetaLineBackground: Color
        get() = if (isDark) Color(0x224B5563) else Color(0x140A66A1)
}

/**
 * 安全获取颜色，缺失类型时回退到 PLAIN 颜色。
 * 标记为 internal，仅供模块内部使用。
 */
internal fun CodeTheme.safeColorFor(type: TokenType): Color {
    return try {
        colorFor(type)
    } catch (_: Exception) {
        colorFor(TokenType.PLAIN)
    }
}
