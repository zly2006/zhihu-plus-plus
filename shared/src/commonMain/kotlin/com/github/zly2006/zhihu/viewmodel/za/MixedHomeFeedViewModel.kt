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

package com.github.zly2006.zhihu.viewmodel.za

import com.github.zly2006.zhihu.shared.data.Feed
import com.github.zly2006.zhihu.shared.data.FeedDisplayItem
import com.github.zly2006.zhihu.viewmodel.PaginationEnvironment
import com.github.zly2006.zhihu.viewmodel.feed.BaseFeedViewModel
import com.github.zly2006.zhihu.viewmodel.feed.HomeFeedInteractionViewModel
import com.github.zly2006.zhihu.viewmodel.feed.HomeFeedViewModel
import com.github.zly2006.zhihu.viewmodel.feed.zhihuMobileTopstoryRecommendUrl
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

    override suspend fun recordContentInteraction(environment: PaginationEnvironment, feed: Feed) {
        web.recordContentInteraction(environment, feed)
    }

    override fun onUiContentClick(environment: PaginationEnvironment, feed: Feed, item: FeedDisplayItem) {
        web.onUiContentClick(environment, feed, item)
    }
}
