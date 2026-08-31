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

package com.hrm.codehigh.renderer

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hrm.codehigh.theme.CodeTheme

@Composable
internal fun CodeBlockHeaderLabels(
    title: String,
    language: String,
    theme: CodeTheme,
    modifier: Modifier = Modifier
) {
    if (title.isBlank() && language.isBlank()) return

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        CodeBlockTitle(title = title, theme = theme)
        LanguageLabel(language = language, theme = theme)
    }
}

@Composable
internal fun CodeBlockTitle(
    title: String,
    theme: CodeTheme,
    modifier: Modifier = Modifier
) {
    if (title.isBlank()) return

    BasicText(
        text = title,
        style = TextStyle(
            color = theme.colorFor(com.hrm.codehigh.ast.TokenType.PLAIN).copy(alpha = 0.92f),
            fontSize = 12.sp,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Medium
        ),
        modifier = modifier.padding(horizontal = 8.dp, vertical = 2.dp)
    )
}

/**
 * 语言标签组件，显示代码语言名称。
 * 标记为 internal，仅供 CodeBlock 内部使用。
 *
 * @param language 语言标识符
 * @param theme 代码主题
 */
@Composable
internal fun LanguageLabel(
    language: String,
    theme: CodeTheme,
    modifier: Modifier = Modifier
) {
    if (language.isBlank()) return

    BasicText(
        text = language.lowercase(),
        style = TextStyle(
            color = theme.colorFor(com.hrm.codehigh.ast.TokenType.COMMENT).copy(alpha = 0.7f),
            fontSize = 11.sp,
            fontFamily = FontFamily.Monospace
        ),
        modifier = modifier.padding(horizontal = 8.dp, vertical = 4.dp)
    )
}
