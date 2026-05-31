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

package com.github.zly2006.zhihu.shared.util

import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class ZhidaSummaryTest {
    @Test
    fun encodesAttachmentValueWithCanonicalContentType() {
        assertEquals("MTIzfDo6fEFOU1dFUg==", encodeZhidaAttachmentValue(123, "answer"))
        assertEquals("MTIzfDo6fEFSVElDTEU=", encodeZhidaAttachmentValue(123, "article"))
        assertEquals("MTIzfDo6fFBJTg==", encodeZhidaAttachmentValue(123, "pin"))
    }

    @Test
    fun buildsAndSerializesSummaryRequest() {
        val request = buildZhidaSummaryRequest(
            contentId = 42,
            contentType = "answer",
            title = "标题",
            messageContent = "总结一下",
        )

        assertEquals("42", request.contentId)
        assertEquals("answer", request.contentType)
        assertEquals("总结一下", request.messageContent)
        assertEquals("标题", request.attachments.single().title)
        assertEquals("NDJ8Ojp8QU5TV0VS", request.attachments.single().value)
        assertEquals(
            """{"quiz_type":"QT_CHAT","attachments":[{"type":"DOC","value":"NDJ8Ojp8QU5TV0VS","title":"标题"}],"message_source_type":"text","session_id":"","zhida_source":"one_tap_summary","content_id":"42","content_type":"answer","message_content":"总结一下"}""",
            serializeZhidaSummaryRequest(request),
        )
    }

    @Test
    fun parsesEnvelopeAndFallbackSsePayloads() {
        val envelope = parseZhidaSsePayload("""{"event":"answer","data":{"summary":"你好","delta":true}}""")
        assertEquals("answer", envelope?.event)
        assertEquals("你好", decodeZhidaAnswerData(envelope!!.data)?.summary)

        val fallback = parseZhidaSsePayload("""{"summary":"世界"}""", fallbackEvent = "answer")
        assertEquals("answer", fallback?.event)
        assertEquals("世界", decodeZhidaAnswerData(fallback!!.data)?.summary)

        assertNull(parseZhidaSsePayload("[DONE]"))
    }

    @Test
    fun decodesErrorMessageFromMultipleShapes() {
        assertEquals("plain", decodeZhidaStreamErrorMessage(JsonPrimitive("plain")))
        assertEquals("top", decodeZhidaStreamErrorMessage(JsonObject(mapOf("message" to JsonPrimitive("top")))))
        assertEquals(
            "nested",
            decodeZhidaStreamErrorMessage(
                JsonObject(
                    mapOf(
                        "error" to JsonObject(mapOf("message" to JsonPrimitive("nested"))),
                    ),
                ),
            ),
        )
    }

    @Test
    fun mergesSummaryChunksWithoutDuplicatingOverlaps() {
        assertEquals("hello", mergeSummaryChunk("", "hello"))
        assertEquals("hello world", mergeSummaryChunk("hello", "hello world"))
        assertEquals("hello", mergeSummaryChunk("hello", "lo"))
        assertEquals("hello world", mergeSummaryChunk("hello ", "world"))
    }
}
