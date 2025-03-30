package com.github.zly2006.zhihu.v2.viewmodel.comment

import android.content.Context
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.zly2006.zhihu.NavDestination
import com.github.zly2006.zhihu.data.AccountData
import com.github.zly2006.zhihu.data.DataHolder
import com.github.zly2006.zhihu.signFetchRequest
import com.github.zly2006.zhihu.v2.viewmodel.CommentItem
import com.github.zly2006.zhihu.v2.viewmodel.feed.BaseFeedViewModel
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject

abstract class BaseCommentViewModel : ViewModel() {
    val comments = mutableStateListOf<CommentItem>()
    protected val commentsMap = mutableMapOf<String, CommentItem>()
    
    var isLoading by mutableStateOf(false)
    var errorMessage by mutableStateOf<String?>(null)
    var paging: BaseFeedViewModel.Paging? = null
    val isEnd get() = paging?.is_end == true

    @Serializable
    class CommentResponse(
        val data: List<DataHolder.Comment>,
        val paging: BaseFeedViewModel.Paging,
    )

    abstract fun getCommentUrl(content: NavDestination?, isRefresh: Boolean): String?

    fun loadComments(content: NavDestination?, httpClient: HttpClient, context: Context, refresh: Boolean = false) {
        if (isLoading) return
        if (isEnd && !refresh) return

        if (refresh) {
            comments.clear()
            commentsMap.clear()
            paging = null
            errorMessage = null
        }

        isLoading = true
        viewModelScope.launch {
            try {
                val url = if (refresh || paging == null) 
                    getCommentUrl(content, true)
                else 
                    paging?.next
                
                if (url == null) {
                    errorMessage = "不支持在此内容下评论"
                    isLoading = false
                    return@launch
                }
                
                val response = httpClient.get(url) {
                    signFetchRequest(context)
                }
                
                if (response.status.isSuccess()) {
                    val jsonObject = response.body<JsonObject>()
                    val parsedComments = AccountData.decodeJson<CommentResponse>(jsonObject)
                    
                    val newComments = parsedComments.data.map { comment ->
                        createCommentItem(comment, content)
                    }
                    
                    comments.addAll(newComments)
                    paging = parsedComments.paging
                } else {
                    errorMessage = "加载评论失败: ${response.status} ${response.bodyAsText()}"
                }
            } catch (e: Exception) {
                Log.e(this::class.simpleName, "Error loading comments", e)
                errorMessage = "加载评论异常: ${e.message}"
            } finally {
                isLoading = false
            }
        }
    }
    
    abstract fun createCommentItem(comment: DataHolder.Comment, content: NavDestination?): CommentItem
    
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
                val url = getCommentUrl(content, true)
                if (url == null) {
                    errorMessage = "不支持在此内容下评论"
                    return@launch
                }
                
                val response = httpClient.post(url) {
                    signFetchRequest(context)
                    contentType(ContentType.Application.Json)
                    setBody("""{"content":"$commentText"}""")
                }
                
                if (response.status.isSuccess()) {
                    // 评论成功后刷新评论列表
                    loadComments(content, httpClient, context, true)
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
