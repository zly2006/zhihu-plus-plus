package com.github.zly2006.zhihu.v2.viewmodel

import android.content.Context
import android.util.Log
import com.github.zly2006.zhihu.data.AccountData
import com.github.zly2006.zhihu.data.Feed
import com.github.zly2006.zhihu.data.target
import com.github.zly2006.zhihu.signFetchRequest
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.client.request.forms.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.add
import kotlinx.serialization.json.buildJsonArray

class HomeFeedViewModel : BaseFeedViewModel() {
    override suspend fun fetchFeeds(context: Context) {
        val httpClient = AccountData.httpClient(context)
        markItemsAsTouched(context, httpClient)

        val url = lastPaging?.next ?: "https://www.zhihu.com/api/v3/feed/topstory/recommend?desktop=true&limit=10"
        val response = httpClient.get(url) {
            signFetchRequest(context)
        }

        if (response.status == HttpStatusCode.OK) {
            val json = response.body<JsonObject>()
            val data = AccountData.decodeJson<FeedResponse>(json)
            processResponse(data, json["data"]!!)
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
