package com.github.zly2006.zhihu.viewmodel.za

import android.content.Context
import android.util.Log
import android.widget.Toast
import androidx.core.net.toUri
import com.github.zly2006.zhihu.Article
import com.github.zly2006.zhihu.data.AccountData
import com.github.zly2006.zhihu.data.AccountData.json
import com.github.zly2006.zhihu.resolveContent
import com.github.zly2006.zhihu.ui.PREFERENCE_NAME
import com.github.zly2006.zhihu.viewmodel.feed.BaseFeedViewModel
import com.github.zly2006.zhihu.viewmodel.filter.ContentFilterExtensions
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.int
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

    /**
     * Find the first JsonObject in the list where the value associated with [key] matches [value].
     */
    @Suppress("NOTHING_TO_INLINE")
    private inline fun List<JsonObject>.joStrMatch(key: String, value: String): JsonObject =
        this.firstOrNull { it[key]?.jsonPrimitive?.content == value }
            ?: throw IllegalStateException("No matching JsonObject found for $key = $value: $this")

    public override suspend fun fetchFeeds(context: Context) {
        try {
            val response = httpClient(context).get(initialUrl)
            if (response.status.isSuccess()) {
                val jojo = response.body<JsonObject>()
                val data = jojo["data"]?.jsonArray ?: throw IllegalStateException("No data found in response")

                // 收集所有待显示的项目
                val itemsToDisplay = mutableListOf<FeedDisplayItem>()

                data
                    .map { it.jsonObject }
                    .forEach { card ->
                        try {
                            if (card["type"]?.jsonPrimitive?.content != "ComponentCard") {
                                return@forEach
                            }
                            val route =
                                card["action"]!!
                                    .jsonObject["parameter"]!!
                                    .jsonPrimitive.content
                                    .substringAfter("route_url=")
                            val routeDest = resolveContent(route.decodeURLPart().toUri()) ?: return@forEach
                            val children = card["children"]?.jsonArray?.map { it.jsonObject } ?: return@forEach
                            val title = children.joStrMatch("id", "Text")["text"]!!.jsonPrimitive.content
                            val summary = children.joStrMatch("id", "text_pin_summary")["text"]!!.jsonPrimitive.content
                            val footer = children.filter { it["type"]!!.jsonPrimitive.content == "Line" }.getOrNull(1) ?: return@forEach
                            val footerText = if (footer["style"]!!.jsonPrimitive.content == "LineFooterReaction_feed_v3") {
                                val footerLine = footer["elements"]!!.jsonArray.map { it.jsonObject }
                                val voteUp = footerLine.joStrMatch("reaction", "Vote")["count"]!!.jsonPrimitive.int
                                val comment = footerLine.joStrMatch("reaction", "Comment")["count"]!!.jsonPrimitive.int
                                val collect = footerLine.joStrMatch("reaction", "Collect")["count"]!!.jsonPrimitive.int
                                "$voteUp 赞同 · $comment 评论 · $collect 收藏"
                            } else {
                                val footerLine = footer["elements"]!!.jsonArray.map { it.jsonObject }
                                footerLine.joStrMatch("type", "Text")["text"]!!.jsonPrimitive.content
                            }
                            val lineAuthor =
                                children
                                    .first {
                                        it["style"]!!.jsonPrimitive.content.startsWith("RecommendAuthorLine") ||
                                                it["style"]!!.jsonPrimitive.content.startsWith("LineAuthor_default")
                                    }["elements"]!!
                                    .jsonArray
                                    .map { it.jsonObject }
                            val avatar = lineAuthor
                                .joStrMatch("style", "Avatar_default")["image"]!!
                                .jsonObject["url"]!!
                                .jsonPrimitive.content
                            val authorName = lineAuthor.joStrMatch("type", "Text")["text"]!!.jsonPrimitive.content
                            if (routeDest is Article) {
                                routeDest.authorName = authorName
                                routeDest.title = title
                                routeDest.avatarSrc = avatar
                            }

                            itemsToDisplay.add(
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
                        } catch (e: Exception) {
                            Log.e("AndroidHomeFeedViewModel", "Failed to process card: $card", e)
                        }
                    }

                // 应用内容过滤
                val filteredItems = ContentFilterExtensions.applyContentFilterToDisplayItems(context, itemsToDisplay)

                // 将过滤后的内容添加到显示列表
                filteredItems
                    .map { item ->
                        coroutineScope {
                            launch(Dispatchers.Main) {
                                if (displayItems.none { it.navDestination == item.navDestination }) {
                                    displayItems.add(item)
                                }
                            }
                        }
                    }.joinAll()
            }
        } catch (e: Exception) {
            Log.e(this::class.simpleName, "Failed to fetch feeds", e)
            context.mainExecutor.execute {
                Toast.makeText(context, "安卓端推荐加载失败: ${e.message}", Toast.LENGTH_SHORT).show()
            }
            throw e
        } finally {
            isLoading = false
        }
    }
}
