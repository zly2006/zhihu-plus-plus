package com.github.zly2006.zhihu.v2.viewmodel

import android.content.Context
import com.github.zly2006.zhihu.MainActivity
import com.github.zly2006.zhihu.data.AccountData
import com.github.zly2006.zhihu.data.Feed
import io.ktor.client.call.*
import io.ktor.client.request.*
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

class FollowViewModel : BaseFeedViewModel() {
    private var offset = 0
    private var isEnd = false

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
                val response = loadFollowingFeeds(context as MainActivity, 0)
                val feeds = response.data
                displayItems.addAll(feeds.map { createDisplayItem(it) })
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
                val response = loadFollowingFeeds(context as MainActivity, offset + 20)
                offset += 20
                val feeds = response.data
                displayItems.addAll(feeds.map { createDisplayItem(it) })
                isEnd = response.paging.is_end
            } catch (e: Exception) {
                errorMessage = e.message
            } finally {
                isLoading = false
            }
        }
    }

    private suspend fun loadFollowingFeeds(activity: MainActivity, offset: Int): FeedResponse {
        val client = AccountData.httpClient(activity)
        val url = "https://api.zhihu.com/moments/recommend?limit=20&offset=$offset"
        val sign = activity.signRequest96(url)
        
        return client.get(url) {
            header("x-zse-96", sign)
        }.body()
    }

    private fun createDisplayItem(feed: Feed): FeedDisplayItem {
        return when (val target = feed.target) {
            is Feed.AnswerTarget -> FeedDisplayItem(
                title = target.question.title,
                summary = target.excerpt,
                details = "${target.author.name} 回答了问题",
                feed = feed,
                avatarSrc = target.author.avatar_url
            )
            is Feed.ArticleTarget -> FeedDisplayItem(
                title = target.title,
                summary = target.excerpt,
                details = "${target.author.name} 发表了文章",
                feed = feed,
                avatarSrc = target.author.avatar_url
            )
            else -> FeedDisplayItem(
                title = "未知内容",
                summary = "",
                details = "",
                feed = feed
            )
        }
    }
}
