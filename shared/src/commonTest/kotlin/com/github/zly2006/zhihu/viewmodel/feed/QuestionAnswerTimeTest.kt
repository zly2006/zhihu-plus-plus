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
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime
import kotlin.test.Test
import kotlin.test.assertTrue
import kotlin.time.Clock

class QuestionAnswerTimeTest {
    @Test
    fun answerListRetainsCreationTimeAndDisplaysItsYear() {
        val zone = TimeZone.currentSystemDefault()
        val year = Clock.System
            .now()
            .toLocalDateTime(zone)
            .year - 1
        val created = LocalDateTime(year, 3, 22, 19, 7).toInstant(zone).epochSeconds
        val answer = Feed.AnswerTarget(
            id = 1,
            url = "https://api.zhihu.com/answers/1",
            question = Feed.QuestionTarget(id = 1, _title = "时间显示", url = "https://api.zhihu.com/questions/1", type = "question"),
            createdTime = created,
            updatedTime = Clock.System.now().epochSeconds,
        )
        val item = QuestionFeedViewModel(1).createDisplayItem(
            object : FeedDisplayEnvironment {},
            CommonFeed(id = "answer-1", target = answer),
        )
        assertTrue(item.details.endsWith(" · $year-03-22"), item.details)
    }
}
