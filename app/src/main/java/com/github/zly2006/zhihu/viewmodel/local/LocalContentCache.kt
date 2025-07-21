package com.github.zly2006.zhihu.viewmodel.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.github.zly2006.zhihu.Person
import com.github.zly2006.zhihu.Question

/*
关于 本地推荐模式：

本地推荐模式旨在全面尊重您的隐私并给予您更多的控制权。
本地推荐基于协同过滤算法，使用您在应用内的行为数据（如点赞、浏览、评论等）来生成个性化推荐内容。
为了您的隐私，本地推荐模式只使用本模式下的行为数据，使用常规的推荐模式（由知乎服务器提供）不会影响本地推荐的结果。
为了您的隐私，本地推荐模式不会上传任何数据到服务器，所有推荐计算都在本地完成。相对的，常规的推荐模式会将您的浏览数据上传给知乎服务器，以便生成个性化推荐。
本地推荐模式基于爬虫运行，在极端情况下，有可能被服务器限制或封禁。
本地推荐模式预制了多种推荐原因和模式，您可以对不同的主题或原因的权重进行手动调整，以完全控制您的浏览体验。
*/

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
     * 相似用户点赞的内容。
     *
     * 从评论区中随机发现相似用户，当用户点赞评论后，提升该用户的权重，
     */
    CollaborativeFiltering
}

interface ZhihuLocalFeedClient {
    /**
     * 在一个问题下，随机获取推荐。热度排序
     */
    fun pickAnswers(
        question: Question,
        offset: Int = 0,
    ): List<CrawlingTask>

    /**
     * 获取某个作者的作品列表。时间顺序。
     */
    fun pickAuthorWorks(
        author: Person,
        offset: Int = 0,
    ): List<CrawlingTask>

    /**
     * 获取某个专栏的文章列表。时间顺序。
     */
    fun pickColumnArticles(
        column: Person,
        offset: Int = 0,
    ): List<CrawlingTask>

    /**
     * 获取某个作者关注的人，粉丝量排序。
     */
    fun pickAuthorFollowing(
        author: Person,
        offset: Int = 0,
    ): List<CrawlingTask>
}

@Entity(tableName = CrawlingTask.TABLE_NAME)
data class CrawlingTask(
    @PrimaryKey val id: Int,
    val url: String,
    val status: CrawlingStatus = CrawlingStatus.NotStarted,
    val reason: CrawlingReason? = null,
    val createdAt: Long = System.currentTimeMillis(), // 任务创建时间
    val lastCrawled: Long = 0L,
    val retryCount: Int = 0, // 重试次数
    val priority: Int = 0, // 任务优先级，数字越大优先级越高
    val errorMessage: String? = null,
) {
    companion object {
        const val TABLE_NAME = "crawling_tasks"
    }
}

@Entity(tableName = CrawlingResult.TABLE_NAME)
data class CrawlingResult(
    @PrimaryKey val id: Int,
    val taskId: Int,
    val title: String,
    val content: String? = null,
    val summary: String? = null,
    val authorName: String? = null,
    val authorId: String? = null, // 作者ID，用于协同过滤
    val upvoteCount: Int = 0, // 点赞数
    val commentCount: Int = 0, // 评论数
    val crawledAt: Long = System.currentTimeMillis(),
    val url: String,
    val contentType: String = "answer", // answer, article, question等
) {
    companion object {
        const val TABLE_NAME = "crawling_results"
    }
}

@Entity(tableName = LocalFeed.TABLE_NAME)
data class LocalFeed(
    @PrimaryKey val id: String,
    val resultId: Int,
    val title: String,
    val summary: String,
    val reasonDisplay: String? = null,
    val navDestination: String? = null,
    val userFeedback: Double = 0.0,
    val generatedAt: Long = System.currentTimeMillis(),
    val viewCount: Int = 0, // 用户查看次数
    val lastViewed: Long = 0L, // 最后查看时间
    val isBookmarked: Boolean = false, // 是否收藏
) {
    companion object {
        const val TABLE_NAME = "local_feeds"
    }
}

@Entity(tableName = UserBehavior.TABLE_NAME)
data class UserBehavior(
    @PrimaryKey val id: String,
    val feedId: String, // 关联的LocalFeed
    val actionType: UserActionType, // 用户行为类型
    val timestamp: Long = System.currentTimeMillis(),
    val duration: Long = 0L, // 阅读时长（毫秒）
    val value: Double = 1.0 // 行为权重值
) {
    companion object {
        const val TABLE_NAME = "user_behaviors"
    }
}

enum class UserActionType(val weight: Double) {
    VIEW(1.0),      // 查看
    LIKE(3.0),      // 点赞
    BOOKMARK(5.0),  // 收藏
    SHARE(4.0),     // 分享
    COMMENT(6.0),   // 评论
    LONG_READ(2.0), // 长时间阅读
    QUICK_SKIP(0.1) // 快速跳过
}
