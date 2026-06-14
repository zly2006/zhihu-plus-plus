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

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class ZhihuHotListClientTest {
    @Test
    fun buildsDefaultHotListUrl() {
        assertEquals(
            "https://www.zhihu.com/api/v3/feed/topstory/hot-lists/total?limit=50&mobile=true",
            zhihuHotListUrl(),
        )
    }

    @Test
    fun buildsCustomHotListUrl() {
        assertEquals(
            "https://www.zhihu.com/api/v3/feed/topstory/hot-lists/total?limit=20&mobile=false",
            zhihuHotListUrl(limit = 20, mobile = false),
        )
    }

    @Test
    fun createsQuestionDisplayItem() {
        val feed = HotListFeed(
            id = "hot-1",
            detailText = "热度 100 万",
            target = Feed.QuestionTarget(
                id = 1,
                _title = "问题标题",
                url = "https://www.zhihu.com/question/1",
                type = "question",
                answerCount = 10,
                followerCount = 20,
                excerpt = "问题摘要",
            ),
        )

        val item = feed.toDisplayItem(enableQualityFilter = false)

        assertEquals("问题标题", item.title)
        assertEquals("问题摘要", item.summary)
        assertEquals("问题 · 20 关注 · 10 回答 · 热度 100 万", item.details)
        assertNull(item.authorName)
    }
}
