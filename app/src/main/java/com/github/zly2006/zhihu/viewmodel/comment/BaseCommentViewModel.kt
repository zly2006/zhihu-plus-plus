package com.github.zly2006.zhihu.viewmodel.comment

import android.content.Context
import android.widget.Toast
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.viewModelScope
import com.github.zly2006.zhihu.NavDestination
import com.github.zly2006.zhihu.data.DataHolder
import com.github.zly2006.zhihu.util.signFetchRequest
import com.github.zly2006.zhihu.viewmodel.CommentItem
import com.github.zly2006.zhihu.viewmodel.PaginationViewModel
import io.ktor.client.HttpClient
import io.ktor.client.request.delete
import io.ktor.client.request.post
import io.ktor.http.isSuccess
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.JsonArray
import kotlin.reflect.typeOf

enum class CommentSortOrder {
    SCORE, // 按热度
    TIME, // 按时间
}

abstract class BaseCommentViewModel(
    val article: NavDestination,
) : PaginationViewModel<DataHolder.Comment>(typeOf<DataHolder.Comment>()) {
    protected val commentsMap = mutableMapOf<String, CommentItem>()
    var sortOrder by mutableStateOf(CommentSortOrder.SCORE)

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

    fun changeSortOrder(newSortOrder: CommentSortOrder, context: Context) {
        if (sortOrder != newSortOrder) {
            sortOrder = newSortOrder
            refresh(context)
        }
    }

    abstract fun submitComment(
        content: NavDestination,
        commentText: String,
        httpClient: HttpClient,
        context: Context,
        onSuccess: () -> Unit,
    )

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
                    httpClient.post("https://www.zhihu.com/api/v4/comments/$commentId/like") {
                        signFetchRequest(context)
                    }
                } else {
                    // 取消点赞
                    httpClient.delete("https://www.zhihu.com/api/v4/comments/$commentId/like") {
                        signFetchRequest(context)
                    }
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
