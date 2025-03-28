package com.github.zly2006.zhihu.v2.viewmodel

import android.content.Context
import androidx.compose.runtime.mutableStateListOf
import androidx.lifecycle.ViewModel
import com.github.zly2006.zhihu.NavDestination
import com.github.zly2006.zhihu.data.AdvertisementFeed
import com.github.zly2006.zhihu.data.CommonFeed
import com.github.zly2006.zhihu.data.Feed
import com.github.zly2006.zhihu.data.GroupFeed
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

abstract class BaseFeedViewModel : ViewModel() {
    val feeds = mutableStateListOf<Feed>()
    val debugData = mutableListOf<JsonElement>() // feed data for debugging
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

    data class FeedDisplayItem(
        val title: String,
        val summary: String?,
        val details: String,
        val feed: Feed?,
        val navDestination: NavDestination? = null,
        val avatarSrc: String? = null,
        val isFiltered: Boolean = false
    )

    abstract fun refresh(context: Context)
    abstract fun loadMore(context: Context)

    fun createDisplayItem(feed: Feed): FeedDisplayItem {
        if (feed is AdvertisementFeed) {
            return FeedDisplayItem(
                title = "已屏蔽",
                summary = feed.action_text,
                details = "广告",
                feed = null,
                isFiltered = true
            )
        }
        feed as CommonFeed
        val filterReason = feed.target?.filterReason()

        return if (filterReason != null) {
            FeedDisplayItem(
                title = "已屏蔽",
                summary = filterReason,
                details = feed.target.detailsText(),
                feed = feed,
                isFiltered = true
            )
        } else {
            when (feed.target) {
                is Feed.AnswerTarget -> {
                    FeedDisplayItem(
                        title = feed.target.question.title,
                        summary = feed.target.excerpt,
                        details = feed.target.detailsText(),
                        feed = feed
                    )
                }

                is Feed.ArticleTarget -> {
                    FeedDisplayItem(
                        title = feed.target.title,
                        summary = feed.target.excerpt,
                        details = feed.target.detailsText(),
                        feed = feed
                    )
                }

                else -> {
                    FeedDisplayItem(
                        title = feed.target?.javaClass?.simpleName ?: "广告",
                        summary = "Not Implemented",
                        details = feed.target?.detailsText() ?: "广告",
                        feed = feed
                    )
                }
            }
        }
    }

    @Suppress("NOTHING_TO_INLINE")
    inline fun List<Feed>.flatten() = flatMap {
        (it as? GroupFeed)?.list ?: listOf(it)
    }
}
