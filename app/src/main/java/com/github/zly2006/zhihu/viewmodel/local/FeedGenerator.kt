package com.github.zly2006.zhihu.viewmodel.local

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * 推荐内容生成器，负责从爬虫结果生成LocalFeed
 */
class FeedGenerator(
    private val context: Context,
) {
    private val database by lazy { LocalContentDatabase.getDatabase(context) }
    private val dao by lazy { database.contentDao() }

    /**
     * 从爬虫结果生成推荐内容
     */
    suspend fun generateFeedFromResult(result: CrawlingResult, reasonDisplay: String): LocalFeed = withContext(Dispatchers.IO) {
        val feed = LocalFeed(
            id = generateFeedId(result),
            resultId = result.id,
            title = result.title,
            summary = result.summary,
            reasonDisplay = reasonDisplay,
            navDestination = generateNavDestination(result),
            userFeedback = 0.0,
            createdAt = System.currentTimeMillis(),
        )

        dao.insertFeed(feed)
        feed
    }

    /**
     * 批量生成推荐内容
     */
    suspend fun generateFeedsFromResults(
        results: List<CrawlingResult>,
        reasonDisplay: String,
    ): List<LocalFeed> = withContext(Dispatchers.IO) {
        results.map { result ->
            generateFeedFromResult(result, reasonDisplay)
        }
    }

    private fun generateFeedId(result: CrawlingResult): String = "local_feed_${result.contentId}_${result.reason.name.lowercase()}_${System.currentTimeMillis()}"

    private fun generateNavDestination(result: CrawlingResult): String? {
        // 根据内容类型生成导航目标
        return when {
            result.url.contains("/question/") -> {
                val questionId = extractQuestionId(result.url)
                questionId?.let { "question/$it" }
            }
            result.url.contains("/answer/") -> {
                val answerId = extractAnswerId(result.url)
                answerId?.let { "answer/$it" }
            }
            result.url.contains("/p/") -> {
                val articleId = extractArticleId(result.url)
                articleId?.let { "article/$it" }
            }
            else -> null
        }
    }

    private fun extractQuestionId(url: String): String? {
        val regex = """/question/(\d+)""".toRegex()
        return regex.find(url)?.groupValues?.get(1)
    }

    private fun extractAnswerId(url: String): String? {
        val regex = """/answer/(\d+)""".toRegex()
        return regex.find(url)?.groupValues?.get(1)
    }

    private fun extractArticleId(url: String): String? {
        val regex = """/p/(\d+)""".toRegex()
        return regex.find(url)?.groupValues?.get(1)
    }

    /**
     * 根据推荐原因获取理由显示文本
     */
    fun getReasonDisplayText(reason: CrawlingReason): String = when (reason) {
        CrawlingReason.Following -> "关注用户的最新动态"
        CrawlingReason.Trending -> "热门推荐"
        CrawlingReason.FollowingUpvote -> "关注用户点赞的内容"
        CrawlingReason.UpvotedQuestion -> "相关问题的优质回答"
        CrawlingReason.CollaborativeFiltering -> "相似用户喜欢的内容"
    }
}
