package com.github.zly2006.zhihu.ui

import android.content.Context
import android.util.Log
import android.widget.Toast
import androidx.compose.runtime.*
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.zly2006.zhihu.Article
import com.github.zly2006.zhihu.MainActivity
import com.github.zly2006.zhihu.data.DataHolder
import com.github.zly2006.zhihu.viewmodel.PaginationViewModel.Paging
import io.ktor.client.HttpClient
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.http.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlin.math.abs

class ArticleViewModel(private val article: Article, val httpClient: HttpClient?) : ViewModel() {
    var title by mutableStateOf("")
    var authorName by mutableStateOf("")
    var authorBio by mutableStateOf("")
    var authorAvatarSrc by mutableStateOf("")
    var content by mutableStateOf("")
    var voteUpCount by mutableStateOf(0)
    var commentCount by mutableStateOf(0)
    var voteUpState by mutableStateOf(VoteUpState.Neutral)
    var questionId by mutableStateOf(0L)
    var collections = mutableStateListOf<Collection>()

    val isFavorited: Boolean
        get() = collections.any { it.is_favorited }

    fun loadArticle(context: Context) {
        if (httpClient == null) return
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                try {
                    if (article.type == "answer") {
                        DataHolder.getAnswerCallback(context, httpClient, article.id) { answer ->
                            if (answer != null) {
                                title = answer.question.title
                                authorName = answer.author.name
                                content = answer.content
                                authorBio = answer.author.headline
                                authorAvatarSrc = answer.author.avatarUrl
                                voteUpCount = answer.voteupCount
                                commentCount = answer.commentCount
                                questionId = answer.question.id
                                voteUpState = when (answer.relationship?.voting) {
                                    1 -> VoteUpState.Up
                                    -1 -> VoteUpState.Down
                                    else -> VoteUpState.Neutral
                                }
                            } else {
                                content = "<h1>回答不存在</h1>"
                                Log.e("ArticleViewModel", "Answer not found")
                            }
                        }
                    } else if (article.type == "article") {
                        DataHolder.getArticleCallback(context, httpClient, article.id) { article ->
                            if (article != null) {
                                title = article.title
                                content = article.content
                                voteUpCount = article.voteupCount
                                commentCount = article.commentCount
                                authorName = article.author.name
                                authorBio = article.author.headline
                                authorAvatarSrc = article.author.avatarUrl
                                voteUpState = when (article.relationship?.voting) {
                                    1 -> VoteUpState.Up
                                    -1 -> VoteUpState.Down
                                    else -> VoteUpState.Neutral
                                }
                            } else {
                                content = "<h1>文章不存在</h1>"
                                Log.e("ArticleViewModel", "Article not found")
                            }
                        }
                    }
                } catch (e: Exception) {
                    Log.e("ArticleViewModel", "Failed to load content", e)
                }
            }
        }
    }

    fun toggleFavorite(collectionId: String, remove: Boolean, context: Context) {
        if (httpClient == null) return
        viewModelScope.launch {
            try {
                val contentType = if (article.type == "answer") "answer" else "article"
                val action = if (remove) "remove" else "add"
                val url = "https://api.zhihu.com/collections/contents/$contentType/${article.id}"
                val body = "${action}_collections=$collectionId"

                val response = httpClient.put(url) {
                    contentType(ContentType.Application.FormUrlEncoded)
                    setBody(body)
                }

                if (response.status.isSuccess()) {
                    loadCollections()
                    Toast.makeText(context, if (remove) "取消收藏成功" else "收藏成功", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(context, "收藏操作失败", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Log.e("ArticleViewModel", "Favorite toggle failed", e)
                Toast.makeText(context, "收藏操作失败: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    fun loadCollections() {
        if (httpClient == null) return
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                try {
                    val contentType = if (article.type == "answer") "answer" else "article"
                    val collectionsUrl = "https://api.zhihu.com/collections/contents/$contentType/${article.id}"
                    val collectionsResponse = httpClient.get(collectionsUrl)

                    if (collectionsResponse.status.isSuccess()) {
                        val collectionsData = collectionsResponse.body<CollectionResponse>()
                        collections.clear()
                        collections.addAll(collectionsData.data)
                    }
                } catch (e: Exception) {
                    Log.e("ArticleViewModel", "Failed to load collections", e)
                }
            }
        }
    }

    fun toggleVoteUp(context: Context) {
        if (httpClient == null) return
        viewModelScope.launch {
            try {
                val newState = if (voteUpState == VoteUpState.Up) VoteUpState.Neutral else VoteUpState.Up
                val endpoint = when (article.type) {
                    "answer" -> "https://www.zhihu.com/api/v4/answers/${article.id}/voters"
                    "article" -> "https://www.zhihu.com/api/v4/articles/${article.id}/voters"
                    else -> return@launch
                }

                val response = httpClient.post(endpoint) {
                    when (article.type) {
                        "answer" -> setBody(mapOf("type" to newState.key))
                        "article" -> setBody(mapOf("voting" to if (newState == VoteUpState.Up) 1 else 0))
                    }
                    contentType(ContentType.Application.Json)
                }.body<Reaction>()

                voteUpState = newState
                voteUpCount = response.voteup_count
            } catch (e: Exception) {
                Log.e("ArticleViewModel", "Vote up failed", e)
                Toast.makeText(context, "点赞失败: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
