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

package com.github.zly2006.zhihu.viewmodel.filter

import android.content.Context
import android.content.SharedPreferences
import android.os.Build
import android.util.Log
import android.widget.Toast
import com.github.zly2006.zhihu.data.AdvertisementFeed
import com.github.zly2006.zhihu.data.ContentDetailCache
import com.github.zly2006.zhihu.data.DataHolder
import com.github.zly2006.zhihu.data.target
import com.github.zly2006.zhihu.navigation.Article
import com.github.zly2006.zhihu.navigation.ArticleType
import com.github.zly2006.zhihu.navigation.Pin
import com.github.zly2006.zhihu.navigation.Question
import com.github.zly2006.zhihu.nlp.BlockedKeywordRepository
import com.github.zly2006.zhihu.ui.PREFERENCE_NAME
import com.github.zly2006.zhihu.viewmodel.feed.BaseFeedViewModel.FeedDisplayItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import org.jsoup.Jsoup

/**
 * 内容过滤扩展工具
 * 提供简化的API用于在UI层集成内容过滤功能
 */
object ContentFilterExtensions {
    /**
     * 过滤所需的内容数据
     */
    data class FilterableContent(
        val title: String,
        val summary: String?,
        val content: String?,
        val authorName: String?,
        val authorId: String?,
        val contentId: String,
        val contentType: String,
        val raw: DataHolder.Content,
        val isFollowing: Boolean = false,
        val questionId: Long? = null,
        val url: String? = null,
        val feedJson: String? = null,
        val navDestinationJson: String? = null,
    )

    /**
     * 检查是否启用了内容过滤功能
     */
    fun isContentFilterEnabled(context: Context): Boolean {
        val preferences = context.getSharedPreferences(PREFERENCE_NAME, Context.MODE_PRIVATE)
        return preferences.getBoolean("enableContentFilter", true)
    }

    /**
     * 检查是否启用了关键词屏蔽功能
     */
    fun isKeywordBlockingEnabled(context: Context): Boolean {
        val preferences = context.getSharedPreferences(PREFERENCE_NAME, Context.MODE_PRIVATE)
        return preferences.getBoolean("enableKeywordBlocking", true)
    }

    /**
     * 检查是否启用了NLP语义屏蔽功能
     */
    fun isNLPBlockingEnabled(context: Context): Boolean {
        val preferences = context.getSharedPreferences(PREFERENCE_NAME, Context.MODE_PRIVATE)
        return preferences.getBoolean("enableNLPBlocking", true)
    }

    /**
     * 获取NLP相似度阈值
     */
    fun getNLPSimilarityThreshold(context: Context): Double {
        val preferences = context.getSharedPreferences(PREFERENCE_NAME, Context.MODE_PRIVATE)
        return preferences.getFloat("nlpSimilarityThreshold", 0.8f).toDouble()
    }

    /**
     * 检查是否启用了用户屏蔽功能
     */
    fun isUserBlockingEnabled(context: Context): Boolean {
        val preferences = context.getSharedPreferences(PREFERENCE_NAME, Context.MODE_PRIVATE)
        return preferences.getBoolean("enableUserBlocking", true)
    }

    /**
     * 检查是否启用了主题屏蔽功能
     */
    fun isTopicBlockingEnabled(context: Context): Boolean {
        val preferences = context.getSharedPreferences(PREFERENCE_NAME, Context.MODE_PRIVATE)
        return preferences.getBoolean("enableTopicBlocking", true)
    }

    /**
     * 获取主题屏蔽阈值
     */
    fun getTopicBlockingThreshold(context: Context): Int {
        val preferences = context.getSharedPreferences(PREFERENCE_NAME, Context.MODE_PRIVATE)
        return preferences.getInt("topicBlockingThreshold", 1)
    }

    /**
     * 在内容显示时记录展示次数
     * 建议在RecyclerView的onBindViewHolder或Compose的LaunchedEffect中调用
     */
    suspend fun recordContentDisplay(context: Context, targetType: String, targetId: String) {
        if (!isContentFilterEnabled(context)) return

        withContext(Dispatchers.IO) {
            try {
                val filterManager = ContentFilterManager.getInstance(context)
                filterManager.recordContentView(targetType, targetId)
            } catch (e: Exception) {
                Log.e("ContentFilterExtensions", "Failed to record content display", e)
            }
        }
    }

