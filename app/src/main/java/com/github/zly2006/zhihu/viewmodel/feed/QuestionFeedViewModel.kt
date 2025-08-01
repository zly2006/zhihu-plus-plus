package com.github.zly2006.zhihu.viewmodel.feed

import com.github.zly2006.zhihu.data.Feed
import com.github.zly2006.zhihu.data.target

class QuestionFeedViewModel(private val questionId: Long) : BaseFeedViewModel() {
    override val initialUrl: String
        get() = "https://www.zhihu.com/api/v4/questions/$questionId/feeds?limit=20"

    override fun createDisplayItem(feed: Feed): FeedDisplayItem {
        val target = feed.target
        if (target is Feed.AnswerTarget) {
            return FeedDisplayItem(
                authorName = target.author?.name ?: "未知作者",
                avatarSrc = target.author?.avatarUrl,
                summary = target.excerpt,
                details = target.detailsText,
                feed = feed,
                title = ""
            )
        }
        return super.createDisplayItem(feed)
    }
}
