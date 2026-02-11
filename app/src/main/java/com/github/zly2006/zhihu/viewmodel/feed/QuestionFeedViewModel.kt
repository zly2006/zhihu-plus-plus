package com.github.zly2006.zhihu.viewmodel.feed

import android.content.Context
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.github.zly2006.zhihu.data.AccountData
import com.github.zly2006.zhihu.data.Feed
import com.github.zly2006.zhihu.data.target
import com.github.zly2006.zhihu.util.signFetchRequest
import io.ktor.http.HttpMethod

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

    override fun createDisplayItem(context: Context, feed: Feed): FeedDisplayItem {
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
        return super.createDisplayItem(context, feed)
    }

    suspend fun followQuestion(context: Context, questionId: Long, follow: Boolean) {
        try {
            val url = "https://www.zhihu.com/api/v4/questions/$questionId/followers"
            AccountData.fetch(context, url) {
                signFetchRequest(context)
                method = if (follow) HttpMethod.Post else HttpMethod.Delete
            }
        } catch (e: Exception) {
            Log.e("QuestionFeedViewModel", "Failed to follow/unfollow question: $questionId", e)
        }
    }
}
