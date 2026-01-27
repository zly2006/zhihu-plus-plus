package com.github.zly2006.zhihu.viewmodel.feed

import android.content.Context
import com.github.zly2006.zhihu.data.AccountData
import com.github.zly2006.zhihu.data.Feed
import com.github.zly2006.zhihu.data.target
import com.github.zly2006.zhihu.viewmodel.filter.ContentFilterExtensions
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import kotlinx.serialization.json.JsonArray

class HotListViewModel : BaseFeedViewModel() {
    override val initialUrl: String
        get() = "https://www.zhihu.com/api/v3/feed/topstory/hot-lists/total?limit=50&mobile=true"

    init {
        allowGuestAccess = true
    }

    override fun createDisplayItem(feed: Feed): FeedDisplayItem {
        return super.createDisplayItem(feed).copy(
            authorName = null,
            avatarSrc = null,
        )
    }
}
