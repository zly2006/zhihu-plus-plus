package com.github.zly2006.zhihu.v2.viewmodel

import android.content.Context
import com.github.zly2006.zhihu.MainActivity
import com.github.zly2006.zhihu.data.AccountData
import io.ktor.client.call.*
import io.ktor.client.request.*
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonArray

class FollowViewModel : BaseFeedViewModel() {
    private var offset = 0
    private var isEnd = false
    private var lastPaging: Paging? = null

    @OptIn(DelicateCoroutinesApi::class)
    override fun refresh(context: Context) {
        if (isLoading) return
        isLoading = true
        errorMessage = null

        offset = 0
        isEnd = false
        displayItems.clear()

        GlobalScope.launch {
            try {
                val data = loadFollowingFeeds(context as MainActivity, 0)
                val response = AccountData.decodeJson<FeedResponse>(data)
                debugData.addAll(data["data"]!!.jsonArray)
                val feeds = response.data
                displayItems.addAll(feeds.flatten().map { createDisplayItem(it) })
                isEnd = response.paging.is_end
            } catch (e: Exception) {
                errorMessage = e.message
            } finally {
                isLoading = false
            }
        }
    }

    @OptIn(DelicateCoroutinesApi::class)
    override fun loadMore(context: Context) {
        if (isLoading || isEnd) return
        isLoading = true

        GlobalScope.launch {
            try {
                val data = loadFollowingFeeds(context as MainActivity, offset + 20)
                offset += 20
                debugData.addAll(data["data"]!!.jsonArray)
                val response = AccountData.decodeJson<FeedResponse>(data)
                feeds.addAll(response.data)
                displayItems.addAll(response.data.flatten().map { createDisplayItem(it) })
                isEnd = response.paging.is_end
            } catch (e: Exception) {
                errorMessage = e.message
            } finally {
                isLoading = false
            }
        }
    }

    private suspend fun loadFollowingFeeds(activity: MainActivity, offset: Int): JsonObject {
        val client = AccountData.httpClient(activity)
        val url = lastPaging?.next ?: "https://www.zhihu.com/api/v3/moments?limit=10&desktop=true"
        val sign = activity.signRequest96(url)

        return client.get(url) {
            header("x-zse-96", sign)
        }.body()
    }
}
