package com.github.zly2006.zhihu.viewmodel

import com.github.zly2006.zhihu.data.ContentDetailCache
import com.github.zly2006.zhihu.data.normalizeArticleContentDetailJson
import com.github.zly2006.zhihu.data.zhihuArticleContentDetailUrl
import com.github.zly2006.zhihu.navigation.AnswerNavigatorPage
import com.github.zly2006.zhihu.navigation.AnswerNavigatorRepository
import com.github.zly2006.zhihu.navigation.Article
import com.github.zly2006.zhihu.navigation.ArticleType
import com.github.zly2006.zhihu.navigation.NavDestination
import com.github.zly2006.zhihu.navigation.zhihuQuestionFeedsUrl
import com.github.zly2006.zhihu.shared.comment.rootCommentUrl
import com.github.zly2006.zhihu.shared.data.CollectionItem
import com.github.zly2006.zhihu.shared.data.CollectionResponse
import com.github.zly2006.zhihu.shared.data.DataHolder
import com.github.zly2006.zhihu.shared.data.Feed
import com.github.zly2006.zhihu.shared.data.ZhihuJson
import com.github.zly2006.zhihu.shared.desktop.DesktopAccountStore
import com.github.zly2006.zhihu.shared.desktop.DesktopHistoryStorage
import com.github.zly2006.zhihu.shared.desktop.desktopZhihuDownloadsDir
import com.github.zly2006.zhihu.shared.filter.ContentOpenEventSupport
import com.github.zly2006.zhihu.shared.filter.ContentOpenFrom
import com.github.zly2006.zhihu.shared.filter.TrackedContentIdentity
import com.github.zly2006.zhihu.shared.platform.UserMessageSink
import com.github.zly2006.zhihu.shared.util.signZhihuFetchRequest
import com.github.zly2006.zhihu.ui.ArticleAnswerSwitchState
import com.github.zly2006.zhihu.util.ARTICLE_EXPORT_TEMPLATE_ASSET
import com.github.zly2006.zhihu.util.buildArticleExportData
import com.github.zly2006.zhihu.util.inlineArticleExportImagesInHtml
import com.github.zly2006.zhihu.util.renderArticleExportHtml
import com.github.zly2006.zhihu.viewmodel.filter.ContentType
import com.github.zly2006.zhihu.viewmodel.filter.desktopContentFilterDatabaseFile
import com.github.zly2006.zhihu.viewmodel.filter.getContentFilterDatabase
import io.ktor.client.HttpClient
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.statement.readRawBytes
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.isSuccess
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.awt.Toolkit
import java.awt.datatransfer.StringSelection
import java.awt.image.BufferedImage
import java.io.ByteArrayInputStream
import java.io.File
import java.net.URLConnection
import java.util.Base64
import javax.imageio.ImageIO
import javax.swing.JEditorPane
import javax.swing.SwingUtilities

internal val desktopArticleAnswerSwitchState = ArticleAnswerSwitchData()
private var desktopPendingContentOpenIdentity: TrackedContentIdentity? = null
private var desktopPendingContentOpenFrom: String? = null

internal fun prepareDesktopPendingContentOpen(
    target: NavDestination,
    currentMainTabOpenFrom: String?,
    source: NavDestination?,
) {
    val identity = ContentOpenEventSupport.toTrackedContentIdentity(target)
    if (identity == null) {
        desktopPendingContentOpenIdentity = null
        desktopPendingContentOpenFrom = null
        return
    }
    desktopPendingContentOpenIdentity = identity
    desktopPendingContentOpenFrom = currentMainTabOpenFrom
        ?: ContentOpenEventSupport.inferOpenFrom(source, target)
}

internal fun consumeDesktopPendingContentOpenFrom(destination: NavDestination): String {
    val identity = ContentOpenEventSupport.toTrackedContentIdentity(destination) ?: return ContentOpenFrom.UNKNOWN
    if (identity != desktopPendingContentOpenIdentity) {
        return ContentOpenFrom.UNKNOWN
    }
    val openFrom = desktopPendingContentOpenFrom ?: ContentOpenFrom.UNKNOWN
    desktopPendingContentOpenIdentity = null
    desktopPendingContentOpenFrom = null
    return openFrom
}

