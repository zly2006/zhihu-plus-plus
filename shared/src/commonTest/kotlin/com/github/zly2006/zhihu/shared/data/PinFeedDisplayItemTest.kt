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

package com.github.zly2006.zhihu.shared.data

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonPrimitive
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class PinFeedDisplayItemTest {
    @Test
    fun pinTargetDecodesTypedContentItems() {
        val pin = assertIs<Feed.PinTarget>(
            ZhihuJson.decodeJson<Feed.Target>(
                Json.parseToJsonElement(
                    """
                    {
                      "type": "pin",
                      "id": 574,
                      "url": "https://www.zhihu.com/pin/574",
                      "author": {
                        "id": "author-id",
                        "url": "https://api.zhihu.com/people/author-id",
                        "user_type": "people",
                        "url_token": "author",
                        "name": "作者",
                        "headline": "",
                        "avatar_url": "https://pic.example/avatar.jpg"
                      },
                      "content": [
                        {
                          "type": "text",
                          "title": "想法标题",
                          "content": "<p>想法正文</p>"
                        },
                        {
                          "type": "image",
                          "url": "https://pic.example/image.jpg",
                          "width": 1200,
                          "height": 800,
                          "is_gif": false,
                          "original_url": "https://pic.example/original.jpg"
                        }
                      ]
                    }
                    """.trimIndent(),
                ),
            ),
        )

        val text = assertIs<DataHolder.Pin.ContentText>(pin.content[0])
        assertEquals("想法标题", text.title)
        assertEquals("<p>想法正文</p>", text.content)

        val image = assertIs<DataHolder.Pin.ContentImage>(pin.content[1])
        assertEquals("https://pic.example/image.jpg", image.url)
        assertEquals(1200, image.width)
        assertEquals("https://pic.example/original.jpg", image.originalUrl)
    }

    @Test
    fun pinTargetKeepsUnknownContentAndFallsBackToExcerpt() {
        val pin = assertIs<Feed.PinTarget>(
            ZhihuJson.decodeJson<Feed.Target>(
                Json.parseToJsonElement(
                    """
                    {
                      "type": "pin",
                      "id": 574,
                      "url": "https://www.zhihu.com/pin/574",
                      "author": {
                        "id": "author-id",
                        "url": "https://api.zhihu.com/people/author-id",
                        "user_type": "people",
                        "url_token": "author",
                        "name": "作者",
                        "headline": "",
                        "avatar_url": "https://pic.example/avatar.jpg"
                      },
                      "content": [
                        {
                          "type": "future_content",
                          "payload": "尚未支持的想法内容"
                        }
                      ],
                      "excerpt_title": "兼容摘要"
                    }
                    """.trimIndent(),
                ),
            ),
        )

        val unknown = assertIs<DataHolder.Pin.ContentUnknown>(pin.content.single())
        assertEquals("future_content", unknown.value["type"]?.jsonPrimitive?.content)
        assertEquals(
            "兼容摘要",
            CommonFeed(id = "pin-feed", target = pin).toDisplayItem().summary,
        )
    }

    @Test
    fun pinPreviewUsesContentTitleAndBody() {
        val item = pinFeed(
            title = "想法标题",
            content = "<p>第一段正文，后面还有<strong>更多内容</strong>。</p>",
            excerptTitle = "旧摘要",
        ).toDisplayItem()

        assertEquals("想法标题", item.title)
        assertEquals("第一段正文，后面还有更多内容。", item.summary)
    }

    @Test
    fun pinPreviewWithoutTitleUsesContentBody() {
        val item = pinFeed(
            title = "",
            content = "没有标题时仍应显示的正文。",
            excerptTitle = "",
        ).toDisplayItem()

        assertEquals("", item.title)
        assertEquals("没有标题时仍应显示的正文。", item.summary)
    }

    @Test
    fun pinPreviewFallsBackToLegacyExcerptWhenContentIsMissing() {
        val item = pinFeed(
            title = null,
            content = null,
            excerptTitle = "旧接口提供的想法预览",
        ).toDisplayItem()

        assertEquals("", item.title)
        assertEquals("旧接口提供的想法预览", item.summary)
    }

    @Test
    fun pinPreviewReadsTextAfterImageContent() {
        val item = pinFeed(
            title = "",
            content = "图片条目后仍可读取的正文。",
            excerptTitle = "",
            includeImage = true,
        ).toDisplayItem()

        assertEquals("", item.title)
        assertEquals("图片条目后仍可读取的正文。", item.summary)
    }

    private fun pinFeed(
        title: String?,
        content: String?,
        excerptTitle: String,
        includeImage: Boolean = false,
    ) = CommonFeed(
        id = "pin-feed",
        target = Feed.PinTarget(
            id = 574,
            url = "https://www.zhihu.com/pin/574",
            author = Person(
                id = "author-id",
                url = "https://www.zhihu.com/people/author",
                userType = "people",
                urlToken = "author",
                name = "作者",
                headline = "",
                avatarUrl = "",
            ),
            content = buildList {
                if (includeImage) {
                    add(
                        DataHolder.Pin.ContentImage(
                            url = "https://example.com/image.jpg",
                            width = 640,
                            height = 480,
                        ),
                    )
                }
                if (content != null) {
                    add(
                        DataHolder.Pin.ContentText(
                            title = title.orEmpty(),
                            content = content,
                        ),
                    )
                }
            },
            excerptTitle = excerptTitle,
        ),
    )
}
