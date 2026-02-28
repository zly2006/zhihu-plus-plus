package com.github.zly2006.zhihu.viewmodel.local

import android.content.Context
import com.github.zly2006.zhihu.data.AccountData
import com.github.zly2006.zhihu.data.Feed
import com.github.zly2006.zhihu.data.target
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.jsonArray

class ZhihuLocalFeedClientImpl(
    private val context: Context,
) : ZhihuLocalFeedClient {
    override suspend fun crawlFollowingFeeds(): List<CrawlingResult> = withContext(Dispatchers.IO) {
        val task = CrawlingTask(
            url = "https://api.zhihu.com/moments_v3?feed_type=recommend",
            reason = CrawlingReason.Following,
            priority = 8,
        )
        executeFollowingCrawl(task)
    }

    override suspend fun crawlTrendingFeeds(): List<CrawlingResult> = withContext(Dispatchers.IO) {
        val task = CrawlingTask(
            url = "https://www.zhihu.com/api/v3/feed/topstory/recommend?desktop=true&limit=20",
            reason = CrawlingReason.Trending,
            priority = 7,
        )
        executeTrendingCrawl(task)
    }

    override suspend fun crawlUpvotedQuestionFeeds(questionId: String): List<CrawlingResult> = withContext(Dispatchers.IO) {
        val task = CrawlingTask(
            url = "https://www.zhihu.com/api/v4/questions/$questionId/feeds?limit=20",
            reason = CrawlingReason.UpvotedQuestion,
            priority = 6,
        )
        executeUpvotedQuestionCrawl(task)
    }

    private suspend fun executeFollowingCrawl(task: CrawlingTask): List<CrawlingResult> {
        val feedArray = AccountData.fetchGet(context, task.url)?.get("data")?.jsonArray ?: JsonArray(emptyList())

        return feedArray.mapNotNull { feedElement ->
            try {
                val feed = AccountData.decodeJson<Feed>(feedElement)
                val target = feed.target
                if (target != null) {
                    CrawlingResult(
                        taskId = task.id,
                        contentId = when (target) {
                            is Feed.AnswerTarget -> target.id.toString()
                            is Feed.ArticleTarget -> target.id.toString()
                            is Feed.QuestionTarget -> target.id.toString()
                            is Feed.VideoTarget -> target.id.toString()
                            is Feed.PinTarget -> target.id.toString()
                        },
                        title = target.title,
                        summary = target.excerpt ?: "",
                        url = target.url,
                        reason = CrawlingReason.Following,
                        score = calculateScore(target),
                    )
                } else {
                    null
                }
            } catch (e: Exception) {
                null
            }
        }
    }

    private suspend fun executeTrendingCrawl(task: CrawlingTask): List<CrawlingResult> {
        val feedArray = AccountData.fetchGet(context, task.url)?.get("data")?.jsonArray ?: JsonArray(emptyList())

        return feedArray.mapNotNull { feedElement ->
            try {
                val feed = AccountData.decodeJson<Feed>(feedElement)
                val target = feed.target
                if (target != null) {
                    CrawlingResult(
                        taskId = task.id,
                        contentId = when (target) {
                            is Feed.AnswerTarget -> target.id.toString()
                            is Feed.ArticleTarget -> target.id.toString()
                            is Feed.QuestionTarget -> target.id.toString()
                            is Feed.VideoTarget -> target.id.toString()
                            is Feed.PinTarget -> target.id.toString()
                        },
                        title = target.title ?: "无标题",
                        summary = target.excerpt ?: "",
                        url = target.url ?: "",
                        reason = CrawlingReason.Trending,
                        score = calculateScore(target) * 1.2,
                    )
                } else {
                    null
                }
            } catch (e: Exception) {
                null
            }
        }
    }

    private suspend fun executeUpvotedQuestionCrawl(task: CrawlingTask): List<CrawlingResult> {
        val feedArray = AccountData.fetchGet(context, task.url)?.get("data")?.jsonArray ?: JsonArray(emptyList())

        return feedArray.mapNotNull { feedElement ->
            try {
                val feed = AccountData.decodeJson<Feed>(feedElement)
                val target = feed.target
                if (target != null) {
                    CrawlingResult(
                        taskId = task.id,
                        contentId = when (target) {
                            is Feed.AnswerTarget -> target.id.toString()
                            is Feed.ArticleTarget -> target.id.toString()
                            is Feed.QuestionTarget -> target.id.toString()
                            is Feed.VideoTarget -> target.id.toString()
                            is Feed.PinTarget -> target.id.toString()
                        },
                        title = target.title,
                        summary = target.excerpt ?: "",
                        url = target.url,
                        reason = CrawlingReason.UpvotedQuestion,
                        score = calculateScore(target) * 1.1,
                    )
                } else {
                    null
                }
            } catch (e: Exception) {
                null
            }
        }
    }

    private fun calculateScore(target: Feed.Target): Double {
        var score = 1.0

        when (target) {
            is Feed.AnswerTarget -> {
                score += (target.voteupCount / 100.0).coerceAtMost(5.0)
                score += (target.commentCount / 50.0).coerceAtMost(2.0)
            }
            is Feed.ArticleTarget -> {
                score += (target.voteupCount / 100.0).coerceAtMost(5.0)
                score += (target.commentCount / 50.0).coerceAtMost(2.0)
            }

            is Feed.PinTarget -> {
                score += (target.favoriteCount / 50.0).coerceAtMost(3.0)
                score += (target.commentCount / 20.0).coerceAtMost(1.0)
            }
            is Feed.QuestionTarget -> {
                score += (target.answerCount / 10.0).coerceAtMost(2.0)
                score += (target.followerCount / 100.0).coerceAtMost(3.0)
                score += (target.commentCount / 20.0).coerceAtMost(1.0)
            }
            is Feed.VideoTarget -> {
                score += (target.commentCount / 50.0).coerceAtMost(1.0)
            }
        }

        val contentLength = target.excerpt?.length ?: 0
        score += when {
            contentLength in 100..500 -> 1.0
            contentLength in 50..100 || contentLength in 500..1000 -> 0.5
            else -> 0.0
        }

        return score.coerceIn(0.1, 10.0)
    }
}
