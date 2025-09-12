package com.github.zly2006.zhihu.viewmodel

import android.content.Context
import android.util.Log
import android.widget.Toast
import androidx.activity.viewModels
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavBackStackEntry
import com.github.zly2006.zhihu.Article
import com.github.zly2006.zhihu.ArticleType
import com.github.zly2006.zhihu.MainActivity
import com.github.zly2006.zhihu.data.AccountData
import com.github.zly2006.zhihu.data.DataHolder
import com.github.zly2006.zhihu.data.Feed
import com.github.zly2006.zhihu.data.target
import com.github.zly2006.zhihu.signFetchRequest
import com.github.zly2006.zhihu.ui.Collection
import com.github.zly2006.zhihu.ui.CollectionResponse
import com.github.zly2006.zhihu.ui.Reaction
import com.github.zly2006.zhihu.ui.VoteUpState
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put

class ArticleViewModel(
    private val article: Article,
    val httpClient: HttpClient?,
    navBackStackEntry: NavBackStackEntry?,
) : ViewModel() {
    var title by mutableStateOf("")
    var authorId by mutableStateOf("")
    var authorUrlToken by mutableStateOf("")
    var authorName by mutableStateOf("")
    var authorBio by mutableStateOf("")
    var authorAvatarSrc by mutableStateOf("")
    var content by mutableStateOf("")
    var voteUpCount by mutableIntStateOf(0)
    var commentCount by mutableIntStateOf(0)
    var voteUpState by mutableStateOf(VoteUpState.Neutral)
    var questionId by mutableLongStateOf(0L)
    var collections = mutableStateListOf<Collection>()
    var updatedAt by mutableLongStateOf(0L)
    var createdAt by mutableLongStateOf(0L)
    var nextAnswerFuture: Deferred<Feed> = CompletableDeferred()

    // scroll fix
    var rememberedScrollY = navBackStackEntry?.savedStateHandle?.getLiveData<Int>("scrollY", initialValue = 0)
        ?: MutableLiveData<Int>(0)
    var rememberedScrollYSync = true

    init {
        Log.i("zhihu-scroll", "me is $this, savedStateHandle is ${navBackStackEntry?.savedStateHandle}")
        navBackStackEntry?.lifecycle?.addObserver(object : androidx.lifecycle.DefaultLifecycleObserver {
            override fun onPause(owner: LifecycleOwner) {
                rememberedScrollYSync = false
            }
        })
    }

    val isFavorited: Boolean
        get() = collections.any { it.isFavorited }

    // todo: replace this with sqlite
    class ArticlesSharedData : ViewModel() {
        var viewingQuestionId: Long = 0L
        var nextUrl: String = ""
        var destinations = mutableListOf<Feed>()
    }

    @OptIn(ExperimentalStdlibApi::class, DelicateCoroutinesApi::class)
    fun loadArticle(context: Context) {
        if (httpClient == null) return
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                try {
                    if (article.type == ArticleType.Answer) {
                        DataHolder.getAnswerCallback(context, httpClient, article.id) { answer ->
                            if (answer != null) {
                                title = answer.question.title
                                authorName = answer.author.name
                                authorId = answer.author.id
                                authorUrlToken = answer.author.urlToken
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
                                updatedAt = answer.updatedTime
                                createdAt = answer.createdTime

                                (context as? MainActivity)?.postHistory(
                                    Article(
                                        id = answer.id,
                                        type = ArticleType.Answer,
                                        title = answer.question.title,
                                        authorName = answer.author.name,
                                        authorBio = answer.author.headline,
                                        avatarSrc = answer.author.avatarUrl,
                                        excerpt = answer.excerpt,
                                    ),
                                )
                                val sharedData by (context as MainActivity).viewModels<ArticlesSharedData>()
                                nextAnswerFuture = GlobalScope.async {
                                    if (sharedData.destinations.isEmpty() || sharedData.viewingQuestionId != questionId) {
                                        val url =
                                            if (questionId == sharedData.viewingQuestionId && sharedData.nextUrl.isNotEmpty()) {
                                                sharedData.nextUrl
                                            } else {
                                                "https://www.zhihu.com/api/v4/questions/$questionId/feeds?limit=2"
                                            }
                                        val response =
                                            httpClient.get(url) {
                                                signFetchRequest(context)
                                            }
                                        val jojo = response.body<JsonObject>()
                                        if ("data" !in jojo) {
                                            Log.e("ArticleViewModel", "No data found in response: $jojo")
                                            context.mainExecutor.execute {
                                                Toast
                                                    .makeText(
                                                        context,
                                                        "获取回答列表失败: ${jojo["message"]?.jsonPrimitive?.content ?: "未知错误"}",
                                                        Toast.LENGTH_LONG,
                                                    ).show()
                                            }
                                        }
                                        val data = AccountData.decodeJson<List<Feed>>(jojo["data"]!!)
                                        sharedData.nextUrl =
                                            jojo["paging"]
                                                ?.jsonObject
                                                ?.get("next")
                                                ?.jsonPrimitive
                                                ?.content ?: ""
                                        sharedData.viewingQuestionId = questionId
                                        sharedData.destinations = data
                                            .filter {
                                                it.target?.navDestination is Article && it != article // filter out the current article
                                            }.toMutableList()
                                    }
                                    sharedData.destinations.removeAt(0)
                                }
                            } else {
                                content = "<h1>回答不存在</h1>"
                                Log.e("ArticleViewModel", "Answer not found")
                            }
                        }
                    } else if (article.type == ArticleType.Article) {
                        DataHolder.getArticleCallback(context, httpClient, article.id) { article ->
                            if (article != null) {
                                title = article.title
                                content = article.content
                                voteUpCount = article.voteupCount
                                commentCount = article.commentCount
                                authorId = article.author.id
                                authorUrlToken = article.author.urlToken
                                authorName = article.author.name
                                authorBio = article.author.headline
                                authorAvatarSrc = article.author.avatarUrl
                                voteUpState = when (article.relationship?.voting) {
                                    1 -> VoteUpState.Up
                                    -1 -> VoteUpState.Down
                                    else -> VoteUpState.Neutral
                                }
                                updatedAt = article.updated
                                createdAt = article.created

                                (context as? MainActivity)?.postHistory(
                                    Article(
                                        id = article.id,
                                        type = ArticleType.Article,
                                        title = article.title,
                                        authorName = article.author.name,
                                        authorBio = article.author.headline,
                                        avatarSrc = article.author.avatarUrl,
                                        excerpt = article.excerpt,
                                    ),
                                )
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
                val contentType = when (article.type) {
                    ArticleType.Answer -> "answer"
                    ArticleType.Article -> "article"
                }
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

    private val collectionOrder = mutableListOf<String>()

    fun loadCollections() {
        if (httpClient == null) return
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                try {
                    val contentType = when (article.type) {
                        ArticleType.Answer -> "answer"
                        ArticleType.Article -> "article"
                    }
                    val collectionsUrl = "https://api.zhihu.com/collections/contents/$contentType/${article.id}"
                    val collectionsResponse = httpClient.get(collectionsUrl)

                    if (collectionsResponse.status.isSuccess()) {
                        val jojo = collectionsResponse.body<JsonObject>()
                        val collectionsData = AccountData.decodeJson<CollectionResponse>(jojo)
                        collections.clear()
                        collections.addAll(
                            collectionsData.data
                                .sortedWith { a, b ->
                                    val indexA = collectionOrder.indexOf(a.id)
                                    val indexB = collectionOrder.indexOf(b.id)
                                    when {
                                        indexA == -1 && indexB == -1 -> 0
                                        // 把新的放前面
                                        indexA == -1 -> -1
                                        indexB == -1 -> 1
                                        else -> indexA.compareTo(indexB)
                                    }
                                },
                        )
                        collectionOrder.clear()
                        collectionOrder.addAll(collections.map { it.id })
                    }
                } catch (e: Exception) {
                    Log.e("ArticleViewModel", "Failed to load collections", e)
                }
            }
        }
    }

    fun createNewCollection(context: Context, title: String, description: String = "", isPublic: Boolean = false) {
        if (httpClient == null) return
        viewModelScope.launch {
            httpClient.post("https://www.zhihu.com/api/v4/collections") {
                contentType(ContentType.Application.Json)
                setBody(
                    buildJsonObject {
                        put("title", title)
                        put("description", description)
                        put("is_public", isPublic)
                    },
                )
                signFetchRequest(context)
            }
            loadCollections()
        }
    }

    fun toggleVoteUp(context: Context, newState: VoteUpState) {
        if (httpClient == null) return
        viewModelScope.launch {
            try {
                val endpoint = when (article.type) {
                    ArticleType.Answer -> "https://www.zhihu.com/api/v4/answers/${article.id}/voters"
                    ArticleType.Article -> "https://www.zhihu.com/api/v4/articles/${article.id}/voters"
                }

                val response = httpClient
                    .post(endpoint) {
                        when (article.type) {
                            ArticleType.Answer -> setBody(mapOf("type" to newState.key))
                            ArticleType.Article -> setBody(mapOf("voting" to if (newState == VoteUpState.Up) 1 else 0))
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
