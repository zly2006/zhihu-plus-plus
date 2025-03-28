package com.github.zly2006.zhihu.v2.viewmodel

import android.content.Context
import com.github.zly2006.zhihu.MainActivity
import com.github.zly2006.zhihu.data.AccountData
import io.ktor.client.call.*
import io.ktor.client.request.*
import kotlinx.serialization.json.JsonObject

class FollowViewModel : BaseFeedViewModel() {
    private var offset = 0

    override suspend fun fetchFeeds(context: Context) {
        val url = lastPaging?.next ?: "https://www.zhihu.com/api/v3/moments?limit=10&desktop=true"
        val sign = (context as MainActivity).signRequest96(url)
        val client = AccountData.httpClient(context)

        val json = client.get(url) {
            header("x-zse-96", sign)
        }.body<JsonObject>()

        val data = AccountData.decodeJson<FeedResponse>(json)
        offset += 20
        processResponse(data, json["data"]!!)
    }
}
