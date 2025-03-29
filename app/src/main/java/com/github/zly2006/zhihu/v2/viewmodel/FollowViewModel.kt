package com.github.zly2006.zhihu.v2.viewmodel

class FollowViewModel : BaseFeedViewModel() {
    override fun getInitialUrl() = "https://www.zhihu.com/api/v3/moments?limit=10&desktop=true"
}
