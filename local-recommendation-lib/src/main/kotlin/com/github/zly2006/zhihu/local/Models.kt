package com.github.zly2006.zhihu.local

import kotlinx.serialization.Serializable

/**
 * 爬虫任务
 */
@Serializable
data class CrawlingTask(
    val id: Long = 0,
    val url: String,
    val reason: CrawlingReason,
    val status: CrawlingStatus = CrawlingStatus.NotStarted,
    val priority: Int = 0,
    val createdAt: Long = System.currentTimeMillis(),
    val executedAt: Long? = null,
    val errorMessage: String? = null,
)

/**
 * 爬虫结果
 */
@Serializable
data class CrawlingResult(
    val id: Long = 0,
    val taskId: Long,
    val contentId: String,
    val title: String,
    val summary: String,
    val url: String,
    val reason: CrawlingReason,
    val score: Double = 0.0,
    val createdAt: Long = System.currentTimeMillis(),
)

/**
 * 本地Feed
 */
@Serializable
data class LocalFeed(
    val id: String,
    val resultId: Long,
    val title: String,
    val summary: String,
    val reasonDisplay: String,
    val navDestination: String?,
    val userFeedback: Double = 0.0,
    val createdAt: Long = System.currentTimeMillis(),
)

/**
 * 用户行为记录
 */
@Serializable
data class UserBehavior(
    val id: Long = 0,
    val contentId: String,
    val action: String, // "like", "dislike", "share", "comment", "view"
    val timestamp: Long = System.currentTimeMillis(),
    val duration: Long? = null, // 阅读时长，毫秒
)

/**
 * 爬虫状态
 */
enum class CrawlingStatus {
    NotStarted,
    InProgress,
    Completed,
    Failed,
}

/**
 * 爬虫原因
 */
enum class CrawlingReason {
    /**
     * 已关注用户的内容
     */
    Following,

    /**
     * 官方推荐的内容
     */
    Trending,

    /**
     * 已关注用户的赞同
     */
    FollowingUpvote,

    /**
     * 用户赞同了某个回答，相同问题的优质回答
     */
    UpvotedQuestion,

    /**
     * 相似用户点赞的内容
     */
    CollaborativeFiltering,
}

/**
 * 知乎本地Feed客户端接口
 */
interface ZhihuLocalFeedClient {
    suspend fun crawlFollowingFeeds(): List<CrawlingResult>
    suspend fun crawlTrendingFeeds(): List<CrawlingResult>
    suspend fun crawlUpvotedQuestionFeeds(questionId: String): List<CrawlingResult>
}
