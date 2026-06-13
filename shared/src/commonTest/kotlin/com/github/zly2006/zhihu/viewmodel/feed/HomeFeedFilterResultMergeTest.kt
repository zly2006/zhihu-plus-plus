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

import com.github.zly2006.zhihu.shared.data.CommonFeed
import com.github.zly2006.zhihu.shared.data.Feed
import com.github.zly2006.zhihu.shared.data.FeedDisplayItem
import com.github.zly2006.zhihu.shared.data.Person
import com.github.zly2006.zhihu.viewmodel.HomeFeedFilterResult
import kotlin.test.Test
import kotlin.test.assertEquals

class HomeFeedFilterResultMergeTest {
    @Test
    fun replacesForegroundItemWithFilteredVersion() {
        val foregroundItem = item("原始文章")
        val filteredItem = foregroundItem.copy(title = "已屏蔽", summary = "规则：文章")
        val displayItems = mutableListOf(foregroundItem)

        displayItems.replaceHomeFeedItemsWithFilteredResult(
            HomeFeedFilterResult(
                foregroundItems = listOf(foregroundItem),
                filteredItems = listOf(filteredItem),
                reverseBlock = false,
            ),
        )

        assertEquals(listOf("已屏蔽"), displayItems.map { it.title })
        assertEquals("规则：文章", displayItems.single().summary)
    }

    @Test
    fun removesForegroundItemMissingFromFilteredItems() {
        val blockedItem = item("被内容过滤")
        val unrelatedItem = FeedDisplayItem(
            title = "旧条目",
            summary = null,
            details = "",
            feed = null,
        )
        val displayItems = mutableListOf(unrelatedItem, blockedItem)

        displayItems.replaceHomeFeedItemsWithFilteredResult(
            HomeFeedFilterResult(
                foregroundItems = listOf(blockedItem),
                filteredItems = emptyList(),
                reverseBlock = false,
            ),
        )

        assertEquals(listOf("旧条目"), displayItems.map { it.title })
    }

    private fun item(title: String): FeedDisplayItem {
        val feed = CommonFeed(
            target = Feed.ArticleTarget(
                id = 100,
                url = "https://zhuanlan.zhihu.com/p/100",
                author = Person(
                    id = "author-id",
                    url = "https://api.zhihu.com/people/author-id",
                    userType = "people",
                    name = "作者",
                    headline = "",
                    avatarUrl = "",
                ),
                title = title,
                voteupCount = 0,
            ),
        )
        return FeedDisplayItem(
            title = title,
            summary = null,
            details = "",
            feed = feed,
        )
    }
}
