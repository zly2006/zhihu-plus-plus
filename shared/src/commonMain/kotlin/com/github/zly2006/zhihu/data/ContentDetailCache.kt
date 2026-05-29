/*
 * Zhihu++ - Free & Ad-Free Zhihu client for Android.
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

package com.github.zly2006.zhihu.data

import com.github.zly2006.zhihu.navigation.Article
import com.github.zly2006.zhihu.navigation.ArticleType
import com.github.zly2006.zhihu.navigation.NavDestination
import com.github.zly2006.zhihu.navigation.Pin
import com.github.zly2006.zhihu.navigation.Question
import com.github.zly2006.zhihu.shared.data.DataHolder
import com.github.zly2006.zhihu.shared.data.ZhihuJson
import com.github.zly2006.zhihu.shared.util.Log
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.long
import kotlin.time.Clock

/**
 * 内容详情缓存管理器
 * 避免对同一内容发起重复的 API 请求
 */
object ContentDetailCache {
    private data class CacheEntry(
        val content: DataHolder.Content,
        val timestamp: Long,
    )

    private data class CacheKey(
        val contentType: String,
        val contentId: String,
    )

    private val cache = mutableMapOf<CacheKey, CacheEntry>()
    private val mutex = Mutex()
    private const val CACHE_EXPIRY_MS = 10 * 60 * 1000L // 10分钟
    private const val MAX_CACHE_SIZE = 100

    /**
     * 获取内容详情，优先从缓存读取
     */
    suspend fun getOrFetch(
        navDestination: NavDestination,
        fetcher: suspend (NavDestination) -> DataHolder.Content?,
    ): DataHolder.Content? {
        val (contentType, contentId) = extractContentInfo(navDestination) ?: return null
        val key = CacheKey(contentType, contentId)

        // 尝试从缓存读取
        mutex.withLock {
            cache[key]?.let { entry ->
                if (Clock.System.now().toEpochMilliseconds() - entry.timestamp < CACHE_EXPIRY_MS) {
                    Log.d("ContentDetailCache", "Cache hit for $contentType:$contentId")
                    return entry.content
                } else {
                    cache.remove(key)
                }
            }
        }

        // 缓存未命中，从 API 获取
        Log.d("ContentDetailCache", "Cache miss for $contentType:$contentId, fetching...")
        val content = fetchContent(navDestination, fetcher) ?: return null

        // 存入缓存
        mutex.withLock {
            // LRU 淘汰策略
            if (cache.size >= MAX_CACHE_SIZE) {
                val oldestKey = cache.entries.minByOrNull { it.value.timestamp }?.key
                oldestKey?.let { cache.remove(it) }
            }
            cache[key] = CacheEntry(content, Clock.System.now().toEpochMilliseconds())
        }

        return content
    }

    /**
     * 从 NavDestination 提取内容类型和 ID
     */
    private fun extractContentInfo(navDestination: NavDestination): Pair<String, String>? = when (navDestination) {
        is Article -> {
            val type = when (navDestination.type) {
                ArticleType.Answer -> "answer"
                ArticleType.Article -> "article"
            }
            Pair(type, navDestination.id.toString())
        }
        is Question -> {
            Pair("question", navDestination.questionId.toString())
        }
        is Pin -> {
            Pair("pin", navDestination.id.toString())
        }
        else -> null
    }

    /**
     * 根据 NavDestination 类型调用对应的 getContentDetail
     */
    private suspend fun fetchContent(
        navDestination: NavDestination,
        fetcher: suspend (NavDestination) -> DataHolder.Content?,
    ): DataHolder.Content? = when (navDestination) {
        is Article -> fetcher(navDestination)
        is Question -> fetcher(navDestination)
        is Pin -> fetcher(navDestination)
        else -> null
    }

    /**
     * 清除过期缓存
     */
    suspend fun clearExpired() {
        mutex.withLock {
            val now = Clock.System.now().toEpochMilliseconds()
            val expiredKeys = cache.entries
                .filter { (_, entry) -> now - entry.timestamp >= CACHE_EXPIRY_MS }
                .map { (key, _) -> key }
            expiredKeys.forEach { cache.remove(it) }
        }
    }

    /**
     * 清空所有缓存
     */
    suspend fun clearAll() {
        mutex.withLock {
            cache.clear()
        }
    }
}

fun normalizeArticleContentDetailJson(jo: JsonObject): JsonObject =
    normalizeLongIdContentDetailJson(jo)

fun normalizeQuestionDetailJson(jo: JsonObject): JsonObject =
    normalizeLongIdContentDetailJson(jo)

fun decodeArticleContentDetail(
    article: Article,
    json: JsonObject,
): DataHolder.Content {
    val normalizedJson = normalizeArticleContentDetailJson(json)
    return when (article.type) {
        ArticleType.Answer -> ZhihuJson.decodeJson<DataHolder.Answer>(normalizedJson)
        ArticleType.Article -> ZhihuJson.decodeJson<DataHolder.Article>(normalizedJson)
    }
}

fun decodeQuestionContentDetail(json: JsonObject): DataHolder.Question =
    ZhihuJson.decodeJson(normalizeQuestionDetailJson(json))

fun decodePinContentDetail(json: JsonObject): DataHolder.Pin =
    ZhihuJson.decodeJson(json)

private fun normalizeLongIdContentDetailJson(jo: JsonObject): JsonObject = buildJsonObject {
    jo.entries.forEach { (key, value) ->
        if (key == "id") {
            put(key, JsonPrimitive(value.jsonPrimitive.long))
        } else {
            put(key, value)
        }
    }
}

fun zhihuArticleContentDetailUrl(article: Article): String = when (article.type) {
    ArticleType.Article -> "https://www.zhihu.com/api/v4/articles/${article.id}?include=content,topics,paid_info,can_comment,excerpt,thanks_count,voteup_count,comment_count,visited_count,relationship,ip_info,relationship.vote,author.badge_v2"
    ArticleType.Answer -> "https://www.zhihu.com/api/v4/answers/${article.id}?include=content,paid_info,can_comment,excerpt,thanks_count,voteup_count,comment_count,visited_count,attachment,reaction,ip_info,pagination_info,question.topics,reaction.relation.voting,author.badge_v2"
}

fun zhihuQuestionContentDetailUrl(question: Question): String =
    "https://www.zhihu.com/api/v4/questions/${question.questionId}?include=read_count,visit_count,answer_count,voteup_count,comment_count,follower_count,detail,excerpt,author,relationship.is_following,topics"

fun zhihuPinContentDetailUrl(pin: Pin): String =
    "https://www.zhihu.com/api/v4/pins/${pin.id}"