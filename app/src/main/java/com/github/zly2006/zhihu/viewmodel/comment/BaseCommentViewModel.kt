package com.github.zly2006.zhihu.viewmodel.comment

import android.content.Context
import android.widget.Toast
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.viewModelScope
import com.github.zly2006.zhihu.NavDestination
import com.github.zly2006.zhihu.data.DataHolder
import com.github.zly2006.zhihu.signFetchRequest
import com.github.zly2006.zhihu.viewmodel.CommentItem
import com.github.zly2006.zhihu.viewmodel.PaginationViewModel
import io.ktor.client.*
import io.ktor.client.request.*
import io.ktor.http.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.JsonArray
import kotlin.reflect.typeOf

abstract class BaseCommentViewModel(
    val article: NavDestination
) : PaginationViewModel<DataHolder.Comment>(typeOf<DataHolder.Comment>()) {
    val comments get() = allData.map { createCommentItem(it, article) }
    protected val commentsMap = mutableMapOf<String, CommentItem>()

    override fun processResponse(data: List<DataHolder.Comment>, rawData: JsonArray) {
        super.processResponse(data, rawData)
        data.forEach { comment ->
            val commentItem = createCommentItem(comment, article)
            commentsMap[comment.id] = commentItem
        }
    }

    abstract fun createCommentItem(comment: DataHolder.Comment, article: NavDestination): CommentItem

    fun getCommentById(id: String): CommentItem? {
        return commentsMap[id]
    }

    fun submitComment(
        content: NavDestination?,
        commentText: String,
        httpClient: HttpClient,
        context: Context,
        onSuccess: () -> Unit
    ) {
        if (commentText.isBlank()) return

        viewModelScope.launch {
            try {
                val url = initialUrl

                val response = httpClient.post(url) {
                    signFetchRequest(context)
                    contentType(ContentType.Application.Json)
                    setBody("""{"content":"$commentText"}""")
                }

                if (response.status.isSuccess()) {
                    // 评论成功后刷新评论列表
                    refresh(context)
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
        onSuccess: () -> Unit
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
