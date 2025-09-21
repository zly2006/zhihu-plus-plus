package com.github.zly2006.zhihu.local.engine

import com.github.zly2006.zhihu.local.*
import java.util.*

class FeedGenerator {

    fun getReasonDisplayText(reason: CrawlingReason): String = when (reason) {
        CrawlingReason.Following -> "来自关注"
        CrawlingReason.Trending -> "热门推荐"
        CrawlingReason.UpvotedQuestion -> "相关问题"
        CrawlingReason.FollowingUpvote -> "关注用户赞同"
        CrawlingReason.CollaborativeFiltering -> "为你推荐"
    }

    fun generateFeedFromResult(result: CrawlingResult, reasonDisplay: String): LocalFeed {
        return LocalFeed(
            id = UUID.randomUUID().toString(),
            resultId = result.id,
            title = result.title,
            summary = result.summary,
            reasonDisplay = reasonDisplay,
            navDestination = result.url,
            userFeedback = 0.0,
            createdAt = System.currentTimeMillis()
        )
    }
}