    /**
     * 在用户与内容交互时记录交互行为
     * 建议在用户点击、点赞、评论等操作时调用
     */
    suspend fun recordContentInteraction(context: Context, targetType: String, targetId: String) {
        if (!isContentFilterEnabled(context)) return

        withContext(Dispatchers.IO) {
            try {
                val filterManager = ContentFilterManager.getInstance(context)
                filterManager.recordContentInteraction(targetType, targetId)
            } catch (e: Exception) {
                Log.e("ContentFilterExtensions", "Failed to record content interaction", e)
            }
        }
    }

    /**
     * 定期清理过期数据（建议在应用启动时调用）
     */
    suspend fun performMaintenanceCleanup(context: Context) {
        if (!isContentFilterEnabled(context)) return

        withContext(Dispatchers.IO) {
            try {
                val filterManager = ContentFilterManager.getInstance(context)
                filterManager.cleanupOldData()
            } catch (e: Exception) {
                Log.e("ContentFilterExtensions", "Failed to perform maintenance cleanup", e)
            }
        }
    }

    /**
     * 前台应用已读过滤（仅访问本地数据库，不依赖网络）。
     * 规则：发布者未被关注且已读过则过滤，并写入屏蔽历史。
     */
    suspend fun applyForegroundReadFilterToDisplayItems(
        context: Context,
        items: List<FeedDisplayItem>,
    ): List<FeedDisplayItem> = withContext(Dispatchers.IO) {
        try {
            val preferences = context.getSharedPreferences(PREFERENCE_NAME, Context.MODE_PRIVATE)
            if (preferences.getBoolean("reverseBlock", false) || !isContentFilterEnabled(context)) {
                return@withContext items
            }

            val filterManager = ContentFilterManager.getInstance(context)
            val itemIdentityPairs = items.map { item -> item to item.resolveContentIdentity() }
            val viewedContentIds = filterManager.getAlreadyViewedContentIds(
                itemIdentityPairs.map { (_, identity) -> identity.type to identity.id },
            )

            val keptItems = mutableListOf<FeedDisplayItem>()
            val blockedItems = mutableListOf<Pair<FilterableContent, String>>()

            itemIdentityPairs.forEach { (item, identity) ->
                val isViewed = ContentViewRecord.generateId(identity.type, identity.id) in viewedContentIds
                val isFollowing = item.feed
                    ?.target
                    ?.author
                    ?.isFollowing ?: false
                // 手机版特供垃圾，根本没人点赞那种。
                // 没人点赞，你乎就只能拿时间和浏览量来招笑了。
                val isLowQualityAndroidFeed = item.details.contains("小时前") || item.details.contains("分钟前") || item.details.contains("浏览")

                if (isFollowing || (!isViewed && !isLowQualityAndroidFeed)) {
                    keptItems.add(item)
                    filterManager.recordContentView(identity.type, identity.id)
                } else {
                    blockedItems.add(
                        item.toFilterableContent(identity, DataHolder.DummyContent) to "已读过且未关注作者",
                    )
                }
            }

            if (blockedItems.isNotEmpty()) {
                saveBlockedFeedRecords(context, blockedItems)
            }

            keptItems
        } catch (e: Exception) {
            Log.e("ContentFilterExtensions", "Failed to apply foreground read filter", e)
            items
        }
    }

