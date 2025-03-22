package com.github.zly2006.zhihu.v2.viewmodel

import android.content.Context
import androidx.compose.runtime.mutableStateListOf
import androidx.lifecycle.ViewModel
import com.github.zly2006.zhihu.data.Feed
import kotlinx.serialization.Serializable

abstract class BaseFeedViewModel : ViewModel() {
    val feeds = mutableStateListOf<Feed>()
    val displayItems = mutableStateListOf<FeedDisplayItem>()
    protected var isLoading = false
    var errorMessage: String? = null
        protected set

    @Serializable
    class FeedResponse(val data: List<Feed>, val paging: Paging)

    @Suppress("PropertyName")
    @Serializable
    class Paging(
        val page: Int = -1,
        val is_end: Boolean,
        val next: String,
    )

    enum class DisplayMode {
        FEED,       // 首页推荐流模式
        QUESTION,   // 问题页面模式
        PROFILE     // 预留个人主页模式
    }

    data class FeedDisplayItem(
        val title: String,
        val summary: String,
        val details: String,
        val feed: Feed?,
        val avatarSrc: String? = null,
        val isFiltered: Boolean = false
    )
    
    abstract fun refresh(context: Context)
    abstract fun loadMore(context: Context)
}
