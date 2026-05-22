package com.github.zly2006.zhihu.viewmodel.feed

import com.github.zly2006.zhihu.shared.data.Feed
import com.github.zly2006.zhihu.shared.data.FeedDisplayItem
import com.github.zly2006.zhihu.viewmodel.PaginationEnvironment

interface HomeFeedInteractionViewModel {
    suspend fun recordContentInteraction(environment: PaginationEnvironment, feed: Feed)

    fun onUiContentClick(environment: PaginationEnvironment, feed: Feed, item: FeedDisplayItem)
}
