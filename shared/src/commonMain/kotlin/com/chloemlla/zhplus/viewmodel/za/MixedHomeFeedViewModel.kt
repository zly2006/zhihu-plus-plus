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

package com.chloemlla.zhplus.viewmodel.za

import com.chloemlla.zhplus.shared.data.Feed
import com.chloemlla.zhplus.shared.data.FeedDisplayItem
import com.chloemlla.zhplus.viewmodel.ContentInteractionEnvironment
import com.chloemlla.zhplus.viewmodel.PaginationEnvironment
import com.chloemlla.zhplus.viewmodel.feed.BaseFeedViewModel
import com.chloemlla.zhplus.viewmodel.feed.HomeFeedInteractionViewModel
import com.chloemlla.zhplus.viewmodel.feed.HomeFeedViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.joinAll

class MixedHomeFeedViewModel :
    BaseFeedViewModel(),
    HomeFeedInteractionViewModel {
    val android = AndroidHomeFeedViewModel()
    val web = HomeFeedViewModel()
    override val initialUrl: String
        get() = "https://api.zhihu.com/topstory/recommend"

    init {
        android.displayItems = this.displayItems
        web.displayItems = this.displayItems
        android.latestLoadedDisplayItems = this.latestLoadedDisplayItems
        web.latestLoadedDisplayItems = this.latestLoadedDisplayItems
    }

    override suspend fun fetchFeeds(environment: PaginationEnvironment) {
        coroutineScope {
            listOf(
                async { android.fetchFeeds(environment) },
                async { web.fetchFeeds(environment) },
            ).joinAll()
        }
        isLoading = false
    }

    override suspend fun recordContentInteraction(environment: ContentInteractionEnvironment, feed: Feed) {
        web.recordContentInteraction(environment, feed)
    }

    override fun onUiContentClick(environment: ContentInteractionEnvironment, feed: Feed, item: FeedDisplayItem) {
        web.onUiContentClick(environment, feed, item)
    }
}
