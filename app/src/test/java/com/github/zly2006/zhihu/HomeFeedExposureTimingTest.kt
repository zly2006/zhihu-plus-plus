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

package com.github.zly2006.zhihu

import com.github.zly2006.zhihu.shared.data.CommonFeed
import com.github.zly2006.zhihu.shared.data.Feed
import com.github.zly2006.zhihu.shared.data.FeedDisplayItem
import com.github.zly2006.zhihu.viewmodel.ContentInteractionEnvironment
import com.github.zly2006.zhihu.viewmodel.feed.HomeFeedViewModel
import io.ktor.client.HttpClient
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test

class HomeFeedExposureTimingTest {
    @Test
    fun settledVisibleSetRecordsOnlyVisibleItems() = runBlocking {
        val recordedTitles = mutableListOf<String>()
        val environment = object : ContentInteractionEnvironment {
            override fun httpClient(): HttpClient = error("单条触达尚未达到发送批次，不应请求网络")

            override fun authenticatedCookies(): Map<String, String> = emptyMap()

            override suspend fun handleFetchFailure(
                tag: String?,
                error: Exception,
            ): Unit = throw error

            override suspend fun recordContentView(item: FeedDisplayItem) {
                recordedTitles += item.title
            }
        }
        val viewModel = HomeFeedViewModel()
        val visibleItem = item(1)
        val offscreenItem = item(2)
        viewModel.addDisplayItems(listOf(visibleItem, offscreenItem))

        viewModel.reportVisibleItems(environment, setOf(visibleItem.stableKey))

        assertEquals(listOf(visibleItem.title), recordedTitles)
    }

    private fun item(id: Long) = FeedDisplayItem(
        title = "条目 $id",
        summary = null,
        details = "",
        feed = CommonFeed(
            target = Feed.QuestionTarget(
                id = id,
                _title = "问题 $id",
                url = "https://www.zhihu.com/question/$id",
                type = "question",
            ),
        ),
    )
}
