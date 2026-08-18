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

package com.github.zly2006.zhihu.navigation

import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals

class NavDestinationTest {
    @Test
    fun resolvesSupportedContentLinks() {
        listOf(
            "https://www.zhihu.com/topic/19550517" to Topic("19550517"),
            "zhihu://topic/19550517" to Topic("19550517"),
            "zhihu://pin20/topic?topic_id=19550517" to Topic("19550517"),
            "https://www.zhihu.com/topic/19550517/hot" to Topic("19550517", section = "hot"),
            "https://www.zhihu.com/topic/19550517/newest" to Topic("19550517", section = "newest"),
            "https://www.zhihu.com/topic/19550517/top-answers" to Topic("19550517", section = "top-answers"),
            "https://www.zhihu.com/topic/19550517/unanswered" to Topic("19550517", section = "unanswered"),
            "zhihu://topic/19550517/newest" to Topic("19550517", section = "newest"),
            "https://www.zhihu.com/question/1/answer/42" to Article(type = ArticleType.Answer, id = 42),
            "zhihu://comment/list/answer/42?anchor_comment_id=123456&is_child=false" to
                CommentHolder("123456", Article(type = ArticleType.Answer, id = 42)),
            "zhihu://comment/list/article/43?anchor_comment_id=123457&is_child=false" to
                CommentHolder("123457", Article(type = ArticleType.Article, id = 43)),
            "zhihu://comment/list/pin/44?anchor_comment_id=123458&is_child=true" to
                CommentHolder("123458", Pin(44)),
            "zhihu://comment/list/question/45?anchor_comment_id=123459&is_child=false" to
                CommentHolder("123459", Question(45)),
            "zhihu://comment/list/answer/46?anchor_comment_id=123460&list_height_ratio=0.66&dragIconVisible=true&segment=%7B%22id%22%3A1%7D" to
                CommentHolder("123460", Article(type = ArticleType.Answer, id = 46)),
            "https://www.zhihu.com/appview/pin/2059710318939301395" to Pin(2059710318939301395),
            "https://www.zhihu.com/appview/answer/2040633177593619876" to
                Article(type = ArticleType.Answer, id = 2040633177593619876),
            "https://www.zhihu.com/appview/p/1981671287999981270" to
                Article(type = ArticleType.Article, id = 1981671287999981270),
            "https://www.zhihu.com/notifications/v3/timeline/entry/system?title=%E7%B3%BB%E7%BB%9F%E6%B6%88%E6%81%AF" to
                Notification.Entry("system", "系统消息"),
            "https://www.zhihu.com/compose_answer_tab?default_selected_page=2&title=%E9%82%80%E8%AF%B7%E5%9B%9E%E7%AD%94" to
                Notification.Invitations,
            "https://www.zhihu.com/inbox/peer-token?title=%E7%9F%A5%E4%B9%8E%E5%B0%8F%E7%AE%A1%E5%AE%B6&source_type=message_list" to
                Notification.Message("peer-token", "知乎小管家"),
        ).forEach { (url, expected) ->
            assertEquals(expected, resolveContent(url), url)
        }
    }

    private val json = Json {
        ignoreUnknownKeys = true
    }

    @Test
    fun serializesSearchDestinationFromCommonCode() {
        val destination: NavDestination = Search(query = "kmp")

        val decoded = json.decodeFromString<NavDestination>(
            json.encodeToString<NavDestination>(destination),
        )

        assertEquals(destination, decoded)
    }
}
