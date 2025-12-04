package com.github.zly2006.zhihu.viewmodel.za

import android.content.Context
import android.util.Log
import androidx.core.net.toUri
import com.github.zly2006.zhihu.Article
import com.github.zly2006.zhihu.MainActivity
import com.github.zly2006.zhihu.checkForAd
import com.github.zly2006.zhihu.data.AccountData
import com.github.zly2006.zhihu.data.AccountData.json
import com.github.zly2006.zhihu.resolveContent
import com.github.zly2006.zhihu.ui.PREFERENCE_NAME
import com.github.zly2006.zhihu.viewmodel.feed.BaseFeedViewModel
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.UserAgent
import io.ktor.client.plugins.api.createClientPlugin
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.cookies.HttpCookies
import io.ktor.client.request.get
import io.ktor.http.decodeURLPart
import io.ktor.http.isSuccess
import io.ktor.serialization.kotlinx.json.json
import io.ktor.util.appendAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

private val ZHIHU_PP_ANDROID_HEADERS = createClientPlugin("ZhihuPPAndroidHeaders", { }) {
    onRequest { request, _ ->
        request.headers.appendAll(AccountData.ANDROID_HEADERS)
    }
}

class AndroidHomeFeedViewModel : BaseFeedViewModel() {
    override val initialUrl: String
        get() = "https://api.zhihu.com/topstory/recommend"

    override fun httpClient(context: Context): HttpClient {
        // 检查是否启用推荐内容时登录设置
        val preferences = context.getSharedPreferences(PREFERENCE_NAME, Context.MODE_PRIVATE)
        val loginForRecommendation = preferences.getBoolean("loginForRecommendation", true)

        return HttpClient {
            install(ContentNegotiation) {
                json(json)
            }
            install(UserAgent) {
                agent = AccountData.ANDROID_USER_AGENT
            }
            install(ZHIHU_PP_ANDROID_HEADERS)
            if (loginForRecommendation) {
                install(HttpCookies) {
                    storage = AccountData.cookieStorage(context, null)
                }
            }
        }
    }

    public override suspend fun fetchFeeds(context: Context) {
        try {
            val response = httpClient(context).get(initialUrl)
            if (response.status.isSuccess()) {
                val jojo = response.body<JsonObject>()
                val data = jojo["data"]?.jsonArray ?: throw IllegalStateException("No data found in response")
                data
                    .map { it.jsonObject }
                    .mapNotNull { card ->
                        if (card["type"]?.jsonPrimitive?.content != "ComponentCard") {
                            return@mapNotNull null
                        }
                        val route =
                            card["action"]!!
                                .jsonObject["parameter"]!!
                                .jsonPrimitive.content
                                .substringAfter("route_url=")
                        val routeDest = resolveContent(route.decodeURLPart().toUri()) ?: return@mapNotNull null
                        val children = card["children"]?.jsonArray?.map { it.jsonObject } ?: return@mapNotNull null
                        val title =
                            children.first { it["id"]?.jsonPrimitive?.content?.endsWith("Text") == true }["text"]!!.jsonPrimitive.content
                        val summary =
                            children.first { it["id"]?.jsonPrimitive?.content == "text_pin_summary" }["text"]!!.jsonPrimitive.content
                        val footerLine =
                            children.first { it["id"]?.jsonPrimitive?.content == "feed_footer" }["elements"]!!.jsonArray.map { it.jsonObject }
                        val footerText =
                            footerLine.first { it["type"]?.jsonPrimitive?.content == "Text" }["text"]!!.jsonPrimitive.content
                        val lineAuthor =
                            children
                                .first {
                                    it["style"]?.jsonPrimitive?.content == "LineAuthor_default"
                                }["elements"]!!
                                .jsonArray
                                .map { it.jsonObject }
                        val avatar =
                            lineAuthor
                                .first { it["style"]?.jsonPrimitive?.content == "Avatar_default" }["image"]!!
                                .jsonObject["url"]!!
                                .jsonPrimitive.content
                        val authorName =
                            lineAuthor.first { it["type"]?.jsonPrimitive?.content == "Text" }["text"]!!.jsonPrimitive.content
                        if (routeDest is Article) {
                            routeDest.authorName = authorName
                            routeDest.title = title
                            routeDest.avatarSrc = avatar
                        }
                        val task = coroutineScope {
                            launch {
                                if (!checkForAd(routeDest, context as MainActivity) && displayItems.none { it.navDestination == routeDest }) {
                                    displayItems.add(
                                        FeedDisplayItem(
                                            navDestination = routeDest,
                                            avatarSrc = avatar,
                                            authorName = authorName,
                                            summary = summary,
                                            title = title,
                                            details = "$footerText · 手机版推荐",
                                            feed = null,
                                        ),
                                    )
                                }
                            }
                        }
                        task
                    }.joinAll()
            }
        } catch (e: Exception) {
            Log.e(this::class.simpleName, "Failed to fetch feeds", e)
            throw e
        } finally {
            isLoading = false
        }
    }
}
