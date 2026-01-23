package com.github.zly2006.zhihu.viewmodel.filter

import android.content.Context
import android.util.Log
import android.widget.Toast
import com.github.zly2006.zhihu.data.AccountData
import com.github.zly2006.zhihu.data.DataHolder
import com.github.zly2006.zhihu.data.target
import com.github.zly2006.zhihu.nlp.BlockedKeywordRepository
import com.github.zly2006.zhihu.ui.PREFERENCE_NAME
import com.github.zly2006.zhihu.util.signFetchRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.decodeFromJsonElement
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
    )

    val `斩杀线` = FilterableContent(
        "斯奎奇大王（牢 a）称「不是有国籍就是中国人」，如何理解此言论",
        "我的理解：他是完全没有意识到言论是有边界的，这样下去可能就是烈火烹油了。例如有一个很火的小众UP主他天天说美国是没事的，有一天他说了泰国那段时间号就没了，这就是言论的边界。沈直播中打断了他关于器官捐献的言论，沈知道国内正在推器官捐献进校，本来抖音上那些人就反对声音一浪接一浪的，沈的边界意识是非常强的。他本地情节厚重吧，张大帅在叙事中评价非常低，因为他儿子功劳大很多时候避开了对他的评价，要知道他杀…",
        null,
        "斯奎奇大王（牢 a）",
        null,
        "1234567890",
        ContentType.ANSWER,
        DataHolder.DummyContent,
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
     * 获取过滤统计信息
     */
    suspend fun getFilterStatistics(context: Context): FilterStats? {
        if (!isContentFilterEnabled(context)) return null

        return withContext(Dispatchers.IO) {
            try {
                val filterManager = ContentFilterManager.getInstance(context)
                filterManager.getFilterStats()
            } catch (e: Exception) {
                Log.e("ContentFilterExtensions", "Failed to get filter statistics", e)
                null
            }
        }
    }

    /**
     * 对FeedDisplayItem列表应用内容过滤
     * 包括广告检测、关键词屏蔽、NLP语义屏蔽和用户屏蔽
     */
    suspend fun applyContentFilterToDisplayItems(
        context: Context,
        items: List<com.github.zly2006.zhihu.viewmodel.feed.BaseFeedViewModel.FeedDisplayItem>,
    ): List<com.github.zly2006.zhihu.viewmodel.feed.BaseFeedViewModel.FeedDisplayItem> = withContext(Dispatchers.IO) {
        try {
            var filteredItems = items

            // 1. 应用关注用户过滤逻辑
            val shouldFilterFollowed = context
                .getSharedPreferences(PREFERENCE_NAME, Context.MODE_PRIVATE)
                .getBoolean("filterFollowedUserContent", false)

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

            // 2. 转换为FilterableContent并获取完整内容
            val filterableContents = otherItems.map { item ->
                val (contentType, contentId) = when (val dest = item.navDestination) {
                    is com.github.zly2006.zhihu.Article -> {
                        val type = when (dest.type) {
                            com.github.zly2006.zhihu.ArticleType.Answer -> ContentType.ANSWER
                            com.github.zly2006.zhihu.ArticleType.Article -> ContentType.ARTICLE
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
                    is com.github.zly2006.zhihu.Article -> getContentDetail(context, dest)
                    else -> DataHolder.DummyContent
                }

                FilterableContent(
                    title = item.title,
                    summary = item.summary,
                    content = item.content,
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
                )
            }

            // 3. 过滤广告和付费内容
            val nonAdContents = filterableContents.filter { content ->
                !checkForAd(content)
            }

            // 4. 应用关键词和NLP过滤
            val filteredContents = filterContents(context, nonAdContents)
            val filteredContentIds = filteredContents.map { it.contentId }.toSet()

            val filteredOtherItems = otherItems.filter { item ->
                val contentId = when (val dest = item.navDestination) {
                    is com.github.zly2006.zhihu.Article -> dest.id.toString()
                    is com.github.zly2006.zhihu.Question -> dest.questionId.toString()
                    else -> item.navDestination.hashCode().toString()
                }
                contentId in filteredContentIds
            }

            // 5. 应用用户屏蔽
            filteredItems = if (isUserBlockingEnabled(context)) {
                val blocklistManager = BlocklistManager.getInstance(context)
                (followedUserItems + filteredOtherItems).filter { item ->
                    val authorId = item.feed
                        ?.target
                        ?.author
                        ?.id
                    !blocklistManager.isUserBlocked(authorId)
                }
            } else {
                followedUserItems + filteredOtherItems
            }

            filteredItems
        } catch (e: Exception) {
            Log.e("ContentFilterExtensions", "Failed to apply content filter to display items", e)
            items
        }
    }

    /**
     * 获取内容详情
     */
    private suspend fun getContentDetail(
        context: Context,
        dest: com.github.zly2006.zhihu.Article,
    ): DataHolder.Content {
        val appViewUrl = when (dest.type) {
            com.github.zly2006.zhihu.ArticleType.Article -> "https://www.zhihu.com/api/v4/articles/${dest.id}?include=content,paid_info"
            com.github.zly2006.zhihu.ArticleType.Answer -> "https://www.zhihu.com/api/v4/answers/${dest.id}?include=content,paid_info"
        }

        return runCatching {
            val jojo = AccountData.fetchGet(context, appViewUrl) {
                signFetchRequest(context)
            }
            // 解析为对应的Content类型
            when (dest.type) {
                com.github.zly2006.zhihu.ArticleType.Answer -> AccountData.json.decodeFromJsonElement<DataHolder.Answer>(jojo)
                com.github.zly2006.zhihu.ArticleType.Article -> AccountData.json.decodeFromJsonElement<DataHolder.Article>(jojo)
            }
        }.getOrElse { DataHolder.DummyContent }
    }

    /**
     * 检测内容是否为广告或付费内容
     */
    private fun checkForAd(content: FilterableContent): Boolean = when (val raw = content.raw) {
        is DataHolder.Answer -> {
            val isAd = "xg.zhihu.com" in raw.content
            val isPaid = raw.paidInfo != null
            isAd || isPaid
        }

        is DataHolder.Article -> {
            val isAd = "xg.zhihu.com" in raw.content
            val isPaid = raw.paidInfo != null
            isAd || isPaid
        }

        else -> {
            false
        }
    }

    /**
     * 对FilterableContent列表应用过滤逻辑
     */
    private suspend fun filterContents(
        context: Context,
        contents: List<FilterableContent>,
    ): List<FilterableContent> {
        var filteredContents = contents

        // 应用关键词屏蔽
        if (isKeywordBlockingEnabled(context)) {
            val blocklistManager = BlocklistManager.getInstance(context)
            filteredContents = filteredContents.filter { content ->
                !blocklistManager.containsBlockedKeyword(content.title) &&
                    !blocklistManager.containsBlockedKeyword(content.summary ?: "") &&
                    !blocklistManager.containsBlockedKeyword(content.content ?: "")
            }
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
                    blockedThisRound.add(content)
                }
            }

            if (blockedThisRound.isNotEmpty()) {
                context.mainExecutor.execute {
                    Toast.makeText(context, "NLP 已屏蔽 ${blockedThisRound.first().title.take(10)}... 等 ${blockedThisRound.size} 条内容", Toast.LENGTH_SHORT).show()
                }
            }

            filteredContents = finalFilteredContents
        }

        return filteredContents
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