class DesktopArticleViewModelRuntime(
    private val store: DesktopAccountStore = DesktopAccountStore(),
    private val userMessages: UserMessageSink,
) : ArticleViewModelRuntime {
    private val contentFilterDatabase = getContentFilterDatabase(desktopContentFilterDatabaseFile())
    private val historyStorage = DesktopHistoryStorage()

    override suspend fun getContentDetail(article: Article): DataHolder.Content? =
        ContentDetailCache.getOrFetch(article) { destination ->
            when (destination) {
                is Article -> fetchArticleContentDetail(destination)
                else -> null
            }
        }

    private suspend fun fetchArticleContentDetail(article: Article): DataHolder.Content? {
        val apiUrl = zhihuArticleContentDetailUrl(article)

        return runCatching {
            val jo = fetchGet(apiUrl) {
                configureSignedRequest(this)
            } ?: return null
            val jojo = normalizeArticleContentDetailJson(jo)
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
            openFrom = consumeDesktopPendingContentOpenFrom(destination),
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
                val url = pageUrl ?: zhihuQuestionFeedsUrl(questionId, limit = 6)
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

    override fun articleAnswerSwitchState(): ArticleAnswerSwitchState? = desktopArticleAnswerSwitchState

    override fun postHistoryDestination(destination: Article) {
        historyStorage.add(destination)
    }

    override suspend fun fetchGet(
        url: String,
        block: HttpRequestBuilder.() -> Unit,
    ): JsonObject? = store.fetchAuthenticatedJson(url) {
        block()
        method = HttpMethod.Get
    }

    override suspend fun fetchPost(
        url: String,
        block: HttpRequestBuilder.() -> Unit,
    ): JsonObject? = store.fetchAuthenticatedJson(url) {
        block()
        method = HttpMethod.Post
    }

    override fun decodeCollectionResponse(json: JsonElement): CollectionResponse =
        ZhihuJson.json.decodeFromJsonElement(CollectionResponse.serializer(), json)

    override fun configureSignedRequest(builder: HttpRequestBuilder) {
        store.load().cookies["d_c0"]?.let { dc0 ->
            builder.signZhihuFetchRequest(dc0 = dc0)
        }
    }

    override fun showMessage(message: String) {
        userMessages.showShortMessage(message)
    }

    override fun showLongMessage(message: String) {
        userMessages.showLongMessage(message)
    }

    override fun setPlainTextClipboard(
        label: String,
        text: String,
    ) {
        Toolkit.getDefaultToolkit().systemClipboard.setContents(StringSelection(text), null)
    }

    override fun xsrfToken(): String = store.load().cookies["_xsrf"] ?: ""

    override fun hasImageExportPermission(): Boolean = true

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

    override fun loadExportAssetText(fileName: String): String =
        javaClass.classLoader
            ?.getResourceAsStream(fileName)
            ?.bufferedReader()
            ?.use { it.readText() }
            ?: sequenceOf(
                File("app/src/main/assets", fileName),
                File(System.getProperty("user.dir"), "app/src/main/assets/$fileName"),
            ).firstOrNull { it.isFile }?.readText()
            ?: ""

    override fun buildArticleExportHtml(
        content: DataHolder.Content,
        includeAppAttribution: Boolean,
        extraSectionsHtml: String,
    ): String = renderArticleExportHtml(
        template = loadExportAssetText(ARTICLE_EXPORT_TEMPLATE_ASSET),
        exportData = buildArticleExportData(
            content = content,
            includeAppAttribution = includeAppAttribution,
        ),
        extraSectionsHtml = extraSectionsHtml,
    )

    override suspend fun buildOfflineArticleExportHtml(
        content: DataHolder.Content,
        includeAppAttribution: Boolean,
        httpClient: HttpClient,
    ): String = inlineArticleExportImagesInHtml(
        html = buildArticleExportHtml(
            content = content,
            includeAppAttribution = includeAppAttribution,
            extraSectionsHtml = "",
        ),
        useOriginalOnImageFetchFailure = true,
    ) { imageUrl ->
        fetchArticleExportImageDataUrl(httpClient, imageUrl)
    }

    override fun saveHtmlToDownloads(
        displayName: String,
        htmlContent: String,
    ): String {
        val downloadsDir = desktopZhihuDownloadsDir()
        val file = File(downloadsDir, displayName)
        file.writeText(htmlContent)
        return file.absolutePath
    }

    override fun saveImageToMediaStore(
        displayName: String,
        bitmap: Any,
    ) {
        val downloadsDir = desktopZhihuDownloadsDir()
        val file = File(downloadsDir, displayName)
        ImageIO.write(bitmap as BufferedImage, "png", file)
    }

    override fun articleImageExportRenderer(loadAssetText: (String) -> String): ArticleImageExportRenderer =
        DesktopArticleExportRenderer()
}

private suspend fun fetchArticleExportImageDataUrl(
    httpClient: HttpClient,
    imageUrl: String,
): String {
    val response = httpClient.get(imageUrl)
    if (!response.status.isSuccess()) {
        throw IllegalStateException("下载图片失败: ${response.status.value}")
    }

    val bytes = response.readRawBytes()
    if (bytes.isEmpty()) {
        throw IllegalStateException("图片内容为空")
    }

    val mimeType = resolveArticleExportImageMimeType(
        contentTypeHeader = response.headers[HttpHeaders.ContentType],
        imageUrl = imageUrl,
        imageBytes = bytes,
    )
    return "data:$mimeType;base64,${Base64.getEncoder().encodeToString(bytes)}"
}

private fun resolveArticleExportImageMimeType(
    contentTypeHeader: String?,
    imageUrl: String,
    imageBytes: ByteArray,
): String {
    contentTypeHeader
        ?.substringBefore(';')
        ?.trim()
        ?.takeIf { it.startsWith("image/") }
        ?.let { return it }

    URLConnection.guessContentTypeFromName(imageUrl.substringBefore('?'))?.let { return it }
    ByteArrayInputStream(imageBytes).use { stream ->
        URLConnection.guessContentTypeFromStream(stream)?.let { return it }
    }

    return "image/jpeg"
}

private data class DesktopPreparedExportContent(
    val htmlContent: String,
) : PreparedArticleExportContent

private class DesktopArticleExportRenderer : ArticleImageExportRenderer {
    override suspend fun prepareExportWebView(
        htmlContent: String,
        timeoutMs: Long,
    ): PreparedArticleExportContent = DesktopPreparedExportContent(htmlContent)

    override suspend fun captureExportBitmap(preparedWebView: PreparedArticleExportContent): Any =
        withContext(Dispatchers.IO) {
            preparedWebView as DesktopPreparedExportContent
            renderHtmlToImage(preparedWebView.htmlContent)
        }

    override suspend fun destroyExportWebView(preparedWebView: PreparedArticleExportContent) = Unit

    override fun recycleExportBitmap(bitmap: Any) = Unit

    private fun renderHtmlToImage(htmlContent: String): BufferedImage = runOnSwingThread {
        val viewportWidthPx = 900
        val editorPane = JEditorPane("text/html", htmlContent).apply {
            isEditable = false
            putClientProperty(JEditorPane.HONOR_DISPLAY_PROPERTIES, true)
            setSize(viewportWidthPx, Int.MAX_VALUE / 4)
        }
        val preferredSize = editorPane.preferredSize
        val contentHeightPx = preferredSize.height.coerceAtLeast(1)
        editorPane.setSize(viewportWidthPx, contentHeightPx)
        editorPane.validate()

        BufferedImage(
            viewportWidthPx,
            contentHeightPx,
            BufferedImage.TYPE_INT_ARGB,
        ).also { image ->
            val graphics = image.createGraphics()
            try {
                graphics.color = java.awt.Color.WHITE
                graphics.fillRect(0, 0, viewportWidthPx, contentHeightPx)
                editorPane.paint(graphics)
            } finally {
                graphics.dispose()
            }
        }
    }
}

private fun <T> runOnSwingThread(block: () -> T): T {
    if (SwingUtilities.isEventDispatchThread()) {
        return block()
    }

    var value: Any? = null
    var error: Throwable? = null
    SwingUtilities.invokeAndWait {
        try {
            value = block()
        } catch (throwable: Throwable) {
            error = throwable
        }
    }
    error?.let { throw it }
    @Suppress("UNCHECKED_CAST")
    return value as T
}
