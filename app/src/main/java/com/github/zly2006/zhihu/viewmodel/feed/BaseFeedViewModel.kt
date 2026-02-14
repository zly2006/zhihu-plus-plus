package com.github.zly2006.zhihu.viewmodel.feed

import android.content.Context
import android.widget.Toast
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.viewModelScope
import com.github.zly2006.zhihu.NavDestination
import com.github.zly2006.zhihu.data.AdvertisementFeed
import com.github.zly2006.zhihu.data.CommonFeed
import com.github.zly2006.zhihu.data.ContentDetailCache
import com.github.zly2006.zhihu.data.DataHolder
import com.github.zly2006.zhihu.data.Feed
import com.github.zly2006.zhihu.data.FeedItemIndexGroup
import com.github.zly2006.zhihu.data.GroupFeed
import com.github.zly2006.zhihu.data.HotListFeed
import com.github.zly2006.zhihu.data.MomentsFeed
import com.github.zly2006.zhihu.data.QuestionFeedCard
import com.github.zly2006.zhihu.data.actionText
import com.github.zly2006.zhihu.data.target
import com.github.zly2006.zhihu.ui.PREFERENCE_NAME
import com.github.zly2006.zhihu.viewmodel.PaginationViewModel
import com.github.zly2006.zhihu.viewmodel.filter.BlocklistManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.JsonArray
import kotlin.reflect.typeOf

abstract class BaseFeedViewModel : PaginationViewModel<Feed>(typeOf<Feed>()) {
    var displayItems = mutableStateListOf<FeedDisplayItem>()
    var isPullToRefresh by mutableStateOf(false)
        protected set

    data class FeedDisplayItem(
        val title: String,
        val summary: String?,
        val details: String,
        val feed: Feed?,
        val navDestination: NavDestination? = feed?.target?.navDestination,
        val avatarSrc: String? = null,
        val authorName: String? = null,
        val isFiltered: Boolean = false,
        val content: String? = null,
        val raw: com.github.zly2006.zhihu.data.DataHolder.Content? = null,
    )

    override fun processResponse(context: Context, data: List<Feed>, rawData: JsonArray) {
        super.processResponse(context, data, rawData)
        displayItems.addAll(data.flatten().map { createDisplayItem(context, it) }) // 展示用的已flatten数据
    }

    override fun refresh(context: Context) {
        displayItems.clear()
        super.refresh(context)
    }

    suspend fun pullToRefresh(context: Context) {
        isPullToRefresh = true
        displayItems.clear()
        if (isLoading) return
        errorMessage = null
        debugData.clear()
        allData.clear()
        lastPaging = null // 重置 lastPaging
        isLoading = true
        try {
            fetchFeeds(context)
        } catch (e: Exception) {
            errorHandle(e)
        }
        isLoading = false
        isPullToRefresh = false
    }

    open fun createDisplayItem(context: Context, feed: Feed): FeedDisplayItem = when (feed) {
        is CommonFeed, is FeedItemIndexGroup, is MomentsFeed, is HotListFeed -> {
            val preferences = context.getSharedPreferences(PREFERENCE_NAME, Context.MODE_PRIVATE)
            val enableQualityFilter = preferences.getBoolean("enableQualityFilter", true)

            val filterReason = if (!enableQualityFilter) null else feed.target?.filterReason()

            if (filterReason != null) {
                FeedDisplayItem(
                    title = "已屏蔽",
                    summary = filterReason,
                    details = feed.target!!.detailsText,
                    feed = feed,
                    isFiltered = true,
                )
            } else {
                when (feed.target) {
                    is Feed.AnswerTarget,
                    is Feed.ArticleTarget,
                    is Feed.QuestionTarget,
                    -> {
                        FeedDisplayItem(
                            title = feed.target!!.title,
                            summary = feed.target!!.excerpt,
                            details = listOfNotNull(feed.target!!.detailsText, feed.actionText)
                                .joinToString(" · "),
                            avatarSrc = feed.target?.author?.avatarUrl,
                            authorName = feed.target?.author?.name,
                            feed = feed,
                        )
                    }

                    is Feed.PinTarget -> {
                        FeedDisplayItem(
                            title = feed.target!!.author!!.name + "的想法",
                            summary = feed.target!!.excerpt,
                            details = feed.target!!.detailsText,
                            avatarSrc = feed.target?.author?.avatarUrl,
                            authorName = feed.target?.author?.name,
                            feed = feed,
                        )
                    }

                    else -> {
                        FeedDisplayItem(
                            title = feed.target?.javaClass?.simpleName ?: "广告",
                            summary = "Not Implemented",
                            details = feed.target?.detailsText ?: "广告",
                            feed = feed,
                        )
                    }
                }
            }
        }

        is AdvertisementFeed -> FeedDisplayItem(
            title = "已屏蔽",
            summary = feed.actionText,
            details = "广告",
            feed = null,
            isFiltered = true,
        )

        is GroupFeed -> error("GroupFeed should not be flatten") // GroupFeed will be handled in the UI
        is QuestionFeedCard -> TODO()
    }

