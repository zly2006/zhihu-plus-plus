package com.github.zly2006.zhihu.viewmodel.local

import android.app.AlertDialog
import android.content.Context
import android.util.Log
import com.github.zly2006.zhihu.data.Feed
import com.github.zly2006.zhihu.ui.IHomeFeedViewModel
import com.github.zly2006.zhihu.viewmodel.feed.BaseFeedViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

class LocalHomeFeedViewModel :
    BaseFeedViewModel(),
    IHomeFeedViewModel {
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
            // 如果推荐引擎失败，提供备用内容
            generateFallbackContent()
        } finally {
            isLoading = false
        }
    }

    private fun createLocalFeedDisplayItem(localFeed: LocalFeed): FeedDisplayItem = FeedDisplayItem(
        title = localFeed.title,
        summary = localFeed.summary,
        details = localFeed.reasonDisplay,
        feed = null,
        isFiltered = false,
    )

    private suspend fun generateFallbackContent() {
        val fallbackItems = listOf(
            FeedDisplayItem(
                title = "本地推荐系统正在学习中...",
                summary = "随着您在应用中的使用，本地推荐系统会逐渐了解您的偏好，为您提供更精准的个性化内容。",
                details = "本地推荐 - 加载失败",
                feed = null,
                isFiltered = false,
            ),
            FeedDisplayItem(
                title = "隐私保护的个性化推荐",
                summary = "本地推荐模式完全在设备上运行，不会上传您的数据到服务器，确保您的隐私安全。",
                details = "本地推荐 - 加载失败",
                feed = null,
                isFiltered = false,
            ),
        )

        fallbackItems.forEach { item ->
            displayItems.add(item)
            delay(500)
        }
    }

    override suspend fun recordContentInteraction(
        context: Context,
        feed: Feed,
    ) {
    }
}