    /**
     * 对FeedDisplayItem列表应用内容过滤。
     * 包括广告检测、关键词屏蔽、NLP语义屏蔽和用户屏蔽。
     * 已读过滤已在前台通过[applyForegroundReadFilterToDisplayItems]执行。
     *
     * 在吃💩模式下，只会返回广告。
     */
    suspend fun applyContentFilterToDisplayItems(
        context: Context,
        items: List<FeedDisplayItem>,
    ): List<FeedDisplayItem> = withContext(Dispatchers.IO) {
        try {
            val preferences = context.getSharedPreferences(PREFERENCE_NAME, Context.MODE_PRIVATE)

            var filteredItems = items

            // 1. 应用关注用户过滤逻辑
            val shouldFilterFollowed = preferences.getBoolean("filterFollowedUserContent", false)

            val (followedUserItems, otherItems) = if (!shouldFilterFollowed) {
                // 分离已关注用户的内容
                filteredItems.partition { item ->
                    item.feed
                        ?.target
                        ?.author
                        ?.isFollowing == true
                }
            } else {
                Pair(emptyList(), filteredItems)
            }

            // 2. 转换为FilterableContent并获取完整内容，同时建立映射关系
            val itemToFilterableMap = mutableMapOf<FeedDisplayItem, FilterableContent>()

            otherItems.forEach { item ->
                val identity = item.resolveContentIdentity()

                // 获取完整内容详情
                val rawContent = when (val dest = item.navDestination) {
                    is Article -> ContentDetailCache.getOrFetch(context, dest) ?: DataHolder.DummyContent
                    is Pin -> ContentDetailCache.getOrFetch(context, dest) ?: DataHolder.DummyContent
                    else -> DataHolder.DummyContent
                }

                if (rawContent is DataHolder.DummyContent) {
                    Log.w("ContentFilterExtensions", "Failed to fetch content details for item '${item.title}' with navDestination '${item.navDestination}'. Using dummy content for filtering.")
                }

                itemToFilterableMap[item] = item.toFilterableContent(identity, rawContent)
            }

            val filterableContents = itemToFilterableMap.values.toList()

            // 3. 过滤广告和付费内容
            val adBlockedContents = mutableListOf<Pair<FilterableContent, String>>()
            if (preferences.getBoolean("reverseBlock", false)) {
                val ads = filterableContents.filter { content -> checkForAd(content) }
                val ids = ads.map { it.contentId }
                return@withContext items.filter { item ->
                    val contentId = item.resolveContentIdentity().id
                    contentId in ids
                } + items.filter { it.feed is AdvertisementFeed }
            }
            val nonAdContents = filterableContents.filter { content ->
                val blockReason = getAdBlockReason(content, preferences)
                if (blockReason != null) adBlockedContents.add(content to blockReason)
                blockReason == null
            }

            // 4. 应用关键词和NLP过滤
            val blockedContents = mutableListOf<Pair<FilterableContent, String>>()
            val filteredContents = filterContents(context, nonAdContents, blockedContents)
            val filteredContentIds = filteredContents.map { it.contentId }.toSet()

            // 5. 根据过滤结果重新构建FeedDisplayItem，并附带raw信息
            val itemToRawMap = itemToFilterableMap.entries.associate { (item, filterable) ->
                filterable.contentId to Pair(item, filterable.raw)
            }

            val filteredOtherItems = otherItems.mapNotNull { item ->
                val contentId = item.resolveContentIdentity().id
                if (contentId in filteredContentIds) {
                    val (_, raw) = itemToRawMap[contentId] ?: (null to null)
                    item.copy(raw = raw)
                } else {
                    null
                }
            }

            filteredItems = followedUserItems + filteredOtherItems

            // 6. 持久化所有屏蔽记录
            val allBlocked = adBlockedContents + blockedContents
            if (allBlocked.isNotEmpty()) {
                saveBlockedFeedRecords(context, allBlocked)
            }

            filteredItems.filter {
                listOf(
                    "感兴趣",
                    "购买",
                ).none { keyword ->
                    val shouldFilter = it.details.contains(keyword)
                    if (shouldFilter) {
                        Log.e("ContentFilterExtensions", "Filtered item '${it.title}' due to keyword '$keyword' in details: ${it.content}")
                    }
                    shouldFilter
                }
            }
        } catch (e: Exception) {
            Log.e("ContentFilterExtensions", "Failed to apply content filter to display items", e)
            items
        }
    }

    private fun checkForAd(content: FilterableContent): Boolean = when (val raw = content.raw) {
        is DataHolder.Answer -> raw.paidInfo != null || getLinkBasedAdReason(raw.content, true, true, true) != null
        is DataHolder.Article -> raw.paidInfo != null || getLinkBasedAdReason(raw.content, true, true, true) != null
        is DataHolder.Pin -> getLinkBasedAdReason(raw.contentHtml, true, true, true) != null
        else -> false
    }

