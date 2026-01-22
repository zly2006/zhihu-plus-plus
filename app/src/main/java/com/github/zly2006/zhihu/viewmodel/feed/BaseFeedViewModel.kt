package com.github.zly2006.zhihu.viewmodel.feed

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.github.zly2006.zhihu.NavDestination
import com.github.zly2006.zhihu.data.AdvertisementFeed
import com.github.zly2006.zhihu.data.CommonFeed
import com.github.zly2006.zhihu.data.Feed
import com.github.zly2006.zhihu.data.FeedItemIndexGroup
import com.github.zly2006.zhihu.data.GroupFeed
import com.github.zly2006.zhihu.data.MomentsFeed
import com.github.zly2006.zhihu.data.QuestionFeedCard
import com.github.zly2006.zhihu.data.actionText
import com.github.zly2006.zhihu.data.target
import com.github.zly2006.zhihu.viewmodel.PaginationViewModel
import kotlinx.serialization.json.JsonArray
import kotlin.reflect.typeOf

abstract class BaseFeedViewModel : PaginationViewModel<Feed>(typeOf<Feed>()) {
    var displayItems = mutableStateListOf<FeedDisplayItem>()
    var isPullToRefresh by mutableStateOf(false)
        protected set

    data class FeedDisplayItem(
        val title: String,
        val summary: String?,
        val details: String,
        val feed: Feed?,
        val navDestination: NavDestination? = feed?.target?.navDestination,
        val avatarSrc: String? = null,
        val authorName: String? = null,
        val isFiltered: Boolean = false,
    )

    override fun processResponse(context: Context, data: List<Feed>, rawData: JsonArray) {
        super.processResponse(context, data, rawData)
        displayItems.addAll(data.flatten().map { createDisplayItem(it) }) // 展示用的已flatten数据
    }

    override fun refresh(context: Context) {
        if (isLoading) return
        displayItems.clear()
        super.refresh(context)
    }

    suspend fun pullToRefresh(context: Context) {
        isPullToRefresh = true
        displayItems.clear()
        if (isLoading) return
        errorMessage = null
        debugData.clear()
        allData.clear()
        lastPaging = null // 重置 lastPaging
        isLoading = true
        try {
            fetchFeeds(context)
        } catch (e: Exception) {
            errorHandle(e)
        }
        isLoading = false
        isPullToRefresh = false
    }

    open fun createDisplayItem(feed: Feed): FeedDisplayItem = when (feed) {
        is CommonFeed, is FeedItemIndexGroup, is MomentsFeed -> {
            val filterReason = feed.target?.filterReason()

            if (filterReason != null) {
                FeedDisplayItem(
                    title = "已屏蔽",
                    summary = filterReason,
                    details = feed.target!!.detailsText,
                    feed = feed,
                    isFiltered = true,
                )
            } else {
                when (feed.target) {
                    is Feed.AnswerTarget,
                    is Feed.ArticleTarget,
                    is Feed.QuestionTarget,
                    -> {
                        FeedDisplayItem(
                            title = feed.target!!.title,
                            summary = feed.target!!.excerpt,
                            details = listOfNotNull(feed.target!!.detailsText, feed.actionText)
                                .joinToString(" · "),
                            avatarSrc = feed.target?.author?.avatarUrl,
                            authorName = feed.target?.author?.name,
                            feed = feed,
                        )
                    }

                    else -> {
                        FeedDisplayItem(
                            title = feed.target?.javaClass?.simpleName ?: "广告",
                            summary = "Not Implemented",
                            details = feed.target?.detailsText ?: "广告",
                            feed = feed,
                        )
                    }
                }
            }
        }

        is AdvertisementFeed -> FeedDisplayItem(
            title = "已屏蔽",
            summary = feed.actionText,
            details = "广告",
            feed = null,
            isFiltered = true,
        )

        is GroupFeed -> error("GroupFeed should not be flatten") // GroupFeed will be handled in the UI
        is QuestionFeedCard -> TODO()
    }

    @Suppress("NOTHING_TO_INLINE")
    inline fun List<Feed>.flatten() = flatMap {
        (it as? GroupFeed)?.list ?: listOf(it)
    }
}
