package com.github.zly2006.zhihu.v2.viewmodel

import android.content.Context
import com.github.zly2006.zhihu.data.AccountData
import com.github.zly2006.zhihu.data.Feed
import com.github.zly2006.zhihu.data.target
import com.github.zly2006.zhihu.signFetchRequest
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.http.*
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
            processResponse(data, json["data"]!!.jsonArray)
        }
    }

    override fun createDisplayItem(feed: Feed): FeedDisplayItem {
        val target = feed.target
        if (target is Feed.AnswerTarget) {
            return FeedDisplayItem(
                title = target.author.name,
                summary = target.excerpt,
                details = target.detailsText(),
                feed = feed,
                avatarSrc = target.author.avatar_url
            )
        }
        return super.createDisplayItem(feed)
    }
}
