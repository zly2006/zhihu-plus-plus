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

package com.github.zly2006.zhihu.editor

import com.github.zly2006.zhihu.data.ZhihuJson
import com.github.zly2006.zhihu.data.installZhihuCommonClientConfig
import com.github.zly2006.zhihu.viewmodel.ZhihuApiEnvironment
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.content.OutgoingContent
import io.ktor.http.headersOf
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse

class ZhihuPinPublisherTest {
    @Test
    fun decodesObservedTopicRecommendationFields() {
        val response = ZhihuJson.decodeJson<PinTopicSuggestionResponse>(
            ZhihuJson.json.parseToJsonElement(
                """{"data":{"is_show_create":true,"list":[{"id":"787460807","name":"AI","topic_id":3858106,"discuss_count":"31.8 万讨论"}]},"code":0}""",
            ),
        )

        assertEquals(
            PinTopicSuggestion("787460807", "AI", 3858106, "31.8 万讨论"),
            response.data.list.single(),
        )
    }

    @Test
    fun serializesPinDraftPayloadWithObservedPublishShape() {
        val request = SavePinDraftRequest(
            data = PinContentPayload(
                publish = PublishTrace(traceId = "trace-id"),
                title = PinContentTitle(title = "标题"),
                hybrid = PinContentHybrid(
                    html = "<p>正文</p>",
                    textLength = 2,
                ),
            ),
        )

        val root = ZhihuJson.json.encodeToJsonElement(SavePinDraftRequest.serializer(), request).jsonObject
        val data = root.getValue("data").jsonObject
        val commentsPermission = data.getValue("commentsPermission").jsonObject
        val extraInfo = data.getValue("extra_info").jsonObject
        val hybrid = data.getValue("hybrid").jsonObject

        assertEquals("pin", root.getValue("action").jsonPrimitive.content)
        assertEquals("all", commentsPermission.getValue("comment_permission").jsonPrimitive.content)
        assertEquals("all", extraInfo.getValue("view_permission").jsonPrimitive.content)
        assertEquals("pc", extraInfo.getValue("publisher").jsonPrimitive.content)
        assertEquals(
            "标题",
            data
                .getValue("title")
                .jsonObject
                .getValue("title")
                .jsonPrimitive.content,
        )
        assertEquals("<p>正文</p>", hybrid.getValue("html").jsonPrimitive.content)
        assertEquals(2, hybrid.getValue("textLength").jsonPrimitive.int)
        assertFalse("attachment" in hybrid)
        assertFalse("meta" in hybrid)
        assertFalse("media" in data)
    }

    @Test
    fun serializesPinImagePayloadWithObservedMediaShape() {
        val request = PublishPinRequest(
            data = PinContentPayload(
                publish = PublishTrace(traceId = "trace-id"),
                hybrid = PinContentHybrid(
                    html = "<p>正文</p>",
                    textLength = 2,
                ),
                media = PinContentMedia(
                    medias = listOf(
                        PinContentMediaItem(
                            image = PinContentImage(
                                height = 64,
                                width = 64,
                                url = "https://picx.zhimg.com/v2-test.png",
                                originalUrl = "https://picx.zhimg.com/v2-test.png",
                                watermark = "watermark",
                                watermarkUrl = null,
                            ),
                        ),
                    ),
                ),
            ),
        )

        val root = ZhihuJson.json.encodeToJsonElement(PublishPinRequest.serializer(), request).jsonObject
        val data = root.getValue("data").jsonObject
        val media = data.getValue("media").jsonObject
        val image = media
            .getValue("medias")
            .jsonArray
            .single()
            .jsonObject
            .getValue("image")
            .jsonObject

        assertEquals("pin", root.getValue("action").jsonPrimitive.content)
        assertEquals(64, image.getValue("height").jsonPrimitive.int)
        assertEquals(64, image.getValue("width").jsonPrimitive.int)
        assertEquals("https://picx.zhimg.com/v2-test.png", image.getValue("url").jsonPrimitive.content)
        assertEquals("https://picx.zhimg.com/v2-test.png", image.getValue("originalUrl").jsonPrimitive.content)
    }

