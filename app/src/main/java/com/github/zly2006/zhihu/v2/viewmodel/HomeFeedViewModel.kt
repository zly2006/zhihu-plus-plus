package com.github.zly2006.zhihu.v2.viewmodel

import android.content.Context
import android.util.Log
import androidx.lifecycle.viewModelScope
import com.github.zly2006.zhihu.data.*
import com.github.zly2006.zhihu.signFetchRequest
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.client.request.forms.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.coroutines.launch
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.add
import kotlinx.serialization.json.buildJsonArray
import java.net.UnknownHostException

class HomeFeedViewModel : BaseFeedViewModel() {
    override fun refresh(context: Context) {
        viewModelScope.launch {
            displayItems.clear()
            feeds.clear()
            fetchFeeds(context)
        }
    }

    override fun loadMore(context: Context) {
        if (isLoading) return
        viewModelScope.launch {
            fetchFeeds(context)
        }
    }

    private suspend fun fetchFeeds(context: Context) {
        if (isLoading) return
        isLoading = true

        try {
            val httpClient = AccountData.httpClient(context)
            markItemsAsTouched(context, httpClient)

            val response =
                httpClient.get("https://www.zhihu.com/api/v3/feed/topstory/recommend?desktop=true&action=down&end_offset=${feeds.size}") {
                    signFetchRequest(context)
                }

            if (response.status == HttpStatusCode.OK) {
                val text = response.body<JsonObject>()
                processResponse(text)
            }
        } catch (e: UnknownHostException) {
            Log.e("HomeFeedViewModel", "Failed to fetch (no network)", e)
            errorMessage = "无法连接到服务器"
        } catch (e: Exception) {
            Log.e("HomeFeedViewModel", "Failed to fetch", e)
            errorMessage = "获取推荐内容失败: ${e.message}"
        } finally {
            isLoading = false
        }
    }

    private fun processResponse(text: JsonObject) {
        try {
            val data = AccountData.decodeJson<FeedResponse>(text)
            feeds.addAll(data.data)

            val newItems = data.data.flatMap {
                (it as? GroupFeed)?.list ?: listOf(it)
            }.map { feed ->
                if (feed is AdvertisementFeed) {
                    return@map FeedDisplayItem(
                        title = "已屏蔽",
                        summary = "广告",
                        details = "广告",
                        feed = null,
                        isFiltered = true
                    )
                }
                feed as CommonFeed
                val filterReason = feed.target?.filterReason()

                if (filterReason != null) {
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

            displayItems.addAll(newItems)
        } catch (e: SerializationException) {
            Log.e("HomeFeedViewModel", "Failed to parse JSON: $text", e)
            errorMessage = "解析数据失败: ${e.message}"
        }
    }

    private suspend fun markItemsAsTouched(context: Context, httpClient: HttpClient = AccountData.httpClient(context)) {
        try {
            val untouchedAnswers = displayItems
                .filter { !it.isFiltered && it.feed?.target is Feed.AnswerTarget }

            if (untouchedAnswers.isNotEmpty()) {
                httpClient.post("https://www.zhihu.com/lastread/touch") {
                    header("x-requested-with", "fetch")
                    setBody(
                        MultiPartFormDataContent(
                            formData {
                                append("items", buildJsonArray {
                                    untouchedAnswers.forEach { item ->
                                        item.feed?.let { feed ->
                                            when (val target = feed.target) {
                                                is Feed.AnswerTarget -> {
                                                    add(buildJsonArray {
                                                        add("answer")
                                                        add(target.id)
                                                        add("touch")
                                                    })
                                                }

                                                else -> {}
                                            }
                                        }
                                    }
                                }.toString())
                            }
                        )
                    )
                }.let { response ->
                    if (!response.status.isSuccess()) {
                        Log.e("Browse-Touch", response.bodyAsText())
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("FeedViewModel", "Failed to mark items as touched", e)
        }
    }
}
