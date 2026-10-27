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

import com.github.zly2006.zhihu.data.CommonFeed
import com.github.zly2006.zhihu.data.Feed
import com.github.zly2006.zhihu.viewmodel.FeedDisplayEnvironment
import kotlin.test.Test
import kotlin.test.assertTrue
import kotlin.time.Clock

class QuestionAnswerTimeTest {
    @Test
    fun questionAnswerCardUsesEditTimeBeforeCreationTime() {
        val now = Clock.System.now().epochSeconds
        val answer = Feed.AnswerTarget(
            id = 1,
            url = "https://www.zhihu.com/api/v4/answers/1",
            question = Feed.QuestionTarget(id = 1, _title = "日期验证", url = "https://www.zhihu.com/api/v4/questions/1", type = "question"),
            createdTime = now - 400 * 86_400,
            updatedTime = now - 2 * 3_600,
        )
        val item = QuestionFeedViewModel(1).createDisplayItem(object : FeedDisplayEnvironment {}, CommonFeed(id = "answer-1", target = answer))
        assertTrue(item.details.orEmpty().endsWith(" · 2小时前"), item.details)
    }
}
