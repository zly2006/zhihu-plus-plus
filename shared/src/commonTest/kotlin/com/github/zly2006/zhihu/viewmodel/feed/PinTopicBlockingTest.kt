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

package com.github.zly2006.zhihu.viewmodel.feed

import com.github.zly2006.zhihu.shared.data.DataHolder
import com.github.zly2006.zhihu.shared.data.FeedDisplayItem
import com.github.zly2006.zhihu.viewmodel.filter.extractTopicIds
import kotlin.test.Test
import kotlin.test.assertEquals

class PinTopicBlockingTest {
    @Test
    fun extractsTopicIdsFromPin() {
        assertEquals(listOf("topic-1", "topic-2"), extractTopicIds(pin("pin-1").raw!!))
    }

    @Test
    fun removesPinItemsByBlockedTopic() {
        val viewModel = TestFeedViewModel()
        viewModel.displayItems.addAll(
            listOf(
                pin("blocked-pin", "topic-blocked"),
                pin("kept-pin", "topic-kept"),
            ),
        )

        removeFeedItemsByBlockedTopic(viewModel, "topic-blocked")

        assertEquals(listOf("kept-pin"), viewModel.displayItems.map { it.title })
    }

    private fun pin(
        title: String,
        vararg topicIds: String = arrayOf("topic-1", "topic-2"),
    ): FeedDisplayItem = FeedDisplayItem(
        title = title,
        summary = null,
        details = "想法",
        feed = null,
        raw = DataHolder.Pin(
            id = title,
            author = author(),
            topics = topicIds.map { topic(it) },
        ),
    )

    private fun topic(id: String): DataHolder.Topic = DataHolder.Topic(
        id = id,
        type = "topic",
        url = "https://www.zhihu.com/topic/$id",
        name = id,
    )

    private fun author(): DataHolder.Author = DataHolder.Author(
        avatarUrl = "",
        gender = 0,
        headline = "",
        id = "author-id",
        isAdvertiser = false,
        isOrg = false,
        name = "作者",
        type = "people",
        url = "",
        urlToken = "author",
        userType = "people",
    )

    private class TestFeedViewModel : BaseFeedViewModel() {
        override val initialUrl: String = "https://api.zhihu.com/topstory/recommend"
    }
}
