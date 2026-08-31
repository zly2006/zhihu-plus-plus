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
    fun parsesZhihuEmphasisWithoutDamagingLiteralTextOrEntities() {
        val highlighted = parseEmphasizedHtmlText(
            "为什么互联网给我一种想<em>搜</em>的东西什么都搜不到，屁用没有的信息一大堆的无力感？",
            Color.Red,
        )

        val highlightedSpan = highlighted.spanStyles.single()
        assertEquals("为什么互联网给我一种想搜的东西什么都搜不到，屁用没有的信息一大堆的无力感？", highlighted.text)
        assertEquals(1, highlighted.spanStyles.size)
        assertEquals(Color.Red, highlightedSpan.item.color)
        assertEquals(
            "搜",
            highlighted.text.substring(highlightedSpan.start, highlightedSpan.end),
        )

        val multiple = parseEmphasizedHtmlText("Search <em>keyword1</em> and <em>keyword2</em>", Color.Red)
        assertEquals("Search keyword1 and keyword2", multiple.text)
        assertEquals(2, multiple.spanStyles.size)
        assertEquals(listOf(Color.Red, Color.Red), multiple.spanStyles.map { it.item.color })

        listOf(
            "为什么Deepseek在输入<think 后会匹配到疑似其他对话?",
            "vector<bool>",
        ).forEach { source ->
            val text = parseEmphasizedHtmlText(source, Color.Red)

            assertEquals(source, text.text)
            assertEquals(0, text.spanStyles.size)
        }

        assertEquals("% 中 😀", parseEmphasizedHtmlText("&#37; &#x4E2D; &#X1F600;", Color.Red).text)
        assertEquals(
            "&#; &#x; &#x110000; &#xD800;",
            parseEmphasizedHtmlText("&#; &#x; &#x110000; &#xD800;", Color.Red).text,
        )
        assertEquals("", parseEmphasizedHtmlText("", Color.Red).text)
        assertEquals(
            "Test <em>keyword</em>",
            parseEmphasizedHtmlText("Test &lt;em&gt;<em>keyword</em>&lt;/em&gt;", Color.Red).text,
        )
    }
}
