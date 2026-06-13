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

package com.github.zly2006.zhihu.shared.data

import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class PersonFollowerCountTest {
    @Test
    fun decodesSearchFeedFollowerCount() {
        val person = ZhihuJson.decodeJson<Person>(
            Json.parseToJsonElement(
                personJson("follower_count", 1997),
            ),
        )

        assertEquals(1997, person.followersCount)
    }

    @Test
    fun decodesRecommendFeedFollowersCount() {
        val person = ZhihuJson.decodeJson<Person>(
            Json.parseToJsonElement(
                personJson("followers_count", 33884),
            ),
        )

        assertEquals(33884, person.followersCount)
    }

    @Test
    fun articleQualityFilterUsesSearchFeedFollowerCount() {
        val article = ZhihuJson.decodeJson<Feed.Target>(
            Json.parseToJsonElement(
                """
                {
                  "type": "article",
                  "id": 557849764,
                  "url": "https://zhuanlan.zhihu.com/p/557849764",
                  "author": ${personJson("follower_count", 1997)},
                  "voteup_count": 30,
                  "comment_count": 0,
                  "title": "牙膏推荐",
                  "excerpt": ""
                }
                """.trimIndent(),
            ),
        )

        assertNull(article.filterReason())
    }

    @Test
    fun videoQualityFilterUsesSearchFeedFollowerCount() {
        val video = ZhihuJson.decodeJson<Feed.Target>(
            Json.parseToJsonElement(
                """
                {
                  "type": "zvideo",
                  "id": 123,
                  "url": "https://www.zhihu.com/zvideo/123",
                  "author": ${personJson("follower_count", 1997)},
                  "vote_count": 0,
                  "comment_count": 0,
                  "title": "牙膏推荐",
                  "description": "",
                  "excerpt": ""
                }
                """.trimIndent(),
            ),
        )

        assertNull(video.filterReason())
    }

    private fun personJson(
        countKey: String,
        count: Int,
    ): String =
        """
        {
          "id": "author-id",
          "url": "https://api.zhihu.com/people/author-id",
          "user_type": "people",
          "url_token": "author",
          "name": "作者",
          "headline": "",
          "avatar_url": "https://pic.example/avatar.jpg",
          "is_org": false,
          "gender": 0,
          "$countKey": $count,
          "is_following": false,
          "is_followed": false
        }
        """.trimIndent()
}
