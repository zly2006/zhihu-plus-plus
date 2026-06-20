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
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Unit test for HTML text parsing functionality
 * Tests parsing of <em> tags in title and content
 */
class HtmlTextDetailedTest {
    /**
     * Test basic <em> tag parsing
     */
    @Test
    fun testEmTagParsing() {
        val html = "This is a <em>test</em> string"
        val emphasisColor = Color.Blue
        val annotatedString = parseHtmlText(html, emphasisColor)

        // Verify the text content is correct
        assertEquals("This is a test string", annotatedString.text)

        // Verify there's at least one style span
        assertTrue(annotatedString.spanStyles.isNotEmpty(), "Should have at least one span style")

        // Verify the emphasized text has the correct color
        val emSpan = annotatedString.spanStyles.find { span ->
            span.start == 10 && span.end == 14 // "test" position
        }
        assertNotNull(emSpan, "Should find emphasized span")
        assertEquals(emphasisColor, emSpan.item.color, "Emphasized text should have the emphasis color")
    }

    /**
     * Test multiple <em> tags
     */
    @Test
    fun testMultipleEmTags() {
        val html = "Search <em>keyword1</em> and <em>keyword2</em>"
        val emphasisColor = Color.Red
        val annotatedString = parseHtmlText(html, emphasisColor)

        // Verify the text content is correct
        assertEquals("Search keyword1 and keyword2", annotatedString.text)

        // Verify there are two emphasized spans
        assertEquals(2, annotatedString.spanStyles.size, "Should have two span styles")
    }

    /**
     * Test plain text without HTML tags
     */
    @Test
    fun testPlainText() {
        val html = "This is plain text"
        val emphasisColor = Color.Green
        val annotatedString = parseHtmlText(html, emphasisColor)

        // Verify the text content is correct
        assertEquals("This is plain text", annotatedString.text)

        // Verify there are no styles
        assertEquals(0, annotatedString.spanStyles.size, "Should have no span styles")
    }

    /**
     * Test nested and complex HTML
     */
    @Test
    fun testNestedHtml() {
        val html = "Test <em>emphasis</em> text"
        val emphasisColor = Color.Yellow
        val annotatedString = parseHtmlText(html, emphasisColor)

        // Verify the text is parsed correctly
        assertEquals("Test emphasis text", annotatedString.text)
        assertTrue(annotatedString.spanStyles.isNotEmpty(), "Should have emphasized text")
    }

    /**
     * Test real Zhihu search result format
     */
    @Test
    fun testZhihuSearchFormat() {
        // Real format from Zhihu search API
        val html = "为什么互联网给我一种想<em>搜</em>的东西什么都搜不到，屁用没有的信息一大堆的无力感？"
        val emphasisColor = Color.Cyan
        val annotatedString = parseHtmlText(html, emphasisColor)

        // Verify the text content
        assertTrue(annotatedString.text.contains("搜"), "Should contain the search keyword")

        // Verify emphasis is applied
        assertTrue(annotatedString.spanStyles.isNotEmpty(), "Should have at least one emphasized span")
    }

    /**
     * Test empty string
     */
    @Test
    fun testEmptyString() {
        val html = ""
        val emphasisColor = Color.Magenta
        val annotatedString = parseHtmlText(html, emphasisColor)

        // Verify empty result
        assertEquals("", annotatedString.text, "Should have empty text")
        assertEquals(0, annotatedString.spanStyles.size, "Should have no styles")
    }

    /**
     * Test HTML entities and special characters
     */
    @Test
    fun testHtmlEntities() {
        val html = "Test &lt;em&gt;<em>keyword</em>&lt;/em&gt;"
        val emphasisColor = Color.Black
        val annotatedString = parseHtmlText(html, emphasisColor)

        // Jsoup automatically decodes HTML entities
        assertTrue(annotatedString.text.contains("<em>"), "Should decode HTML entities")
        assertTrue(annotatedString.spanStyles.isNotEmpty(), "Should have emphasized text")
    }
}
