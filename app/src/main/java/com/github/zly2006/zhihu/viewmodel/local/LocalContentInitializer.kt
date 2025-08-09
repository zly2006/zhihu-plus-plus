package com.github.zly2006.zhihu.viewmodel.local

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class LocalContentInitializer(
    private val context: Context,
) {
    private val database by lazy { LocalContentDatabase.getDatabase(context) }
    private val dao by lazy { database.contentDao() }

    suspend fun initializeIfNeeded() {
        withContext(Dispatchers.IO) {
            val isInitialized = checkDatabaseHasSufficientData()
            if (!isInitialized) {
                initializeFromHistory()
            } else {
                // 检查是否需要补充爬虫任务
                ensureSufficientTasks()
            }
        }
    }

    private suspend fun checkDatabaseHasSufficientData(): Boolean {
        // 检查每种推荐原因是否都有足够的任务或结果
        val minDataPerReason = 2 // 每种原因至少需要2个任务或结果

        return CrawlingReason.entries.all { reason ->
            val pendingTasks = dao.getTaskCountByReasonAndStatus(reason, CrawlingStatus.NotStarted)
            val completedResults = dao.getResultCountByReason(reason)
            (pendingTasks + completedResults) >= minDataPerReason
        }
    }

    private suspend fun initializeFromHistory() {
        val tasks = mutableListOf<CrawlingTask>()

        // 1. 基于关注用户的内容初始化Following任务
        initializeFollowingTasks(tasks)

        // 2. 初始化热门推荐任务
        initializeTrendingTasks(tasks)

        // 3. 基于历史点赞初始化UpvotedQuestion任务
        initializeUpvotedQuestionTasks(tasks)

        // 4. 初始化关注用户点赞任务
        initializeFollowingUpvoteTasks(tasks)

        // 5. 初始化协同过滤任务
        initializeCollaborativeFilteringTasks(tasks)

        if (tasks.isNotEmpty()) {
            dao.insertTasks(tasks)
        }
    }

    private suspend fun ensureSufficientTasks() {
        val minTasksPerReason = 3
        val tasks = mutableListOf<CrawlingTask>()

        CrawlingReason.entries.forEach { reason ->
            val pendingCount = dao.getTaskCountByReasonAndStatus(reason, CrawlingStatus.NotStarted)
            val completedCount = dao.getResultCountByReason(reason)

            // 如果该类型的任务或结果不足，则创建新任务
            if (pendingCount < minTasksPerReason && completedCount < 10) {
                val additionalTasks = when (reason) {
                    CrawlingReason.Following -> createFollowingTasks(minTasksPerReason - pendingCount)
                    CrawlingReason.Trending -> createTrendingTasks(minTasksPerReason - pendingCount)
                    CrawlingReason.UpvotedQuestion -> createUpvotedQuestionTasks(minTasksPerReason - pendingCount)
                    CrawlingReason.FollowingUpvote -> createFollowingUpvoteTasks(minTasksPerReason - pendingCount)
                    CrawlingReason.CollaborativeFiltering -> createCollaborativeFilteringTasks(minTasksPerReason - pendingCount)
                }
                tasks.addAll(additionalTasks)
            }
        }

        if (tasks.isNotEmpty()) {
            dao.insertTasks(tasks)
        }
    }

    private fun initializeFollowingTasks(tasks: MutableList<CrawlingTask>) {
        // 创建基础的关注用户内容爬虫任务
        repeat(3) { index ->
            tasks.add(
                CrawlingTask(
                    url = "https://api.zhihu.com/moments_v3?feed_type=recommend&offset=${index * 10}",
                    reason = CrawlingReason.Following,
                    priority = 8,
                ),
            )
        }
    }

    private fun initializeTrendingTasks(tasks: MutableList<CrawlingTask>) {
        // 创建热门推荐爬虫任务
        repeat(3) { index ->
            tasks.add(
                CrawlingTask(
                    url = "https://www.zhihu.com/api/v3/feed/topstory/recommend?desktop=true&limit=20&offset=${index * 20}",
                    reason = CrawlingReason.Trending,
                    priority = 7,
                ),
            )
        }
    }

    private suspend fun initializeUpvotedQuestionTasks(tasks: MutableList<CrawlingTask>) {
        // 基于用户的历史点赞行为创建相关问题的爬虫任务
        val recentLikes = dao.getBehaviorsByActionSince("like", System.currentTimeMillis() - 30 * 24 * 60 * 60 * 1000L)

        // 提取问题ID并创建任务
        val questionIds = recentLikes
            .mapNotNull { behavior ->
                extractQuestionIdFromContentId(behavior.contentId)
            }.distinct()
            .take(5)

        questionIds.forEach { questionId ->
            tasks.add(
                CrawlingTask(
                    url = "https://www.zhihu.com/api/v4/questions/$questionId/feeds?limit=20",
                    reason = CrawlingReason.UpvotedQuestion,
                    priority = 6,
                ),
            )
        }

        // 如果没有历史数据，创建默认任务
        if (questionIds.isEmpty()) {
            repeat(2) { index ->
                tasks.add(
                    CrawlingTask(
                        url = "https://www.zhihu.com/api/v3/feed/topstory/recommend?desktop=true&limit=10&offset=${index * 10}",
                        reason = CrawlingReason.UpvotedQuestion,
                        priority = 6,
                    ),
                )
            }
        }
    }

    private fun initializeFollowingUpvoteTasks(tasks: MutableList<CrawlingTask>) {
        // 创建关注用户点赞内容的爬虫任务
        repeat(2) { index ->
            tasks.add(
                CrawlingTask(
                    url = "https://www.zhihu.com/api/v3/feed/topstory/recommend?action_feed=True&limit=20&offset=${index * 20}",
                    reason = CrawlingReason.FollowingUpvote,
                    priority = 5,
                ),
            )
        }
    }

    private fun initializeCollaborativeFilteringTasks(tasks: MutableList<CrawlingTask>) {
        // 创建协同过滤推荐任务
        repeat(2) { index ->
            tasks.add(
                CrawlingTask(
                    url = "https://www.zhihu.com/api/v3/feed/topstory/recommend?desktop=true&limit=15&offset=${index * 15}",
                    reason = CrawlingReason.CollaborativeFiltering,
                    priority = 4,
                ),
            )
        }
    }

    private fun createFollowingTasks(count: Int): List<CrawlingTask> = (0 until count).map { index ->
        CrawlingTask(
            url = "https://api.zhihu.com/moments_v3?feed_type=recommend&offset=${index * 10}",
            reason = CrawlingReason.Following,
            priority = 8,
        )
    }

    private fun createTrendingTasks(count: Int): List<CrawlingTask> = (0 until count).map { index ->
        CrawlingTask(
            url = "https://www.zhihu.com/api/v3/feed/topstory/recommend?desktop=true&limit=20&offset=${index * 20}",
            reason = CrawlingReason.Trending,
            priority = 7,
        )
    }

    private suspend fun createUpvotedQuestionTasks(count: Int): List<CrawlingTask> {
        val tasks = mutableListOf<CrawlingTask>()

        // 尝试基于最近的用户行为创建任务
        val recentContent = dao.getMostLikedContent(count)

        recentContent.forEach { (contentId, _) ->
            val questionId = extractQuestionIdFromContentId(contentId)
            if (questionId != null) {
                tasks.add(
                    CrawlingTask(
                        url = "https://www.zhihu.com/api/v4/questions/$questionId/feeds?limit=20",
                        reason = CrawlingReason.UpvotedQuestion,
                        priority = 6,
                    ),
                )
            }
        }

        // 如果不足，用默认任务补充
        while (tasks.size < count) {
            tasks.add(
                CrawlingTask(
                    url = "https://www.zhihu.com/api/v3/feed/topstory/recommend?desktop=true&limit=10&offset=${tasks.size * 10}",
                    reason = CrawlingReason.UpvotedQuestion,
                    priority = 6,
                ),
            )
        }

        return tasks
    }

    private fun createFollowingUpvoteTasks(count: Int): List<CrawlingTask> = (0 until count).map { index ->
        CrawlingTask(
            url = "https://www.zhihu.com/api/v3/feed/topstory/recommend?action_feed=True&limit=20&offset=${index * 20}",
            reason = CrawlingReason.FollowingUpvote,
            priority = 5,
        )
    }

    private fun createCollaborativeFilteringTasks(count: Int): List<CrawlingTask> = (0 until count).map { index ->
        CrawlingTask(
            url = "https://www.zhihu.com/api/v3/feed/topstory/recommend?desktop=true&limit=15&offset=${index * 15}",
            reason = CrawlingReason.CollaborativeFiltering,
            priority = 4,
        )
    }

    private fun extractQuestionIdFromContentId(contentId: String): String? {
        // 从内容ID中提取问题ID（假设内容ID包含问题信息）
        val regex = """(\d+)""".toRegex()
        return regex.find(contentId)?.value
    }
}
