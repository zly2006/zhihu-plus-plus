package com.github.zly2006.zhihu.viewmodel.local

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class LocalContentInitializer(private val context: Context) {
    private val database by lazy { LocalContentDatabase.getDatabase(context) }
    private val dao by lazy { database.contentDao() }

    suspend fun initializeIfNeeded() {
        withContext(Dispatchers.IO) {
            try {
                // 检查是否已有任务
                val followingCount = dao.getResultCountByReason(CrawlingReason.Following)
                if (followingCount > 0) return@withContext

                // 初始化各种类型的爬虫任务
                initializeFollowingTasks()
                initializeTrendingTasks()
                initializeUpvoteTasks()
                initializeQuestionTasks()
                initializeCollaborativeTasks()
            } catch (e: Exception) {
                // 静默处理初始化错误
            }
        }
    }

    private suspend fun initializeFollowingTasks() {
        val taskUrls = listOf(
            "https://www.zhihu.com/question/123456789/answer/987654321",
            "https://zhuanlan.zhihu.com/p/123456789",
            "https://www.zhihu.com/question/234567890/answer/876543210",
            "https://zhuanlan.zhihu.com/p/234567890",
            "https://www.zhihu.com/question/345678901/answer/765432109"
        )

        taskUrls.forEachIndexed { index, url ->
            val task = CrawlingTask(
                id = 10000 + index,
                url = url,
                status = CrawlingStatus.NotStarted,
                reason = CrawlingReason.Following,
                lastCrawled = 0L
            )
            dao.insertTask(task)
        }
    }

    private suspend fun initializeTrendingTasks() {
        val taskUrls = listOf(
            "https://www.zhihu.com/question/trending/111111",
            "https://www.zhihu.com/question/trending/222222",
            "https://www.zhihu.com/question/trending/333333",
            "https://zhuanlan.zhihu.com/p/trending/444444",
            "https://www.zhihu.com/question/trending/555555"
        )

        taskUrls.forEachIndexed { index, url ->
            val task = CrawlingTask(
                id = 20000 + index,
                url = url,
                status = CrawlingStatus.NotStarted,
                reason = CrawlingReason.Trending,
                lastCrawled = 0L
            )
            dao.insertTask(task)
        }
    }

    private suspend fun initializeUpvoteTasks() {
        val taskUrls = listOf(
            "https://www.zhihu.com/question/upvoted/111111",
            "https://zhuanlan.zhihu.com/p/upvoted/222222",
            "https://www.zhihu.com/question/upvoted/333333",
            "https://zhuanlan.zhihu.com/p/upvoted/444444"
        )

        taskUrls.forEachIndexed { index, url ->
            val task = CrawlingTask(
                id = 30000 + index,
                url = url,
                status = CrawlingStatus.NotStarted,
                reason = CrawlingReason.FollowingUpvote,
                lastCrawled = 0L
            )
            dao.insertTask(task)
        }
    }

    private suspend fun initializeQuestionTasks() {
        val taskUrls = listOf(
            "https://www.zhihu.com/question/related/111111",
            "https://www.zhihu.com/question/related/222222",
            "https://www.zhihu.com/question/related/333333"
        )

        taskUrls.forEachIndexed { index, url ->
            val task = CrawlingTask(
                id = 40000 + index,
                url = url,
                status = CrawlingStatus.NotStarted,
                reason = CrawlingReason.UpvotedQuestion,
                lastCrawled = 0L
            )
            dao.insertTask(task)
        }
    }

    private suspend fun initializeCollaborativeTasks() {
        val taskUrls = listOf(
            "https://www.zhihu.com/recommendation/collaborative/111111",
            "https://www.zhihu.com/recommendation/collaborative/222222"
        )

        taskUrls.forEachIndexed { index, url ->
            val task = CrawlingTask(
                id = 50000 + index,
                url = url,
                status = CrawlingStatus.NotStarted,
                reason = CrawlingReason.CollaborativeFiltering,
                lastCrawled = 0L
            )
            dao.insertTask(task)
        }
    }
}
