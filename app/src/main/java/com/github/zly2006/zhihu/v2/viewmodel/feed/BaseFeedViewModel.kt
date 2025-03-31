package com.github.zly2006.zhihu.v2.viewmodel.feed

import android.content.Context
import android.util.Log
import android.widget.Toast
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.zly2006.zhihu.MainActivity
import com.github.zly2006.zhihu.NavDestination
import com.github.zly2006.zhihu.data.*
import com.github.zly2006.zhihu.signFetchRequest
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.http.*
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonArray

abstract class BaseFeedViewModel : ViewModel() {
    val feeds = mutableStateListOf<Feed>()
    val debugData = mutableListOf<JsonElement>() // feed data for debugging
    val displayItems = mutableStateListOf<FeedDisplayItem>()
    var isLoading by mutableStateOf(false)
    var errorMessage: String? = null
        protected set
    protected var lastPaging: Paging? = null
    open val isEnd get() = lastPaging?.is_end == true  // 新增getter,保持向后兼容

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

    protected fun errorHandle(e: Exception) {
        errorMessage = e.message
        isLoading = false
    }

    protected fun processResponse(response: FeedResponse, rawData: JsonArray) {
        debugData.addAll(rawData) // 保存原始JSON
        feeds.addAll(response.data) // 保存未flatten的数据
        displayItems.addAll(response.data.flatten().map { createDisplayItem(it) }) // 展示用的已flatten数据
        lastPaging = response.paging
        isLoading = false
    }

    protected abstract val initialUrl: String
    protected open fun processData(json: JsonObject): FeedResponse {
        return AccountData.decodeJson(json)
    }

    fun httpClient(context: Context): HttpClient {
        if (context is MainActivity) {
            return context.httpClient
        }
        return AccountData.httpClient(context)
    }

    protected open suspend fun fetchFeeds(context: Context) {
        try {
            val url = lastPaging?.next ?: initialUrl
            val httpClient = httpClient(context)

            val response = httpClient.get(url) {
                signFetchRequest(context)
            }

            if (response.status == HttpStatusCode.OK) {
                val json = response.body<JsonObject>()
                val data = processData(json)
                processResponse(data, json["data"]!!.jsonArray)
            } else {
                context.mainExecutor.execute {
                    Toast.makeText(context, "获取数据失败: ${response.status}", Toast.LENGTH_SHORT).show()
                }
            }
        } catch (e: Exception) {
            Log.e(this::class.simpleName, "Failed to fetch feeds", e)
            context.mainExecutor.execute {
                Toast.makeText(context, "加载失败: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    open fun refresh(context: Context) {
        if (isLoading) return
        errorMessage = null
        debugData.clear()
        displayItems.clear()
        feeds.clear()
        lastPaging = null  // 重置 lastPaging
        loadMore(context)
    }

    open fun loadMore(context: Context) {
        if (isLoading || isEnd) return  // 使用新的isEnd getter
        isLoading = true
        viewModelScope.launch {
            try {
                fetchFeeds(context)
            } catch (e: Exception) {
                errorHandle(e)
            }
        }
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
                        details = feed.target.detailsText() + " · " + feed.action_text,
                        feed = feed
                    )
                }

                is Feed.ArticleTarget -> {
                    FeedDisplayItem(
                        title = feed.target.title,
                        summary = feed.target.excerpt,
                        details = feed.target.detailsText() + " · " + feed.action_text,
                        feed = feed
                    )
                }

                is Feed.QuestionTarget -> {
                    FeedDisplayItem(
                        title = feed.target.title,
                        summary = feed.target.excerpt,
                        details = feed.target.detailsText() + " · " + feed.action_text,
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
