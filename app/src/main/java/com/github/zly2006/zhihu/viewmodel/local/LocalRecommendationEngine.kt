package com.github.zly2006.zhihu.viewmodel.local

import android.content.Context
import android.net.NetworkCapabilities
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withContext

class LocalRecommendationEngine(
    private val context: Context,
) {
    private val database by lazy { LocalContentDatabase.getDatabase(context) }
    private val dao by lazy { database.contentDao() }
    private val feedGenerator by lazy { FeedGenerator(context) }
    private val userBehaviorAnalyzer by lazy { UserBehaviorAnalyzer(context) }
    private val contentInitializer by lazy { LocalContentInitializer(context) }
    private val taskScheduler by lazy { TaskScheduler(context) }
    private val crawlingExecutor by lazy { CrawlingExecutor(context) }

    suspend fun initialize() {
        withContext(Dispatchers.IO) {
            // 初始化数据库和基础数据
            contentInitializer.initializeIfNeeded()

            // 启动任务调度器
            taskScheduler.startScheduling()
        }
    }

    suspend fun generateRecommendations(limit: Int = 20): List<LocalFeed> {
        return withContext(Dispatchers.IO) {
            // 检查数据库是否有足够的数据进行推荐
            if (!hasEnoughDataForRecommendation()) {
                // 如果数据不足，创建或执行更多爬虫任务
                ensurePendingTasks()

                // 尝试执行一些高优先级任务来快速获取数据
                executeHighPriorityTasks()

                // 等待一些任务完成，最多等待30秒
                waitForTaskCompletion(30_000L)

                // 如果仍然没有足够数据，返回现有的最佳内容
                if (!hasEnoughDataForRecommendation()) {
                    return@withContext getFallbackRecommendations(limit)
                }
            }

            // 获取用户偏好权重
            val userPreferences = userBehaviorAnalyzer.getUserPreferences()

            // 从各个推荐原因获取内容并按权重排序
            val allResults = mutableListOf<CrawlingResult>()

            CrawlingReason.entries.forEach { reason ->
                val weight = userPreferences[reason] ?: getDefaultWeight(reason)
                val results = dao.getResultsByReason(reason)

                // 根据权重调整分数并添加到结果中
                val weightedResults = results.map { result ->
                    result.copy(score = result.score * weight)
                }
                allResults.addAll(weightedResults)
            }

            // 按分数排序并取前N个，同时确保多样性
            val topResults = allResults
                .sortedByDescending { it.score }
                .distinctBy { it.contentId } // 去重
                .take(limit * 2) // 取更多结果以确保多样性
                .let { results ->
                    // 确保每种推荐原因都有代表
                    ensureReasonDiversity(results, limit)
                }

            // 转换为LocalFeed
            topResults.mapNotNull { result ->
                dao.getFeedByResultId(result.id) ?: run {
                    val reasonDisplay = feedGenerator.getReasonDisplayText(result.reason)
                    feedGenerator.generateFeedFromResult(result, reasonDisplay)
                }
            }
        }
    }

    /**
     * 检查数据库是否有足够的数据进行推荐
     */
    private suspend fun hasEnoughDataForRecommendation(): Boolean {
        val minResultsPerReason = 5
        val totalMinResults = 20

        // 检查每种推荐原因是否有足够的数据
        val reasonCounts = CrawlingReason.entries.map { reason ->
            dao.getResultCountByReason(reason)
        }

        val totalResults = reasonCounts.sum()
        val reasonsWithEnoughData = reasonCounts.count { it >= minResultsPerReason }

        // 至少需要总共20个结果，且至少3种推荐原因有足够数据
        return totalResults >= totalMinResults && reasonsWithEnoughData >= 3
    }

    /**
     * 确保有足够的待处理任务
     */
    private suspend fun ensurePendingTasks() {
        val tasks = mutableListOf<CrawlingTask>()

        CrawlingReason.entries.forEach { reason ->
            val pendingCount = dao.getTaskCountByReasonAndStatus(reason, CrawlingStatus.NotStarted)
            val inProgressCount = dao.getTaskCountByReasonAndStatus(reason, CrawlingStatus.InProgress)

            // 如果待处理和进行中的任务总数少于2个，创建新任务
            if (pendingCount + inProgressCount < 2) {
                repeat(3 - pendingCount - inProgressCount) {
                    tasks.add(createTaskForReason(reason))
                }
            }
        }

        if (tasks.isNotEmpty()) {
            dao.insertTasks(tasks)
        }
    }

    /**
     * 执行高优先级任务以快速获取数据
     */
    private suspend fun executeHighPriorityTasks() {
        // 检查网络连接
        if (!isNetworkAvailable()) {
            android.util.Log.w("LocalRecommendationEngine", "No network connection, skipping task execution")
            return
        }

        val highPriorityTasks = dao
            .getTasksByStatus(CrawlingStatus.NotStarted)
            .sortedByDescending { it.priority }
            .take(3) // 减少并发任务数量以避免被限流

        highPriorityTasks.forEach { task ->
            try {
                crawlingExecutor.executeTask(task)
                // 添加延迟以避免请求过于频繁
                kotlinx.coroutines.delay(1000L)
            } catch (e: Exception) {
                // 记录错误但继续处理其他任务
                android.util.Log.e("LocalRecommendationEngine", "Task execution failed: ${e.message}", e)
            }
        }
    }

    /**
     * 检查网络连接是否可用
     */
    private fun isNetworkAvailable(): Boolean = try {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as android.net.ConnectivityManager
        val activeNetwork = connectivityManager.activeNetwork
        val networkCapabilities = connectivityManager.getNetworkCapabilities(activeNetwork)
        networkCapabilities?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) == true
    } catch (_: Exception) {
        false
    }

    /**
     * 等待任务完成
     */
    private suspend fun waitForTaskCompletion(maxWaitTimeMs: Long) {
        val startTime = System.currentTimeMillis()
        val checkInterval = 2000L // 每2秒检查一次

        while (System.currentTimeMillis() - startTime < maxWaitTimeMs) {
            val inProgressTasks = dao.getTasksByStatus(CrawlingStatus.InProgress)
            if (inProgressTasks.isEmpty()) {
                break // 没有进行中的任务，可以继续
            }

            kotlinx.coroutines.delay(checkInterval)
        }
    }

    /**
     * 获取备用推荐内容（当数据不足时）
     */
    private suspend fun getFallbackRecommendations(limit: Int): List<LocalFeed> {
        // 尝试从现有的评分较高的内容中获取
        val topRatedFeeds = dao.getTopRatedFeeds(limit / 2)

        // 尝试从最近的内容中获取
        val recentFeeds = dao.getRecentFeeds(limit / 2)

        // 合并并去重
        return (topRatedFeeds + recentFeeds)
            .distinctBy { it.id }
            .take(limit)
    }

    /**
     * 确保推荐结果的多样性，每种推荐原因都有代表
     */
    private fun ensureReasonDiversity(results: List<CrawlingResult>, limit: Int): List<CrawlingResult> {
        val reasonGroups = results.groupBy { it.reason }
        val diverseResults = mutableListOf<CrawlingResult>()

        // 首先从每种原因中至少取一个
        reasonGroups.values.forEach { group ->
            if (group.isNotEmpty() && diverseResults.size < limit) {
                diverseResults.add(group.first())
            }
        }

        // 然后按分数继续添加，直到达到限制
        val remainingResults = results.filter { it !in diverseResults }
        diverseResults.addAll(remainingResults.take(limit - diverseResults.size))

        return diverseResults
    }

    suspend fun recordUserFeedback(feedId: String, feedback: Double) {
        withContext(Dispatchers.IO) {
            dao.updateFeedFeedback(feedId, feedback)

            // 同时记录用户行为
            val action = when {
                feedback > 0.5 -> "like"
                feedback < -0.5 -> "dislike"
                else -> "neutral"
            }
            userBehaviorAnalyzer.recordBehavior(feedId, action)
        }
    }

    fun getRecommendationStream(): Flow<List<LocalFeed>> = flow {
        while (true) {
            try {
                val recommendations = generateRecommendations()
                emit(recommendations)
                kotlinx.coroutines.delay(30_000L) // 每30秒更新一次
            } catch (e: Exception) {
                kotlinx.coroutines.delay(60_000L) // 出错时等待1分钟
            }
        }
    }

    suspend fun cleanup() {
        withContext(Dispatchers.IO) {
            taskScheduler.stopScheduling()

            // 清理旧数据
            val oneWeekAgo = System.currentTimeMillis() - 7 * 24 * 60 * 60 * 1000L
            val oneMonthAgo = System.currentTimeMillis() - 30 * 24 * 60 * 60 * 1000L

            dao.cleanupOldTasks(CrawlingStatus.Completed, oneWeekAgo)
            dao.cleanupOldTasks(CrawlingStatus.Failed, oneWeekAgo)
            dao.cleanupOldResults(oneMonthAgo)
            dao.cleanupOldFeeds(oneMonthAgo)
            dao.cleanupOldBehaviors(oneMonthAgo)
        }
    }

    private fun getDefaultWeight(reason: CrawlingReason): Double = when (reason) {
        CrawlingReason.Following -> 1.2 // 关注内容权重较高
        CrawlingReason.Trending -> 1.0 // 热门内容标准权重
        CrawlingReason.UpvotedQuestion -> 0.9 // 相关问题稍低
        CrawlingReason.FollowingUpvote -> 0.8 // 关注点赞内容较低
        CrawlingReason.CollaborativeFiltering -> 0.7 // 协同过滤最低
    }

    private fun createTaskForReason(reason: CrawlingReason): CrawlingTask {
        val (url, priority) = when (reason) {
            CrawlingReason.Following -> {
                "https://api.zhihu.com/moments_v3?feed_type=recommend" to 8
            }
            CrawlingReason.Trending -> {
                "https://www.zhihu.com/api/v3/feed/topstory/recommend?desktop=true&limit=20" to 7
            }
            CrawlingReason.UpvotedQuestion -> {
                "https://www.zhihu.com/api/v3/feed/topstory/recommend?desktop=true&limit=15" to 6
            }
            CrawlingReason.FollowingUpvote -> {
                "https://www.zhihu.com/api/v3/feed/topstory/recommend?action_feed=True&limit=15" to 5
            }
            CrawlingReason.CollaborativeFiltering -> {
                "https://www.zhihu.com/api/v3/feed/topstory/recommend?desktop=true&limit=10" to 4
            }
        }

        return CrawlingTask(
            url = url,
            reason = reason,
            priority = priority,
        )
    }
}
