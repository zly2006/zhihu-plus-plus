package com.github.zly2006.zhihu.viewmodel.comment

import android.content.Context
import androidx.core.text.htmlEncode
import androidx.lifecycle.viewModelScope
import com.github.zly2006.zhihu.CommentHolder
import com.github.zly2006.zhihu.NavDestination
import com.github.zly2006.zhihu.data.AccountData
import com.github.zly2006.zhihu.data.DataHolder
import com.github.zly2006.zhihu.util.signFetchRequest
import com.github.zly2006.zhihu.viewmodel.CommentItem
import com.github.zly2006.zhihu.viewmodel.comment.RootCommentViewModel.Companion.submitCommentUrl
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

/**
 * 注意：此view model不按照正常VM生命期管理，不要使用viewModel()函数创建
 */
class ChildCommentViewModel(
    content: NavDestination,
) : BaseCommentViewModel(content) {
    override val initialUrl: String = when (content) {
        is CommentHolder -> {
            "https://www.zhihu.com/api/v4/comment_v5/comment/${content.commentId}/child_comment"
        }

        else -> ""
    }

    override fun createCommentItem(comment: DataHolder.Comment, article: NavDestination): CommentItem {
        // 子评论通常不需要可点击的目标
        val commentItem = CommentItem(comment, null)
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
        val commentHolder = content as CommentHolder
        if (commentText.isBlank()) return

        viewModelScope.launch {
            try {
                // Escape HTML special characters to prevent HTML injection
                val escapedText = commentText.htmlEncode()

                // Use buildJsonObject to properly escape JSON special characters
                val requestBody = buildJsonObject {
                    put("content", "<p>$escapedText</p>")
                    put("reply_comment_id", commentHolder.commentId)
                }

                val response = httpClient.post(commentHolder.article.submitCommentUrl) {
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
