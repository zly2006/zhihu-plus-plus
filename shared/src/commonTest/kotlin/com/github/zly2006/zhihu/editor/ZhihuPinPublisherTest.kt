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

import com.github.zly2006.zhihu.shared.data.ZhihuJson
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse

class ZhihuPinPublisherTest {
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
    fun calculatesPinTextLengthFromHtmlText() {
        assertEquals(
            7,
            calculatePinHtmlTextLength("<p>正文 &amp; <b>更多</b></p>"),
        )
    }
}
