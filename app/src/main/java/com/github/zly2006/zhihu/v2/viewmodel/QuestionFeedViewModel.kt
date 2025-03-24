package com.github.zly2006.zhihu.v2.viewmodel

import android.content.Context
import android.util.Log
import androidx.lifecycle.viewModelScope
import com.github.zly2006.zhihu.data.AccountData
import com.github.zly2006.zhihu.data.Feed
import com.github.zly2006.zhihu.signFetchRequest
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.http.*
import kotlinx.coroutines.launch
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.JsonObject

class QuestionFeedViewModel(private val questionId: Long) : BaseFeedViewModel() {
    var lastPaging: Paging? = null

    val isEnd: Boolean get() = lastPaging != null && lastPaging!!.is_end

    override fun refresh(context: Context) {
        viewModelScope.launch {
            displayItems.clear()
            feeds.clear()
            fetchAnswers(context)
        }
    }

    override fun loadMore(context: Context) {
        if (isLoading) return
        viewModelScope.launch {
            fetchAnswers(context)
        }
    }

    private suspend fun fetchAnswers(context: Context) {
        if (isLoading) return
        isLoading = true

        try {
            val httpClient = AccountData.httpClient(context)
            val response = httpClient.get(
                lastPaging?.next
                    ?: "https://www.zhihu.com/api/v4/questions/$questionId/feeds?limit=20&offset=${feeds.size}"
            ) {
                signFetchRequest(context)
            }

            if (response.status == HttpStatusCode.OK) {
                val text = response.body<JsonObject>()
                processResponse(text)
            }
        } catch (e: Exception) {
            Log.e("QuestionFeedViewModel", "Failed to fetch", e)
            errorMessage = "获取问题内容失败: ${e.message}"
        } finally {
            isLoading = false
        }
    }

    private fun processResponse(text: JsonObject) {
        try {
            val data = AccountData.decodeJson<FeedResponse>(text)
            lastPaging = data.paging
            val newFeeds = data.data.filter { it.target !is Feed.AdvertTarget }

            feeds.addAll(newFeeds)

            val newItems = newFeeds.map { feed ->
                when (feed.target) {
                    is Feed.AnswerTarget -> {
                        FeedDisplayItem(
                            title = feed.target.author.name,
                            summary = feed.target.excerpt,
                            details = feed.target.detailsText(),
                            feed = feed,
                            avatarSrc = feed.target.author.avatar_url
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

            displayItems.addAll(newItems)
        } catch (e: SerializationException) {
            Log.e("QuestionFeedViewModel", "Failed to parse JSON: $text", e)
            errorMessage = "解析数据失败: ${e.message}"
        }
    }
}
