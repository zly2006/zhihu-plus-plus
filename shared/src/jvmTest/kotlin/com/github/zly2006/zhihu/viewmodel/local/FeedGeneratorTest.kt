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

package com.github.zly2006.zhihu.viewmodel.local

import kotlinx.coroutines.test.runTest
import kotlin.io.path.createTempDirectory
import kotlin.test.Test
import kotlin.test.assertEquals

class FeedGeneratorTest {
    @Test
    fun generateFeedFromResultKeepsExistingFeedbackAndTimestamp() = runTest {
        val database = testLocalContentDatabase()
        val dao = database.contentDao()
        val generator = FeedGenerator(dao)
        val existingFeed = LocalFeed(
            id = "local_feed_answer_42",
            resultId = 1L,
            title = "旧标题",
            summary = "旧摘要",
            reasonDisplay = "旧理由",
            navDestination = "answer:42",
            userFeedback = 1.0,
            createdAt = 1234L,
        )
        dao.insertFeed(existingFeed)

        val feed = generator.generateFeedFromResult(
            result = CrawlingResult(
                id = 2L,
                taskId = 1L,
                contentId = "answer:42",
                title = "新标题",
                summary = "新摘要",
                url = "https://www.zhihu.com/answer/42",
                reason = CrawlingReason.Trending,
            ),
            reasonDisplay = "热门推荐",
        )

        assertEquals("local_feed_answer_42", feed.id)
        assertEquals(2L, feed.resultId)
        assertEquals("新标题", feed.title)
        assertEquals("新摘要", feed.summary)
        assertEquals("热门推荐", feed.reasonDisplay)
        assertEquals("answer:42", feed.navDestination)
        assertEquals(1.0, feed.userFeedback)
        assertEquals(1234L, feed.createdAt)
        assertEquals(feed, dao.getFeedById("local_feed_answer_42"))
        database.close()
    }

    @Test
    fun crawlingReasonKeepsReasonLabels() {
        assertEquals("关注用户的最新动态", CrawlingReason.Following.displayText)
        assertEquals("热门推荐", CrawlingReason.Trending.displayText)
        assertEquals("关注用户点赞的内容", CrawlingReason.FollowingUpvote.displayText)
        assertEquals("相关问题的优质回答", CrawlingReason.UpvotedQuestion.displayText)
        assertEquals("相似用户喜欢的内容", CrawlingReason.CollaborativeFiltering.displayText)
    }

    private fun testLocalContentDatabase(): LocalContentDatabase =
        getLocalContentDatabase(
            createTempDirectory("feed-generator-room").resolve("local-content.db").toFile(),
        )
}
