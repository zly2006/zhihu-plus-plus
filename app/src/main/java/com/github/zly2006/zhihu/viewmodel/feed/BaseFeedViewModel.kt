package com.github.zly2006.zhihu.viewmodel.feed

import android.content.Context
import androidx.compose.runtime.mutableStateListOf
import com.github.zly2006.zhihu.NavDestination
import com.github.zly2006.zhihu.data.*
import com.github.zly2006.zhihu.viewmodel.PaginationViewModel
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonArray
import kotlin.reflect.typeOf

abstract class BaseFeedViewModel : PaginationViewModel<Feed>(typeOf<Feed>()) {
    val displayItems = mutableStateListOf<FeedDisplayItem>()

    @Serializable
    class FeedResponse(val data: List<Feed>, val paging: Paging)

    data class FeedDisplayItem(
        val title: String,
        val summary: String?,
        val details: String,
        val feed: Feed?,
        val navDestination: NavDestination? = feed?.target?.navDestination,
        val avatarSrc: String? = null,
        val isFiltered: Boolean = false
    )

    override fun processResponse(data: List<Feed>, rawData: JsonArray) {
        super.processResponse(data, rawData)
        displayItems.addAll(data.flatten().map { createDisplayItem(it) }) // 展示用的已flatten数据
    }

    override fun refresh(context: Context) {
        if (isLoading) return
        displayItems.clear()
        super.refresh(context)
    }

    open fun createDisplayItem(feed: Feed): FeedDisplayItem {
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
                details = feed.target.detailsText,
                feed = feed,
                isFiltered = true
            )
        } else {
            when (feed.target) {
                is Feed.AnswerTarget -> {
                    FeedDisplayItem(
                        title = feed.target.question.title,
                        summary = feed.target.excerpt,
                        details = "${feed.target.detailsText} · ${feed.action_text}",
                        feed = feed
                    )
                }

                is Feed.ArticleTarget -> {
                    FeedDisplayItem(
                        title = feed.target.title,
                        summary = feed.target.excerpt,
                        details = "${feed.target.detailsText} · ${feed.action_text}",
                        feed = feed
                    )
                }

                is Feed.QuestionTarget -> {
                    FeedDisplayItem(
                        title = feed.target.title,
                        summary = feed.target.excerpt,
                        details = "${feed.target.detailsText} · ${feed.action_text}",
                        feed = feed
                    )
                }

                else -> {
                    FeedDisplayItem(
                        title = feed.target?.javaClass?.simpleName ?: "广告",
                        summary = "Not Implemented",
                        details = feed.target?.detailsText ?: "广告",
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