    /**
     * 获取广告或付费内容的具体屏蔽原因
     */
    private fun getAdBlockReason(content: FilterableContent, preferences: SharedPreferences): String? {
        val blockZhihuAdPlatform = preferences.getBoolean("blockZhihuAdPlatform", true)
        val blockZhihuSchool = preferences.getBoolean("blockZhihuSchool", true)
        val blockWeChatOfficialAccount = preferences.getBoolean("blockWeChatOfficialAccount", true)
        val blockPaidContent = preferences.getBoolean("blockPaidContent", true)

        return when (val raw = content.raw) {
            is DataHolder.Answer -> {
                if (blockPaidContent && raw.paidInfo != null) {
                    "知乎盐选付费内容"
                } else {
                    getLinkBasedAdReason(raw.content, blockZhihuAdPlatform, blockZhihuSchool, blockWeChatOfficialAccount)
                }
            }
            is DataHolder.Article -> {
                if (blockPaidContent && raw.paidInfo != null) {
                    "知乎盐选付费内容"
                } else {
                    getLinkBasedAdReason(raw.content, blockZhihuAdPlatform, blockZhihuSchool, blockWeChatOfficialAccount)
                }
            }
            is DataHolder.Pin -> getLinkBasedAdReason(raw.contentHtml, blockZhihuAdPlatform, blockZhihuSchool, blockWeChatOfficialAccount)
            else -> null
        }
    }

    private fun getLinkBasedAdReason(
        content: String,
        blockZhihuAdPlatform: Boolean,
        blockZhihuSchool: Boolean,
        blockWeChatOfficialAccount: Boolean,
    ): String? {
        if (blockZhihuAdPlatform && "xg.zhihu.com" in content) return "知乎广告平台内容"
        if (blockZhihuSchool && ("d.zhihu.com" in content || "data-edu-card-id" in content)) return "知乎学堂内容"
        if (blockWeChatOfficialAccount && "mp.weixin.qq.com" in content) return "微信公众号文章"
        return null
    }

    /**
     * 对FilterableContent列表进行用户自定义规则屏蔽
     * @param blocked 收集被屏蔽内容及原因，用于后续持久化
     */
    private suspend fun filterContents(
        context: Context,
        contents: List<FilterableContent>,
        blocked: MutableList<Pair<FilterableContent, String>>,
    ): List<FilterableContent> {
        var filteredContents = contents

        // 应用作者屏蔽
        if (isUserBlockingEnabled(context)) {
            val blocklistManager = BlocklistManager.getInstance(context)
            val (kept, removed) = filteredContents.partition { !blocklistManager.isUserBlocked(it.authorId) }
            removed.forEach { blocked.add(it to "屏蔽作者：${it.authorName ?: it.authorId}") }
            filteredContents = kept
        }

        // 应用关键词屏蔽
        if (isKeywordBlockingEnabled(context)) {
            val blocklistManager = BlocklistManager.getInstance(context)
            val (kept, removed) = filteredContents.partition { content ->
                !blocklistManager.containsBlockedKeyword(content.title) &&
                    !blocklistManager.containsBlockedKeyword(content.summary ?: "") &&
                    !blocklistManager.containsBlockedKeyword(content.content ?: "")
            }
            removed.forEach { blocked.add(it to "关键词屏蔽") }
            filteredContents = kept
        }

        // 应用NLP语义屏蔽
        if (isNLPBlockingEnabled(context)) {
            val blockedThisRound = mutableListOf<FilterableContent>()
            val nlpRepository = BlockedKeywordRepository(context)
            val threshold = getNLPSimilarityThreshold(context)
            val finalFilteredContents = mutableListOf<FilterableContent>()

            for (content in filteredContents) {
                val (shouldBlock, matchedKeywords) = nlpRepository.checkNLPBlockingWithWeight(
                    title = content.title,
                    excerpt = content.summary,
                    content = content.content?.let { Jsoup.parse(it).text() },
                    threshold = threshold,
                )

                if (!shouldBlock) {
                    finalFilteredContents.add(content)
                } else {
                    nlpRepository.recordBlockedContent(
                        contentId = content.contentId,
                        contentType = content.contentType,
                        title = content.title,
                        excerpt = content.summary ?: "",
                        authorName = content.authorName,
                        authorId = content.authorId,
                        matchedKeywords = matchedKeywords,
                    )
                    val keywordNames = matchedKeywords.joinToString("、") { it.keyword }
                    blocked.add(content to "NLP语义屏蔽：$keywordNames")
                    blockedThisRound.add(content)
                }
            }

            if (blockedThisRound.isNotEmpty()) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    context.mainExecutor.execute {
                        Toast.makeText(context, "NLP 已屏蔽 ${blockedThisRound.first().title.take(10)}... 等 ${blockedThisRound.size} 条内容", Toast.LENGTH_SHORT).show()
                    }
                }
            }