    @Test
    fun serializesSelectedTopicsAndOmitsEmptyTopicPayload() {
        val selected = PublishPinRequest(
            data = PinContentPayload(
                publish = PublishTrace(traceId = "trace-id"),
                topic = PinContentTopic(listOf(PinContentTopicItem("19550517", "#互联网#"))),
            ),
        )
        val selectedData = ZhihuJson.json
            .encodeToJsonElement(PublishPinRequest.serializer(), selected)
            .jsonObject
            .getValue("data")
            .jsonObject
        val topic = selectedData
            .getValue("topic")
            .jsonObject
            .getValue("topics")
            .jsonArray
            .single()
            .jsonObject
        assertEquals("19550517", topic.getValue("topic_id").jsonPrimitive.content)
        assertEquals("#互联网#", topic.getValue("topic_name").jsonPrimitive.content)

        val empty = PublishPinRequest(data = PinContentPayload(publish = PublishTrace(traceId = "trace-id")))
        val emptyData = ZhihuJson.json
            .encodeToJsonElement(PublishPinRequest.serializer(), empty)
            .jsonObject
            .getValue("data")
            .jsonObject
        assertFalse("topic" in emptyData)
    }

    @Test
    fun compilesSelectedTopicAsInlineNodeAndUsesInternalTopicId() {
        val html = compilePinMarkdownToZhihuHtml(
            "正文 #编程 后文",
            listOf(PinContentTopicMarker(PinContentTopicItem("1354", "编程"), 3, 6)),
        )
        assertEquals(
            "<p>正文 <a class=\"hash_tag\" data-topic-name=\"#编程#\" data-topic-id=\"1354\">#编程#</a> 后文</p>",
            html,
        )
    }

    @Test
    fun compilesTopicNamesWithMarkdownAndHtmlCharacters() {
        val topic = PinContentTopicItem("9&10", "C++ [A&B]")
        val markdown = "正文 ${topic.inlineMarker} 后文"

        assertEquals(
            "<p>正文 <a class=\"hash_tag\" data-topic-name=\"#C++ [A&amp;B]#\" data-topic-id=\"9&amp;10\">#C++ [A&amp;B]#</a> 后文</p>",
            compilePinMarkdownToZhihuHtml(
                markdown,
                listOf(PinContentTopicMarker(topic, 3, 3 + topic.inlineMarker.length)),
            ),
        )
    }

    @Test
    fun capturesInlineTopicsInFinalDraftRequest() = runTest {
        var requestBody = ""
        val client = HttpClient(
            MockEngine { request ->
                requestBody = (request.body as OutgoingContent.ByteArrayContent).bytes().decodeToString()
                respond(
                    "{}",
                    HttpStatusCode.OK,
                    headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString()),
                )
            },
        ) {
            installZhihuCommonClientConfig(mutableMapOf(), "test-agent")
        }
        val publisher = ZhihuApiPinPublisher(
            object : ZhihuApiEnvironment {
                override fun httpClient() = client

                override fun authenticatedCookies() = mapOf("_xsrf" to "xsrf", "d_c0" to "device")

                override suspend fun handleFetchFailure(tag: String?, error: Exception) = Unit
            },
        )
        val topic = PinContentTopicItem("1354", "编程")
        val html = compilePinMarkdownToZhihuHtml("正文 #编程 ", listOf(PinContentTopicMarker(topic, 3, 6)))

        publisher.savePinDraft("", html, calculatePinHtmlTextLength(html), emptyList(), listOf(topic))

        val data = ZhihuJson.json
            .parseToJsonElement(requestBody)
            .jsonObject
            .getValue("data")
            .jsonObject
        val payloadTopic = data
            .getValue("topic")
            .jsonObject
            .getValue("topics")
            .jsonArray
            .single()
            .jsonObject
        assertEquals("1354", payloadTopic.getValue("topic_id").jsonPrimitive.content)
        assertEquals("#编程#", payloadTopic.getValue("topic_name").jsonPrimitive.content)
        assertEquals(
            "<p>正文 <a class=\"hash_tag\" data-topic-name=\"#编程#\" data-topic-id=\"1354\">#编程#</a></p>",
            data
                .getValue("hybrid")
                .jsonObject
                .getValue("html")
                .jsonPrimitive.content,
        )
    }

    @Test
    fun calculatesPinTextLengthFromHtmlText() {
        assertEquals(
            7,
            calculatePinHtmlTextLength("<p>正文 &amp; <b>更多</b></p>"),
        )
    }
}
