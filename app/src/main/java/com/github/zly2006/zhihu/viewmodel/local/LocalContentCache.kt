package com.github.zly2006.zhihu.viewmodel.local

import androidx.room.Entity
import androidx.room.PrimaryKey

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
     * 相同问题的优质回答
     */
    Question,
}

@Entity(tableName = CrawlingContentCache.TABLE_NAME)
class CrawlingContentCache(
    @PrimaryKey val id: Int,
    val url: String,
    val status: CrawlingStatus = CrawlingStatus.NotStarted,
    val reason: CrawlingReason? = null,
    val lastCrawled: Long = 0L,
) {
    companion object {
        const val TABLE_NAME = "crawling_content_cache"
    }
}

@Entity(tableName = LocalFeed.TABLE_NAME)
class LocalFeed(
    @PrimaryKey val id: String,
    val content: CrawlingContentCache,
    val reasonDisplay: String? = null, // 用于展示的理由文本
    val navDestination: String? = null, // 使用字符串存储 NavDestination 的序列化形式
    val userFeedback: Double = 0.0,
) {
    companion object {
        const val TABLE_NAME = "local_feeds"
    }
}
