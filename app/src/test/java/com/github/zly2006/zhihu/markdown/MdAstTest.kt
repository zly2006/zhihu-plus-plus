/*
 * Zhihu++ - Free & Ad-Free Zhihu client for Android.
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

package com.github.zly2006.zhihu.markdown

import com.hrm.markdown.parser.ast.ContainerNode
import com.hrm.markdown.parser.ast.Figure
import com.hrm.markdown.parser.ast.InlineMath
import com.hrm.markdown.parser.ast.MathBlock
import com.hrm.markdown.parser.ast.NativeBlock
import com.hrm.markdown.parser.ast.Node
import com.hrm.markdown.parser.ast.Paragraph
import org.jsoup.Jsoup
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class MdAstTest {
    @Test
    fun video_box_anchor_should_convert_to_native_block() {
        val document = htmlToMdAst(
            """
            <a class="video-box" href="https://link.zhihu.com/?target=https%3A//www.zhihu.com/video/2029631316597973958" data-lens-id="2029631316597973958">
              <img src="https://example.com/cover.jpg" />
            </a>
            """.trimIndent(),
        )

        assertEquals(1, document.children.size)
        assertTrue(document.children.single() is NativeBlock)
    }

    @Test
    fun inline_equation_in_list_item_should_stay_in_single_paragraph() {
        val document = htmlToMdAst(
            """
            <ul>
              <li>直角边是静止能量 <img eeimg="1" src="https://www.zhihu.com/equation?tex=m_0+c%5E2" alt="m_0 c^2"/>。</li>
            </ul>
            """.trimIndent(),
        )
        val nodes = document.allNodes()

        assertEquals(1, nodes.count { it is Paragraph })
        assertEquals(1, nodes.count { it is InlineMath && it.literal == "m_0 c^2" })
        assertFalse(nodes.any { it is Figure && it.imageUrl.contains("/equation?tex=") })
    }

    @Test
    fun extracted_answer_content_should_keep_equation_as_math_not_figure() {
        val htmlFile = File(
            requireNotNull(javaClass.classLoader?.getResource("zhihu_answer_2035661632110585441_content.html")).toURI(),
        )
        val parsedHtml = Jsoup.parse(htmlFile, Charsets.UTF_8.name())
        val contentElement = parsedHtml.selectFirst("article.RichContent-inner")!!
        val blockEquationCount = contentElement.select("""img[eeimg="2"][src*="/equation?tex="]""").size
        val content = contentElement.html()
        val nodes = htmlToMdAst(content).allNodes()

        assertTrue(contentElement.selectFirst("""p[data-pid="_WhLYpom"]""") != null)
        assertEquals(11, blockEquationCount)
        assertTrue(nodes.any { it is InlineMath && it.literal == "1/2" })
        assertEquals(blockEquationCount, nodes.count { it is MathBlock })
        assertFalse(nodes.any { it is Figure && it.imageUrl.contains("/equation?tex=") })
    }

    private fun Node.allNodes(): List<Node> =
        listOf(this) + if (this is ContainerNode) children.flatMap { it.allNodes() } else emptyList()
}
