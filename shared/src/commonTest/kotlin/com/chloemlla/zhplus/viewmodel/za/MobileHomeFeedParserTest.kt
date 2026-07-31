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

package com.chloemlla.zhplus.viewmodel.za

import com.chloemlla.zhplus.navigation.Article
import com.chloemlla.zhplus.navigation.ArticleType
import com.chloemlla.zhplus.navigation.Pin
import com.chloemlla.zhplus.shared.data.CommonFeed
import com.chloemlla.zhplus.shared.data.DataHolder
import com.chloemlla.zhplus.shared.data.Feed
import com.chloemlla.zhplus.shared.data.navDestination
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull

@OptIn(ExperimentalEncodingApi::class)
class MobileHomeFeedParserTest {
    @Test
    fun parsesMobileRecommendationCardToFeedDisplayItem() {
        val item = parseMobileHomeFeedDisplayItem(mobileCard())

        requireNotNull(item)
        assertEquals("回答标题", item.title)
        assertEquals("回答摘要", item.summary)
        assertEquals("10 赞同 · 2 评论 · 3 收藏 · 手机版推荐", item.details)
        assertEquals("作者名", item.authorName)
        assertEquals("https://example.com/avatar.jpg", item.avatarSrc)

        val destination = item.navDestination as Article
        assertEquals(ArticleType.Answer, destination.type)
        assertEquals(456L, destination.id)
        assertEquals("回答标题", destination.title)
        assertEquals("作者名", destination.authorName)
        assertEquals("https://example.com/avatar.jpg", destination.avatarSrc)
    }

    @Test
    fun skipsNonComponentCard() {
        val item = parseMobileHomeFeedDisplayItem(
            buildJsonObject {
                put("type", "OtherCard")
            },
        )

        assertNull(item)
    }

    @Test
    fun parsesAndroidPinImagesIntoTypedFeedTarget() {
        val originalContent = buildJsonObject {
            put(
                "media_info",
                buildJsonObject {
                    put(
                        "images",
                        buildJsonArray {
                            repeat(9) { index ->
                                add(
                                    buildJsonObject {
                                        put("url", "https://example.com/original-$index.jpg")
                                        put("width", JsonPrimitive(1900 + index))
                                        put("height", JsonPrimitive(1000 + index))
                                    },
                                )
                            }
                        },
                    )
                },
            )
        }
        val card = buildJsonObject {
            put("type", "ComponentCard")
            put("id", "pin-card-2064056340616631575")
            put(
                "action",
                buildJsonObject {
                    put(
                        "parameter",
                        "route_url=https%3A%2F%2Fwww.zhihu.com%2Fpin%2F2064056340616631575",
                    )
                },
            )
            put(
                "children",
                buildJsonArray {
                    add(
                        buildJsonObject {
                            put("type", "Text")
                            put("id", "Text")
                            put("style", "text_recommend_title")
                            put("text", "九图想法标题")
                        },
                    )
                    add(
                        buildJsonObject {
                            put("type", "Text")
                            put("id", "text_pin_summary")
                            put("style", "text_pin_summary")
                            put("text", "九图想法摘要")
                        },
                    )
                    add(lineWithText("想法"))
                    add(reactionFooter())
                    add(authorLine())
                },
            )
            put(
                "extra",
                buildJsonObject {
                    put(
                        "business_ext_map",
                        buildJsonObject {
                            put(
                                "ori_content",
                                Base64.Default.encode(originalContent.toString().encodeToByteArray()),
                            )
                            put(
                                "images",
                                buildJsonArray {
                                    add(
                                        buildJsonObject {
                                            put("url", "https://example.com/thumbnail-0.jpg")
                                            put("width", JsonPrimitive(720))
                                            put("height", JsonPrimitive(400))
                                        },
                                    )
                                },
                            )
                        },
                    )
                    put(
                        "passthrough_info",
                        buildJsonObject {
                            put(
                                "author",
                                buildJsonObject {
                                    put("id", "author-id")
                                    put("url", "https://www.zhihu.com/people/author-token")
                                    put("url_token", "author-token")
                                    put("is_following", true)
                                },
                            )
                            put(
                                "content",
                                buildJsonObject {
                                    put("title", JsonNull)
                                },
                            )
                        },
                    )
                },
            )
        }

        val item = requireNotNull(parseMobileHomeFeedDisplayItem(card))

        assertEquals("九图想法标题", item.title)
        assertEquals(2064056340616631575L, assertIs<Pin>(item.navDestination).id)
        val target = assertIs<Feed.PinTarget>(assertIs<CommonFeed>(item.feed).target)
        assertEquals(2064056340616631575L, target.id)
        assertEquals("author-token", target.author.urlToken)
        assertEquals(true, target.author.isFollowing)
        assertEquals(
            DataHolder.Pin.ContentText(title = "九图想法标题", content = "九图想法摘要"),
            target.content.first(),
        )
        val images = target.content.drop(1).map { assertIs<DataHolder.Pin.ContentImage>(it) }
        assertEquals(9, images.size)
        assertEquals("https://example.com/original-0.jpg", images.first().url)
        assertEquals("https://example.com/thumbnail-0.jpg", images.first().thumbnail)
        assertEquals("", images[1].thumbnail)
        assertEquals("https://example.com/original-8.jpg", images.last().url)
    }

    private fun mobileCard(): JsonObject = buildJsonObject {
        put("type", "ComponentCard")
        put(
            "action",
            buildJsonObject {
                put("parameter", "route_url=https%3A%2F%2Fwww.zhihu.com%2Fquestion%2F123%2Fanswer%2F456")
            },
        )
        put(
            "children",
            buildJsonArray {
                add(
                    buildJsonObject {
                        put("type", "Text")
                        put("id", "Text")
                        put("style", "Text")
                        put("text", "回答标题")
                    },
                )
                add(
                    buildJsonObject {
                        put("type", "Text")
                        put("id", "text_pin_summary")
                        put("style", "text_pin_summary")
                        put("text", "回答摘要")
                    },
                )
                add(lineWithText("置顶"))
                add(reactionFooter())
                add(authorLine())
            },
        )
    }

    private fun lineWithText(text: String): JsonObject = buildJsonObject {
        put("type", "Line")
        put("style", "LineText")
        put(
            "elements",
            buildJsonArray {
                add(
                    buildJsonObject {
                        put("type", "Text")
                        put("text", text)
                    },
                )
            },
        )
    }

    private fun reactionFooter(): JsonObject = buildJsonObject {
        put("type", "Line")
        put("style", "LineFooterReaction_feed_v3")
        put(
            "elements",
            buildJsonArray {
                add(reaction("Vote", 10))
                add(reaction("Comment", 2))
                add(reaction("Collect", 3))
            },
        )
    }

    private fun reaction(
        reaction: String,
        count: Int,
    ): JsonObject = buildJsonObject {
        put("reaction", reaction)
        put("count", JsonPrimitive(count))
    }

    private fun authorLine(): JsonObject = buildJsonObject {
        put("type", "Line")
        put("style", "RecommendAuthorLine")
        put(
            "elements",
            buildJsonArray {
                add(
                    buildJsonObject {
                        put("style", "Avatar_default")
                        put(
                            "image",
                            buildJsonObject {
                                put("url", "https://example.com/avatar.jpg")
                            },
                        )
                    },
                )
                add(
                    buildJsonObject {
                        put("type", "Text")
                        put("text", "作者名")
                    },
                )
            },
        )
    }
}
