package com.github.zly2006.zhihu.v2.viewmodel

import com.github.zly2006.zhihu.data.Feed
import com.github.zly2006.zhihu.data.target

class QuestionFeedViewModel(private val questionId: Long) : BaseFeedViewModel() {
    override fun getInitialUrl() = "https://www.zhihu.com/api/v4/questions/$questionId/feeds?limit=20&offset=${feeds.size}"

    override fun createDisplayItem(feed: Feed): FeedDisplayItem {
        val target = feed.target
        if (target is Feed.AnswerTarget) {
            return FeedDisplayItem(
                title = target.author.name,
                summary = target.excerpt,
                details = target.detailsText(),
                feed = feed,
                avatarSrc = target.author.avatar_url
            )
        }
        return super.createDisplayItem(feed)
    }
}
