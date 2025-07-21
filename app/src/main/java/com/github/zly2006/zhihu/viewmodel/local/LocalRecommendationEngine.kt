package com.github.zly2006.zhihu.viewmodel.local

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.random.Random

class LocalRecommendationEngine(private val context: Context) {
    private val database by lazy { LocalContentDatabase.getDatabase(context) }
    private val dao by lazy { database.contentDao() }
    private val initializer by lazy { LocalContentInitializer(context) }
    private val crawlingExecutor by lazy { CrawlingExecutor(context) }
    private val feedGenerator by lazy { FeedGenerator(context) }

    // 推荐权重配置，可以让用户调整
    private val reasonWeights = mapOf(
        CrawlingReason.Following to 0.3,
        CrawlingReason.Trending to 0.2,
        CrawlingReason.FollowingUpvote to 0.25,
        CrawlingReason.UpvotedQuestion to 0.15,
        CrawlingReason.CollaborativeFiltering to 0.1
    )

    suspend fun generateRecommendations(count: Int = 10): List<LocalFeed> {
        return withContext(Dispatchers.IO) {
            try {
                // 首次使用时初始化数据
                initializer.initializeIfNeeded()

                // 执行待执行的爬虫任务
                executePendingTasks()

                // 获取各种类型的爬虫结果
                val followingResults = dao.getResultsByReason(CrawlingReason.Following)
                val trendingResults = dao.getResultsByReason(CrawlingReason.Trending)
                val upvoteResults = dao.getResultsByReason(CrawlingReason.FollowingUpvote)
                val questionResults = dao.getResultsByReason(CrawlingReason.UpvotedQuestion)
                val collaborativeResults = dao.getResultsByReason(CrawlingReason.CollaborativeFiltering)

                // 根据权重分配每种类型的推荐数量
                val followingCount = (count * reasonWeights[CrawlingReason.Following]!!).toInt()
                val trendingCount = (count * reasonWeights[CrawlingReason.Trending]!!).toInt()
                val upvoteCount = (count * reasonWeights[CrawlingReason.FollowingUpvote]!!).toInt()
                val questionCount = (count * reasonWeights[CrawlingReason.UpvotedQuestion]!!).toInt()
                val collaborativeCount = count - followingCount - trendingCount - upvoteCount - questionCount

                val recommendations = mutableListOf<LocalFeed>()

                // 从爬虫结果生成推荐内容
                recommendations.addAll(generateFeedsFromResults(followingResults, followingCount, "关注用户的最新动态"))
                recommendations.addAll(generateFeedsFromResults(trendingResults, trendingCount, "热门推荐"))
                recommendations.addAll(generateFeedsFromResults(upvoteResults, upvoteCount, "关注用户点赞的内容"))
                recommendations.addAll(generateFeedsFromResults(questionResults, questionCount, "相关问题的优质回答"))
                recommendations.addAll(generateFeedsFromResults(collaborativeResults, collaborativeCount, "相似用户喜欢的内容"))

                // 打乱顺序，避免按类型聚集
                recommendations.shuffled()
            } catch (e: Exception) {
                // 如果出现错误，返回已有的推荐内容或模拟数据
                generateFallbackRecommendations(count)
            }
        }
    }

    /**
     * 执行待执行的爬虫任务
     */
    private suspend fun executePendingTasks() {
        val pendingTasks = dao.getTasksByStatus(CrawlingStatus.NotStarted)
        pendingTasks.take(5).forEach { task -> // 限制并发执行的任务数量
            try {
                crawlingExecutor.executeTask(task)
            } catch (e: Exception) {
                // 忽略单个任务的执行错误
            }
        }
    }

    /**
     * 从爬虫结果生成推荐内容
     */
    private suspend fun generateFeedsFromResults(
        results: List<CrawlingResult>,
        count: Int,
        reasonDisplay: String
    ): List<LocalFeed> {
        if (results.isEmpty()) return emptyList()

        return results.shuffled().take(count).mapNotNull { result ->
            try {
                // 检查是否已经生成过推荐
                val existingFeed = dao.getFeedByResultId(result.id)
                if (existingFeed != null) {
                    existingFeed
                } else {
                    feedGenerator.generateFeedFromResult(result, reasonDisplay)
                }
            } catch (e: Exception) {
                null
            }
        }
    }

    private suspend fun generateFallbackRecommendations(count: Int): List<LocalFeed> {
        // 尝试从已有的推荐内容中获取
        val existingFeeds = dao.getTopRatedFeeds(count)
        if (existingFeeds.isNotEmpty()) {
            return existingFeeds
        }

        // 如果没有任何内容，生成基础推荐
        val fallbackContent = listOf(
            "本地推荐系统正在学习中..." to "随着您在应用中的使用，本地推荐系统会逐渐了解您的偏好，为您提供更精准的个性化内容。",
            "隐私保护的个性化推荐" to "本地推荐模式完全在设备上运行，不会上传您的数据到服务器，确保您的隐私安全。",
            "开始您的本地推荐之旅" to "本地推荐系统正在为您收集和分析内容，很快您就能看到个性化的推荐内容了。"
        )

        return fallbackContent.take(count).mapIndexed { index, (title, summary) ->
            LocalFeed(
                id = "fallback_$index",
                resultId = -1,
                title = title,
                summary = summary,
                reasonDisplay = "本地推荐 - 初始化",
                navDestination = null,
                userFeedback = 0.0
            )
        }
    }
}
