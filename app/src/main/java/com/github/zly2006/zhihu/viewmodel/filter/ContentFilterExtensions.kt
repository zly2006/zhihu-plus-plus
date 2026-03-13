package com.github.zly2006.zhihu.viewmodel.filter

import android.content.Context
import android.os.Build
import android.util.Log
import android.widget.Toast
import com.github.zly2006.zhihu.ArticleType
import com.github.zly2006.zhihu.data.ContentDetailCache
import com.github.zly2006.zhihu.data.DataHolder
import com.github.zly2006.zhihu.data.Feed
import com.github.zly2006.zhihu.data.target
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
     * 对FeedDisplayItem列表应用内容过滤
     * 包括广告检测、关键词屏蔽、NLP语义屏蔽和用户屏蔽
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
                val (contentType, contentId) = when (val dest = item.navDestination) {
                    is com.github.zly2006.zhihu.Article -> {
                        val type = when (dest.type) {
                            ArticleType.Answer -> ContentType.ANSWER
                            ArticleType.Article -> ContentType.ARTICLE
                        }
                        Pair(type, dest.id.toString())
                    }
                    is com.github.zly2006.zhihu.Question -> {
                        Pair(ContentType.QUESTION, dest.questionId.toString())
                    }
                    else -> {
                        Pair("unknown", item.navDestination.hashCode().toString())
                    }
                }

                // 获取完整内容详情
                val rawContent = when (val dest = item.navDestination) {
                    is com.github.zly2006.zhihu.Article -> ContentDetailCache.getOrFetch(context, dest) ?: DataHolder.DummyContent
                    is com.github.zly2006.zhihu.Pin -> ContentDetailCache.getOrFetch(context, dest) ?: DataHolder.DummyContent
                    else -> DataHolder.DummyContent
                }

                val questionId = (item.feed?.target as? Feed.AnswerTarget)?.question?.id

                val filterableContent = FilterableContent(
                    title = item.title,
                    summary = item.summary,
                    content = when (rawContent) {
                        is DataHolder.Answer -> rawContent.content
                        is DataHolder.Article -> rawContent.content
                        is DataHolder.Pin -> rawContent.contentHtml
                        else -> null
                    } ?: item.content,
                    authorName = item.authorName,
                    authorId = item.feed
                        ?.target
                        ?.author
                        ?.id,
                    contentId = contentId,
                    contentType = contentType,
                    raw = rawContent,
                    isFollowing = item.feed
                        ?.target
                        ?.author
                        ?.isFollowing ?: false,
                    questionId = questionId,
                    url = item.feed?.target?.url,
                    feedJson = item.feed?.let { runCatching { recordJson.encodeToString(it) }.getOrNull() },
                    navDestinationJson = item.navDestination?.let { runCatching { recordJson.encodeToString(it) }.getOrNull() },
                )

                itemToFilterableMap[item] = filterableContent
            }

            val filterableContents = itemToFilterableMap.values.toList()

            // 3. 过滤广告和付费内容
            val adBlockedContents = mutableListOf<Pair<FilterableContent, String>>()
            if (preferences.getBoolean("reverseBlock", false)) {
                val ads = filterableContents.filter { content -> checkForAd(content) }
                val ids = ads.map { it.contentId }
                return@withContext items.filter { item ->
                    val contentId = when (val dest = item.navDestination) {
                        is com.github.zly2006.zhihu.Article -> dest.id.toString()
                        is com.github.zly2006.zhihu.Question -> dest.questionId.toString()
                        is com.github.zly2006.zhihu.Pin -> dest.id.toString()
                        else -> item.navDestination.hashCode().toString()
                    }
                    contentId in ids
                }
            }
            val nonAdContents = filterableContents.filter { content ->
                val isAd = checkForAd(content)
                if (isAd) adBlockedContents.add(content to "广告或付费内容")
                !isAd
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
                val contentId = when (val dest = item.navDestination) {
                    is com.github.zly2006.zhihu.Article -> dest.id.toString()
                    is com.github.zly2006.zhihu.Question -> dest.questionId.toString()
                    is com.github.zly2006.zhihu.Pin -> dest.id.toString()
                    else -> item.navDestination.hashCode().toString()
                }
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

            filteredItems
        } catch (e: Exception) {
            Log.e("ContentFilterExtensions", "Failed to apply content filter to display items", e)
            items
        }
    }

    /**
     * 检测内容是否为广告或付费内容
     */
    private fun checkForAd(content: FilterableContent): Boolean {
        val blocklist = listOf(
            "xg.zhihu.com", // 知乎广告平台域名，常见于广告内容中
            "d.zhihu.com", // 知乎学堂
            "data-edu-card-id", // 知乎学堂
            "mp.weixin.qq.com", // 微信公众号文章链接，常见于被推广的内容中
        )
        return when (val raw = content.raw) {
            is DataHolder.Answer -> blocklist.any { blockWord -> blockWord in raw.content } || raw.paidInfo != null
            is DataHolder.Article -> blocklist.any { blockWord -> blockWord in raw.content } || raw.paidInfo != null
            is DataHolder.Pin -> blocklist.any { blockWord -> blockWord in raw.contentHtml }
            else -> false
        }
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

        // 应用已读屏蔽：如果发布者未被关注，且已经展示过，则过滤
        if (isContentFilterEnabled(context)) {
            val filterManager = ContentFilterManager.getInstance(context)
            val contentPairs = filteredContents.map { it.contentType to it.contentId }
            val viewedContentIds = filterManager.getAlreadyViewedContentIds(contentPairs)

            val (kept, removed) = filteredContents.partition { content ->
                val isViewed = ContentViewRecord.generateId(content.contentType, content.contentId) in viewedContentIds
                content.isFollowing || !isViewed
            }
            removed.forEach { blocked.add(it to "已读过且未关注作者") }
            filteredContents = kept
        }

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

/** 用于序列化屏蔽记录的 Json 实例 */
internal val recordJson = Json {
    ignoreUnknownKeys = true
    encodeDefaults = false
}
