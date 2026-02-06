package com.github.zly2006.zhihu.viewmodel.comment

import android.content.Context
import androidx.core.text.htmlEncode
import androidx.lifecycle.viewModelScope
import com.github.zly2006.zhihu.Article
import com.github.zly2006.zhihu.ArticleType
import com.github.zly2006.zhihu.CommentHolder
import com.github.zly2006.zhihu.NavDestination
import com.github.zly2006.zhihu.Pin
import com.github.zly2006.zhihu.Question
import com.github.zly2006.zhihu.data.AccountData
import com.github.zly2006.zhihu.data.DataHolder
import com.github.zly2006.zhihu.util.signFetchRequest
import com.github.zly2006.zhihu.viewmodel.CommentItem
import com.github.zly2006.zhihu.viewmodel.comment.CommentSortOrder
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import kotlinx.coroutines.launch
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

class RootCommentViewModel(
    content: NavDestination,
) : BaseCommentViewModel(content) {
    companion object {
        val NavDestination.submitCommentUrl: String
            get() = when (this) {
                is Article -> {
                    when (type) {
                        ArticleType.Answer -> "https://www.zhihu.com/api/v4/comment_v5/answers/$id/comment"
                        ArticleType.Article -> "https://www.zhihu.com/api/v4/comment_v5/articles/$id/comment"
                    }
                }

                is Pin -> {
                    "https://www.zhihu.com/api/v4/comment_v5/pins/$id/comment"
                }

                is Question -> {
                    "https://www.zhihu.com/api/v4/comment_v5/questions/$questionId/comment"
                }

                else -> ""
            }
    }

    override val initialUrl: String
        get() {
            val baseUrl = when (article) {
                is Article -> {
                    when (article.type) {
                        ArticleType.Answer -> "https://www.zhihu.com/api/v4/comment_v5/answers/${article.id}/root_comment"
                        ArticleType.Article -> "https://www.zhihu.com/api/v4/comment_v5/articles/${article.id}/root_comment"
                    }
                }

                is Pin -> {
                    "https://www.zhihu.com/api/v4/comment_v5/pins/${article.id}/root_comment"
                }

                is Question -> {
                    "https://www.zhihu.com/api/v4/comment_v5/questions/${article.questionId}/root_comment"
                }

                else -> ""
            }
            // 添加排序参数
            val orderParam = when (sortOrder) {
                CommentSortOrder.SCORE -> "score"
                CommentSortOrder.TIME -> "time"
            }
            return "$baseUrl?order=$orderParam"
        }

    override fun createCommentItem(comment: DataHolder.Comment, article: NavDestination): CommentItem {
        val clickTarget = CommentHolder(comment.id, article)

        val commentItem = CommentItem(comment, clickTarget)
        commentsMap[comment.id] = commentItem
        return commentItem
    }

    override fun submitComment(
        content: NavDestination,
        commentText: String,
        httpClient: HttpClient,
        context: Context,
        onSuccess: () -> Unit,
    ) {
        if (commentText.isBlank()) return

        viewModelScope.launch {
            try {
                // Escape HTML special characters to prevent HTML injection
                val escapedText = commentText.htmlEncode()

                // Use buildJsonObject to properly escape JSON special characters
                val requestBody = buildJsonObject {
                    put("content", "<p>$escapedText</p>")
                }

                val response = httpClient.post(content.submitCommentUrl) {
                    signFetchRequest(context)
                    contentType(ContentType.Application.Json)
                    setBody(requestBody)
                }

                if (response.status.isSuccess()) {
                    // 评论成功后，把它添加到第一个。
                    val model = AccountData.decodeJson<DataHolder.Comment>(response.body<JsonObject>())
                    allData.add(0, model)
                    onSuccess()
                } else {
                    errorMessage = "评论发送失败: ${response.status}"
                }
            } catch (e: Exception) {
                errorMessage = "评论发送异常: ${e.message}"
            }
        }
    }
}
