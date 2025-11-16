package com.github.zly2006.zhihu.viewmodel.za

import android.content.Context
import com.github.zly2006.zhihu.data.AccountData.data
import com.github.zly2006.zhihu.data.AccountData.json
import com.github.zly2006.zhihu.data.Feed
import com.github.zly2006.zhihu.ui.IHomeFeedViewModel
import com.github.zly2006.zhihu.ui.PREFERENCE_NAME
import com.github.zly2006.zhihu.viewmodel.feed.BaseFeedViewModel
import com.github.zly2006.zhihu.viewmodel.feed.HomeFeedViewModel
import io.ktor.client.HttpClient
import io.ktor.client.plugins.UserAgent
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.joinAll

class MixedHomeFeedViewModel :
    BaseFeedViewModel(),
    IHomeFeedViewModel {
    val android = AndroidHomeFeedViewModel()
    val web = HomeFeedViewModel()
    override val initialUrl: String
        get() = "https://api.zhihu.com/topstory/recommend"

    init {
        android.displayItems = this.displayItems
        web.displayItems = this.displayItems
    }

    override fun httpClient(context: Context): HttpClient {
        // 检查是否启用推荐内容时登录设置
        val preferences = context.getSharedPreferences(PREFERENCE_NAME, Context.MODE_PRIVATE)
        val loginForRecommendation = preferences.getBoolean("loginForRecommendation", true)
        if (!loginForRecommendation) {
            return HttpClient {
                install(ContentNegotiation) {
                    json(json)
                }
                install(UserAgent) {
                    agent = data.userAgent
                }
            }
        }
        return super.httpClient(context)
    }

    override suspend fun fetchFeeds(context: Context) {
        coroutineScope {
            listOf(
                async { android.fetchFeeds(context) },
                async { web.fetchFeeds(context) },
            ).joinAll()
        }
        isLoading = false
    }

    override suspend fun recordContentInteraction(context: Context, feed: Feed) {
        web.recordContentInteraction(context, feed)
    }
}
