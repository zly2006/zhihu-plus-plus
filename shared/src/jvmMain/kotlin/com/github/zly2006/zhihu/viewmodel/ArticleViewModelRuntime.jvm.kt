package com.github.zly2006.zhihu.viewmodel

import com.github.zly2006.zhihu.navigation.AnswerNavigatorPage
import com.github.zly2006.zhihu.navigation.AnswerNavigatorRepository
import com.github.zly2006.zhihu.navigation.Article
import com.github.zly2006.zhihu.navigation.ArticleType
import com.github.zly2006.zhihu.shared.comment.rootCommentUrl
import com.github.zly2006.zhihu.shared.data.CollectionItem
import com.github.zly2006.zhihu.shared.data.CollectionResponse
import com.github.zly2006.zhihu.shared.data.DataHolder
import com.github.zly2006.zhihu.shared.data.Feed
import com.github.zly2006.zhihu.shared.data.ZhihuJson
import com.github.zly2006.zhihu.shared.desktop.DesktopAccountStore
import com.github.zly2006.zhihu.shared.desktop.DesktopHistoryStorage
import com.github.zly2006.zhihu.shared.filter.ContentOpenEventSupport
import com.github.zly2006.zhihu.shared.util.signZhihuFetchRequest
import com.github.zly2006.zhihu.ui.ArticleAnswerSwitchState
import com.github.zly2006.zhihu.viewmodel.filter.ContentType
import com.github.zly2006.zhihu.viewmodel.filter.getContentFilterDatabase
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.request.post
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.long
import java.awt.Toolkit
import java.awt.datatransfer.StringSelection
import java.io.File

