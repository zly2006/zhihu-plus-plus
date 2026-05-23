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

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fleeksoft.ksoup.Ksoup
import com.fleeksoft.ksoup.nodes.Element
import com.fleeksoft.ksoup.nodes.TextNode
import com.github.zly2006.zhihu.navigation.Article
import com.github.zly2006.zhihu.navigation.ArticleType
import com.github.zly2006.zhihu.navigation.CollectionAnswerNavigator
import com.github.zly2006.zhihu.navigation.PaginationInfoNavigator
import com.github.zly2006.zhihu.navigation.QuestionAnswerNavigator
import com.github.zly2006.zhihu.shared.article.CachedAnswerContent
import com.github.zly2006.zhihu.shared.article.VoteUpState
import com.github.zly2006.zhihu.shared.data.Collection
import com.github.zly2006.zhihu.shared.data.DataHolder
import com.github.zly2006.zhihu.shared.data.OfficialBadge
import com.github.zly2006.zhihu.shared.data.ZhihuJson
import com.github.zly2006.zhihu.shared.data.officialBadge
import com.github.zly2006.zhihu.shared.util.Log
import com.github.zly2006.zhihu.shared.util.ZhidaSummarySsePayload
import com.github.zly2006.zhihu.shared.util.applySegmentInfosToHtml
import com.github.zly2006.zhihu.shared.util.buildZhidaSummaryRequest
import com.github.zly2006.zhihu.shared.util.decodeZhidaAnswerData
import com.github.zly2006.zhihu.shared.util.decodeZhidaStreamErrorMessage
import com.github.zly2006.zhihu.shared.util.mergeSummaryChunk
import com.github.zly2006.zhihu.shared.util.parseZhidaSsePayload
import com.github.zly2006.zhihu.ui.ArticleAnswerTransitionDirection
import com.github.zly2006.zhihu.ui.ArticlePreviewWebViewStore
import com.github.zly2006.zhihu.ui.formatArticleDateTime
import com.github.zly2006.zhihu.util.ArticleExportComment
import com.github.zly2006.zhihu.util.buildArticleExportCommentsHtml
import com.github.zly2006.zhihu.util.buildArticleExportFileName
import com.github.zly2006.zhihu.util.prepareArticleExportComment
import io.ktor.client.HttpClient
import io.ktor.client.request.accept
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
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

