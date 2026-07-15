/*
 * Zhihu++ - Free & Ad-Free Zhihu client for all platforms.
 * Copyright (C) 2024-2026, zly2006 <i@zly2006.me>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation (version 3 only).
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.github.zly2006.zhihu.util

import androidx.compose.ui.graphics.Color
import kotlin.test.Test
import kotlin.test.assertEquals

class HtmlTextTest {
    @Test
    fun parseEmphasizedHtmlTextKeepsEmphasisSpan() {
        val text = parseEmphasizedHtmlText("普通<em>高亮</em>文本", Color.Red)

        assertEquals("普通高亮文本", text.text)
        assertEquals(1, text.spanStyles.size)
        val span = text.spanStyles.single()
        assertEquals(2, span.start)
        assertEquals(4, span.end)
        assertEquals(Color.Red, span.item.color)
    }

    @Test
    fun parseEmphasizedHtmlTextKeepsAngleBracketText() {
        listOf(
            "为什么Deepseek在输入<think 后会匹配到疑似其他对话?",
            "vector<bool>",
        ).forEach { source ->
            val text = parseEmphasizedHtmlText(source, Color.Red)

            assertEquals(source, text.text)
            assertEquals(0, text.spanStyles.size)
        }
    }

    @Test
    fun parseEmphasizedHtmlTextDecodesNumericCharacterReferences() {
        val text = parseEmphasizedHtmlText("&#37; &#x4E2D; &#X1F600;", Color.Red)

        assertEquals("% 中 😀", text.text)
    }

    @Test
    fun parseEmphasizedHtmlTextKeepsInvalidNumericCharacterReferences() {
        val text = parseEmphasizedHtmlText("&#; &#x; &#x110000; &#xD800;", Color.Red)

        assertEquals("&#; &#x; &#x110000; &#xD800;", text.text)
    }
}
