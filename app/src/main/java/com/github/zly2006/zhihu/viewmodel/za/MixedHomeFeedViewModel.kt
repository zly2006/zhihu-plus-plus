package com.github.zly2006.zhihu.viewmodel.za

import android.content.Context
import com.github.zly2006.zhihu.data.Feed
import com.github.zly2006.zhihu.ui.IHomeFeedViewModel
import com.github.zly2006.zhihu.viewmodel.feed.BaseFeedViewModel
import com.github.zly2006.zhihu.viewmodel.feed.HomeFeedViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.joinAll

class MixedHomeFeedViewModel :
    BaseFeedViewModel(),
    IHomeFeedViewModel {
    val android = AndroidHomeFeedViewModel()
    val web = HomeFeedViewModel()
    override val initialUrl: String
        get() = "https://api.zhihu.com/topstory/recommend"

    init {
        android.displayItems = this.displayItems
        web.displayItems = this.displayItems
    }

    override suspend fun fetchFeeds(context: Context) {
        coroutineScope {
            listOf(
                async { android.fetchFeeds(context) },
                async { web.fetchFeeds(context) },
            ).joinAll()
        }
        isLoading = false
    }

    override suspend fun recordContentInteraction(context: Context, feed: Feed) {
        web.recordContentInteraction(context, feed)
    }
}
