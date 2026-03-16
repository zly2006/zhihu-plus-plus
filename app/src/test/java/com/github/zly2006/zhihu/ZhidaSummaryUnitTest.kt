package com.github.zly2006.zhihu

import com.github.zly2006.zhihu.util.buildZhidaSummaryPayload
import com.github.zly2006.zhihu.util.encodeZhidaAttachmentValue
import com.github.zly2006.zhihu.util.parseSummaryChunkFromSseData
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ZhidaSummaryUnitTest {
    @Test
    fun testEncodeAttachmentValueForAnswer() {
        val value = encodeZhidaAttachmentValue(contentId = 2016563638144623487, contentType = "answer")
        assertEquals("MjAxNjU2MzYzODE0NDYyMzQ4N3w6OnxBTlNXRVI=", value)
    }

    @Test
    fun testEncodeAttachmentValueForArticle() {
        val value = encodeZhidaAttachmentValue(contentId = 721186888, contentType = "article")
        assertEquals("NzIxMTg2ODg4fDo6fEFSVElDTEU=", value)
    }

    @Test
    fun testBuildPayload() {
        val payload = buildZhidaSummaryPayload(
            contentId = 2016563638144623487,
            contentType = "answer",
            title = "测试标题",
        )
        val json = Json.parseToJsonElement(payload).jsonObject

        assertEquals("QT_CHAT", json["quiz_type"]!!.jsonPrimitive.content)
        assertEquals("text", json["message_source_type"]!!.jsonPrimitive.content)
        assertEquals("", json["session_id"]!!.jsonPrimitive.content)
        assertEquals("one_tap_summary", json["zhida_source"]!!.jsonPrimitive.content)
        assertEquals("2016563638144623487", json["content_id"]!!.jsonPrimitive.content)
        assertEquals("answer", json["content_type"]!!.jsonPrimitive.content)
        assertEquals("这篇内容讲了什么", json["message_content"]!!.jsonPrimitive.content)

        val attachment = json["attachments"]!!.jsonArray.first().jsonObject
        assertEquals("DOC", attachment["type"]!!.jsonPrimitive.content)
        assertEquals("测试标题", attachment["title"]!!.jsonPrimitive.content)
        assertEquals("MjAxNjU2MzYzODE0NDYyMzQ4N3w6OnxBTlNXRVI=", attachment["value"]!!.jsonPrimitive.content)
    }

    @Test
    fun testParseOpenAiStyleChunk() {
        val chunk = parseSummaryChunkFromSseData("""{"choices":[{"delta":{"content":"你好"}}]}""")
        assertEquals("你好", chunk)
    }

    @Test
    fun testParseNestedDataChunk() {
        val chunk = parseSummaryChunkFromSseData("""{"data":{"content":"总结内容"}}""")
        assertEquals("总结内容", chunk)
    }

    @Test
    fun testParseRecursiveFallbackChunk() {
        val chunk = parseSummaryChunkFromSseData("""{"foo":{"bar":{"text":"递归提取"}}}""")
        assertEquals("递归提取", chunk)
    }

    @Test
    fun testParseRawChunk() {
        val chunk = parseSummaryChunkFromSseData("直接文本")
        assertEquals("直接文本", chunk)
    }

    @Test
    fun testParseDoneMarker() {
        assertNull(parseSummaryChunkFromSseData("[DONE]"))
    }
}
