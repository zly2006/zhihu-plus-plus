package com.github.zly2006.zhihu.data

import android.content.Context
import android.util.Log
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

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
        context: Context,
        navDestination: com.github.zly2006.zhihu.NavDestination,
    ): DataHolder.Content? {
        val (contentType, contentId) = extractContentInfo(navDestination) ?: return null
        val key = CacheKey(contentType, contentId)

        // 尝试从缓存读取
        mutex.withLock {
            cache[key]?.let { entry ->
                if (System.currentTimeMillis() - entry.timestamp < CACHE_EXPIRY_MS) {
                    Log.d("ContentDetailCache", "Cache hit for $contentType:$contentId")
                    return entry.content
                } else {
                    cache.remove(key)
                }
            }
        }

        // 缓存未命中，从 API 获取
        Log.d("ContentDetailCache", "Cache miss for $contentType:$contentId, fetching...")
        val content = fetchContent(context, navDestination) ?: return null

        // 存入缓存
        mutex.withLock {
            // LRU 淘汰策略
            if (cache.size >= MAX_CACHE_SIZE) {
                val oldestKey = cache.entries.minByOrNull { it.value.timestamp }?.key
                oldestKey?.let { cache.remove(it) }
            }
            cache[key] = CacheEntry(content, System.currentTimeMillis())
        }

        return content
    }

    /**
     * 从 NavDestination 提取内容类型和 ID
     */
    private fun extractContentInfo(navDestination: com.github.zly2006.zhihu.NavDestination): Pair<String, String>? = when (navDestination) {
        is com.github.zly2006.zhihu.Article -> {
            val type = when (navDestination.type) {
                com.github.zly2006.zhihu.ArticleType.Answer -> "answer"
                com.github.zly2006.zhihu.ArticleType.Article -> "article"
            }
            Pair(type, navDestination.id.toString())
        }
        is com.github.zly2006.zhihu.Question -> {
            Pair("question", navDestination.questionId.toString())
        }
        is com.github.zly2006.zhihu.Pin -> {
            Pair("pin", navDestination.id.toString())
        }
        else -> null
    }

    /**
     * 根据 NavDestination 类型调用对应的 getContentDetail
     */
    private suspend fun fetchContent(
        context: Context,
        navDestination: com.github.zly2006.zhihu.NavDestination,
    ): DataHolder.Content? = when (navDestination) {
        is com.github.zly2006.zhihu.Article -> DataHolder.getContentDetail(context, navDestination)
        is com.github.zly2006.zhihu.Question -> DataHolder.getContentDetail(context, navDestination)
        is com.github.zly2006.zhihu.Pin -> DataHolder.getContentDetail(context, navDestination)
        else -> null
    }

    /**
     * 清除过期缓存
     */
    suspend fun clearExpired() {
        mutex.withLock {
            val now = System.currentTimeMillis()
            cache.entries.removeIf { (_, entry) ->
                now - entry.timestamp >= CACHE_EXPIRY_MS
            }
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
