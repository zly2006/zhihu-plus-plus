package com.github.zly2006.zhihu.v2.viewmodel

import android.content.Context
import android.util.Log
import androidx.compose.runtime.mutableStateListOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.zly2006.zhihu.data.AccountData
import com.github.zly2006.zhihu.data.Feed
import com.github.zly2006.zhihu.signFetchRequest
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.client.request.forms.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.add
import kotlinx.serialization.json.buildJsonArray

class FeedViewModel : ViewModel() {
    val feeds = mutableStateListOf<Feed>()
    val displayItems = mutableStateListOf<FeedDisplayItem>()
    private var isLoading = false
    var errorMessage: String? = null
        private set
    
    @Suppress("PropertyName")
    @Serializable
    class FeedResponse(val data: List<Feed>, val fresh_text: String, val paging: JsonObject)
    
    data class FeedDisplayItem(
        val title: String,
        val summary: String,
        val details: String,
        val feed: Feed?,
        val isFiltered: Boolean = false
    )
    
    fun refresh(context: Context) {
        viewModelScope.launch {
            val size = displayItems.size
            displayItems.clear()
            feeds.clear()
            fetchFeeds(context)
        }
    }
    
    fun loadMore(context: Context) {
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
            
            // 首先标记已读
            markItemsAsTouched(context, httpClient)
            
            // 获取新内容
            val response = httpClient.get("https://www.zhihu.com/api/v3/feed/topstory/recommend?desktop=true&action=down&end_offset=${feeds.size}") {
                signFetchRequest(context)
            }
            
            if (response.status == HttpStatusCode.OK) {
                val text = response.body<JsonObject>()
                
                try {
                    val data = AccountData.decodeJson<FeedResponse>(text)
                    val newFeeds = data.data.filter { it.target !is Feed.AdvertTarget }
                    
                    feeds.addAll(newFeeds)
                    
                    val newItems = newFeeds.map { feed ->
                        val filterReason = feed.target.filterReason()
                        
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
                                        title = feed.target.javaClass.simpleName,
                                        summary = "Not Implemented",
                                        details = feed.target.detailsText(),
                                        feed = feed
                                    )
                                }
                            }
                        }
                    }
                    
                    displayItems.addAll(newItems)
                } catch (e: SerializationException) {
                    Log.e("FeedViewModel", "Failed to parse JSON: $text", e)
                    errorMessage = "解析数据失败: ${e.message}"
                }
            }
        } catch (e: Exception) {
            Log.e("FeedViewModel", "Failed to fetch", e)
            errorMessage = "获取推荐内容失败: ${e.message}"
        } finally {
            isLoading = false
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
                                            if (feed.target is Feed.AnswerTarget) {
                                                add(buildJsonArray {
                                                    add("answer")
                                                    add(feed.target.id)
                                                    add("touch")
                                                })
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
