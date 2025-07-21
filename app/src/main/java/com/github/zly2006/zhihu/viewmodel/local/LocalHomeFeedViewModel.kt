package com.github.zly2006.zhihu.viewmodel.local

import android.content.Context
import com.github.zly2006.zhihu.viewmodel.feed.BaseFeedViewModel
import kotlinx.coroutines.delay

class LocalHomeFeedViewModel : BaseFeedViewModel() {
    private lateinit var recommendationEngine: LocalRecommendationEngine

    override val initialUrl: String
        get() = error("LocalHomeFeedViewModel should not be used directly. Use LocalFeedViewModel instead.")

    override suspend fun fetchFeeds(context: Context) {
        try {
            if (!::recommendationEngine.isInitialized) {
                recommendationEngine = LocalRecommendationEngine(context)
            }

            // 获取本地推荐内容
            val recommendations = recommendationEngine.generateRecommendations(20)

            // 转换为显示项目
            recommendations.forEach { localFeed ->
                val displayItem = createLocalFeedDisplayItem(localFeed)
                displayItems.add(displayItem)
                // 模拟逐步加载效果
                delay(100)
            }
        } catch (e: Exception) {
            // 如果推荐引擎失败，提供备用内容
            generateFallbackContent()
        } finally {
            isLoading = false
        }
    }

    private fun createLocalFeedDisplayItem(localFeed: LocalFeed): FeedDisplayItem {
        return FeedDisplayItem(
            title = localFeed.title,
            summary = localFeed.summary,
            details = localFeed.reasonDisplay ?: "本地推荐",
            feed = null,
            isFiltered = false
        )
    }

    private suspend fun generateFallbackContent() {
        val fallbackItems = listOf(
            FeedDisplayItem(
                title = "本地推荐系统正在学习中...",
                summary = "随着您在应用中的使用，本地推荐系统会逐渐了解您的偏好，为您提供更精准的个性化内容。",
                details = "本地推荐 - 初始化中",
                feed = null,
                isFiltered = false
            ),
            FeedDisplayItem(
                title = "隐私保护的个性化推荐",
                summary = "本地推荐模式完全在设备上运行，不会上传您的数据到服务器，确保您的隐私安全。",
                details = "本地推荐 - 隐私模式",
                feed = null,
                isFiltered = false
            )
        )

        fallbackItems.forEach { item ->
            displayItems.add(item)
            delay(500)
        }
    }
}
