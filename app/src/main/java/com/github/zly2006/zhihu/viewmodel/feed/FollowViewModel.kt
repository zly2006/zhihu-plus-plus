package com.github.zly2006.zhihu.viewmodel.feed

class FollowViewModel : BaseFeedViewModel() {
    override val initialUrl: String
        get() = "https://www.zhihu.com/api/v3/moments?limit=10&desktop=true"
}
