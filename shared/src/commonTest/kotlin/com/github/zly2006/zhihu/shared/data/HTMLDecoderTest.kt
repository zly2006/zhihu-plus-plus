package com.github.zly2006.zhihu.shared.data

import kotlinx.serialization.Serializable
import kotlin.test.Test
import kotlin.test.assertEquals

class HTMLDecoderTest {
    @Test
    fun decodesHtmlTextAndRemovesInvisibleContent() {
        val item = ZhihuJson.json.decodeFromString<HtmlDecodedItem>(
            """
            {
              "text": "<p>Hello <span class=\"invisible\">hidden</span><b>world</b></p>"
            }
            """.trimIndent(),
        )

        assertEquals("Hello world", item.text)
    }

    @Serializable
    private data class HtmlDecodedItem(
        @Serializable(HTMLDecoder::class)
        val text: String,
    )
}
