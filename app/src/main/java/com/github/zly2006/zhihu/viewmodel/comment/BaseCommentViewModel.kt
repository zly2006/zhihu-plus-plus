package com.github.zly2006.zhihu.viewmodel.comment

import android.content.Context
import android.widget.Toast
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.viewModelScope
import com.github.zly2006.zhihu.NavDestination
import com.github.zly2006.zhihu.data.AccountData
import com.github.zly2006.zhihu.data.DataHolder
import com.github.zly2006.zhihu.util.signFetchRequest
import com.github.zly2006.zhihu.viewmodel.CommentItem
import com.github.zly2006.zhihu.viewmodel.PaginationViewModel
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.delete
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlin.reflect.typeOf

abstract class BaseCommentViewModel(
    val article: NavDestination,
) : PaginationViewModel<DataHolder.Comment>(typeOf<DataHolder.Comment>()) {
    abstract val submitCommentUrl: String
    protected val commentsMap = mutableMapOf<String, CommentItem>()

    override fun processResponse(context: Context, data: List<DataHolder.Comment>, rawData: JsonArray) {
        super.processResponse(context, data, rawData)
        data.forEach { comment ->
            val commentItem = createCommentItem(comment, article)
            commentsMap[comment.id] = commentItem
            // 载入可见的子评论
            comment.childComments.forEach {
                val childCommentItem = createCommentItem(it, article)
                commentsMap[it.id] = childCommentItem
            }
        }
    }

    abstract fun createCommentItem(comment: DataHolder.Comment, article: NavDestination): CommentItem

    fun getCommentById(id: String): CommentItem? = commentsMap[id]

    fun submitComment(
        content: NavDestination?,
        commentText: String,
        httpClient: HttpClient,
        context: Context,
        onSuccess: () -> Unit,
    ) {
        if (commentText.isBlank()) return

        viewModelScope.launch {
            try {
                val response = httpClient.post(submitCommentUrl) {
                    signFetchRequest(context)
                    contentType(ContentType.Application.Json)
                    setBody("""{"content":"<p>$commentText</p>"}""")
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

    var isLikeLoading by mutableStateOf(false)

    fun toggleLikeComment(
        commentData: DataHolder.Comment,
        httpClient: HttpClient,
        context: Context,
        onSuccess: () -> Unit,
    ) {
        if (isLikeLoading) return
        isLikeLoading = true

        val commentId = commentData.id
        val newLikeState = !commentData.liked
        viewModelScope.launch {
            try {
                val response = if (newLikeState) {
                    // 点赞
                    httpClient.post("https://www.zhihu.com/api/v4/comments/$commentId/like")
                } else {
                    // 取消点赞
                    httpClient.delete("https://www.zhihu.com/api/v4/comments/$commentId/like")
                }

                if (response.status.isSuccess()) {
                    withContext(Dispatchers.Main) {
                        onSuccess()
                    }
                } else {
                    Toast.makeText(context, "操作失败：${response.status}", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "操作失败：${e.message}", Toast.LENGTH_SHORT).show()
                }
            } finally {
                withContext(Dispatchers.Main) {
                    isLikeLoading = false
                }
            }
        }
    }
}
