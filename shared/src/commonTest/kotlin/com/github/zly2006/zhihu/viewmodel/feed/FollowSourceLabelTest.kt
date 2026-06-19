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

package com.github.zly2006.zhihu.viewmodel.feed

import com.github.zly2006.zhihu.shared.data.CommonFeed
import com.github.zly2006.zhihu.shared.data.Feed
import com.github.zly2006.zhihu.shared.data.GroupFeed
import com.github.zly2006.zhihu.shared.data.Person
import com.github.zly2006.zhihu.shared.data.sourceLabel
import com.github.zly2006.zhihu.shared.data.toDisplayItem
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class FollowSourceLabelTest {
    @Test
    fun followDynamicSourceLabelUsesFeedActionTextAndKeepsDetailsFocusedOnTargetStats() {
        val feed = CommonFeed(
            id = "moment-1",
            target = answerTarget(),
            actionText = "关注用户赞同了回答",
        )

        val item = feed.toDisplayItem().withFollowSourceLabel(feed)

        assertEquals("关注用户赞同了回答", feed.sourceLabel)
        assertEquals("关注用户赞同了回答", item.sourceLabel)
        assertEquals("回答 · 42 赞同 · 7 评论", item.details)
    }

    @Test
    fun followDynamicSourceLabelDoesNotExposeSourceForFilteredCards() {
        val feed = CommonFeed(
            id = "moment-2",
            target = answerTarget(
                author = author(isFollowing = false),
                voteupCount = 1,
            ),
            actionText = "关注用户赞同了回答",
        )

        val item = feed.toDisplayItem().withFollowSourceLabel(feed)

        assertNull(item.sourceLabel)
        assertEquals("已屏蔽", item.title)
    }

    @Test
    fun groupFeedTitleIsNotUsedAsSingleCardSourceLabel() {
        val feed = CommonFeed(
            id = "moment-3",
            target = answerTarget(),
            actionText = "关注用户收藏了回答",
        )
        val groupFeed = GroupFeed(
            brief = "",
            groupText = "今日动态",
            list = listOf(feed),
        )

        assertNull(groupFeed.sourceLabel)
        assertEquals("关注用户收藏了回答", feed.sourceLabel)
    }

    private fun answerTarget(
        author: Person = author(),
        voteupCount: Int = 42,
    ) = Feed.AnswerTarget(
        id = 1001,
        url = "https://api.zhihu.com/answers/1001",
        author = author,
        voteupCount = voteupCount,
        commentCount = 7,
        question = Feed.QuestionTarget(
            id = 2001,
            _title = "测试问题",
            url = "https://api.zhihu.com/questions/2001",
            type = "question",
        ),
        excerpt = "回答摘要",
    )

    private fun author(isFollowing: Boolean = true) = Person(
        id = "author-1",
        url = "https://api.zhihu.com/people/author-1",
        userType = "people",
        urlToken = "author-token",
        name = "作者",
        headline = "",
        avatarUrl = "",
        isFollowing = isFollowing,
    )
}
