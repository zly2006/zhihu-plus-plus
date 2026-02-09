package com.github.zly2006.zhihu.viewmodel.feed

import android.content.Context
import com.github.zly2006.zhihu.data.Feed

class HotListViewModel : BaseFeedViewModel() {
    override val initialUrl: String
        get() = "https://www.zhihu.com/api/v3/feed/topstory/hot-lists/total?limit=50&mobile=true"

    init {
        allowGuestAccess = true
    }

    override fun createDisplayItem(context: Context, feed: Feed): FeedDisplayItem = super.createDisplayItem(context, feed).copy(
        authorName = null,
        avatarSrc = null,
    )
}
