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

package com.github.zly2006.zhihu.markdown

import com.hrm.markdown.parser.ast.ContainerNode
import com.hrm.markdown.parser.ast.Figure
import com.hrm.markdown.parser.ast.InlineMath
import com.hrm.markdown.parser.ast.ListBlock
import com.hrm.markdown.parser.ast.ListItem
import com.hrm.markdown.parser.ast.MathBlock
import com.hrm.markdown.parser.ast.NativeBlock
import com.hrm.markdown.parser.ast.Node
import com.hrm.markdown.parser.ast.Paragraph
import com.hrm.markdown.parser.ast.StrongEmphasis
import com.hrm.markdown.parser.ast.Text
import org.jsoup.Jsoup
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

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
    fun video_box_anchor_should_convert_to_link_when_native_blocks_disabled() {
        val document = htmlToMdAst(
            """
            <a class="video-box" href="https://link.zhihu.com/?target=https%3A//www.zhihu.com/video/2029631316597973958" data-lens-id="2029631316597973958">
              <img src="https://example.com/cover.jpg" />
            </a>
            """.trimIndent(),
            noNativeBlock = true,
        )

        assertFalse(document.allNodes().any { it is NativeBlock })
        assertEquals("[视频](https://www.zhihu.com/video/2029631316597973958)", document.toMarkdown())
    }

    @Test
    fun highlighted_paragraph_should_skip_segmented_native_block_when_native_blocks_disabled() {
        val document = htmlToMdAst(
            """
            <p data-pid="seg-1"><span class="highlight-wrap other has-comments"
                data-highlight-id="abc"
                data-highlight-like-count="5"
                data-highlight-comment-count="1"
                data-highlight-my-comment-count="0"
                data-highlight-is-like="true"
                data-highlight-is-span="false"
                data-highlight-content-id="42"
                data-highlight-content-type="answer"
                data-highlight-pid="seg-1"
                data-highlight-start-offset="0"
                data-highlight-end-offset="7">第一句需要划线</span>，第二句保持原样。</p>
            """.trimIndent(),
            noNativeBlock = true,
        )

        assertFalse(document.allNodes().any { it is NativeBlock })
        assertEquals("第一句需要划线，第二句保持原样。", document.toMarkdown())
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
    fun paragraph_with_only_inline_equation_with_space_should_convert_to_math_block() {
        val document = htmlToMdAst(
            """
            <p>  <img eeimg="1" src="https://www.zhihu.com/equation?tex=E%3Dmc%5E2" alt="E=mc^2"/>  </p>
            """.trimIndent(),
        )

        assertEquals(1, document.children.size)
        assertTrue(document.children.single() is MathBlock)
        assertEquals("E=mc^2", (document.children.single() as MathBlock).literal)
        assertFalse(document.allNodes().any { it is InlineMath })
    }

    @Test
    fun paragraph_inline_strong_text_should_keep_space_between_english_words() {
        val document = htmlToMdAst(
            """
            <p>The <strong>quick</strong> <strong>brown</strong> fox jumps.</p>
            """.trimIndent(),
        )

        assertEquals("The quick brown fox jumps.", document.plainText())
        assertEquals(2, document.allNodes().count { it is StrongEmphasis })
    }

    @Test
    fun blockquote_inline_strong_text_should_keep_space_between_english_words() {
        val document = htmlToMdAst(
            """
            <blockquote data-pid="P3CclW4S">你看<b>过</b>某样东西，不代表你真的<b>看见</b>它了。<br>Just because you have <b>looked</b> at something doesn’t mean that you have <b>seen</b> it.</blockquote>
            """.trimIndent(),
        )

        assertEquals(
            "你看过某样东西，不代表你真的看见它了。Just because you have looked at something doesn’t mean that you have seen it.",
            document.plainText(),
        )
        assertEquals(4, document.allNodes().count { it is StrongEmphasis })
    }

    @Test
    fun sibling_ordered_lists_should_nest_under_the_preceding_list_item() {
        val document = htmlToMdAst(
            """
            <ol>
              <li>继续跳票。</li>
              <li>在较短时间内推出，但是</li>
              <ol>
                <li>分词器和灰测表现不一致</li>
                <ol>
                  <li>正式版性能约等于 Fable。</li>
                  <li>正式版性能远不及 Fable。</li>
                </ol>
                <li>分词器和灰测表现一致</li>
                <ol>
                  <li>正式版的性能接近 Fable。</li>
                  <li>正式版的表现远不及 Fable。</li>
                </ol>
              </ol>
            </ol>
            """.trimIndent(),
        )

        val outerList = document.children.single() as ListBlock
        val outerItems = outerList.children.filterIsInstance<ListItem>()
        val secondLevelList = outerItems[1].children.filterIsInstance<ListBlock>().single()
        val secondLevelItems = secondLevelList.children.filterIsInstance<ListItem>()

        assertEquals(2, outerItems.size)
        assertEquals(2, secondLevelItems.size)
        assertEquals(
            2,
            secondLevelItems[0]
                .children
                .filterIsInstance<ListBlock>()
                .single()
                .children.size,
        )
        assertEquals(
            2,
            secondLevelItems[1]
                .children
                .filterIsInstance<ListBlock>()
                .single()
                .children.size,
        )
    }

    @Test
    fun preview_image_urls_should_keep_document_order_and_drop_duplicates() {
        val document = htmlToMdAst(
            """
            <p>第一张</p>
            <figure><img src="https://pic1.zhimg.com/v2-a.jpg" /></figure>
            <p>第二张 <img src="https://pic1.zhimg.com/v2-b.jpg" /></p>
            <figure><img src="https://pic1.zhimg.com/v2-a.jpg" /></figure>
            <img src="data:image/png;base64,abc" />
            """.trimIndent(),
        )

        assertEquals(
            listOf(
                "https://pic1.zhimg.com/v2-a.jpg",
                "https://pic1.zhimg.com/v2-b.jpg",
            ),
            document.previewImageUrls(),
        )
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

    private fun Node.plainText(): String = when (this) {
        is Text -> literal
        is ContainerNode -> children.joinToString(separator = "") { it.plainText() }
        else -> ""
    }
}
