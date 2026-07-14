/*
 * Zhihu++ - Free & Ad-Free Zhihu client for all platforms.
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

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.zly2006.zhihu.markdown.htmlToMdAst
import com.github.zly2006.zhihu.markdown.toMarkdown
import com.github.zly2006.zhihu.navigation.Article
import com.github.zly2006.zhihu.navigation.ArticleType
import com.github.zly2006.zhihu.navigation.CollectionAnswerNavigator
import com.github.zly2006.zhihu.navigation.PaginationInfoNavigator
import com.github.zly2006.zhihu.navigation.QuestionAnswerNavigator
import com.github.zly2006.zhihu.shared.aigc.AigcVoteFlagResponse
import com.github.zly2006.zhihu.shared.aigc.AigcVoteFlagStatusResponse
import com.github.zly2006.zhihu.shared.aigc.AigcVoteFlagSubmission
import com.github.zly2006.zhihu.shared.aigc.AigcVoteNamedVoter
import com.github.zly2006.zhihu.shared.aigc.AigcVoteReadEvent
import com.github.zly2006.zhihu.shared.aigc.AigcVoteReadEvidence
import com.github.zly2006.zhihu.shared.comment.decodeZhihuCommentData
import com.github.zly2006.zhihu.shared.data.DataHolder
import com.github.zly2006.zhihu.shared.data.OfficialBadge
import com.github.zly2006.zhihu.shared.data.ZhihuJson
import com.github.zly2006.zhihu.shared.data.officialBadge
import com.github.zly2006.zhihu.shared.platform.UserMessageSink
import com.github.zly2006.zhihu.shared.util.Log
import com.github.zly2006.zhihu.shared.util.ZhidaSummarySsePayload
import com.github.zly2006.zhihu.shared.util.ZhihuFetchSignature
import com.github.zly2006.zhihu.shared.util.applySegmentInfosToHtml
import com.github.zly2006.zhihu.shared.util.buildZhidaSummaryRequest
import com.github.zly2006.zhihu.shared.util.decodeZhidaAnswerData
import com.github.zly2006.zhihu.shared.util.decodeZhidaStreamErrorMessage
import com.github.zly2006.zhihu.shared.util.mergeSummaryChunk
import com.github.zly2006.zhihu.shared.util.parseZhidaSsePayload
import com.github.zly2006.zhihu.shared.util.serializeZhidaSummaryRequest
import com.github.zly2006.zhihu.shared.util.twoDigitString
import com.github.zly2006.zhihu.ui.Collection
import com.github.zly2006.zhihu.ui.CollectionResponse
import com.github.zly2006.zhihu.ui.VoteUpState
import com.github.zly2006.zhihu.util.ArticleExportComment
import com.github.zly2006.zhihu.util.buildArticleExportCommentsHtml
import com.github.zly2006.zhihu.util.buildArticleExportFileName
import com.github.zly2006.zhihu.util.prepareArticleExportComment
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.accept
import io.ktor.client.request.get
import io.ktor.client.request.header
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
import kotlinx.datetime.number
import kotlinx.datetime.toLocalDateTime
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import kotlin.time.Clock

class ArticleViewModel(
    private val article: Article,
    val httpClient: HttpClient?,
    private val userMessages: UserMessageSink = UserMessageSink(showShortMessage = {}),
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
    var votersTotal by mutableIntStateOf(0)
        private set
    var votersNextUrl by mutableStateOf<String?>(null)
        private set
    var votersLoading by mutableStateOf(false)
        private set
    var votersError by mutableStateOf<String?>(null)
        private set
    var votersSocialText by mutableStateOf("")
        private set
    val voters = mutableStateListOf<DataHolder.Author>()
    var questionId by mutableLongStateOf(0L)
    var collections = mutableStateListOf<Collection>()
    var updatedAt by mutableLongStateOf(0L)
    var createdAt by mutableLongStateOf(0L)
    var ipInfo by mutableStateOf<String?>(null)
    var endorsements by mutableStateOf<List<DataHolder.AnswerEndorsementDisplay>>(emptyList())
    var endorsementTexts: List<String>
        get() = endorsements.map { endorsement -> endorsement.text }
        set(value) {
            endorsements = value.map { text -> DataHolder.AnswerEndorsementDisplay(text = text) }
        }
    var aiSummaryText by mutableStateOf("")
        private set
    var aiSummaryError by mutableStateOf<String?>(null)
        private set
    var aiSummaryLoading by mutableStateOf(false)
        private set
    var aigcVoteAvailable by mutableStateOf(false)
        private set
    var aigcVoteLoading by mutableStateOf(false)
        private set
    var aigcVoteError by mutableStateOf<String?>(null)
        private set
    var aigcVoteCredit by mutableIntStateOf(0)
        private set
    var aigcVoteProgress by mutableIntStateOf(0)
        private set
    var aigcVoteCap by mutableIntStateOf(5)
        private set
    var aigcCreditBypassAvailable by mutableStateOf(false)
        private set
    var aigcVoterName by mutableStateOf("")
        private set
    var aigcNamedVoters by mutableStateOf<List<AigcVoteNamedVoter>>(emptyList())
        private set

    /** 自家后端支持人数：在 Zhihu++ 服务中把该内容标记为 AIGC 的有效用户数。 */
    var aigcEffectiveFlagCount by mutableIntStateOf(0)
        private set
    var aigcCurrentVersionFlagCount by mutableIntStateOf(0)
        private set

    /** 外部来源支持人数：仅来自 zhihuai.sx349.xyz 的支持票投票人数。 */
    var zhihuaiAigcSupportVoterCount by mutableIntStateOf(0)
        private set

    /** 展示用 AIGC 支持总人数：自家后端支持人数 + zhihuai 外部支持人数。 */
    val aigcSupportVoterCount: Int
        get() = aigcEffectiveFlagCount + zhihuaiAigcSupportVoterCount
    var aigcFlagged by mutableStateOf(false)
        private set
    private var aigcMaxScrollRatio by mutableFloatStateOf(0f)
    private var aigcReadSyncStarted = false
    private val openedAtEpochSeconds = Clock.System.now().epochSeconds
    private var aiSummaryJob: Job? = null
    private var exportSourceContent: DataHolder.Content? = null

    // scroll fix
    var rememberedScrollY by mutableIntStateOf(0)
    var rememberedScrollYSync = true

    /**
     * 缓存的回答完整内容，用于水平滑动预览。
     */
    data class CachedAnswerContent(
        val article: Article,
        val title: String,
        val authorName: String,
        val authorBio: String,
        val authorAvatarUrl: String,
        val authorBadge: OfficialBadge? = null,
        val content: String,
        val voteUpCount: Int,
        val commentCount: Int,
        val createdAt: Long = 0L,
        val updatedAt: Long = 0L,
        val ipInfo: String? = null,
        val endorsements: List<DataHolder.AnswerEndorsementDisplay> = emptyList(),
        /** 来源标签，用于 UI 显示，例如 "此问题"、"「收藏夹名称」" */
        val sourceLabel: String = "此问题",
    ) {
        val endorsementTexts: List<String>
            get() = endorsements.map { endorsement -> endorsement.text }
    }

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
        endorsements = endorsements,
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

    // todo: replace this with sqlite
    open class ArticlesSharedData : ArticleAnswerSwitchData()

    @OptIn(ExperimentalStdlibApi::class)
    fun loadArticle(environment: ArticleLoadEnvironment) {
        if (httpClient == null) return
        viewModelScope.launch {
            withContext(Dispatchers.Default) {
                try {
                    if (article.type == ArticleType.Answer) {
                        val sharedData = environment.articleAnswerSwitchState()
                        val answer = environment.fetchContentDetail(article) as? DataHolder.Answer
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
                            votersTotal = answer.voteupCount
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
                            endorsements = answer.endorsementItems

                            environment.postHistoryDestination(
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
                            environment.recordOpenEvent(article, answer.question.id)
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
                                        environment = environment,
                                    )
                                }
                            }
                            sharedData?.navigator?.pushAnswer(toCachedContent(sourceLabel = sharedData.navigator?.sourceName ?: "此问题"))
                            loadAnswerRelationshipEndorsement(environment)
                            loadMoreVoters(environment, reset = true)

                            // 仅在无前向历史时预取下一个回答
                            sharedData?.navigator?.let { nav ->
                                if (nav.currentAnswerIndex >= nav.answerHistory.size - 1) {
                                    nav.prefetchNext(article.id)
                                }
                                nav.prefetchPrevious(article.id)
                            }
                        } else {
                            content = "<h1>你似乎来到了没有知识存在的荒原</h1>"
                            endorsements = emptyList()
                            Log.e("ArticleViewModel", "Answer not found")
                        }
                    } else if (article.type == ArticleType.Article) {
                        val article = environment.fetchContentDetail(article) as? DataHolder.Article
                        if (article != null) {
                            endorsements = emptyList()
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
                            votersTotal = article.voteupCount
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

                            environment.postHistoryDestination(
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
                            environment.recordOpenEvent(this@ArticleViewModel.article, null)
                        } else {
                            content = "<h1>你似乎来到了没有知识存在的荒原</h1>"
                            Log.e("ArticleViewModel", "Article not found")
                        }
                    }
                } catch (e: Exception) {
                    Log.e("ArticleViewModel", "Failed to load content", e)
                }
            }
        }
    }

    fun toggleFavorite(collectionId: String, remove: Boolean, environment: ZhihuApiEnvironment) {
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
                    loadCollections(environment)
                    userMessages.showShortMessage(if (remove) "取消收藏成功" else "收藏成功")
                } else {
                    userMessages.showShortMessage("收藏操作失败")
                }
            } catch (e: Exception) {
                Log.e("ArticleViewModel", "Favorite toggle failed", e)
                userMessages.showShortMessage("收藏操作失败: ${e.message}")
            }
        }
    }

    fun requestAiSummary(environment: ZhihuApiEnvironment) {
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
                val response = environment.postSigned("https://www.zhihu.com/ai_ingress/stream/completion") {
                    accept(ContentType.Text.EventStream)
                    contentType(ContentType.Application.Json)
                    header("x-xsrftoken", environment.xsrfToken())
                    setBody(serializeZhidaSummaryRequest(request))
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

    fun loadCollections(environment: ZhihuApiEnvironment) {
        if (httpClient == null) return
        viewModelScope.launch {
            withContext(Dispatchers.Default) {
                try {
                    val contentType = when (article.type) {
                        ArticleType.Answer -> "answer"
                        ArticleType.Article -> "article"
                    }
                    val collectionsUrl = "https://api.zhihu.com/collections/contents/$contentType/${article.id}?limit=50"
                    val jojo = environment.fetchJson(collectionsUrl, "") ?: return@withContext
                    val collectionsData = ZhihuJson.decodeJson<CollectionResponse>(jojo)
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

    fun createNewCollection(
        environment: ZhihuApiEnvironment,
        title: String,
        description: String = "",
        isPublic: Boolean = false,
    ) {
        if (httpClient == null) return
        viewModelScope.launch {
            environment.postSigned("https://www.zhihu.com/api/v4/collections") {
                contentType(ContentType.Application.Json)
                setBody(
                    buildJsonObject {
                        put("title", title)
                        put("description", description)
                        put("is_public", isPublic)
                    },
                )
            }
            loadCollections(environment)
        }
    }

    fun toggleVoteUp(environment: ZhihuApiEnvironment, newState: VoteUpState) {
        viewModelScope.launch {
            try {
                val endpoint = when (article.type) {
                    ArticleType.Answer -> "https://www.zhihu.com/api/v4/answers/${article.id}/voters"
                    ArticleType.Article -> "https://www.zhihu.com/api/v4/articles/${article.id}/voters"
                }

                val response = environment
                    .postSigned(endpoint) {
                        when (article.type) {
                            ArticleType.Answer -> setBody(mapOf("type" to newState.key))
                            ArticleType.Article -> setBody(mapOf("voting" to if (newState == VoteUpState.Up) 1 else 0))
                        }
                        contentType(ContentType.Application.Json)
                    }.body<JsonObject>()

                voteUpState = newState
                voteUpCount = response["voteup_count"]!!.jsonPrimitive.int
                votersTotal = voteUpCount
                if (article.type == ArticleType.Answer) {
                    loadAnswerRelationshipEndorsement(environment)
                    loadMoreVoters(environment, reset = true)
                }
            } catch (e: Exception) {
                Log.e("ArticleViewModel", "Vote up failed", e)
                userMessages.showShortMessage("点赞失败: ${e.message}")
            }
        }
    }

    fun updateAigcReadProgress(
        currentScroll: Int,
        maxScroll: Int,
    ) {
        val ratio = when {
            content.isBlank() -> 0f
            maxScroll <= 0 -> 1f
            else -> (currentScroll.toFloat() / maxScroll.toFloat()).coerceIn(0f, 1f)
        }
        if (ratio > aigcMaxScrollRatio) {
            aigcMaxScrollRatio = ratio
        }
    }

    fun isAigcFlagEvidenceReady(): Boolean = currentAigcReadEvidence().isEligibleForCredit()

    fun loadAigcFlagStatus(environment: AigcVoteEnvironment) {
        val client = environment.aigcVoteClient()
        aigcVoteAvailable = client != null
        val voter = environment.aigcVoteVoter()
        aigcVoterName = voter?.name.orEmpty()
        if (client == null) return

        viewModelScope.launch {
            aigcVoteLoading = true
            aigcVoteError = null
            try {
                applyAigcStatus(
                    client.getFlagStatus(
                        contentType = aigcContentType(),
                        contentId = article.id.toString(),
                        voter = voter,
                    ),
                )
            } catch (e: Exception) {
                Log.e("ArticleViewModel", "Failed to load AIGC vote status", e)
                aigcVoteError = e.message ?: "AIGC 投票状态加载失败"
            } finally {
                aigcVoteLoading = false
            }
        }
    }

    fun syncAigcReadEventIfEligible(environment: AigcVoteEnvironment) {
        val client = environment.aigcVoteClient()
        aigcVoteAvailable = client != null
        if (client == null || aigcReadSyncStarted || content.isBlank()) return

        val evidence = currentAigcReadEvidence()
        val contentUpdatedAt = currentContentUpdatedAt()
        if (!evidence.isEligibleForCredit() || contentUpdatedAt <= 0) return
        aigcReadSyncStarted = true

        viewModelScope.launch {
            try {
                val response = client.syncReadEvent(
                    AigcVoteReadEvent(
                        contentType = aigcContentType(),
                        contentId = article.id.toString(),
                        title = title,
                        authorHash = currentAuthorHash(),
                        contentHtml = content,
                        contentUpdatedAt = contentUpdatedAt,
                        evidence = evidence,
                    ),
                )
                aigcVoteCredit = response.credit
                aigcVoteProgress = response.progress
                aigcVoteCap = response.cap
                aigcVoteError = null
            } catch (e: Exception) {
                Log.e("ArticleViewModel", "Failed to sync AIGC read event", e)
                aigcVoteError = e.message ?: "AIGC 阅读积分同步失败"
                aigcReadSyncStarted = false
            }
        }
    }

    fun submitAigcFlag(environment: AigcVoteEnvironment) {
        val client = environment.aigcVoteClient()
        aigcVoteAvailable = client != null
        if (client == null) {
            aigcVoteError = "未配置 AIGC 投票服务"
            userMessages.showShortMessage(aigcVoteError!!)
            return
        }
        if (content.isBlank()) {
            aigcVoteError = "正文尚未加载完成"
            userMessages.showShortMessage(aigcVoteError!!)
            return
        }
        val voter = environment.aigcVoteVoter()
        aigcVoterName = voter?.name.orEmpty()
        if (voter == null) {
            aigcVoteError = "需要登录后才能记名投票"
            userMessages.showShortMessage(aigcVoteError!!)
            return
        }
        if (aigcVoteCredit <= 0 && !aigcFlagged && !aigcCreditBypassAvailable) {
            aigcVoteError = "投票积分不足"
            userMessages.showShortMessage(aigcVoteError!!)
            return
        }
        if (!aigcCreditBypassAvailable && !isAigcFlagEvidenceReady()) {
            aigcVoteError = "需要继续阅读后才能标记"
            userMessages.showShortMessage(aigcVoteError!!)
            return
        }

        viewModelScope.launch {
            aigcVoteLoading = true
            aigcVoteError = null
            try {
                applyAigcFlagResponse(
                    client.submitFlag(
                        AigcVoteFlagSubmission(
                            contentType = aigcContentType(),
                            contentId = article.id.toString(),
                            voter = voter,
                            title = title,
                            authorHash = currentAuthorHash(),
                            contentHtml = content,
                            contentUpdatedAt = currentContentUpdatedAt(),
                            evidence = currentAigcReadEvidence(),
                        ),
                    ),
                )
                userMessages.showShortMessage("已标记疑似 AIGC")
            } catch (e: Exception) {
                Log.e("ArticleViewModel", "AIGC flag failed", e)
                aigcVoteError = e.message ?: "AIGC 标记失败"
                userMessages.showShortMessage("AIGC 标记失败: ${e.message}")
            } finally {
                aigcVoteLoading = false
            }
        }
    }

    private fun applyAigcStatus(response: AigcVoteFlagStatusResponse) {
        aigcFlagged = response.myFlagged
        aigcVoteCredit = response.credit
        aigcVoteProgress = response.progress
        aigcVoteCap = response.cap
        aigcCreditBypassAvailable = response.creditBypassAvailable
        aigcEffectiveFlagCount = response.effectiveFlagCount
        aigcNamedVoters = response.voters
        zhihuaiAigcSupportVoterCount = response.externalSource?.voterCount ?: 0
    }

    private fun applyAigcFlagResponse(response: AigcVoteFlagResponse) {
        aigcFlagged = response.myFlagged
        aigcVoteCredit = response.credit
        aigcCreditBypassAvailable = response.creditBypassAvailable
        aigcEffectiveFlagCount = response.effectiveFlagCount
        aigcCurrentVersionFlagCount = response.currentVersionFlagCount
        aigcNamedVoters = response.voters
        zhihuaiAigcSupportVoterCount = response.externalSource?.voterCount ?: 0
    }

    private fun currentAigcReadEvidence(): AigcVoteReadEvidence {
        val nowEpochSeconds = Clock.System.now().epochSeconds
        return AigcVoteReadEvidence(
            foregroundDurationMs = (nowEpochSeconds - openedAtEpochSeconds).coerceAtLeast(0) * 1_000,
            maxScrollRatio = aigcMaxScrollRatio.toDouble(),
            openedAtEpochSeconds = openedAtEpochSeconds,
        )
    }

    private fun currentContentUpdatedAt(): Long = when {
        updatedAt > 0L -> updatedAt
        createdAt > 0L -> createdAt
        else -> 0L
    }

    private fun currentAuthorHash(): String = ZhihuFetchSignature.md5Hex(
        authorId.ifBlank { authorUrlToken.ifBlank { authorName } },
    )

    private fun aigcContentType(): String = when (article.type) {
        ArticleType.Answer -> "answer"
        ArticleType.Article -> "article"
    }

    fun loadMoreVoters(environment: ZhihuApiEnvironment, reset: Boolean = false) {
        if (article.type != ArticleType.Answer || votersLoading) return
        viewModelScope.launch {
            votersLoading = true
            votersError = null
            try {
                val page = loadVotersPage(
                    environment = environment,
                    initialUrl = "https://www.zhihu.com/api/v4/answers/${article.id}/upvoters?limit=10&offset=0",
                    nextUrl = votersNextUrl,
                    reset = reset,
                )
                voters.replaceOrAppendUniqueVoters(page.data, reset)
                votersTotal = page.paging.totals.takeIf { it > 0 } ?: voteUpCount
                votersNextUrl = page.nextUrlOrNull()
            } catch (e: Exception) {
                Log.e("ArticleViewModel", "Failed to load answer voters", e)
                votersError = e.message ?: "加载赞同者失败"
            } finally {
                votersLoading = false
            }
        }
    }

    fun loadAnswerRelationshipEndorsement(environment: ZhihuApiEnvironment) {
        if (article.type != ArticleType.Answer) return
        viewModelScope.launch {
            try {
                val response = environment.fetchJson("https://www.zhihu.com/api/v4/answers/${article.id}/relationship?desktop=true", "")
                    ?: return@launch
                val endorsement = ZhihuJson.decodeJson<AnswerRelationshipEndorsement>(response)
                votersSocialText = endorsement.text
            } catch (e: Exception) {
                Log.e("ArticleViewModel", "Failed to load answer relationship endorsement", e)
                votersSocialText = ""
            }
        }
    }

    // 导出为图片 - 使用WebView渲染
    suspend fun exportToImage(
        environment: ArticleExportContentEnvironment,
        includeAppAttribution: Boolean,
        onComplete: (Boolean) -> Unit,
    ) {
        exportToImageInternal(
            environment = environment,
            includeComments = false,
            commentCount = 0,
            includeAppAttribution = includeAppAttribution,
            successMessage = "图片已保存到相册",
            onComplete = onComplete,
        )
    }

    // 导出为带评论的图片 - 使用WebView渲染
    suspend fun exportToImageWithComments(
        environment: ArticleExportContentEnvironment,
        commentCount: Int,
        includeAppAttribution: Boolean,
        onComplete: (Boolean) -> Unit,
    ) {
        exportToImageInternal(
            environment = environment,
            includeComments = true,
            commentCount = commentCount,
            includeAppAttribution = includeAppAttribution,
            successMessage = "带评论图片已保存到相册",
            onComplete = onComplete,
        )
    }

    suspend fun exportToHtml(
        environment: ArticleExportContentEnvironment,
        includeAppAttribution: Boolean,
        onComplete: (Boolean) -> Unit,
    ) {
        runCatching { requireExportSourceContent() }.onFailure { error ->
            withContext(Dispatchers.Main) {
                userMessages.showShortMessage(error.message ?: "内容未加载完成")
                onComplete(false)
            }
            return
        }

        if (environment.requiresHtmlExportPermission() && !environment.hasImageExportPermission()) {
            withContext(Dispatchers.Main) {
                environment.requestImageExportPermission()
                permissionRequestCount++
                userMessages.showShortMessage("需要存储权限才能导出 HTML，正在请求权限")
                onComplete(false)
            }
            return
        }

        try {
            val htmlContent = withContext(Dispatchers.Default) {
                environment.buildOfflineArticleExportHtml(
                    content = requireExportSourceContent(),
                    includeAppAttribution = includeAppAttribution,
                    httpClient = httpClient ?: environment.httpClient(),
                )
            }
            val savedLocation = withContext(Dispatchers.Default) {
                environment.saveHtmlToDownloads(
                    displayName = buildArticleExportFileName(
                        content = requireExportSourceContent(),
                        extension = "html",
                    ),
                    htmlContent = htmlContent,
                )
            }
            withContext(Dispatchers.Main) {
                userMessages.showLongMessage("HTML 已保存到 $savedLocation")
                onComplete(true)
            }
        } catch (e: Exception) {
            Log.e("ArticleViewModel", "HTML export failed", e)
            withContext(Dispatchers.Main) {
                userMessages.showShortMessage("HTML 导出失败: ${e.message}")
                onComplete(false)
            }
        }
    }

    private suspend fun exportToImageInternal(
        environment: ArticleExportContentEnvironment,
        includeComments: Boolean,
        commentCount: Int,
        includeAppAttribution: Boolean,
        successMessage: String,
        onComplete: (Boolean) -> Unit,
    ) {
        runCatching { requireExportSourceContent() }.onFailure { error ->
            withContext(Dispatchers.Main) {
                userMessages.showShortMessage(error.message ?: "内容未加载完成")
                onComplete(false)
            }
            return
        }

        if (!environment.hasImageExportPermission()) {
            withContext(Dispatchers.Main) {
                environment.requestImageExportPermission()
                permissionRequestCount++
                userMessages.showShortMessage("需要存储权限才能导出图片，正在请求权限")
                onComplete(false)
            }
            return
        }

        var preparedWebView: PreparedArticleExportContent? = null
        var bitmap: Any? = null
        val renderer = environment.articleImageExportRenderer { fileName ->
            try {
                environment.loadExportAssetText(fileName)
            } catch (e: Exception) {
                Log.e("ArticleViewModel", "Failed to load export asset: $fileName", e)
                ""
            }
        }!!
        try {
            preparedWebView = renderer.prepareExportWebView(
                htmlContent = createHtmlContent(
                    environment = environment,
                    includeComments = includeComments,
                    commentCount = commentCount,
                    includeAppAttribution = includeAppAttribution,
                ),
                timeoutMs = if (includeComments) 18_000L else 15_000L,
            )
            val capturedBitmap = renderer.captureExportBitmap(preparedWebView)
            bitmap = capturedBitmap
            withContext(Dispatchers.Default) {
                environment.saveImageToMediaStore(
                    displayName = buildArticleExportFileName(
                        content = requireExportSourceContent(),
                        extension = "jpg",
                    ),
                    bitmap = capturedBitmap,
                )
            }
            withContext(Dispatchers.Main) {
                userMessages.showLongMessage(successMessage)
                onComplete(true)
            }
        } catch (e: Exception) {
            Log.e("ArticleViewModel", "Image export failed", e)
            val errorPrefix = if (includeComments) "带评论图片导出失败" else "图片导出失败"
            withContext(Dispatchers.Main) {
                userMessages.showShortMessage("$errorPrefix: ${e.message}")
                onComplete(false)
            }
        } finally {
            bitmap?.let { renderer.recycleExportBitmap(it) }
            preparedWebView?.let { renderer.destroyExportWebView(it) }
        }
    }

    // 创建HTML内容
    private suspend fun createHtmlContent(
        environment: ArticleExportContentEnvironment,
        includeComments: Boolean,
        commentCount: Int,
        includeAppAttribution: Boolean,
    ): String {
        val commentsHtml = if (includeComments && commentCount > 0) {
            buildArticleExportCommentsHtml(
                comments = fetchExportComments(environment, commentCount),
                requestedCount = commentCount,
            )
        } else {
            ""
        }

        return environment.buildArticleExportHtml(
            content = requireExportSourceContent(),
            includeAppAttribution = includeAppAttribution,
            extraSectionsHtml = commentsHtml,
        )
    }

    private suspend fun fetchExportComments(
        environment: ArticleExportContentEnvironment,
        requestedCount: Int,
    ): List<ArticleExportComment> {
        val safeRequestedCount = requestedCount.coerceAtLeast(0)
        if (safeRequestedCount == 0) return emptyList()

        val url = when (article.type) {
            ArticleType.Answer -> "https://www.zhihu.com/api/v4/comment_v5/answers/${article.id}/root_comment"
            ArticleType.Article -> "https://www.zhihu.com/api/v4/comment_v5/articles/${article.id}/root_comment"
        }
        val json = environment.fetchJson(
            url = "$url?order=score&limit=${safeRequestedCount.coerceAtMost(20)}",
            include = "data[*].content,excerpt,headline",
        ) ?: return emptyList()
        return decodeZhihuCommentData(json, safeRequestedCount)
            .map { comment ->
                prepareArticleExportComment(
                    authorName = comment.author.name,
                    content = comment.content,
                    createdTimeText = formatArticleDateTime(comment.createdTime).dropLast(3),
                )
            }
    }

    private fun requireExportSourceContent(): DataHolder.Content = exportSourceContent
        ?: throw IllegalStateException("内容未加载完成")

    fun convertToMarkdown(): String {
        val sb = StringBuilder()

        sb.append("# $title\n\n")

        sb.append("**作者**: $authorName\n\n")
        if (authorBio.isNotEmpty()) {
            sb.append("**简介**: $authorBio\n\n")
        }

        sb.append("---\n\n")
        sb.append(htmlToMdAst(content, enableInteractiveBlocks = false).toMarkdown())

        return sb.toString()
    }

    // 导出到剪贴板
    fun exportToClipboard(environment: ClipboardEnvironment) {
        val markdown = convertToMarkdown()

        // 将Markdown文本复制到剪贴板
        environment.setPlainTextClipboard("Zhihu Article", markdown)

        userMessages.showShortMessage("文章已复制到剪贴板")
    }
}

fun formatArticleDateTime(seconds: Long): String {
    val instant = kotlin.time.Instant.fromEpochSeconds(seconds)
    val dateTime = instant.toLocalDateTime(kotlinx.datetime.TimeZone.currentSystemDefault())
    return buildString {
        append(dateTime.year.toString().padStart(4, '0'))
        append('-')
        append(dateTime.month.number.twoDigitString())
        append('-')
        append(dateTime.day.twoDigitString())
        append(' ')
        append(dateTime.hour.twoDigitString())
        append(':')
        append(dateTime.minute.twoDigitString())
        append(':')
        append(dateTime.second.twoDigitString())
    }
}

@Serializable
private data class AnswerRelationshipEndorsement(
    val type: String = "",
    val text: String = "",
)
