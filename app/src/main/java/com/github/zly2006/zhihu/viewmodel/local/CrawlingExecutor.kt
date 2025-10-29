package com.github.zly2006.zhihu.viewmodel.local

import android.content.Context
import com.github.zly2006.zhihu.data.AccountData
import com.github.zly2006.zhihu.data.CommonFeed
import com.github.zly2006.zhihu.data.Feed
import com.github.zly2006.zhihu.data.target
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject

/**
 * 爬虫执行器，负责执行爬虫任务并生成结果
 */
class CrawlingExecutor(
    private val context: Context,
) {
    private val database by lazy { LocalContentDatabase.getDatabase(context) }
    private val dao by lazy { database.contentDao() }

    /**
     * 执行爬虫任务
     */
    suspend fun executeTask(task: CrawlingTask) {
        withContext(Dispatchers.IO) {
            try {
                // 更新任务状态为进行中
                dao.updateTask(
                    task.copy(
                        status = CrawlingStatus.InProgress,
                        executedAt = System.currentTimeMillis(),
                    ),
                )

                val results = when (task.reason) {
                    CrawlingReason.Following -> executeFollowingTask(task)
                    CrawlingReason.Trending -> executeTrendingTask(task)
                    CrawlingReason.UpvotedQuestion -> executeUpvotedQuestionTask(task)
                    CrawlingReason.FollowingUpvote -> executeFollowingUpvoteTask(task)
                    CrawlingReason.CollaborativeFiltering -> executeCollaborativeFilteringTask(task)
                }

                // 保存爬虫结果
                dao.insertResults(results)

                // 更新任务状态为已完成
                dao.updateTask(task.copy(status = CrawlingStatus.Completed))
            } catch (e: Exception) {
                // 更新任务状态为失败
                dao.updateTask(
                    task.copy(
                        status = CrawlingStatus.Failed,
                        errorMessage = e.message,
                    ),
                )
            }
        }
    }

    /**
     * Generic helper to fetch and process feeds from an API endpoint
     * Reduces code duplication across task execution methods
     */
    private suspend fun fetchAndProcessFeeds(
        url: String,
        taskId: Long,
        reason: CrawlingReason,
        scoreMultiplier: Double = 1.0,
        filter: (Feed) -> Boolean = { true }
    ): List<CrawlingResult> {
        val httpClient = AccountData.httpClient(context)
        val response = httpClient.get(url)
        val jsonData = Json.parseToJsonElement(response.bodyAsText())
        val feedArray = jsonData.jsonObject["data"]?.jsonArray ?: JsonArray(emptyList())

        return feedArray.mapNotNull { feedElement ->
            try {
                val feed = AccountData.decodeJson<Feed>(feedElement)
                if (filter(feed)) {
                    createCrawlingResult(feed, taskId, reason, scoreMultiplier)
                } else {
                    null
                }
            } catch (_: Exception) {
                null
            }
        }
    }

    private suspend fun executeFollowingTask(task: CrawlingTask): List<CrawlingResult> {
        val url = "https://api.zhihu.com/moments_v3?feed_type=recommend"
        return fetchAndProcessFeeds(url, task.id, CrawlingReason.Following)
    }

    private suspend fun executeTrendingTask(task: CrawlingTask): List<CrawlingResult> {
        val url = "https://www.zhihu.com/api/v3/feed/topstory/recommend?desktop=true&limit=20"
        return fetchAndProcessFeeds(url, task.id, CrawlingReason.Trending, scoreMultiplier = 1.2)
    }

    private suspend fun executeUpvotedQuestionTask(task: CrawlingTask): List<CrawlingResult> {
        val questionId = extractQuestionIdFromUrl(task.url) ?: return emptyList()
        val url = "https://www.zhihu.com/api/v4/questions/$questionId/feeds?limit=20"
        return fetchAndProcessFeeds(url, task.id, CrawlingReason.UpvotedQuestion, scoreMultiplier = 1.1)
    }

    private suspend fun executeFollowingUpvoteTask(task: CrawlingTask): List<CrawlingResult> {
        val url = "https://www.zhihu.com/api/v3/feed/topstory/recommend?action_feed=True&limit=20"
        return fetchAndProcessFeeds(
            url = url,
            taskId = task.id,
            reason = CrawlingReason.FollowingUpvote,
            filter = ::isVoteupFeed
        )
    }

    private suspend fun executeCollaborativeFilteringTask(task: CrawlingTask): List<CrawlingResult> {
        // 基于用户行为的协同过滤推荐
        val recentLikes = dao.getBehaviorsByActionSince("like", System.currentTimeMillis() - 7 * 24 * 60 * 60 * 1000)

        if (recentLikes.isEmpty()) {
            // 如果没有点赞行为，回退到推荐内容
            return executeTrendingTask(task.copy(reason = CrawlingReason.CollaborativeFiltering))
        }

        // 基于最近点赞的内容获取相关推荐
        val url = "https://www.zhihu.com/api/v3/feed/topstory/recommend?desktop=true&limit=20"
        return fetchAndProcessFeeds(url, task.id, CrawlingReason.CollaborativeFiltering, scoreMultiplier = 0.9)
    }

    private fun createCrawlingResult(
        feed: Feed,
        taskId: Long,
        reason: CrawlingReason,
        scoreMultiplier: Double = 1.0,
    ): CrawlingResult? {
        val target = feed.target ?: return null

        return CrawlingResult(
            taskId = taskId,
            contentId = when (target) {
                is Feed.AnswerTarget -> target.id.toString()
                is Feed.ArticleTarget -> target.id.toString()
                is Feed.VideoTarget -> target.id.toString()
                is Feed.QuestionTarget -> target.id.toString()
                is Feed.PinTarget -> target.id.toString()
            },
            title = target.title,
            summary = target.excerpt ?: "",
            url = when (target) {
                is Feed.AnswerTarget -> target.url
                is Feed.ArticleTarget -> target.url
                else -> ""
            },
            reason = reason,
            score = calculateContentScore(target) * scoreMultiplier,
        )
    }

    private fun calculateContentScore(target: Feed.Target): Double {
        // 基于内容特征计算评分
        var score = 1.0

        // 基于点赞数和评论数
        when (target) {
            is Feed.AnswerTarget -> {
                score += (target.voteupCount / 100.0).coerceAtMost(5.0)
                score += (target.commentCount / 50.0).coerceAtMost(2.0)
            }
            is Feed.ArticleTarget -> {
                score += (target.voteupCount / 100.0).coerceAtMost(5.0)
                score += (target.commentCount / 50.0).coerceAtMost(2.0)
            }
            is Feed.VideoTarget -> {
                score += (target.voteCount / 100.0).coerceAtMost(5.0)
                score += (target.commentCount / 50.0).coerceAtMost(2.0)
            }
            else -> {
                // 对于其他类型的内容给予基础分数
                score += 0.5
            }
        }

        // 基于内容长度（适中的长度获得更高分数）
        val contentLength = target.excerpt?.length ?: 0
        score += when {
            contentLength in 100..500 -> 1.0
            contentLength in 50..100 || contentLength in 500..1000 -> 0.5
            else -> 0.0
        }

        return score.coerceIn(0.1, 10.0)
    }

    private fun isVoteupFeed(feed: Feed): Boolean {
        // 简化的判断逻辑，根据Feed类型判断是否为点赞相关
        return when (feed) {
            is CommonFeed -> {
                // 检查attachedInfo或brief中是否包含点赞相关信息
                feed.brief.contains("赞同") ||
                    feed.brief.contains("点赞") ||
                    feed.attachedInfo.contains("VOTEUP")
            }
            else -> false
        }
    }

    private fun extractQuestionIdFromUrl(url: String): String? {
        val regex = """question/(\d+)""".toRegex()
        return regex.find(url)?.groupValues?.get(1)
    }
}
