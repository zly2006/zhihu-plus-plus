package com.github.zly2006.zhihu.viewmodel.za

import android.content.Context
import android.util.Log
import androidx.core.net.toUri
import com.github.zly2006.zhihu.Article
import com.github.zly2006.zhihu.data.AccountData
import com.github.zly2006.zhihu.resolveContent
import com.github.zly2006.zhihu.viewmodel.feed.BaseFeedViewModel
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.http.*
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

class AndroidHomeFeedViewModel: BaseFeedViewModel() {
    override val initialUrl: String
        get() = "https://api.zhihu.com/topstory/recommend"

    override suspend fun fetchFeeds(context: Context) {
        try {
            val response = httpClient(context).get(initialUrl) {
                AccountData.ANDROID_HEADERS.forEach { (key, value) ->
                    header(key, value)
                }
                header(
                    HttpHeaders.UserAgent,
                    "com.zhihu.android/Futureve/10.61.0 Mozilla/5.0 (Linux; Android 12; sdk_gphone64_arm64 Build/SE1A.220630.001.A1; wv) AppleWebKit/537.36 (KHTML, like Gecko) Version/4.0 Chrome/57.0.1000.10 Mobile Safari/537.36"
                )
            }
            if (response.status.isSuccess()) {
                val jojo = response.body<JsonObject>()
                val data = jojo["data"]?.jsonArray ?: throw IllegalStateException("No data found in response")
                data.map { it.jsonObject }.forEach { card ->
                    if (card["type"]?.jsonPrimitive?.content != "ComponentCard") {
                        return@forEach
                    }
                    val route =
                        card["action"]!!.jsonObject["parameter"]!!.jsonPrimitive.content.substringAfter("route_url=")
                    val routeDest = resolveContent(route.decodeURLPart().toUri()) ?: return@forEach
                    val children = card["children"]?.jsonArray?.map { it.jsonObject } ?: return@forEach
                    val title =
                        children.first { it["id"]?.jsonPrimitive?.content?.endsWith("Text") == true }["text"]!!.jsonPrimitive.content
                    val summary =
                        children.first { it["id"]?.jsonPrimitive?.content == "text_pin_summary" }["text"]!!.jsonPrimitive.content
                    val footerLine =
                        children.first { it["id"]?.jsonPrimitive?.content == "feed_footer" }["elements"]!!.jsonArray.map { it.jsonObject }
                    val footerText =
                        footerLine.first { it["type"]?.jsonPrimitive?.content == "Text" }["text"]!!.jsonPrimitive.content
                    val lineAuthor =
                        children.first { it["style"]?.jsonPrimitive?.content == "LineAuthor_default" }["elements"]!!.jsonArray.map { it.jsonObject }
                    val avatar =
                        lineAuthor.first { it["style"]?.jsonPrimitive?.content == "Avatar_default" }["image"]!!.jsonObject["url"]!!.jsonPrimitive.content
                    val authorName =
                        lineAuthor.first { it["type"]?.jsonPrimitive?.content == "Text" }["text"]!!.jsonPrimitive.content
                    if (routeDest is Article) {
                        routeDest.authorName = authorName
                        routeDest.title = title
                        routeDest.avatarSrc = avatar
                    }
                    displayItems.add(
                        FeedDisplayItem(
                            navDestination = routeDest,
                            avatarSrc = avatar,
                            summary = summary,
                            title = title,
                            details = "$footerText · 手机版推荐",
                            feed = null,
                        )
                    )
                }
            }
        } catch (e: Exception) {
            Log.e(this::class.simpleName, "Failed to fetch feeds", e)
            throw e
        } finally {
            isLoading = false
        }
    }
}
