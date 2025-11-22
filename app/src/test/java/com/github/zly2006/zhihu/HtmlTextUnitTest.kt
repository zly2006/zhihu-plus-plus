package com.github.zly2006.zhihu

import androidx.compose.ui.graphics.Color
import com.github.zly2006.zhihu.util.parseHtmlText
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit test for HTML text parsing functionality
 * Tests parsing of <em> tags in title and content
 */
class HtmlTextUnitTest {
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
        assertTrue("Should have at least one span style", annotatedString.spanStyles.isNotEmpty())

        // Verify the emphasized text has the correct color
        val emSpan = annotatedString.spanStyles.find { span ->
            span.start == 10 && span.end == 14 // "test" position
        }
        assertTrue("Should find emphasized span", emSpan != null)
        assertEquals("Emphasized text should have the emphasis color", emphasisColor, emSpan?.item?.color)
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
        assertEquals("Should have two span styles", 2, annotatedString.spanStyles.size)
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
        assertEquals("Should have no span styles", 0, annotatedString.spanStyles.size)
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
        assertTrue("Should have emphasized text", annotatedString.spanStyles.isNotEmpty())
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
        assertTrue("Should contain the search keyword", annotatedString.text.contains("搜"))

        // Verify emphasis is applied
        assertTrue("Should have at least one emphasized span", annotatedString.spanStyles.isNotEmpty())
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
        assertEquals("Should have empty text", "", annotatedString.text)
        assertEquals("Should have no styles", 0, annotatedString.spanStyles.size)
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
        assertTrue("Should decode HTML entities", annotatedString.text.contains("<em>"))
        assertTrue("Should have emphasized text", annotatedString.spanStyles.isNotEmpty())
    }
}
