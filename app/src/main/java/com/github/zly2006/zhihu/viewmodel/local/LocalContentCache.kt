package com.github.zly2006.zhihu.viewmodel.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

/*
关于 本地推荐模式：

本地推荐模式旨在全面尊重您的隐私并给予您更多的控制权。
本地推荐基于协同过滤算法，使用您在应用内的行为数据（如点赞、浏览、评论等）来生成个性化推荐内容。
为了您的隐私，本地推荐模式只使用本模式下的行为数据，使用常规的推荐模式（由知乎服务器提供）不会影响本地推荐的结果。
为了您的隐私，本地推荐模式不会上传任何数据到服务器，所有推荐计算都在本地完成。相对的，常规的推荐模式会将您的浏览数据上传给知乎服务器，以便生成个性化推荐。
本地推荐模式基于爬虫运行，在极端情况下，有可能被服务器限制或封禁。
本地推荐模式预制了多种推荐原因和模式，您可以对不同的主题或原因的权重进行手动调整，以完全控制您的浏览体验。
*/

@Entity(tableName = "crawling_tasks")
data class CrawlingTask(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val url: String,
    val reason: CrawlingReason,
    val status: CrawlingStatus = CrawlingStatus.NotStarted,
    val priority: Int = 0,
    val createdAt: Long = System.currentTimeMillis(),
    val executedAt: Long? = null,
    val errorMessage: String? = null
)

@Entity(tableName = "crawling_results")
data class CrawlingResult(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val taskId: Long,
    /**
     * 内容ID，通常是知乎的唯一标识符
     *
     * "answer:12345678", "question:87654321", etc.
     */
    val contentId: String,
    val title: String,
    val summary: String,
    val url: String,
    val reason: CrawlingReason,
    val score: Double = 0.0,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "local_feeds")
data class LocalFeed(
    @PrimaryKey
    val id: String,
    val resultId: Long,
    val title: String,
    val summary: String,
    val reasonDisplay: String,
    val navDestination: String?,
    val userFeedback: Double = 0.0,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "user_behaviors")
data class UserBehavior(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val contentId: String,
    val action: String, // "like", "dislike", "share", "comment", "view"
    val timestamp: Long = System.currentTimeMillis(),
    val duration: Long? = null // 阅读时长，毫秒
)

enum class CrawlingStatus {
    NotStarted,
    InProgress,
    Completed,
    Failed
}

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
    CollaborativeFiltering
}

interface ZhihuLocalFeedClient {
    suspend fun crawlFollowingFeeds(): List<CrawlingResult>
    suspend fun crawlTrendingFeeds(): List<CrawlingResult>
    suspend fun crawlUpvotedQuestionFeeds(questionId: String): List<CrawlingResult>
}
