package com.github.zly2006.zhihu.viewmodel.feed

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.github.zly2006.zhihu.data.Feed
import com.github.zly2006.zhihu.data.target

class QuestionFeedViewModel(
    private val questionId: Long,
) : BaseFeedViewModel() {
    var sortOrder by mutableStateOf("default")
        private set

    override val initialUrl: String
        get() = "https://www.zhihu.com/api/v4/questions/$questionId/feeds?limit=20&order=$sortOrder"

    fun updateSortOrder(order: String) {
        if (sortOrder != order) {
            sortOrder = order
        }
    }

    override fun createDisplayItem(feed: Feed): FeedDisplayItem {
        val target = feed.target
        if (target is Feed.AnswerTarget) {
            return FeedDisplayItem(
                authorName = target.author?.name ?: "未知作者",
                avatarSrc = target.author?.avatarUrl,
                summary = target.excerpt,
                details = target.detailsText,
                feed = feed,
                title = "",
            )
        }
        return super.createDisplayItem(feed)
    }
}