class ArticleViewModel(
    private val article: Article,
    val httpClient: HttpClient?,
    private val runtimeProvider: (Context) -> ArticleViewModelRuntime = { context ->
        AndroidArticleViewModelRuntime(context)
    },
    registerOnPause: (((() -> Unit) -> Unit))? = null,
) : ViewModel() {
    var permissionRequestCount by mutableIntStateOf(0)
    var title by mutableStateOf("")
    var authorId by mutableStateOf("")
    var authorUrlToken by mutableStateOf("")
    var authorName by mutableStateOf("")
    var authorBio by mutableStateOf("")
    var authorAvatarSrc by mutableStateOf("")
    var authorBadge by mutableStateOf<OfficialBadge?>(null)
    var content by mutableStateOf("")
    var attachment by mutableStateOf<JsonElement?>(null)
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
    var rememberedScrollY by mutableIntStateOf(0)
    var rememberedScrollYSync = true

    fun toCachedContent(sourceLabel: String = "此问题"): CachedAnswerContent = CachedAnswerContent(
        article = article,
        title = title,
        authorName = authorName,
        authorBio = authorBio,
        authorAvatarUrl = authorAvatarSrc,
        authorBadge = authorBadge,
        content = content,
        voteUpCount = voteUpCount,
        commentCount = commentCount,
        createdAt = createdAt,
        updatedAt = updatedAt,
        ipInfo = ipInfo,
        sourceLabel = sourceLabel,
    )

    init {
        Log.i("zhihu-scroll", "me is $this")
        registerOnPause?.invoke {
            rememberedScrollYSync = false
        }
    }

    val isFavorited: Boolean
        get() = collections.any { it.isFavorited }

    private fun articleRuntime(context: Context): ArticleViewModelRuntime =
        runtimeProvider(context)

    // todo: replace this with sqlite
    class ArticlesSharedData :
        ArticleAnswerSwitchData(),
        ArticlePreviewWebViewStore {
        private val previewWebViews = AndroidArticlePreviewWebViewStore()

        fun getOrCreateMainWebView(context: Context, answerId: Long) =
            previewWebViews.getOrCreateMainWebView(context, answerId)

        /**
         * 导航时旋转三个 WebView：
         * NEXT: prev→destroy, main→prev, next→main
         * PREVIOUS: next→destroy, main→next, prev→main
         */
        override fun promoteForNavigation(direction: ArticleAnswerTransitionDirection) {
            super.promoteForNavigation(direction)
            previewWebViews.promoteForNavigation(direction)
        }

        override fun getOrCreatePreviewWebView(
            context: Context,
            isNext: Boolean,
            answerId: Long,
        ) = previewWebViews.getOrCreatePreviewWebView(context, isNext, answerId)

        override fun reset() {
            super.reset()
            previewWebViews.clearContentIds()
        }

        override fun onCleared() {
            previewWebViews.destroyAll()
            super.onCleared()
        }
    }

    @OptIn(ExperimentalStdlibApi::class)
    fun loadArticle(context: Context) {
        if (httpClient == null) return
        val runtime = articleRuntime(context)
        loadArticle(runtime)
    }

    @OptIn(ExperimentalStdlibApi::class)
    fun loadArticle(runtime: ArticleViewModelRuntime) {
        if (httpClient == null) return
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                try {
                    if (article.type == ArticleType.Answer) {
                        val sharedData = runtime.articleAnswerSwitchState()
                        val answer = runtime.getContentDetail(article) as? DataHolder.Answer
                        if (answer != null) {
                            exportSourceContent = answer
                            title = answer.question.title
                            authorName = answer.author.name
                            authorId = answer.author.id
                            authorUrlToken = answer.author.urlToken
                            content = applySegmentInfosToHtml(
                                content = answer.content,
                                segmentInfos = answer.segmentInfos,
                                sourceUrl = "https://www.zhihu.com/question/${answer.question.id}/answer/${answer.id}",
                                contentId = answer.id.toString(),
                                contentType = "answer",
                            )
                            attachment = answer.attachment
                            authorBio = answer.author.headline
                            authorAvatarSrc = answer.author.avatarUrl
                            authorBadge = answer.author.badgeV2.officialBadge()
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

                            runtime.postHistoryDestination(
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
                            runtime.recordOpenEvent(article, answer.question.id)
                            // 设置问题回答导航器（如果当前不是收藏夹导航器）
                            if (sharedData?.navigator !is CollectionAnswerNavigator) {
                                val existingNav = sharedData?.navigator
                                val isSameQuestion = when (existingNav) {
                                    is QuestionAnswerNavigator -> existingNav.questionId == questionId
                                    is PaginationInfoNavigator -> existingNav.questionId == questionId
                                    else -> false
                                }
                                if (!isSameQuestion) {
                                    sharedData?.navigator = QuestionAnswerNavigator(
                                        questionId = questionId,
                                        repository = runtime.answerNavigatorRepository(),
                                    )
                                }
                            }
                            sharedData?.navigator?.pushAnswer(toCachedContent(sourceLabel = sharedData.navigator?.sourceName ?: "此问题"))

                            // 仅在无前向历史时预取下一个回答
                            sharedData?.navigator?.let { nav ->
                                if (nav.currentAnswerIndex >= nav.answerHistory.size - 1) {
                                    nav.prefetchNext(article.id)
                                }
                                nav.prefetchPrevious(article.id)
                            }
                        } else {
                            content = "<h1>回答不存在</h1>"
                            Log.e("ArticleViewModel", "Answer not found")
                        }
                    } else if (article.type == ArticleType.Article) {
                        val article = runtime.getContentDetail(article) as? DataHolder.Article
                        if (article != null) {
                            exportSourceContent = article
                            title = article.title
                            content = applySegmentInfosToHtml(
                                content = article.content,
                                segmentInfos = article.segmentInfos,
                                sourceUrl = "https://zhuanlan.zhihu.com/p/${article.id}",
                                contentId = article.id.toString(),
                                contentType = "article",
                            )
                            voteUpCount = article.voteupCount
                            commentCount = article.commentCount
                            authorId = article.author.id
                            authorUrlToken = article.author.urlToken
                            authorName = article.author.name
                            authorBio = article.author.headline
                            authorAvatarSrc = article.author.avatarUrl
                            authorBadge = article.author.badgeV2.officialBadge()
                            voteUpState = when (article.reaction?.relation?.vote) {
                                "UP" -> VoteUpState.Up
                                "DOWN" -> VoteUpState.Down
                                "Neutral" -> VoteUpState.Neutral
                                else -> VoteUpState.Neutral
                            }
                            updatedAt = article.updated
                            createdAt = article.created
                            ipInfo = article.ipInfo

                            runtime.postHistoryDestination(
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
                            runtime.recordOpenEvent(this@ArticleViewModel.article, null)
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
        val runtime = articleRuntime(context)
        toggleFavorite(collectionId, remove, runtime)
    }

    fun toggleFavorite(collectionId: String, remove: Boolean, runtime: ArticleViewModelRuntime) {
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
                    loadCollections(runtime)
                    runtime.showMessage(if (remove) "取消收藏成功" else "收藏成功")
                } else {
                    runtime.showMessage("收藏操作失败")
                }
            } catch (e: Exception) {
                Log.e("ArticleViewModel", "Favorite toggle failed", e)
                runtime.showMessage("收藏操作失败: ${e.message}")
            }
        }
    }

    fun requestAiSummary(context: Context) {
        if (httpClient == null) {
            aiSummaryError = "未初始化网络客户端"
            return
        }
        val runtime = articleRuntime(context)
        requestAiSummary(runtime)
    }

    fun requestAiSummary(runtime: ArticleViewModelRuntime) {
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
                    header("x-xsrftoken", runtime.xsrfToken())
                    setBody(request)
                    runtime.configureSignedRequest(this)
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
        val json = ZhihuJson.json.parseToJsonElement(responseBody).jsonObject
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
        val runtime = articleRuntime(context)
        loadCollections(runtime)
    }

    fun loadCollections(runtime: ArticleViewModelRuntime) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                try {
                    val contentType = when (article.type) {
                        ArticleType.Answer -> "answer"
                        ArticleType.Article -> "article"
                    }
                    val collectionsData = runtime.loadCollections(contentType, article.id)
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
        val runtime = articleRuntime(context)
        createNewCollection(runtime, title, description, isPublic)
    }

    fun createNewCollection(
        runtime: ArticleViewModelRuntime,
        title: String,
        description: String = "",
        isPublic: Boolean = false,
    ) {
        if (httpClient == null) return
        viewModelScope.launch {
            runtime.createNewCollection(title, description, isPublic)
            loadCollections(runtime)
        }
    }

    fun toggleVoteUp(context: Context, newState: VoteUpState) {
        val runtime = articleRuntime(context)
        toggleVoteUp(runtime, newState)
    }

    fun toggleVoteUp(runtime: ArticleViewModelRuntime, newState: VoteUpState) {
        viewModelScope.launch {
            try {
                val response = runtime.voteArticle(article, newState)

                voteUpState = newState
                voteUpCount = response["voteup_count"]!!.jsonPrimitive.int
            } catch (e: Exception) {
                Log.e("ArticleViewModel", "Vote up failed", e)
                runtime.showMessage("点赞失败: ${e.message}")
            }
        }
    }

    // 导出为图片 - 使用WebView渲染
    suspend fun exportToImage(context: Context, includeAppAttribution: Boolean, onComplete: (Boolean) -> Unit) {
        val runtime = articleRuntime(context)
        exportToImage(context, runtime, includeAppAttribution, onComplete)
    }

    suspend fun exportToImage(
        context: Context,
        runtime: ArticleViewModelRuntime,
        includeAppAttribution: Boolean,
        onComplete: (Boolean) -> Unit,
    ) {
        exportToImageInternal(
            context = context,
            runtime = runtime,
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
        val runtime = articleRuntime(context)
        exportToImageWithComments(context, runtime, commentCount, includeAppAttribution, onComplete)
    }

    suspend fun exportToImageWithComments(
        context: Context,
        runtime: ArticleViewModelRuntime,
        commentCount: Int,
        includeAppAttribution: Boolean,
        onComplete: (Boolean) -> Unit,
    ) {
        exportToImageInternal(
            context = context,
            runtime = runtime,
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
        val runtime = articleRuntime(context)
        exportToHtml(context, runtime, includeAppAttribution, onComplete)
    }

    suspend fun exportToHtml(
        context: Context,
        runtime: ArticleViewModelRuntime,
        includeAppAttribution: Boolean,
        onComplete: (Boolean) -> Unit,
    ) {
        runCatching { requireExportSourceContent() }.onFailure { error ->
            withContext(Dispatchers.Main) {
                runtime.showMessage(error.message ?: "内容未加载完成")
                onComplete(false)
            }
            return
        }

        if (runtime.requiresHtmlExportPermission() && !runtime.hasImageExportPermission()) {
            withContext(Dispatchers.Main) {
                runtime.requestImageExportPermission()
                permissionRequestCount++
                runtime.showMessage("需要存储权限才能导出 HTML，正在请求权限")
                onComplete(false)
            }
            return
        }

        try {
            val htmlContent = createOfflineHtmlContent(runtime, includeAppAttribution)
            val savedLocation = withContext(Dispatchers.IO) {
                saveHtmlToDownloads(runtime, htmlContent)
            }
            withContext(Dispatchers.Main) {
                runtime.showLongMessage("HTML 已保存到 $savedLocation")
                onComplete(true)
            }
        } catch (e: Exception) {
            Log.e("ArticleViewModel", "HTML export failed", e)
            withContext(Dispatchers.Main) {
                runtime.showMessage("HTML 导出失败: ${e.message}")
                onComplete(false)
            }
        }
    }

    private suspend fun exportToImageInternal(
        context: Context,
        runtime: ArticleViewModelRuntime,
        includeComments: Boolean,
        commentCount: Int,
        includeAppAttribution: Boolean,
        successMessage: String,
        onComplete: (Boolean) -> Unit,
    ) {
        runCatching { requireExportSourceContent() }.onFailure { error ->
            withContext(Dispatchers.Main) {
                runtime.showMessage(error.message ?: "内容未加载完成")
                onComplete(false)
            }
            return
        }

        if (!runtime.hasImageExportPermission()) {
            withContext(Dispatchers.Main) {
                runtime.requestImageExportPermission()
                permissionRequestCount++
                runtime.showMessage("需要存储权限才能导出图片，正在请求权限")
                onComplete(false)
            }
            return
        }

        var preparedWebView: PreparedArticleExportContent? = null
        var bitmap: Any? = null
        val renderer = articleImageExportRenderer(runtime)
        try {
            preparedWebView = prepareExportWebView(
                renderer = renderer,
                htmlContent = createHtmlContent(
                    runtime = runtime,
                    includeComments = includeComments,
                    commentCount = commentCount,
                    includeAppAttribution = includeAppAttribution,
                ),
                timeoutMs = if (includeComments) 18_000L else 15_000L,
            )
            bitmap = captureExportBitmap(renderer, preparedWebView)
            withContext(Dispatchers.IO) {
                saveImageToMediaStore(runtime, bitmap)
            }
            withContext(Dispatchers.Main) {
                runtime.showLongMessage(successMessage)
                onComplete(true)
            }
        } catch (e: Exception) {
            Log.e("ArticleViewModel", "Image export failed", e)
            val errorPrefix = if (includeComments) "带评论图片导出失败" else "图片导出失败"
            withContext(Dispatchers.Main) {
                runtime.showMessage("$errorPrefix: ${e.message}")
                onComplete(false)
            }
        } finally {
            bitmap?.let { recycleExportBitmap(renderer, it) }
            preparedWebView?.let { destroyExportWebView(renderer, it) }
        }
    }

    private suspend fun prepareExportWebView(
        renderer: ArticleImageExportRenderer,
        htmlContent: String,
        timeoutMs: Long,
    ): PreparedArticleExportContent = renderer.prepareExportWebView(htmlContent, timeoutMs)

    private fun loadExportAssetText(runtime: ArticleViewModelRuntime, fileName: String): String = try {
        runtime.loadExportAssetText(fileName)
    } catch (e: Exception) {
        Log.e("ArticleViewModel", "Failed to load export asset: $fileName", e)
        ""
    }

    private suspend fun captureExportBitmap(
        renderer: ArticleImageExportRenderer,
        preparedWebView: PreparedArticleExportContent,
    ): Any = renderer.captureExportBitmap(preparedWebView)

    private suspend fun destroyExportWebView(
        renderer: ArticleImageExportRenderer,
        preparedWebView: PreparedArticleExportContent,
    ) = renderer.destroyExportWebView(preparedWebView)

    private fun recycleExportBitmap(renderer: ArticleImageExportRenderer, bitmap: Any) =
        renderer.recycleExportBitmap(bitmap)

    private fun articleImageExportRenderer(runtime: ArticleViewModelRuntime): ArticleImageExportRenderer =
        runtime.articleImageExportRenderer { fileName ->
            loadExportAssetText(runtime, fileName)
        }

    // 创建HTML内容
    private suspend fun createHtmlContent(
        runtime: ArticleViewModelRuntime,
        includeComments: Boolean,
        commentCount: Int,
        includeAppAttribution: Boolean,
    ): String {
        val commentsHtml = if (includeComments && commentCount > 0) {
            buildArticleExportCommentsHtml(
                comments = fetchExportComments(runtime, commentCount),
                requestedCount = commentCount,
            )
        } else {
            ""
        }

        return runtime.buildArticleExportHtml(
            content = requireExportSourceContent(),
            includeAppAttribution = includeAppAttribution,
            extraSectionsHtml = commentsHtml,
        )
    }

    private suspend fun createOfflineHtmlContent(
        runtime: ArticleViewModelRuntime,
        includeAppAttribution: Boolean,
    ): String = withContext(Dispatchers.IO) {
        runtime.buildOfflineArticleExportHtml(
            content = requireExportSourceContent(),
            includeAppAttribution = includeAppAttribution,
            httpClient = httpClient ?: runtime.accountHttpClient(),
        )
    }

    private suspend fun fetchExportComments(
        runtime: ArticleViewModelRuntime,
        requestedCount: Int,
    ): List<ArticleExportComment> {
        val safeRequestedCount = requestedCount.coerceAtLeast(0)
        if (safeRequestedCount == 0) return emptyList()

        return runtime
            .fetchExportComments(article, safeRequestedCount)
            .map(::mapExportComment)
    }

    private fun mapExportComment(comment: DataHolder.Comment): ArticleExportComment = prepareArticleExportComment(
        authorName = comment.author.name,
        content = comment.content,
        createdTimeText = formatArticleDateTime(comment.createdTime).dropLast(3),
    )

    private fun buildExportFileName(extension: String): String = buildArticleExportFileName(
        content = requireExportSourceContent(),
        extension = extension,
    )

    private fun requireExportSourceContent(): DataHolder.Content = exportSourceContent
        ?: throw IllegalStateException("内容未加载完成")

    // 使用MediaStore保存图片到公共目录
    private fun saveImageToMediaStore(runtime: ArticleViewModelRuntime, bitmap: Any) {
        val displayName = buildExportFileName("png")
        runtime.saveImageToMediaStore(displayName, bitmap)
    }

    private fun saveHtmlToDownloads(runtime: ArticleViewModelRuntime, htmlContent: String): String {
        val displayName = buildExportFileName("html")
        return runtime.saveHtmlToDownloads(displayName, htmlContent)
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

        // 内容 - 解析 HTML 并转换为 Markdown
        val document = Ksoup.parse(content)
        sb.append(htmlToMarkdown(document.body()))

        return sb.toString()
    }

    // HTML 转 Markdown 的递归函数
    private fun htmlToMarkdown(element: Element): String {
        val sb = StringBuilder()

        for (node in element.childNodes()) {
            when (node) {
                is Element -> {
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
                is TextNode -> {
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
        val runtime = articleRuntime(context)
        exportToClipboard(runtime, markdown)
    }

    fun exportToClipboard(runtime: ArticleViewModelRuntime, markdown: String = convertToMarkdown()) {
        // 将Markdown文本复制到剪贴板
        runtime.copyArticleMarkdownToClipboard(markdown)

        runtime.showMessage("文章已复制到剪贴板")
    }
}
