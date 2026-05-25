package com.github.zly2006.zhihu.util

import androidx.compose.ui.graphics.Color
import kotlin.test.Test
import kotlin.test.assertEquals

class HtmlTextTest {
    @Test
    fun parseHtmlTextKeepsEmphasisSpan() {
        val text = parseHtmlText("普通<em>高亮</em>文本", Color.Red)

        assertEquals("普通高亮文本", text.text)
        assertEquals(1, text.spanStyles.size)
        val span = text.spanStyles.single()
        assertEquals(2, span.start)
        assertEquals(4, span.end)
        assertEquals(Color.Red, span.item.color)
    }
}
