package com.github.zly2006.zhihu

import com.github.zly2006.zhihu.util.buildZhidaSummaryRequest
import com.github.zly2006.zhihu.util.decodeZhidaAnswerData
import com.github.zly2006.zhihu.util.decodeZhidaStreamErrorMessage
import com.github.zly2006.zhihu.util.encodeZhidaAttachmentValue
import com.github.zly2006.zhihu.util.parseZhidaSsePayload
import com.github.zly2006.zhihu.util.serializeZhidaSummaryRequest
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
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
    fun testBuildRequestSerialization() {
        val request = buildZhidaSummaryRequest(
            contentId = 2016563638144623487,
            contentType = "answer",
            title = "测试标题",
        )
        val payload = serializeZhidaSummaryRequest(request)
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
    fun testParseSseAnswerEvent() {
        val payload = parseZhidaSsePayload(
            """{"event":"Answer","data":{"status":1,"delta":true,"summary":"你好"}}""",
        )
        assertNotNull(payload)
        assertEquals("Answer", payload!!.event)

        val answer = decodeZhidaAnswerData(payload.data)
        assertNotNull(answer)
        assertEquals(1, answer!!.status)
        assertEquals(true, answer.delta)
        assertEquals("你好", answer.summary)
    }

    @Test
    fun testParseSseWithFallbackEvent() {
        val payload = parseZhidaSsePayload(
            """{"status":1,"delta":true,"summary":"分段文本"}""",
            fallbackEvent = "Answer",
        )
        assertNotNull(payload)
        assertEquals("Answer", payload!!.event)
        val answer = decodeZhidaAnswerData(payload.data)
        assertNotNull(answer)
        assertEquals(true, answer!!.delta)
        assertEquals("分段文本", answer.summary)
    }

    @Test
    fun testParseSseAnswerEventWithStringData() {
        val payload = parseZhidaSsePayload(
            """{"event":"Answer","data":"{\"status\":1,\"delta\":false,\"summary\":\"最终文本\"}"}""",
        )
        assertNotNull(payload)
        val answer = decodeZhidaAnswerData(payload!!.data)
        assertNotNull(answer)
        assertEquals(false, answer!!.delta)
        assertEquals("最终文本", answer.summary)
    }

    @Test
    fun testDecodeSseErrorMessage() {
        val payload = parseZhidaSsePayload("""{"event":"Error","data":{"error":{"message":"请求失败"}}}""")
        assertNotNull(payload)
        assertEquals("请求失败", decodeZhidaStreamErrorMessage(payload!!.data))
    }

    @Test
    fun testParseSseDoneMarker() {
        assertNull(parseZhidaSsePayload("[DONE]"))
    }
}
