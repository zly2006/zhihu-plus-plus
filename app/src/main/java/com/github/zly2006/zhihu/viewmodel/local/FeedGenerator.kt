package com.github.zly2006.zhihu.viewmodel.local

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * 推荐内容生成器，负责从爬虫结果生成LocalFeed
 */
class FeedGenerator(private val context: Context) {
    private val database by lazy { LocalContentDatabase.getDatabase(context) }
    private val dao by lazy { database.contentDao() }

    /**
     * 从爬虫结果生成推荐内容
     */
    suspend fun generateFeedFromResult(result: CrawlingResult, reasonDisplay: String): LocalFeed {
        return withContext(Dispatchers.IO) {
            val feed = LocalFeed(
                id = "feed_${result.id}_${System.currentTimeMillis()}",
                resultId = result.id,
                title = result.title,
                summary = result.summary ?: result.content?.take(200) ?: "暂无摘要",
                reasonDisplay = reasonDisplay,
                navDestination = result.url,
                userFeedback = 0.0
            )

            dao.insertFeed(feed)
            feed
        }
    }

    /**
     * 批量生成推荐内容
     */
    suspend fun generateFeedsFromResults(
        results: List<CrawlingResult>,
        reasonDisplay: String
    ): List<LocalFeed> {
        return withContext(Dispatchers.IO) {
            results.map { result ->
                generateFeedFromResult(result, reasonDisplay)
            }
        }
    }

    /**
     * 根据推荐原因获取理由显示文本
     */
    fun getReasonDisplayText(reason: CrawlingReason): String {
        return when (reason) {
            CrawlingReason.Following -> "关注用户的最新动态"
            CrawlingReason.Trending -> "热门推荐"
            CrawlingReason.FollowingUpvote -> "关注用户点赞的内容"
            CrawlingReason.UpvotedQuestion -> "相关问题的优质回答"
            CrawlingReason.CollaborativeFiltering -> "相似用户喜欢的内容"
        }
    }
}
