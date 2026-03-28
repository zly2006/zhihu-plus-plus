package com.github.zly2006.zhihu.viewmodel.local

import android.app.AlertDialog
import android.content.Context
import android.util.Log
import androidx.lifecycle.viewModelScope
import com.github.zly2006.zhihu.data.Feed
import com.github.zly2006.zhihu.ui.IHomeFeedViewModel
import com.github.zly2006.zhihu.viewmodel.feed.BaseFeedViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class LocalHomeFeedViewModel :
    BaseFeedViewModel(),
    IHomeFeedViewModel {
    private lateinit var recommendationEngine: LocalRecommendationEngine

    override val initialUrl: String
        get() = error("LocalHomeFeedViewModel should not be used directly. Use LocalFeedViewModel instead.")

    override fun loadMore(context: Context) {
        if (displayItems.isEmpty()) {
            super.loadMore(context)
        }
    }

    override suspend fun fetchFeeds(context: Context) {
        try {
            val engine = ensureEngine(context)
            val recommendations = engine.generateRecommendations(20)

            if (recommendations.isEmpty()) {
                generateFallbackContent()
            } else {
                addDisplayItems(recommendations.map(::createLocalFeedDisplayItem))
            }
        } catch (e: Exception) {
            Log.e("LocalHomeFeedViewModel", "Error fetching local feeds", e)
            if (e.message?.contains("does not exist. Is Room annotation processor correctly configured?") == true) {
                withContext(Dispatchers.Main) {
                    AlertDialog
                        .Builder(context)
                        .setTitle("数据库错误")
                        .setMessage("本地推荐系统的数据库未正确初始化。请尝试重启应用或清除应用数据。")
                        .setPositiveButton("确定") { dialog, _ -> dialog.dismiss() }
                        .show()
                }
            }
            generateFallbackContent()
        } finally {
            isLoading = false
        }
    }

    fun onLocalItemOpened(context: Context, item: FeedDisplayItem) {
        val contentId = item.localContentId ?: return
        val reason = item.localReason?.let { runCatching { CrawlingReason.valueOf(it) }.getOrNull() } ?: return
        viewModelScope.launch(Dispatchers.IO) {
            ensureEngine(context).recordContentOpened(contentId, reason)
        }
    }

    fun onLocalItemFeedback(context: Context, item: FeedDisplayItem, feedback: Double) {
        val contentId = item.localContentId ?: return
        val reason = item.localReason?.let { runCatching { CrawlingReason.valueOf(it) }.getOrNull() } ?: return
        viewModelScope.launch(Dispatchers.IO) {
            ensureEngine(context).recordRecommendationFeedback(
                feedId = item.localFeedId,
                contentId = contentId,
                reason = reason,
                feedback = feedback,
            )
            if (feedback < 0) {
                withContext(Dispatchers.Main) {
                    displayItems.remove(item)
                }
            }
        }
    }

    private suspend fun ensureEngine(context: Context): LocalRecommendationEngine {
        if (!::recommendationEngine.isInitialized) {
            recommendationEngine = LocalRecommendationEngine(context)
        }
        recommendationEngine.initialize()
        return recommendationEngine
    }

    private fun createLocalFeedDisplayItem(entry: LocalRecommendationEngine.LocalRecommendationEntry): FeedDisplayItem = FeedDisplayItem(
        title = entry.feed.title,
        summary = entry.feed.summary,
        details = entry.feed.reasonDisplay,
        feed = null,
        navDestination = entry.navDestination,
        isFiltered = false,
        localContentId = entry.result.contentId,
        localFeedId = entry.feed.id,
        localReason = entry.result.reason.name,
    )

    private suspend fun generateFallbackContent() {
        val fallbackItems = listOf(
            FeedDisplayItem(
                title = "本地推荐正在建立候选池",
                summary = "系统会先抓取关注动态、热门内容和相关话题，再根据你的点击与反馈逐步调整排序。",
                details = "本地推荐 · 冷启动",
                feed = null,
                isFiltered = false,
            ),
            FeedDisplayItem(
                title = "你的行为只在本地学习",
                summary = "点开、喜欢、不喜欢都会影响后续排序，但这些学习信号不会作为推荐特征上传到服务器。",
                details = "本地推荐 · 隐私优先",
                feed = null,
                isFiltered = false,
            ),
        )

        fallbackItems.forEach { item ->
            if (displayItems.none { existing -> existing.stableKey == item.stableKey }) {
                displayItems.add(item)
            }
            delay(300)
        }
    }

    override suspend fun recordContentInteraction(
        context: Context,
        feed: Feed,
    ) {
    }

    override fun onUiContentClick(context: Context, feed: Feed, item: FeedDisplayItem) {
    }
}
