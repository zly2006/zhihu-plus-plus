/*
 * Zhihu++ - Free & Ad-Free Zhihu client for all platforms.
 * Copyright (C) 2024-2026, zly2006 <i@zly2006.me>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation (version 3 only).
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.github.zly2006.zhihu.viewmodel.za
import androidx.lifecycle.viewModelScope
import com.github.zly2006.zhihu.navigation.Article
import com.github.zly2006.zhihu.navigation.Pin
import com.github.zly2006.zhihu.navigation.resolveContent
import com.github.zly2006.zhihu.shared.data.CommonFeed
import com.github.zly2006.zhihu.shared.data.DataHolder
import com.github.zly2006.zhihu.shared.data.Feed
import com.github.zly2006.zhihu.shared.data.FeedDisplayItem
import com.github.zly2006.zhihu.shared.data.Person
import com.github.zly2006.zhihu.shared.data.ZhihuJson
import com.github.zly2006.zhihu.shared.data.target
import com.github.zly2006.zhihu.shared.data.toFeedDisplayItemNavDestinationJson
import com.github.zly2006.zhihu.viewmodel.ContentInteractionEnvironment
import com.github.zly2006.zhihu.viewmodel.PaginationEnvironment
import com.github.zly2006.zhihu.viewmodel.feed.BaseFeedViewModel
import com.github.zly2006.zhihu.viewmodel.feed.HomeFeedInteractionViewModel
import com.github.zly2006.zhihu.viewmodel.feed.replaceHomeFeedItemsWithFilteredResult
import com.github.zly2006.zhihu.viewmodel.postSigned
import io.ktor.client.call.body
import io.ktor.client.request.forms.MultiPartFormDataContent
import io.ktor.client.request.forms.formData
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.setBody
import io.ktor.http.decodeURLPart
import io.ktor.http.isSuccess
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

class AndroidHomeFeedViewModel :
    BaseFeedViewModel(),
    HomeFeedInteractionViewModel {
    override val initialUrl: String
        get() = "https://api.zhihu.com/topstory/recommend"

    public override suspend fun fetchFeeds(environment: PaginationEnvironment) {
        try {
            val response = environment.mobileHomeFeedHttpClient().get(lastPaging?.next ?: initialUrl)
            if (response.status.isSuccess()) {
                val jojo = response.body<JsonObject>()
                val data = jojo["data"]?.jsonArray ?: throw IllegalStateException("No data found in response")

                // 收集所有待显示的项目
                val itemsToDisplay = mutableListOf<FeedDisplayItem>()

                data
                    .map { it.jsonObject }
                    .forEach { card ->
                        try {
                            val displayItem = parseMobileHomeFeedDisplayItem(card) ?: return@forEach
                            itemsToDisplay.add(displayItem)
                        } catch (e: Exception) {
                            environment.logDecodeFailure("AndroidHomeFeedViewModel", card, e)
                        }
                    }

                // 前台先做本地已读过滤，再立即展示
                val filterResult = environment.applyHomeFeedFilters(itemsToDisplay)
                if (!filterResult.reverseBlock) {
                    withContext(Dispatchers.Main) {
                        addDisplayItems(filterResult.foregroundItems)
                    }
                }

                if (filterResult.reverseBlock) {
                    addDisplayItems(filterResult.filteredItems)
                }

                // 移除被过滤的条目，并更新已保留条目的 raw 内容
                withContext(Dispatchers.Main) {
                    displayItems.replaceHomeFeedItemsWithFilteredResult(filterResult)
                    latestLoadedDisplayItems.value = filterResult.filteredItems
                }

                lastPaging = if ("paging" in jojo) {
                    ZhihuJson.decodeJson(jojo["paging"]!!)
                } else {
                    null
                }
            }
        } catch (e: Exception) {
            if (e !is CancellationException) {
                environment.handleMobileHomeFeedFailure(e)
            }
            throw e
        } finally {
            isLoading = false
        }
    }

    override suspend fun recordContentInteraction(environment: ContentInteractionEnvironment, feed: Feed) {
        // Android 版本暂不记录交互
    }

    override fun onUiContentClick(environment: ContentInteractionEnvironment, feed: Feed, item: FeedDisplayItem) {
        viewModelScope.launch(Dispatchers.Default) {
            if (environment.authenticatedCookies()["d_c0"] != null) {
                val payloadItem = when (val target = feed.target) {
                    is Feed.AnswerTarget -> listOf("answer", target.id.toString(), "read")
                    is Feed.ArticleTarget -> listOf("article", target.id.toString(), "read")
                    is Feed.PinTarget -> listOf("pin", target.id.toString(), "read")
                    else -> null
                }
                if (payloadItem != null) {
                    environment.postSigned("https://www.zhihu.com/lastread/touch") {
                        header("x-requested-with", "fetch")
                        setBody(
                            MultiPartFormDataContent(
                                formData {
                                    append("items", ZhihuJson.json.encodeToString(listOf(payloadItem)))
                                },
                            ),
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalEncodingApi::class)
fun parseMobileHomeFeedDisplayItem(card: JsonObject): FeedDisplayItem? {
    if (card["type"]?.jsonPrimitive?.content != "ComponentCard") {
        return null
    }
    val route =
        card["action"]!!
            .jsonObject["parameter"]!!
            .jsonPrimitive.content
            .substringAfter("route_url=")
    val routeUrl = route.decodeURLPart()
    val routeDest = resolveContent(routeUrl) ?: return null
    val children = card["children"]?.jsonArray?.map { it.jsonObject } ?: return null
    val extra = if (routeDest is Pin) {
        card["extra"]?.let { ZhihuJson.decodeJson<MobileHomeCardExtra>(it) }
    } else {
        null
    }
    val originalContent = extra
        ?.businessExtMap
        ?.oriContent
        ?.takeIf { it.isNotBlank() }
        ?.let { encoded ->
            runCatching {
                ZhihuJson.decodeJson<MobileHomeOriginalContent>(
                    ZhihuJson.json.parseToJsonElement(Base64.Default.decode(encoded).decodeToString()),
                )
            }.getOrNull()
        }
    val title = if (routeDest is Pin) {
        extra
            ?.passthroughInfo
            ?.content
            ?.title
            ?.takeIf { it.isNotBlank() }
            ?: children
                .firstOrNull { it["id"]?.jsonPrimitive?.content == "Text" }
                ?.get("text")
                ?.jsonPrimitive
                ?.content
                .orEmpty()
    } else {
        children.joStrMatch("id", "Text")["text"]!!.jsonPrimitive.content
    }
    val summary = children.joStrMatch("id", "text_pin_summary")["text"]!!.jsonPrimitive.content
    val footer = children.filter { it["type"]!!.jsonPrimitive.content == "Line" }.getOrNull(1) ?: return null
    val footerLine = footer["elements"]!!.jsonArray.map { it.jsonObject }
    val voteUp = footerLine.firstOrNull { it["reaction"]?.jsonPrimitive?.content == "Vote" }
    val comment = footerLine.firstOrNull { it["reaction"]?.jsonPrimitive?.content == "Comment" }
    val collect = footerLine.firstOrNull { it["reaction"]?.jsonPrimitive?.content == "Collect" }
    val footerText = if (voteUp != null && comment != null && collect != null) {
        val voteUpCount = voteUp["count"]!!.jsonPrimitive.int
        val commentCount = comment["count"]!!.jsonPrimitive.int
        val collectCount = collect["count"]!!.jsonPrimitive.int
        "$voteUpCount 赞同 · $commentCount 评论 · $collectCount 收藏"
    } else {
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
    val feed = if (routeDest is Pin) {
        val author = extra?.passthroughInfo?.author
        CommonFeed(
            id = card["id"]?.jsonPrimitive?.content.orEmpty(),
            target = Feed.PinTarget(
                id = routeDest.id,
                url = routeUrl,
                author = Person(
                    id = author?.id.orEmpty(),
                    url = author?.url.orEmpty(),
                    userType = "people",
                    urlToken = author?.urlToken,
                    name = authorName,
                    headline = "",
                    avatarUrl = avatar,
                    isFollowing = author?.isFollowing == true,
                    isFollowed = author?.isFollowed == true,
                ),
                content = buildList {
                    add(DataHolder.Pin.ContentText(title = title, content = summary))
                    originalContent
                        ?.mediaInfo
                        ?.images
                        .orEmpty()
                        .filter { it.url.isNotBlank() }
                        .forEachIndexed { index, image ->
                            add(
                                DataHolder.Pin.ContentImage(
                                    url = image.url,
                                    thumbnail = extra
                                        ?.businessExtMap
                                        ?.images
                                        ?.getOrNull(index)
                                        ?.url
                                        .orEmpty(),
                                    width = image.width,
                                    height = image.height,
                                ),
                            )
                        }
                },
                excerptTitle = summary,
            ),
        )
    } else {
        null
    }

    return FeedDisplayItem(
        navDestinationJson = routeDest.toFeedDisplayItemNavDestinationJson(),
        avatarSrc = avatar,
        authorName = authorName,
        summary = summary,
        title = title,
        details = "$footerText · 手机版推荐",
        feed = feed,
    )
}

@Serializable
private data class MobileHomeCardExtra(
    val businessExtMap: MobileHomeBusinessExtMap = MobileHomeBusinessExtMap(),
    val passthroughInfo: MobileHomePassthroughInfo? = null,
)

@Serializable
private data class MobileHomeBusinessExtMap(
    val oriContent: String = "",
    val images: List<MobileHomeImage> = emptyList(),
)

@Serializable
private data class MobileHomePassthroughInfo(
    val author: MobileHomeAuthor? = null,
    val content: MobileHomeContent? = null,
)

@Serializable
private data class MobileHomeAuthor(
    val id: String = "",
    val url: String = "",
    val urlToken: String? = null,
    val isFollowing: Boolean = false,
    val isFollowed: Boolean = false,
)

@Serializable
private data class MobileHomeContent(
    val title: String? = null,
)

@Serializable
private data class MobileHomeOriginalContent(
    val mediaInfo: MobileHomeMediaInfo? = null,
)

@Serializable
private data class MobileHomeMediaInfo(
    val images: List<MobileHomeImage> = emptyList(),
)

@Serializable
private data class MobileHomeImage(
    val url: String = "",
    val width: Int = 0,
    val height: Int = 0,
)

/**
 * Find the first JsonObject in the list where the value associated with [key] matches [value].
 */
@Suppress("NOTHING_TO_INLINE")
private inline fun List<JsonObject>.joStrMatch(key: String, value: String): JsonObject =
    this.firstOrNull { it[key]?.jsonPrimitive?.content == value }
        ?: throw IllegalStateException("No matching JsonObject found for $key = $value: $this")