class DesktopArticleViewModelRuntime(
    private val store: DesktopAccountStore = DesktopAccountStore(),
) : ArticleViewModelRuntime {
    private val contentFilterDatabase = getContentFilterDatabase(
        File(System.getProperty("user.home"), ".zhihu-plus/content-filter.db"),
    )
    private val historyStorage = DesktopHistoryStorage()

    override suspend fun getContentDetail(article: Article): DataHolder.Content? {
        val apiUrl = when (article.type) {
            ArticleType.Article -> "https://www.zhihu.com/api/v4/articles/${article.id}?include=content,topics,paid_info,can_comment,excerpt,thanks_count,voteup_count,comment_count,visited_count,relationship,ip_info,relationship.vote,author.badge_v2"
            ArticleType.Answer -> "https://www.zhihu.com/api/v4/answers/${article.id}?include=content,paid_info,can_comment,excerpt,thanks_count,voteup_count,comment_count,visited_count,attachment,reaction,ip_info,pagination_info,question.topics,reaction.relation.voting,author.badge_v2"
        }

        return runCatching {
            val jo = fetchGet(apiUrl) {
                configureSignedRequest(this)
            } ?: return null
            val jojo = buildJsonObject {
                jo.entries.forEach { (key, value) ->
                    if (key == "id") {
                        put(key, JsonPrimitive(value.jsonPrimitive.long))
                    } else {
                        put(key, value)
                    }
                }
            }
            when (article.type) {
                ArticleType.Answer -> ZhihuJson.decodeJson<DataHolder.Answer>(jojo)
                ArticleType.Article -> ZhihuJson.decodeJson<DataHolder.Article>(jojo)
            }
        }.getOrNull()
    }

    override suspend fun recordOpenEvent(
        destination: Article,
        questionId: Long?,
    ) {
        ContentOpenEventSupport.recordOpenEvent(
            database = contentFilterDatabase,
            destination = destination,
            questionId = questionId,
        )
    }

    override fun answerNavigatorRepository(): AnswerNavigatorRepository =
        object : AnswerNavigatorRepository {
            override suspend fun fetchAnswerContent(article: Article): DataHolder.Answer? =
                getContentDetail(article) as? DataHolder.Answer

            override suspend fun fetchQuestionFeeds(
                questionId: Long,
                pageUrl: String?,
            ): AnswerNavigatorPage<Feed> {
                val url = pageUrl ?: "https://www.zhihu.com/api/v4/questions/$questionId/feeds?limit=6"
                val jojo = fetchGet(url) {
                    configureSignedRequest(this)
                } ?: return AnswerNavigatorPage(emptyList(), "")
                return AnswerNavigatorPage(
                    items = ZhihuJson.json
                        .decodeFromJsonElement(
                            JsonArray.serializer(),
                            jojo["data"] ?: return AnswerNavigatorPage(emptyList(), ""),
                        ).mapNotNull { element ->
                            runCatching { ZhihuJson.decodeJson<Feed>(element) }.getOrNull()
                        },
                    nextUrl = jojo["paging"]
                        ?.jsonObject
                        ?.get("next")
                        ?.jsonPrimitive
                        ?.content ?: "",
                )
            }

            override suspend fun fetchCollectionItems(pageUrl: String): AnswerNavigatorPage<CollectionItem> =
                fetchGet(pageUrl) {
                    configureSignedRequest(this)
                }?.let { jojo ->
                    AnswerNavigatorPage(
                        items = ZhihuJson.json
                            .decodeFromJsonElement(
                                JsonArray.serializer(),
                                jojo["data"] ?: return AnswerNavigatorPage(emptyList(), ""),
                            ).mapNotNull { element ->
                                runCatching { ZhihuJson.decodeJson<CollectionItem>(element) }.getOrNull()
                            },
                        nextUrl = jojo["paging"]
                            ?.jsonObject
                            ?.get("next")
                            ?.jsonPrimitive
                            ?.content ?: "",
                    )
                } ?: AnswerNavigatorPage(emptyList(), "")

            override suspend fun getAlreadyOpenedAnswerIds(answerIds: List<Long>): Set<Long> =
                ContentOpenEventSupport
                    .getAlreadyOpenedContentIds(
                        database = contentFilterDatabase,
                        content = answerIds.map { ContentType.ANSWER to it.toString() },
                    ).mapNotNullTo(mutableSetOf()) { key ->
                        key.substringAfter(':', "").toLongOrNull()
                    }
        }

    override fun articleAnswerSwitchState(): ArticleAnswerSwitchState? = null

    override fun postHistoryDestination(destination: Article) {
        historyStorage.add(destination)
    }

    override suspend fun fetchGet(
        url: String,
        block: HttpRequestBuilder.() -> Unit,
    ): JsonObject? = accountHttpClient().use { client ->
        client
            .get(url) {
                block()
            }.body()
    }

    override suspend fun fetchPost(
        url: String,
        block: HttpRequestBuilder.() -> Unit,
    ): JsonObject? = accountHttpClient().use { client ->
        client
            .post(url) {
                block()
            }.body()
    }

    override fun decodeCollectionResponse(json: JsonElement): CollectionResponse =
        ZhihuJson.json.decodeFromJsonElement(CollectionResponse.serializer(), json)

    override fun configureSignedRequest(builder: HttpRequestBuilder) {
        store.load().cookies["d_c0"]?.let { dc0 ->
            builder.signZhihuFetchRequest(dc0 = dc0)
        }
    }

    override fun showMessage(message: String) {
        println(message)
    }

    override fun showLongMessage(message: String) {
        println(message)
    }

    override fun newPlainTextClip(
        label: String,
        text: String,
    ): Any = StringSelection(text)

    override fun setPrimaryClip(clip: Any) {
        Toolkit.getDefaultToolkit().systemClipboard.setContents(clip as StringSelection, null)
    }

    override fun xsrfToken(): String = store.load().cookies["_xsrf"] ?: ""

    override fun hasImageExportPermission(): Boolean = false

    override fun requiresHtmlExportPermission(): Boolean = false

    override fun requestImageExportPermission() = Unit

    override suspend fun fetchExportComments(
        article: Article,
        requestedCount: Int,
    ): List<DataHolder.Comment> {
        val safeRequestedCount = requestedCount.coerceAtLeast(0)
        if (safeRequestedCount == 0) return emptyList()

        val json = fetchGet(article.rootCommentUrl) {
            parameter("order", "score")
            parameter("limit", safeRequestedCount.coerceAtMost(20).toString())
            parameter("include", "data[*].content,excerpt,headline")
            configureSignedRequest(this)
        } ?: return emptyList()

        return json["data"]
            ?.jsonArray
            ?.mapNotNull { element ->
                runCatching {
                    ZhihuJson.decodeJson<DataHolder.Comment>(element)
                }.getOrNull()
            }?.take(safeRequestedCount)
            .orEmpty()
    }

    override fun accountHttpClient(): HttpClient =
        store.createHttpClient(store.load().cookies)

    override fun loadExportAssetText(fileName: String): String = ""

    override fun buildArticleExportHtml(
        content: DataHolder.Content,
        includeAppAttribution: Boolean,
        extraSectionsHtml: String,
    ): String = contentHtml(content) + extraSectionsHtml

    override suspend fun buildOfflineArticleExportHtml(
        content: DataHolder.Content,
        includeAppAttribution: Boolean,
        httpClient: HttpClient,
    ): String = contentHtml(content)

    override fun saveHtmlToDownloads(
        displayName: String,
        htmlContent: String,
    ): String = displayName

    override fun saveImageToMediaStore(
        displayName: String,
        bitmap: Any,
    ) = Unit

    override fun articleImageExportRenderer(loadAssetText: (String) -> String): ArticleImageExportRenderer =
        object : ArticleImageExportRenderer {
            override suspend fun prepareExportWebView(
                htmlContent: String,
                timeoutMs: Long,
            ): PreparedArticleExportContent =
                error("Desktop image export renderer is not implemented")

            override suspend fun captureExportBitmap(preparedWebView: PreparedArticleExportContent): Any =
                error("Desktop image export renderer is not implemented")

            override suspend fun destroyExportWebView(preparedWebView: PreparedArticleExportContent) = Unit

            override fun recycleExportBitmap(bitmap: Any) = Unit
        }

    private fun contentHtml(content: DataHolder.Content): String = when (content) {
        is DataHolder.Answer -> content.content
        is DataHolder.Article -> content.content
        else -> ""
    }
}
