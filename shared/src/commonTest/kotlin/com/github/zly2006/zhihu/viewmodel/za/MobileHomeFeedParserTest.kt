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

package com.github.zly2006.zhihu.viewmodel.za

import com.github.zly2006.zhihu.navigation.Article
import com.github.zly2006.zhihu.navigation.ArticleType
import com.github.zly2006.zhihu.shared.data.navDestination
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

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