            filteredContents = finalFilteredContents
        }

        // 应用主题屏蔽
        if (isTopicBlockingEnabled(context)) {
            val blocklistManager = BlocklistManager.getInstance(context)
            val threshold = getTopicBlockingThreshold(context)

            filteredContents = filteredContents.filter { content ->
                val topicIds = extractTopicIds(content.raw)
                val kept = blocklistManager.countBlockedTopics(topicIds) < threshold
                if (!kept) {
                    val topicName = topicIds
                        ?.first { topicId ->
                            blocklistManager.isTopicBlocked(topicId)
                        }?.let { topicId ->
                            blocklistManager.getTopicName(topicId)
                        }
                    blocked.add(content to "屏蔽主题：$topicName")
                }
                kept
            }
        }

        return filteredContents
    }

    private data class ContentIdentity(
        val type: String,
        val id: String,
    )

    private fun FeedDisplayItem.resolveContentIdentity(): ContentIdentity = when (val dest = navDestination) {
        is Article -> {
            val type = when (dest.type) {
                ArticleType.Answer -> ContentType.ANSWER
                ArticleType.Article -> ContentType.ARTICLE
            }
            ContentIdentity(type, dest.id.toString())
        }
        is Question -> {
            ContentIdentity(ContentType.QUESTION, dest.questionId.toString())
        }
        else -> {
            ContentIdentity("unknown", navDestination.hashCode().toString())
        }
    }

    private fun FeedDisplayItem.toFilterableContent(
        identity: ContentIdentity,
        rawContent: DataHolder.Content,
    ): FilterableContent = FilterableContent(
        title = title,
        summary = summary,
        content = when (rawContent) {
            is DataHolder.Answer -> rawContent.content
            is DataHolder.Article -> rawContent.content
            is DataHolder.Pin -> rawContent.contentHtml
            else -> null
        } ?: content ?: summary,
        authorName = authorName,
        authorId = rawContent.author?.id,
        contentId = identity.id,
        contentType = identity.type,
        raw = rawContent,
        isFollowing = rawContent.author?.isFollowing ?: false,
        questionId = (rawContent as? DataHolder.Answer)?.question?.id,
        url = feed?.target?.url,
        feedJson = feed?.let { runCatching { recordJson.encodeToString(it) }.getOrNull() },
        navDestinationJson = navDestination?.let { runCatching { recordJson.encodeToString(it) }.getOrNull() },
    )

    private suspend fun saveBlockedFeedRecords(
        context: Context,
        blocked: List<Pair<FilterableContent, String>>,
    ) {
        try {
            val dao = ContentFilterDatabase.getDatabase(context).blockedFeedRecordDao()
            blocked.forEach { (content, reason) ->
                dao.insert(
                    BlockedFeedRecord(
                        title = content.title,
                        questionId = content.questionId,
                        authorName = content.authorName,
                        authorId = content.authorId,
                        url = content.url,
                        content = content.content,
                        blockedReason = reason,
                        navDestinationJson = content.navDestinationJson,
                        feedJson = content.feedJson,
                    ),
                )
            }
            dao.maintainLimit()
        } catch (e: Exception) {
            Log.e("ContentFilterExtensions", "Failed to save blocked feed records", e)
        }
    }

    /**
     * 从raw content中提取主题ID列表
     */
    private fun extractTopicIds(raw: DataHolder.Content): List<String>? = when (raw) {
        is DataHolder.Answer -> raw.question.topics.map { it.id }
        is DataHolder.Question -> raw.topics.map { it.id }
        is DataHolder.Article -> raw.topics?.map { it.id }
        else -> null
    }
}

/**
 * 定义常见的内容类型常量
 */
object ContentType {
    const val ANSWER = "answer"
    const val ARTICLE = "article"
    const val QUESTION = "question"
    const val TOPIC = "topic"
    const val COLUMN = "column"
    const val VIDEO = "video"
    const val PIN = "pin"
}

private val DataHolder.Content.author: DataHolder.Author?
    get() = when (this) {
        is DataHolder.Answer -> this.author
        is DataHolder.Article -> this.author
        is DataHolder.Pin -> this.author
        is DataHolder.Question -> this.author
//        is DataHolder.Comment -> this.author
        else -> null
    }

/** 用于序列化屏蔽记录的 Json 实例 */
internal val recordJson = Json {
    ignoreUnknownKeys = true
    encodeDefaults = false
}
