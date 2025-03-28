package com.github.zly2006.zhihu.v2.viewmodel

import android.content.Context
import android.util.Log
import com.github.zly2006.zhihu.data.AccountData
import com.github.zly2006.zhihu.data.Feed
import com.github.zly2006.zhihu.data.QuestionFeedCard
import com.github.zly2006.zhihu.signFetchRequest
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.http.*
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonArray

class QuestionFeedViewModel(private val questionId: Long) : BaseFeedViewModel() {
    override suspend fun fetchFeeds(context: Context) {
        val httpClient = AccountData.httpClient(context)
        val response = httpClient.get(
            lastPaging?.next
                ?: "https://www.zhihu.com/api/v4/questions/$questionId/feeds?limit=20&offset=${feeds.size}"
        ) {
            signFetchRequest(context)
        }

        if (response.status == HttpStatusCode.OK) {
            val json = response.body<JsonObject>()
            val data = AccountData.decodeJson<FeedResponse>(json)
            processResponse(data, json["data"]!!)
        }
    }

    private fun processResponse(data: FeedResponse, rawData: JsonObject) {
        try {
            debugData.addAll(rawData.jsonArray)
            val newFeeds = data.data.filterIsInstance<QuestionFeedCard>()

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
                            title = feed.target.javaClass.simpleName,
                            summary = "Not Implemented",
                            details = feed.target.detailsText(),
                            feed = feed
                        )
                    }
                }
            }

            displayItems.addAll(newItems)
        } catch (e: SerializationException) {
            Log.e("QuestionFeedViewModel", "Failed to parse JSON: ${rawData}", e)
            errorMessage = "解析数据失败: ${e.message}"
        }
    }
}
