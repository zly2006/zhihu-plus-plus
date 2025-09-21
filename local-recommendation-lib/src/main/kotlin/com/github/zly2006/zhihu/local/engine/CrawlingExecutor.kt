package com.github.zly2006.zhihu.local.engine

import com.github.zly2006.zhihu.local.*
import com.github.zly2006.zhihu.local.database.LocalContentDao
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.plugins.logging.*
import io.ktor.client.request.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.coroutines.delay
import kotlinx.serialization.json.*
import org.slf4j.LoggerFactory
import java.time.Instant

class CrawlingExecutor(private val dao: LocalContentDao) {
    private val logger = LoggerFactory.getLogger(CrawlingExecutor::class.java)

    private val httpClient = HttpClient(CIO) {
        install(ContentNegotiation) {
            json(Json {
                ignoreUnknownKeys = true
                isLenient = true
            })
        }
        install(Logging) {
            level = LogLevel.INFO
        }
    }

    suspend fun executeTask(task: CrawlingTask) {
        logger.info("Executing task ${task.id} for reason ${task.reason}")

        try {
            // 更新任务状态为进行中
            val updatedTask = task.copy(
                status = CrawlingStatus.InProgress,
                executedAt = System.currentTimeMillis()
            )
            dao.updateTask(updatedTask)

            // 执行爬虫逻辑
            val results = when (task.reason) {
                CrawlingReason.Following -> crawlFollowingFeeds(task.url)
                CrawlingReason.Trending -> crawlTrendingFeeds(task.url)
                CrawlingReason.UpvotedQuestion -> crawlUpvotedQuestionFeeds(task.url)
                CrawlingReason.FollowingUpvote -> crawlFollowingUpvoteFeeds(task.url)
                CrawlingReason.CollaborativeFiltering -> crawlCollaborativeFilteringFeeds(task.url)
            }

            // 保存结果
            if (results.isNotEmpty()) {
                val crawlingResults = results.map { result ->
                    result.copy(taskId = task.id, reason = task.reason)
                }
                dao.insertResults(crawlingResults)

                // 更新任务状态为完成
                dao.updateTask(updatedTask.copy(status = CrawlingStatus.Completed))

                logger.info("Task ${task.id} completed successfully with ${results.size} results")
            } else {
                dao.updateTask(updatedTask.copy(
                    status = CrawlingStatus.Failed,
                    errorMessage = "No results found"
                ))
                logger.warn("Task ${task.id} completed but no results found")
            }

        } catch (e: Exception) {
            logger.error("Task ${task.id} failed: ${e.message}", e)

            // 更新任务状态为失败
            val failedTask = task.copy(
                status = CrawlingStatus.Failed,
                executedAt = System.currentTimeMillis(),
                errorMessage = e.message
            )
            dao.updateTask(failedTask)
        }
    }

    private suspend fun crawlFollowingFeeds(url: String): List<CrawlingResult> {
        return try {
            delay(1000) // 模拟网络延迟

            // 模拟爬虫结果（实际实现需要解析真实API响应）
            listOf(
                CrawlingResult(
                    taskId = 0,
                    contentId = "following_${System.currentTimeMillis()}",
                    title = "关注用户的最新动态",
                    summary = "这是来自关注用户的内容摘要...",
                    url = "https://zhihu.com/question/123456",
                    reason = CrawlingReason.Following,
                    score = 0.8 + Math.random() * 0.2
                )
            )
        } catch (e: Exception) {
            logger.error("Failed to crawl following feeds: ${e.message}", e)
            emptyList()
        }
    }

    private suspend fun crawlTrendingFeeds(url: String): List<CrawlingResult> {
        return try {
            delay(1000)

            // 模拟热门内容
            (1..5).map { i ->
                CrawlingResult(
                    taskId = 0,
                    contentId = "trending_${System.currentTimeMillis()}_$i",
                    title = "热门话题 #$i",
                    summary = "这是热门内容的摘要...",
                    url = "https://zhihu.com/question/${123456 + i}",
                    reason = CrawlingReason.Trending,
                    score = 0.7 + Math.random() * 0.3
                )
            }
        } catch (e: Exception) {
            logger.error("Failed to crawl trending feeds: ${e.message}", e)
            emptyList()
        }
    }

    private suspend fun crawlUpvotedQuestionFeeds(url: String): List<CrawlingResult> {
        return try {
            delay(1000)

            // 模拟相关问题
            (1..3).map { i ->
                CrawlingResult(
                    taskId = 0,
                    contentId = "upvoted_question_${System.currentTimeMillis()}_$i",
                    title = "相关问题 #$i",
                    summary = "这是相关问题的摘要...",
                    url = "https://zhihu.com/question/${234567 + i}",
                    reason = CrawlingReason.UpvotedQuestion,
                    score = 0.6 + Math.random() * 0.3
                )
            }
        } catch (e: Exception) {
            logger.error("Failed to crawl upvoted question feeds: ${e.message}", e)
            emptyList()
        }
    }

    private suspend fun crawlFollowingUpvoteFeeds(url: String): List<CrawlingResult> {
        return try {
            delay(1000)

            // 模拟关注用户的赞同
            (1..2).map { i ->
                CrawlingResult(
                    taskId = 0,
                    contentId = "following_upvote_${System.currentTimeMillis()}_$i",
                    title = "关注用户赞同的内容 #$i",
                    summary = "这是关注用户赞同的内容摘要...",
                    url = "https://zhihu.com/answer/${345678 + i}",
                    reason = CrawlingReason.FollowingUpvote,
                    score = 0.5 + Math.random() * 0.4
                )
            }
        } catch (e: Exception) {
            logger.error("Failed to crawl following upvote feeds: ${e.message}", e)
            emptyList()
        }
    }

    private suspend fun crawlCollaborativeFilteringFeeds(url: String): List<CrawlingResult> {
        return try {
            delay(1000)

            // 模拟协同过滤推荐
            (1..3).map { i ->
                CrawlingResult(
                    taskId = 0,
                    contentId = "collaborative_${System.currentTimeMillis()}_$i",
                    title = "为你推荐的内容 #$i",
                    summary = "基于协同过滤算法推荐的内容摘要...",
                    url = "https://zhihu.com/question/${456789 + i}",
                    reason = CrawlingReason.CollaborativeFiltering,
                    score = 0.4 + Math.random() * 0.5
                )
            }
        } catch (e: Exception) {
            logger.error("Failed to crawl collaborative filtering feeds: ${e.message}", e)
            emptyList()
        }
    }
}