    @Suppress("NOTHING_TO_INLINE")
    inline fun List<Feed>.flatten() = flatMap {
        (it as? GroupFeed)?.list ?: listOf(it)
    }

    /**
     * 确保能获取作者信息，优先从 feed.target.author，失败时 lazy 加载完整内容
     */
    private suspend fun ensureAuthorInfo(
        context: Context,
        feedItem: FeedDisplayItem,
    ): Pair<String, String>? = withContext(Dispatchers.IO) {
        // 优先从 feed.target.author 获取
        feedItem.feed?.target?.author?.let { author ->
            return@withContext Pair(author.id, author.name)
        }

        feedItem.navDestination?.let { navDest ->
            if (navDest is com.github.zly2006.zhihu.Article) {
                when (val fullContent = ContentDetailCache.getOrFetch(context, navDest)) {
                    is DataHolder.Answer -> {
                        return@withContext fullContent.author.let { Pair(it.id, it.name) }
                    }
                    is DataHolder.Article -> {
                        return@withContext fullContent.author.let { Pair(it.id, it.name) }
                    }
                    else -> {}
                }
            }
        }

        // 无法获取作者信息
        null
    }

    /**
     * 确保能获取关键词屏蔽所需的内容（标题+摘要+正文），优先从 feed.target，失败时 lazy 加载
     */
    private suspend fun ensureContentForKeywordBlocking(
        context: Context,
        feedItem: FeedDisplayItem,
    ): Triple<String, String, String?>? = withContext(Dispatchers.IO) {
        // 优先使用已有的数据
        val title = feedItem.title
        val summary = feedItem.summary ?: feedItem.feed?.target?.excerpt ?: ""
        var content = feedItem.content

        // 如果 content 为空，尝试 lazy 加载
        if (content == null) {
            feedItem.navDestination?.let { navDest ->
                val fullContent = ContentDetailCache.getOrFetch(context, navDest)
                content = when (fullContent) {
                    is DataHolder.Answer -> fullContent.content
                    is DataHolder.Article -> fullContent.content
                    is DataHolder.Question -> fullContent.detail
                    else -> null
                }
            }
        }

        if (title.isNotEmpty() || summary.isNotEmpty() || content != null) {
            Triple(title, summary, content)
        } else {
            null
        }
    }

    /**
     * 处理用户屏蔽（包含数据获取和屏蔽逻辑）
     */
    fun handleBlockUser(
        context: Context,
        feedItem: FeedDisplayItem,
        onShowDialog: (Pair<String, String>) -> Unit,
    ) {
        viewModelScope.launch {
            val authorInfo = ensureAuthorInfo(context, feedItem)
            if (authorInfo != null) {
                onShowDialog(authorInfo)
            } else {
                Toast.makeText(context, "无法获取屏蔽用户所需的数据，请尝试进入内容详情页操作", Toast.LENGTH_LONG).show()
            }
        }
    }

    /**
     * 处理关键词屏蔽（包含数据获取和屏蔽逻辑）
     */
    fun handleBlockByKeywords(
        context: Context,
        feedItem: FeedDisplayItem,
        onShowDialog: (Pair<FeedDisplayItem, Triple<String, String, String?>>) -> Unit,
    ) {
        viewModelScope.launch {
            val contentInfo = ensureContentForKeywordBlocking(context, feedItem)
            if (contentInfo != null) {
                onShowDialog(feedItem to contentInfo)
            } else {
                Toast.makeText(context, "无法获取关键词屏蔽所需的数据，请尝试进入内容详情页操作", Toast.LENGTH_LONG).show()
            }
        }
    }

    /**
     * 屏蔽主题
     */
    fun handleBlockTopic(
        context: Context,
        topicId: String,
        topicName: String,
    ) {
        viewModelScope.launch {
            try {
                val blocklistManager = BlocklistManager.getInstance(context)
                blocklistManager.addBlockedTopic(topicId, topicName)
                Toast.makeText(context, "已屏蔽主题「$topicName」", Toast.LENGTH_SHORT).show()
                displayItems.removeAll {
                    val topics = when (it.raw) {
                        is com.github.zly2006.zhihu.data.DataHolder.Answer -> it.raw.question.topics
                        is com.github.zly2006.zhihu.data.DataHolder.Article -> it.raw.topics
                        is com.github.zly2006.zhihu.data.DataHolder.Question -> it.raw.topics
                        else -> null
                    }
                    topics?.any { topic -> topic.id == topicId } == true
                }
            } catch (e: Exception) {
                val message = "屏蔽失败: ${e.message}"
                Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
            }
        }
    }
}
