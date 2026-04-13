/*
 * Zhihu++ - Free & Ad-Free Zhihu client for Android.
 * Copyright (C) 2024-2026, zly2006 <i@zly2006.me>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation (version 3 only).
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.github.zly2006.zhihu.viewmodel

import android.app.Activity
import android.content.ClipData
import android.content.ContentValues
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.os.Build
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import android.util.Log
import android.view.View
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.widget.Toast
import androidx.activity.viewModels
import androidx.annotation.RequiresApi
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
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
import com.github.zly2006.zhihu.navigator.CollectionAnswerNavigator
import com.github.zly2006.zhihu.navigator.PaginationInfoNavigator
import com.github.zly2006.zhihu.navigator.QuestionAnswerNavigator
import com.github.zly2006.zhihu.ui.Collection
import com.github.zly2006.zhihu.ui.CollectionResponse
import com.github.zly2006.zhihu.ui.PREFERENCE_NAME
import com.github.zly2006.zhihu.ui.VoteUpState
import com.github.zly2006.zhihu.ui.components.CustomWebView
import com.github.zly2006.zhihu.ui.components.setupUpWebviewClient
import com.github.zly2006.zhihu.util.ArticleExportComment
import com.github.zly2006.zhihu.util.ZhidaSummarySsePayload
import com.github.zly2006.zhihu.util.buildArticleExportCommentsHtml
import com.github.zly2006.zhihu.util.buildArticleExportData
import com.github.zly2006.zhihu.util.buildArticleExportFileName
import com.github.zly2006.zhihu.util.buildArticleExportHtml
import com.github.zly2006.zhihu.util.buildOfflineArticleExportHtml
import com.github.zly2006.zhihu.util.buildZhidaSummaryRequest
import com.github.zly2006.zhihu.util.clipboardManager
import com.github.zly2006.zhihu.util.decodeZhidaAnswerData
import com.github.zly2006.zhihu.util.decodeZhidaStreamErrorMessage
import com.github.zly2006.zhihu.util.mergeSummaryChunk
import com.github.zly2006.zhihu.util.parseZhidaSsePayload
import com.github.zly2006.zhihu.util.prepareArticleExportComment
import com.github.zly2006.zhihu.util.signFetchRequest
import com.github.zly2006.zhihu.viewmodel.comment.RootCommentViewModel.Companion.rootCommentUrl
import io.ktor.client.HttpClient
import io.ktor.client.request.accept
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsChannel
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import io.ktor.utils.io.readLine
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import org.jsoup.Jsoup
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.math.roundToInt
import kotlin.math.sqrt

class ArticleViewModel(
    private val article: Article,
    val httpClient: HttpClient?,
    navBackStackEntry: NavBackStackEntry?,
) : ViewModel() {
    val permissionRequested = MutableLiveData<Unit>()
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
    var ipInfo by mutableStateOf<String?>(null)
    var aiSummaryText by mutableStateOf("")
        private set
    var aiSummaryError by mutableStateOf<String?>(null)
        private set
    var aiSummaryLoading by mutableStateOf(false)
        private set
    private var aiSummaryJob: Job? = null
    private var exportSourceContent: DataHolder.Content? = null

    // scroll fix
    var rememberedScrollY = MutableLiveData(0)
    var rememberedScrollYSync = true

    fun toCachedContent(sourceLabel: String = "此问题"): CachedAnswerContent = CachedAnswerContent(
        article = article,
        title = title,
        authorName = authorName,
        authorBio = authorBio,
        authorAvatarUrl = authorAvatarSrc,
        content = content,
        voteUpCount = voteUpCount,
        commentCount = commentCount,
        sourceLabel = sourceLabel,
    )

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

    // 检查存储权限
    fun hasStoragePermission(context: Context): Boolean = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        // Android 13+ 只需要媒体权限
        ContextCompat.checkSelfPermission(context, android.Manifest.permission.READ_MEDIA_IMAGES) == PackageManager.PERMISSION_GRANTED
    } else {
        // Android 12及以下需要读写外部存储权限
        ContextCompat.checkSelfPermission(context, android.Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED &&
            ContextCompat.checkSelfPermission(context, android.Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
    }

    // 请求存储权限
    fun requestStoragePermission(activity: Activity) {
        val permissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            arrayOf(android.Manifest.permission.READ_MEDIA_IMAGES)
        } else {
            arrayOf(
                android.Manifest.permission.WRITE_EXTERNAL_STORAGE,
                android.Manifest.permission.READ_EXTERNAL_STORAGE,
            )
        }
        ActivityCompat.requestPermissions(activity, permissions, 1001) // 使用请求码1001
    }

    enum class AnswerTransitionDirection {
        DEFAULT,
        VERTICAL_NEXT,
        VERTICAL_PREVIOUS,
        HORIZONTAL_NEXT,
        HORIZONTAL_PREVIOUS,
    }

    /**
     * 缓存的回答完整内容，用于水平滑动预览。
     */
    data class CachedAnswerContent(
        val article: Article,
        val title: String,
        val authorName: String,
        val authorBio: String,
        val authorAvatarUrl: String,
        val content: String,
        val voteUpCount: Int,
        val commentCount: Int,
        /** 来源标签，用于 UI 显示，例如 "此问题"、"「收藏夹名称」" */
        val sourceLabel: String = "此问题",
    )

    // todo: replace this with sqlite
    class ArticlesSharedData : ViewModel() {
        /** 活跃的导航器：管理来源、历史记录和预取 */
        var navigator: com.github.zly2006.zhihu.navigator.AnswerNavigator? by androidx.compose.runtime.mutableStateOf(null)

        /**
         * 导航前由来源界面设置（如 CollectionContentScreen）。
         * [reset] 时会将其应用到 [navigator]。
         */
        var pendingNavigator: com.github.zly2006.zhihu.navigator.AnswerNavigator? = null

        // 缓存的三个 WebView 实例，跨导航存活，避免重建闪动
        var mainWebView: CustomWebView? = null
            private set
        var previousPreviewWebView: CustomWebView? = null
            private set
        var nextPreviewWebView: CustomWebView? = null
            private set

        // 与 WebView 实例绑定的 tag，用于 FrameLayout 包装模式中定位子 WebView
        var mainTag: String? = null
            private set
        var prevTag: String? = null
            private set
        var nextTag: String? = null
            private set

        fun getOrCreateMainWebView(context: Context, answerId: Long): CustomWebView {
            mainWebView?.let { return it }
            val preferences = context.getSharedPreferences(PREFERENCE_NAME, Context.MODE_PRIVATE)
            val useHardwareAcceleration = preferences.getBoolean("webviewHardwareAcceleration", true)
            return CustomWebView(context)
                .apply {
                    if (useHardwareAcceleration) {
                        setLayerType(WebView.LAYER_TYPE_HARDWARE, null)
                    } else {
                        setLayerType(WebView.LAYER_TYPE_SOFTWARE, null)
                    }
                    setupUpWebviewClient()
                }.also {
                    mainWebView = it
                    mainTag = "wv_main_$answerId"
                    it.tag = mainTag
                }
        }

        /**
         * 导航时旋转三个 WebView：
         * NEXT: prev→destroy, main→prev, next→main
         * PREVIOUS: next→destroy, main→next, prev→main
         */
        fun promoteForNavigation(direction: AnswerTransitionDirection) {
            when (direction) {
                AnswerTransitionDirection.HORIZONTAL_NEXT, AnswerTransitionDirection.VERTICAL_NEXT -> {
                    previousPreviewWebView?.destroy()
                    previousPreviewWebView = mainWebView
                    prevTag = mainTag
                    mainWebView = nextPreviewWebView
                    mainTag = nextTag
                    nextPreviewWebView = null
                    nextTag = null
                }
                AnswerTransitionDirection.HORIZONTAL_PREVIOUS, AnswerTransitionDirection.VERTICAL_PREVIOUS -> {
                    nextPreviewWebView?.destroy()
                    nextPreviewWebView = mainWebView
                    nextTag = mainTag
                    mainWebView = previousPreviewWebView
                    mainTag = prevTag
                    previousPreviewWebView = null
                    prevTag = null
                }
                else -> {}
            }
        }

        fun getOrCreatePreviewWebView(context: Context, isNext: Boolean, answerId: Long): CustomWebView {
            val existing = if (isNext) nextPreviewWebView else previousPreviewWebView
            if (existing != null) return existing
            val preferences = context.getSharedPreferences(PREFERENCE_NAME, Context.MODE_PRIVATE)
            val useHardwareAcceleration = preferences.getBoolean("webviewHardwareAcceleration", true)
            return CustomWebView(context)
                .apply {
                    if (useHardwareAcceleration) {
                        setLayerType(WebView.LAYER_TYPE_HARDWARE, null)
                    } else {
                        setLayerType(WebView.LAYER_TYPE_SOFTWARE, null)
                    }
                    setupUpWebviewClient()
                }.also {
                    if (isNext) {
                        nextPreviewWebView = it
                        nextTag = "wv_next_$answerId"
                        it.tag = nextTag
                    } else {
                        previousPreviewWebView = it
                        prevTag = "wv_prev_$answerId"
                        it.tag = prevTag
                    }
                }
        }

        // 用于消除切换闪动：导航前设置，新页面用它初始化
        var pendingInitialContent: CachedAnswerContent? = null

        // 标记是否从回答切换导航进入（避免被 LaunchedEffect 重置方向后误判）
        @Volatile
        var navigatingFromAnswerSwitch = false

        // 导航动画方向
        var answerTransitionDirection = AnswerTransitionDirection.DEFAULT

        fun reset() {
            navigator = pendingNavigator
            pendingNavigator = null
            pendingInitialContent = null
            navigatingFromAnswerSwitch = false
            // 不销毁缓存 WebView，只清除 contentId 让下次重新加载
            mainWebView?.contentId = null
            previousPreviewWebView?.contentId = null
            nextPreviewWebView?.contentId = null
        }

        override fun onCleared() {
            mainWebView?.destroy()
            mainWebView = null
            mainTag = null
            previousPreviewWebView?.destroy()
            previousPreviewWebView = null
            prevTag = null
            nextPreviewWebView?.destroy()
            nextPreviewWebView = null
            nextTag = null
            super.onCleared()
        }
    }

    @OptIn(ExperimentalStdlibApi::class)
    fun loadArticle(context: Context) {
        if (httpClient == null) return
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                try {
                    if (article.type == ArticleType.Answer) {
                        val sharedData by (context as MainActivity).viewModels<ArticlesSharedData>()
                        val answer = DataHolder.getContentDetail(context, article) as? DataHolder.Answer
                        if (answer != null) {
                            exportSourceContent = answer
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
                            voteUpState = when (answer.reaction?.relation?.vote) {
                                "UP" -> VoteUpState.Up
                                "DOWN" -> VoteUpState.Down
                                "Neutral" -> VoteUpState.Neutral
                                else -> VoteUpState.Neutral
                            }
                            updatedAt = answer.updatedTime
                            createdAt = answer.createdTime
                            ipInfo = answer.ipInfo

                            context.postHistory(
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
                            // 设置问题回答导航器（如果当前不是收藏夹导航器）
                            if (sharedData.navigator !is CollectionAnswerNavigator) {
                                val existingNav = sharedData.navigator
                                val isSameQuestion = when (existingNav) {
                                    is QuestionAnswerNavigator -> existingNav.questionId == questionId
                                    is PaginationInfoNavigator -> existingNav.questionId == questionId
                                    else -> false
                                }
                                if (isSameQuestion) {
                                    // 同一问题内导航：更新队列，补充新回答的 prev/next ids
                                    (existingNav as? PaginationInfoNavigator)
                                        ?.let { nav -> answer.paginationInfo?.let { nav.updateFromPaginationInfo(it) } }
                                } else {
                                    sharedData.navigator = answer.paginationInfo?.let {
                                        PaginationInfoNavigator(questionId, it)
                                    } ?: run {
                                        withContext(Dispatchers.Main) {
                                            Toast.makeText(context, "【回答切换】无法获取分页信息，使用默认回答排序", Toast.LENGTH_SHORT).show()
                                        }
                                        QuestionAnswerNavigator(questionId)
                                    }
                                }
                            }
                            sharedData.navigator?.pushAnswer(toCachedContent(sourceLabel = sharedData.navigator?.sourceName ?: "此问题"))

                            // 仅在无前向历史时预取下一个回答
                            sharedData.navigator?.let { nav ->
                                if (nav.currentAnswerIndex >= nav.answerHistory.size - 1) {
                                    nav.prefetchNext(context, article.id)
                                }
                                nav.prefetchPrevious(context, article.id)
                            }
                        } else {
                            content = "<h1>回答不存在</h1>"
                            Log.e("ArticleViewModel", "Answer not found")
                        }
                    } else if (article.type == ArticleType.Article) {
                        val article = DataHolder.getContentDetail(context, article) as? DataHolder.Article
                        if (article != null) {
                            exportSourceContent = article
                            title = article.title
                            content = article.content
                            voteUpCount = article.voteupCount
                            commentCount = article.commentCount
                            authorId = article.author.id
                            authorUrlToken = article.author.urlToken
                            authorName = article.author.name
                            authorBio = article.author.headline
                            authorAvatarSrc = article.author.avatarUrl
                            voteUpState = when (article.reaction?.relation?.vote) {
                                "UP" -> VoteUpState.Up
                                "DOWN" -> VoteUpState.Down
                                "Neutral" -> VoteUpState.Neutral
                                else -> VoteUpState.Neutral
                            }
                            updatedAt = article.updated
                            createdAt = article.created
                            ipInfo = article.ipInfo

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
                    loadCollections(context)
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

    fun requestAiSummary(context: Context) {
        if (httpClient == null) {
            aiSummaryError = "未初始化网络客户端"
            return
        }
        aiSummaryJob?.cancel()
        aiSummaryJob = viewModelScope.launch {
            aiSummaryLoading = true
            aiSummaryError = null
            aiSummaryText = ""
            try {
                val contentType = when (article.type) {
                    ArticleType.Answer -> "answer"
                    ArticleType.Article -> "article"
                }
                val request = buildZhidaSummaryRequest(
                    contentId = article.id,
                    contentType = contentType,
                    title = title.ifBlank { "知乎内容" },
                )
                val response = httpClient.post("https://www.zhihu.com/ai_ingress/stream/completion") {
                    accept(ContentType.Text.EventStream)
                    contentType(ContentType.Application.Json)
                    header("x-xsrftoken", AccountData.data.cookies["_xsrf"] ?: "")
                    setBody(request)
                    signFetchRequest()
                }
                if (!response.status.isSuccess()) {
                    val errorBody = response.bodyAsText()
                    val status = response.status.value
                    val message = parseSummaryErrorMessage(errorBody)
                    throw IllegalStateException(message ?: "总结请求失败（HTTP $status）")
                }

                val channel = response.bodyAsChannel()
                var seenAnswerEvent = false
                var streamEnded = false
                var frameEvent: String? = null
                val frameDataLines = mutableListOf<String>()

                fun handlePayload(payload: ZhidaSummarySsePayload) {
                    when (payload.event.lowercase()) {
                        "answer" -> {
                            val answer = decodeZhidaAnswerData(payload.data) ?: return
                            if (answer.summary.isBlank()) return
                            seenAnswerEvent = true
                            aiSummaryText = if (answer.delta) {
                                mergeSummaryChunk(aiSummaryText, answer.summary)
                            } else {
                                answer.summary
                            }
                        }
                        "error" -> {
                            val message = decodeZhidaStreamErrorMessage(payload.data) ?: "总结失败"
                            throw IllegalStateException(message)
                        }
                        "end" -> streamEnded = true
                        else -> Unit
                    }
                }

                fun flushFrame() {
                    if (frameDataLines.isEmpty()) return
                    val joinedData = frameDataLines.joinToString("\n")
                    frameDataLines.clear()
                    val payload = parseZhidaSsePayload(joinedData, frameEvent) ?: return
                    handlePayload(payload)
                }

                while (!streamEnded) {
                    val line = channel.readLine() ?: break
                    when {
                        line.startsWith("event:") -> {
                            frameEvent = line.substringAfter("event:").trim()
                        }
                        line.startsWith("data:") -> {
                            frameDataLines += line.substringAfter("data:")
                        }
                        line.isBlank() -> {
                            flushFrame()
                            frameEvent = null
                        }
                        line.startsWith(":") -> Unit
                        else -> Unit
                    }
                }

                if (!streamEnded) {
                    flushFrame()
                }

                if (!seenAnswerEvent || aiSummaryText.isBlank()) {
                    aiSummaryError = "未返回可显示的总结内容"
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e("ArticleViewModel", "Failed to summarize article", e)
                aiSummaryError = e.message ?: "总结失败"
            } finally {
                aiSummaryLoading = false
            }
        }
    }

    fun cancelAiSummary() {
        aiSummaryJob?.cancel()
        aiSummaryJob = null
        aiSummaryLoading = false
    }

    private fun parseSummaryErrorMessage(responseBody: String): String? = runCatching {
        val json = AccountData.json.parseToJsonElement(responseBody).jsonObject
        json["error"]
            ?.jsonObject
            ?.get("message")
            ?.jsonPrimitive
            ?.contentOrNull
            ?: json["message"]?.jsonPrimitive?.contentOrNull
    }.getOrNull()

    private val collectionOrder = mutableListOf<String>()

    fun loadCollections(context: Context) {
        if (httpClient == null) return
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                try {
                    val contentType = when (article.type) {
                        ArticleType.Answer -> "answer"
                        ArticleType.Article -> "article"
                    }
                    val collectionsUrl = "https://api.zhihu.com/collections/contents/$contentType/${article.id}?limit=50"
                    val jojo = AccountData.fetchGet(context, collectionsUrl) {
                        signFetchRequest()
                    }!!
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
                } catch (e: Exception) {
                    Log.e("ArticleViewModel", "Failed to load collections", e)
                }
            }
        }
    }

    fun createNewCollection(context: Context, title: String, description: String = "", isPublic: Boolean = false) {
        if (httpClient == null) return
        viewModelScope.launch {
            AccountData.fetchPost(context, "https://www.zhihu.com/api/v4/collections") {
                contentType(ContentType.Application.Json)
                setBody(
                    buildJsonObject {
                        put("title", title)
                        put("description", description)
                        put("is_public", isPublic)
                    },
                )
                signFetchRequest()
            }
            loadCollections(context)
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

                val response = AccountData.fetchPost(context, endpoint) {
                    when (article.type) {
                        ArticleType.Answer -> setBody(mapOf("type" to newState.key))
                        ArticleType.Article -> setBody(mapOf("voting" to if (newState == VoteUpState.Up) 1 else 0))
                    }
                    contentType(ContentType.Application.Json)
                }!!

                voteUpState = newState
                voteUpCount = response["voteup_count"]!!.jsonPrimitive.int
            } catch (e: Exception) {
                Log.e("ArticleViewModel", "Vote up failed", e)
                Toast.makeText(context, "点赞失败: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private data class PreparedExportWebView(
        val webView: WebView,
        val viewportWidthPx: Int,
        val contentHeightPx: Int,
    )

    // 导出为图片 - 使用WebView渲染
    suspend fun exportToImage(context: Context, includeAppAttribution: Boolean, onComplete: (Boolean) -> Unit) {
        exportToImageInternal(
            context = context,
            includeComments = false,
            commentCount = 0,
            includeAppAttribution = includeAppAttribution,
            successMessage = "图片已保存到相册",
            onComplete = onComplete,
        )
    }

    // 导出为带评论的图片 - 使用WebView渲染
    suspend fun exportToImageWithComments(
        context: Context,
        commentCount: Int,
        includeAppAttribution: Boolean,
        onComplete: (Boolean) -> Unit,
    ) {
        exportToImageInternal(
            context = context,
            includeComments = true,
            commentCount = commentCount,
            includeAppAttribution = includeAppAttribution,
            successMessage = "带评论图片已保存到相册",
            onComplete = onComplete,
        )
    }

    suspend fun exportToHtml(
        context: Context,
        includeAppAttribution: Boolean,
        onComplete: (Boolean) -> Unit,
    ) {
        runCatching { requireExportSourceContent() }.onFailure { error ->
            withContext(Dispatchers.Main) {
                Toast.makeText(context, error.message ?: "内容未加载完成", Toast.LENGTH_SHORT).show()
                onComplete(false)
            }
            return
        }

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q && !hasStoragePermission(context)) {
            withContext(Dispatchers.Main) {
                requestStoragePermission(context as Activity)
                permissionRequested.value = Unit
                Toast.makeText(context, "需要存储权限才能导出 HTML，正在请求权限", Toast.LENGTH_SHORT).show()
                onComplete(false)
            }
            return
        }

        try {
            val htmlContent = createOfflineHtmlContent(context, includeAppAttribution)
            val savedLocation = withContext(Dispatchers.IO) {
                saveHtmlToDownloads(context, htmlContent)
            }
            withContext(Dispatchers.Main) {
                Toast.makeText(context, "HTML 已保存到 $savedLocation", Toast.LENGTH_LONG).show()
                onComplete(true)
            }
        } catch (e: Exception) {
            Log.e("ArticleViewModel", "HTML export failed", e)
            withContext(Dispatchers.Main) {
                Toast.makeText(context, "HTML 导出失败: ${e.message}", Toast.LENGTH_SHORT).show()
                onComplete(false)
            }
        }
    }

    private suspend fun exportToImageInternal(
        context: Context,
        includeComments: Boolean,
        commentCount: Int,
        includeAppAttribution: Boolean,
        successMessage: String,
        onComplete: (Boolean) -> Unit,
    ) {
        runCatching { requireExportSourceContent() }.onFailure { error ->
            withContext(Dispatchers.Main) {
                Toast.makeText(context, error.message ?: "内容未加载完成", Toast.LENGTH_SHORT).show()
                onComplete(false)
            }
            return
        }

        if (!hasStoragePermission(context)) {
            withContext(Dispatchers.Main) {
                requestStoragePermission(context as Activity)
                permissionRequested.value = Unit
                Toast.makeText(context, "需要存储权限才能导出图片，正在请求权限", Toast.LENGTH_SHORT).show()
                onComplete(false)
            }
            return
        }

        var preparedWebView: PreparedExportWebView? = null
        var bitmap: Bitmap? = null
        try {
            preparedWebView = prepareExportWebView(
                context = context,
                htmlContent = createHtmlContent(
                    context = context,
                    includeComments = includeComments,
                    commentCount = commentCount,
                    includeAppAttribution = includeAppAttribution,
                ),
                timeoutMs = if (includeComments) 18_000L else 15_000L,
            )
            bitmap = captureExportBitmap(preparedWebView)
            withContext(Dispatchers.IO) {
                saveImageToMediaStore(context, bitmap)
            }
            withContext(Dispatchers.Main) {
                Toast.makeText(context, successMessage, Toast.LENGTH_LONG).show()
                onComplete(true)
            }
        } catch (e: Exception) {
            Log.e("ArticleViewModel", "Image export failed", e)
            val errorPrefix = if (includeComments) "带评论图片导出失败" else "图片导出失败"
            withContext(Dispatchers.Main) {
                Toast.makeText(context, "$errorPrefix: ${e.message}", Toast.LENGTH_SHORT).show()
                onComplete(false)
            }
        } finally {
            bitmap?.recycle()
            preparedWebView?.let { destroyExportWebView(it.webView) }
        }
    }

    private suspend fun prepareExportWebView(
        context: Context,
        htmlContent: String,
        timeoutMs: Long,
    ): PreparedExportWebView = withContext(Dispatchers.Main) {
        suspendCancellableCoroutine { continuation ->
            val webView = createExportWebView(context)
            val mainHandler = Handler(Looper.getMainLooper())
            val viewportWidthPx = resolveExportViewportWidthPx(context)
            var isFinished = false
            var timeoutRunnable = Runnable {}

            fun fail(error: Throwable) {
                if (isFinished) return
                isFinished = true
                mainHandler.removeCallbacks(timeoutRunnable)
                runCatching { webView.stopLoading() }
                runCatching { webView.destroy() }
                if (continuation.isActive) {
                    continuation.resumeWithException(error)
                }
            }

            fun finish(contentHeightPx: Int) {
                if (isFinished) return
                isFinished = true
                mainHandler.removeCallbacks(timeoutRunnable)
                if (continuation.isActive) {
                    continuation.resume(
                        PreparedExportWebView(
                            webView = webView,
                            viewportWidthPx = viewportWidthPx,
                            contentHeightPx = contentHeightPx,
                        ),
                    )
                }
            }

            fun scheduleReadinessCheck(
                attempt: Int = 0,
                lastHeightPx: Int = -1,
                stablePasses: Int = 0,
            ) {
                mainHandler.postDelayed({
                    if (isFinished) return@postDelayed

                    val contentHeightPx = computeExportContentHeightPx(webView)
                    if (contentHeightPx <= 1 && attempt >= 24) {
                        fail(IllegalStateException("内容为空"))
                        return@postDelayed
                    }

                    measureAndLayoutExportWebView(
                        webView = webView,
                        widthPx = viewportWidthPx,
                        heightPx = contentHeightPx.coerceAtLeast(1),
                    )

                    val nextStablePasses = if (contentHeightPx == lastHeightPx) stablePasses + 1 else 0
                    if (contentHeightPx > 1 && (nextStablePasses >= 2 || attempt >= 24)) {
                        finish(contentHeightPx)
                    } else {
                        scheduleReadinessCheck(
                            attempt = attempt + 1,
                            lastHeightPx = contentHeightPx,
                            stablePasses = nextStablePasses,
                        )
                    }
                }, if (attempt == 0) 450L else 180L)
            }

            timeoutRunnable = Runnable {
                fail(IllegalStateException("超时"))
            }

            webView.webViewClient = object : android.webkit.WebViewClient() {
                override fun onPageFinished(view: WebView?, url: String?) {
                    super.onPageFinished(view, url)
                    if (!isFinished) {
                        injectExportFootnoteScript(context, webView) {
                            if (!isFinished) {
                                scheduleReadinessCheck()
                            }
                        }
                    }
                }

                override fun onReceivedError(
                    view: WebView?,
                    request: WebResourceRequest?,
                    error: android.webkit.WebResourceError?,
                ) {
                    super.onReceivedError(view, request, error)
                    if (request?.isForMainFrame != false) {
                        fail(IllegalStateException("加载错误"))
                    }
                }
            }

            measureAndLayoutExportWebView(
                webView = webView,
                widthPx = viewportWidthPx,
                heightPx = 1,
            )
            mainHandler.postDelayed(timeoutRunnable, timeoutMs)
            webView.loadDataWithBaseURL(
                "https://www.zhihu.com",
                htmlContent,
                "text/html",
                "UTF-8",
                null,
            )

            continuation.invokeOnCancellation {
                if (!isFinished) {
                    isFinished = true
                    mainHandler.removeCallbacks(timeoutRunnable)
                    runCatching { webView.stopLoading() }
                    runCatching { webView.destroy() }
                }
            }
        }
    }

    private fun createExportWebView(context: Context): WebView = WebView(context).apply {
        settings.javaScriptEnabled = true
        settings.domStorageEnabled = false
        settings.useWideViewPort = true
        settings.loadWithOverviewMode = false
        settings.builtInZoomControls = false
        settings.displayZoomControls = false
        setBackgroundColor(android.graphics.Color.WHITE)
        isVerticalScrollBarEnabled = false
        isHorizontalScrollBarEnabled = false
    }

    private fun injectExportFootnoteScript(context: Context, webView: WebView, onInjected: () -> Unit) {
        val jsCode = loadExportAssetText(context, "footnotes.js")
        if (jsCode.isBlank()) {
            onInjected()
            return
        }

        runCatching {
            webView.evaluateJavascript(jsCode) {
                onInjected()
            }
        }.onFailure { error ->
            Log.e("ArticleViewModel", "Failed to inject export footnotes", error)
            onInjected()
        }
    }

    private fun loadExportAssetText(context: Context, fileName: String): String = try {
        context.assets.open(fileName).use { inputStream ->
            inputStream.bufferedReader().use { reader ->
                reader.readText()
            }
        }
    } catch (e: Exception) {
        Log.e("ArticleViewModel", "Failed to load export asset: $fileName", e)
        ""
    }

    private fun measureAndLayoutExportWebView(webView: WebView, widthPx: Int, heightPx: Int) {
        val safeHeight = heightPx.coerceAtLeast(1)
        webView.measure(
            View.MeasureSpec.makeMeasureSpec(widthPx.coerceAtLeast(1), View.MeasureSpec.EXACTLY),
            View.MeasureSpec.makeMeasureSpec(safeHeight, View.MeasureSpec.EXACTLY),
        )
        webView.layout(0, 0, widthPx.coerceAtLeast(1), safeHeight)
    }

    private fun resolveExportViewportWidthPx(context: Context): Int = context.resources.displayMetrics.widthPixels
        .coerceAtLeast(1)

    private fun computeExportContentHeightPx(webView: WebView): Int {
        val density = webView.resources.displayMetrics.density
        val contentHeightPx = (webView.contentHeight * density).roundToInt()
        return maxOf(contentHeightPx, webView.measuredHeight, webView.height, 1)
    }

    private suspend fun captureExportBitmap(preparedWebView: PreparedExportWebView): Bitmap = withContext(Dispatchers.Main) {
        val rawWidth = preparedWebView.viewportWidthPx.coerceAtLeast(1)
        val rawHeight = preparedWebView.contentHeightPx.coerceAtLeast(1)
        val maxPixels = 24_000_000.0
        val rawPixels = rawWidth.toDouble() * rawHeight.toDouble()
        val scale = if (rawPixels > maxPixels) sqrt(maxPixels / rawPixels) else 1.0
        val bitmapWidth = (rawWidth * scale).roundToInt().coerceAtLeast(1)
        val bitmapHeight = (rawHeight * scale).roundToInt().coerceAtLeast(1)

        Bitmap.createBitmap(bitmapWidth, bitmapHeight, Bitmap.Config.ARGB_8888).also { bitmap ->
            val canvas = Canvas(bitmap)
            canvas.drawColor(android.graphics.Color.WHITE)
            canvas.scale(
                bitmapWidth.toFloat() / rawWidth.toFloat(),
                bitmapHeight.toFloat() / rawHeight.toFloat(),
            )
            preparedWebView.webView.draw(canvas)
        }
    }

    private suspend fun destroyExportWebView(webView: WebView) {
        withContext(Dispatchers.Main) {
            runCatching {
                webView.stopLoading()
                webView.destroy()
            }
        }
    }

    // 创建HTML内容
    private suspend fun createHtmlContent(
        context: Context,
        includeComments: Boolean,
        commentCount: Int,
        includeAppAttribution: Boolean,
    ): String {
        val commentsHtml = if (includeComments && commentCount > 0) {
            buildArticleExportCommentsHtml(
                comments = fetchExportComments(context, commentCount),
                requestedCount = commentCount,
            )
        } else {
            ""
        }

        return buildArticleExportHtml(
            context = context,
            exportData = buildArticleExportData(
                content = requireExportSourceContent(),
                includeAppAttribution = includeAppAttribution,
            ),
            extraSectionsHtml = commentsHtml,
        )
    }

    private suspend fun createOfflineHtmlContent(
        context: Context,
        includeAppAttribution: Boolean,
    ): String = withContext(Dispatchers.IO) {
        buildOfflineArticleExportHtml(
            context = context,
            content = requireExportSourceContent(),
            includeAppAttribution = includeAppAttribution,
            httpClient = httpClient ?: AccountData.httpClient(context),
        )
    }

    private suspend fun fetchExportComments(
        context: Context,
        requestedCount: Int,
    ): List<ArticleExportComment> {
        val safeRequestedCount = requestedCount.coerceAtLeast(0)
        if (safeRequestedCount == 0) return emptyList()

        val json = AccountData.fetchGet(context, article.rootCommentUrl) {
            url {
                parameters["order"] = "score"
                parameters["limit"] = safeRequestedCount.coerceAtMost(20).toString()
                parameters["include"] = "data[*].content,excerpt,headline"
            }
            signFetchRequest()
        } ?: return emptyList()

        return json["data"]
            ?.jsonArray
            ?.mapNotNull { element ->
                runCatching {
                    AccountData.decodeJson<DataHolder.Comment>(element)
                }.getOrNull()
            }?.take(safeRequestedCount)
            ?.map(::mapExportComment)
            .orEmpty()
    }

    private fun mapExportComment(comment: DataHolder.Comment): ArticleExportComment = prepareArticleExportComment(
        authorName = comment.author.name,
        content = comment.content,
        createdTimeText = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
            .format(Date(comment.createdTime * 1000)),
    )

    private fun buildExportFileName(extension: String): String = buildArticleExportFileName(
        content = requireExportSourceContent(),
        extension = extension,
    )

    private fun requireExportSourceContent(): DataHolder.Content = exportSourceContent
        ?: throw IllegalStateException("内容未加载完成")

    // 使用MediaStore保存图片到公共目录
    private fun saveImageToMediaStore(context: Context, bitmap: Bitmap) {
        val contentResolver = context.contentResolver
        val displayName = buildExportFileName("png")

        val contentValues = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, displayName)
            put(MediaStore.MediaColumns.MIME_TYPE, "image/png")
            put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/Zhihu++")
        }

        val uri = contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)

        uri?.let { imageUri ->
            try {
                contentResolver.openOutputStream(imageUri)?.use { outputStream ->
                    bitmap.compress(Bitmap.CompressFormat.PNG, 90, outputStream)
                }
            } catch (e: Exception) {
                Log.e("ArticleViewModel", "Failed to save image to MediaStore", e)
                throw e
            }
        } ?: throw Exception("Failed to create MediaStore entry")
    }

    private fun saveHtmlToDownloads(context: Context, htmlContent: String): String {
        val displayName = buildExportFileName("html")
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            saveHtmlToDownloadsWithMediaStore(context, displayName, htmlContent)
        } else {
            saveHtmlToLegacyDownloads(displayName, htmlContent)
        }
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    private fun saveHtmlToDownloadsWithMediaStore(
        context: Context,
        displayName: String,
        htmlContent: String,
    ): String {
        val resolver = context.contentResolver
        val contentValues = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, displayName)
            put(MediaStore.MediaColumns.MIME_TYPE, "text/html")
            put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS + "/Zhihu++")
            put(MediaStore.MediaColumns.IS_PENDING, 1)
        }

        val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
            ?: throw IllegalStateException("无法创建下载文件")

        return try {
            resolver.openOutputStream(uri)?.bufferedWriter(Charsets.UTF_8)?.use { writer ->
                writer.write(htmlContent)
            } ?: throw IllegalStateException("无法打开下载文件")

            contentValues.clear()
            contentValues.put(MediaStore.MediaColumns.IS_PENDING, 0)
            resolver.update(uri, contentValues, null, null)
            "Zhihu++/$displayName"
        } catch (e: Exception) {
            resolver.delete(uri, null, null)
            throw e
        }
    }

    @Suppress("DEPRECATION")
    private fun saveHtmlToLegacyDownloads(displayName: String, htmlContent: String): String {
        val downloadsDir = File(
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
            "Zhihu++",
        )
        if (!downloadsDir.exists() && !downloadsDir.mkdirs()) {
            throw IllegalStateException("无法创建下载目录")
        }

        val file = File(downloadsDir, displayName)
        file.writeText(htmlContent)
        return file.absolutePath
    }

    // 转换为Markdown格式
    fun convertToMarkdown(): String {
        val sb = StringBuilder()

        // 标题
        sb.append("# $title\n\n")

        // 作者信息
        sb.append("**作者**: $authorName\n\n")
        if (authorBio.isNotEmpty()) {
            sb.append("**简介**: $authorBio\n\n")
        }

        // 分隔线
        sb.append("---\n\n")

        // 内容 - 使用 Jsoup 解析 HTML 并转换为 Markdown
        val document = Jsoup.parse(content)
        sb.append(htmlToMarkdown(document.body()))

        return sb.toString()
    }

    // HTML 转 Markdown 的递归函数
    private fun htmlToMarkdown(element: org.jsoup.nodes.Element): String {
        val sb = StringBuilder()

        for (node in element.childNodes()) {
            when (node) {
                is org.jsoup.nodes.Element -> {
                    when (node.tagName().lowercase()) {
                        "h1" -> sb.append("# ${node.text()}\n\n")
                        "h2" -> sb.append("## ${node.text()}\n\n")
                        "h3" -> sb.append("### ${node.text()}\n\n")
                        "h4" -> sb.append("#### ${node.text()}\n\n")
                        "h5" -> sb.append("##### ${node.text()}\n\n")
                        "h6" -> sb.append("###### ${node.text()}\n\n")
                        "p" -> sb.append("${htmlToMarkdown(node)}\n\n")
                        "br" -> sb.append("\n")
                        "strong", "b" -> sb.append("**${node.text()}**")
                        "em", "i" -> sb.append("*${node.text()}*")
                        "u" -> sb.append("_${node.text()}_")
                        "code" -> sb.append("`${node.text()}`")
                        "pre" -> sb.append("```\n${node.text()}\n```\n\n")
                        "blockquote" -> {
                            val lines = htmlToMarkdown(node).trim().split("\n")
                            for (line in lines) {
                                sb.append("> $line\n")
                            }
                            sb.append("\n")
                        }
                        "ul", "ol" -> {
                            val items = node.select("li")
                            items.forEachIndexed { index, item ->
                                val prefix = if (node.tagName() == "ul") "- " else "${index + 1}. "
                                sb.append("$prefix${htmlToMarkdown(item).trim()}\n")
                            }
                            sb.append("\n")
                        }
                        "li" -> sb.append(htmlToMarkdown(node))
                        "a" -> {
                            val href = node.attr("href")
                            val text = node.text()
                            if (href.isNotEmpty()) {
                                sb.append("[$text]($href)")
                            } else {
                                sb.append(text)
                            }
                        }
                        "img" -> {
                            val src = node.attr("src").ifEmpty { node.attr("data-actualsrc") }
                            val alt = node.attr("alt").ifEmpty { "image" }
                            if (src.isNotEmpty()) {
                                sb.append("![$alt]($src)\n\n")
                            }
                        }
                        "figure" -> {
                            // 知乎的图片通常在 figure 标签中
                            val img = node.selectFirst("img")
                            if (img != null) {
                                val src = img.attr("src").ifEmpty { img.attr("data-actualsrc") }
                                val alt = img.attr("alt").ifEmpty { "image" }
                                if (src.isNotEmpty()) {
                                    sb.append("![$alt]($src)\n\n")
                                }
                            } else {
                                sb.append(htmlToMarkdown(node))
                            }
                        }
                        "hr" -> sb.append("---\n\n")
                        "table" -> {
                            // 简单的表格处理
                            val rows = node.select("tr")
                            if (rows.isNotEmpty()) {
                                // 表头
                                val headerCells = rows[0].select("th, td")
                                if (headerCells.isNotEmpty()) {
                                    sb.append("| ")
                                    headerCells.forEach { cell ->
                                        sb.append("${cell.text()} | ")
                                    }
                                    sb.append("\n")
                                    // 分隔线
                                    sb.append("| ")
                                    headerCells.forEach { _ ->
                                        sb.append("--- | ")
                                    }
                                    sb.append("\n")
                                }
                                // 表格内容
                                for (i in 1 until rows.size) {
                                    val cells = rows[i].select("td")
                                    if (cells.isNotEmpty()) {
                                        sb.append("| ")
                                        cells.forEach { cell ->
                                            sb.append("${cell.text()} | ")
                                        }
                                        sb.append("\n")
                                    }
                                }
                                sb.append("\n")
                            }
                        }
                        "div", "span" -> {
                            // 检查是否是知乎的特殊标签
                            val className = node.attr("class")
                            if (className.contains("highlight")) {
                                // 代码块
                                val code = node.selectFirst("code")
                                if (code != null) {
                                    sb.append("```\n${code.text()}\n```\n\n")
                                } else {
                                    sb.append(htmlToMarkdown(node))
                                }
                            } else {
                                sb.append(htmlToMarkdown(node))
                            }
                        }
                        else -> sb.append(htmlToMarkdown(node))
                    }
                }
                is org.jsoup.nodes.TextNode -> {
                    val text = node.text()
                    if (text.isNotBlank()) {
                        sb.append(text)
                    }
                }
            }
        }

        return sb.toString()
    }

    // 导出到剪贴板
    fun exportToClipboard(context: Context) {
        val markdown = convertToMarkdown()

        // 将Markdown文本复制到剪贴板
        val clip = ClipData.newPlainText("Zhihu Article", markdown)
        context.clipboardManager.setPrimaryClip(clip)

        Toast.makeText(context, "文章已复制到剪贴板", Toast.LENGTH_SHORT).show()
    }
}
