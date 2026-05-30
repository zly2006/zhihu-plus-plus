package com.github.zly2006.zhihu.viewmodel

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.github.zly2006.zhihu.data.ContentDetailCache
import com.github.zly2006.zhihu.data.decodeArticleContentDetail
import com.github.zly2006.zhihu.data.decodePinContentDetail
import com.github.zly2006.zhihu.data.decodeQuestionContentDetail
import com.github.zly2006.zhihu.navigation.AnswerNavigatorPage
import com.github.zly2006.zhihu.navigation.AnswerNavigatorRepository
import com.github.zly2006.zhihu.navigation.Article
import com.github.zly2006.zhihu.navigation.ArticleType
import com.github.zly2006.zhihu.navigation.NavDestination
import com.github.zly2006.zhihu.navigation.Pin
import com.github.zly2006.zhihu.navigation.Question
import com.github.zly2006.zhihu.navigation.answerNavigatorPageFromJson
import com.github.zly2006.zhihu.navigation.zhihuQuestionFeedsUrl
import com.github.zly2006.zhihu.shared.data.CollectionItem
import com.github.zly2006.zhihu.shared.data.DataHolder
import com.github.zly2006.zhihu.shared.data.Feed
import com.github.zly2006.zhihu.shared.data.FeedDisplayItem
import com.github.zly2006.zhihu.shared.data.ZHIHU_CLEAR_ONLINE_HISTORY_URL
import com.github.zly2006.zhihu.shared.data.ZHIHU_LAST_READ_TOUCH_URL
import com.github.zly2006.zhihu.shared.data.ZhihuJson
import com.github.zly2006.zhihu.shared.data.encodeZhihuClearOnlineHistoryBody
import com.github.zly2006.zhihu.shared.data.encodeZhihuLastReadTouchItems
import com.github.zly2006.zhihu.shared.data.navDestination
import com.github.zly2006.zhihu.shared.data.zhihuLastReadTouchItem
import com.github.zly2006.zhihu.shared.data.zhihuLastReadTouchItems
import com.github.zly2006.zhihu.shared.desktop.DesktopAccountStore
import com.github.zly2006.zhihu.shared.desktop.DesktopHistoryStorage
import com.github.zly2006.zhihu.shared.desktop.copyDesktopPlainText
import com.github.zly2006.zhihu.shared.desktop.desktopZhihuDataFile
import com.github.zly2006.zhihu.shared.desktop.desktopZhihuDownloadsDir
import com.github.zly2006.zhihu.shared.desktop.signDesktopRequest
import com.github.zly2006.zhihu.shared.desktop.signedFetchJson
import com.github.zly2006.zhihu.shared.desktop.signedWithResponse
import com.github.zly2006.zhihu.shared.filter.ContentOpenEventSupport
import com.github.zly2006.zhihu.shared.filter.ContentOpenFrom
import com.github.zly2006.zhihu.shared.filter.TrackedContentIdentity
import com.github.zly2006.zhihu.shared.notification.NotificationSettingsStore
import com.github.zly2006.zhihu.shared.notification.desktopNotificationSettingsStore
import com.github.zly2006.zhihu.shared.platform.desktopSettingsStore
import com.github.zly2006.zhihu.shared.util.Log
import com.github.zly2006.zhihu.ui.ArticleAnswerSwitchState
import com.github.zly2006.zhihu.util.ARTICLE_EXPORT_TEMPLATE_ASSET
import com.github.zly2006.zhihu.util.buildArticleExportData
import com.github.zly2006.zhihu.util.buildArticleExportFileName
import com.github.zly2006.zhihu.util.inlineArticleExportImagesInHtml
import com.github.zly2006.zhihu.util.renderArticleExportHtml
import com.github.zly2006.zhihu.util.sanitizeArticleExportFileNamePart
import com.github.zly2006.zhihu.viewmodel.filter.ContentDetailProvider
import com.github.zly2006.zhihu.viewmodel.filter.ContentType
import com.github.zly2006.zhihu.viewmodel.filter.applyContentFilterToDisplayItems
import com.github.zly2006.zhihu.viewmodel.filter.applyForegroundReadFilterToDisplayItems
import com.github.zly2006.zhihu.viewmodel.filter.createBlocklistManager
import com.github.zly2006.zhihu.viewmodel.filter.desktopContentFilterDatabaseFile
import com.github.zly2006.zhihu.viewmodel.filter.desktopKeywordSemanticMatcher
import com.github.zly2006.zhihu.viewmodel.filter.getContentFilterDatabase
import com.github.zly2006.zhihu.viewmodel.filter.recordFeedContentInteraction
import com.github.zly2006.zhihu.viewmodel.filter.toFeedFilterSettings
import com.github.zly2006.zhihu.viewmodel.local.LocalRecommendationEngine
import com.github.zly2006.zhihu.viewmodel.local.buildLocalRecommendationEngine
import com.github.zly2006.zhihu.viewmodel.local.getLocalContentDatabase
import io.ktor.client.HttpClient
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.forms.MultiPartFormDataContent
import io.ktor.client.request.forms.formData
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.parameter
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.client.statement.readRawBytes
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonArray
import java.awt.image.BufferedImage
import java.io.ByteArrayInputStream
import java.io.File
import java.net.URLConnection
import java.text.SimpleDateFormat
import java.util.Base64
import java.util.Date
import java.util.Locale
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import javax.imageio.ImageIO
import javax.swing.JEditorPane
import javax.swing.SwingUtilities
import io.ktor.http.ContentType as KtorContentType

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

class DesktopPaginationEnvironment(
    private val store: DesktopAccountStore = DesktopAccountStore(),
    override val notificationSettingsStore: NotificationSettingsStore = desktopNotificationSettingsStore(),
    private val showFetchFailureMessage: ((String) -> Unit)? = null,
) : PaginationEnvironment,
    CollectionContentEnvironment,
    NotificationPaginationEnvironment {
    private val settingsStore = desktopSettingsStore()
    private val historyStorage = DesktopHistoryStorage()
    private val contentFilterDatabase = getContentFilterDatabase(desktopContentFilterDatabaseFile())
    private val localRecommendationEngine by lazy { createLocalRecommendationEngine() }

    override fun httpClient(): HttpClient = store.createHttpClient(store.load().cookies)

    override fun xsrfToken(): String = store.load().cookies["_xsrf"] ?: ""

    override suspend fun fetchJson(
        url: String,
        include: String,
    ): JsonObject? = store.signedFetchJson(url) {
        if (include.isNotEmpty()) {
            parameter("include", include)
        }
        method = HttpMethod.Get
    }

    override suspend fun handleFetchFailure(
        tag: String?,
        error: Exception,
    ) {
        Log.e(tag ?: "PaginationViewModel", "Failed to fetch feeds", error)
        showFetchFailureMessage?.invoke("加载失败: ${error.message}")
    }

    override fun feedDisplaySettings(): FeedDisplaySettings = FeedDisplaySettings(
        enableQualityFilter = false,
        reverseBlock = settingsStore.toFeedFilterSettings().reverseBlock,
    )

    override fun localHistory(): List<NavDestination> =
        historyStorage.history

    override fun configureSignedRequest(builder: HttpRequestBuilder) {
        builder.signDesktopRequest(store.load().cookies)
    }

    override fun accountHttpClient(): HttpClient = httpClient()

    override fun answerNavigatorRepository(): AnswerNavigatorRepository =
        object : AnswerNavigatorRepository {
            override suspend fun fetchAnswerContent(article: Article): DataHolder.Answer? =
                getContentDetail(article) as? DataHolder.Answer

            override suspend fun fetchQuestionFeeds(
                questionId: Long,
                pageUrl: String?,
            ): AnswerNavigatorPage<Feed> {
                val url = pageUrl ?: zhihuQuestionFeedsUrl(questionId, limit = 6)
                val jojo = store.signedFetchJson(url) {
                    method = HttpMethod.Get
                    configureSignedRequest(this)
                } ?: return AnswerNavigatorPage(emptyList(), "")
                return answerNavigatorPageFromJson(jojo) { data ->
                    data.jsonArray.mapNotNull { element ->
                        runCatching { ZhihuJson.decodeJson<Feed>(element) }.getOrNull()
                    }
                }
            }

            override suspend fun fetchCollectionItems(pageUrl: String): AnswerNavigatorPage<CollectionItem> =
                store
                    .signedFetchJson(pageUrl) {
                        method = HttpMethod.Get
                        configureSignedRequest(this)
                    }?.let { jojo ->
                        answerNavigatorPageFromJson(jojo) { data ->
                            data.jsonArray.mapNotNull { element ->
                                runCatching { ZhihuJson.decodeJson<CollectionItem>(element) }.getOrNull()
                            }
                        }
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

    override suspend fun addReadHistory(
        contentToken: String,
        contentTypeName: String,
    ) {
        store.addReadHistory(
            contentToken = contentToken,
            contentTypeName = contentTypeName,
        )
    }

    override suspend fun postHistoryDestination(destination: NavDestination) {
        historyStorage.add(destination)
    }

    override fun setPlainTextClipboard(
        label: String,
        text: String,
    ) {
        copyDesktopPlainText(text)
    }

    override suspend fun isUserBlocked(userId: String): Boolean =
        contentFilterDatabase.createBlocklistManager().isUserBlocked(userId)

    override fun blockedUserIds(): Set<String> =
        runBlocking {
            contentFilterDatabase
                .createBlocklistManager()
                .getAllBlockedUsers()
                .map { it.userId }
                .toSet()
        }

    override suspend fun addBlockedUser(
        userId: String,
        userName: String,
        urlToken: String?,
        avatarUrl: String?,
    ) {
        contentFilterDatabase.createBlocklistManager().addBlockedUser(
            userId = userId,
            userName = userName,
            urlToken = urlToken,
            avatarUrl = avatarUrl,
        )
    }

    override suspend fun removeBlockedUser(userId: String) {
        contentFilterDatabase.createBlocklistManager().removeBlockedUser(userId)
    }

    override suspend fun recordContentOpenEvent(
        destination: NavDestination,
        questionId: Long?,
        openFrom: String,
    ) {
        val resolvedOpenFrom = openFrom.ifBlank {
            consumeDesktopPendingContentOpenFrom(destination)
        }
        ContentOpenEventSupport.recordOpenEvent(
            database = contentFilterDatabase,
            destination = destination,
            questionId = questionId,
            openFrom = resolvedOpenFrom.ifBlank { "unknown" },
        )
    }

    override suspend fun getContentDetail(article: Article): DataHolder.Content? =
        fetchContentDetail(article)

    internal suspend fun getContentDetail(destination: NavDestination): DataHolder.Content? =
        fetchContentDetail(destination)

    override suspend fun recordOpenEvent(
        destination: Article,
        questionId: Long?,
    ) {
        recordContentOpenEvent(destination, questionId)
    }

    override suspend fun followQuestion(
        questionId: Long,
        follow: Boolean,
    ) {
        if (store.load().cookies["d_c0"] == null) return
        store.signedFetchJson("https://www.zhihu.com/api/v4/questions/$questionId/followers") {
            method = if (follow) HttpMethod.Post else HttpMethod.Delete
        }
    }

    override suspend fun applyHomeFeedFilters(items: List<FeedDisplayItem>): HomeFeedFilterResult {
        val settings = settingsStore.toFeedFilterSettings()
        val foregroundItems = applyForegroundReadFilterToDisplayItems(
            settings = settings,
            database = contentFilterDatabase,
            items = items,
        )
        val filteredItems = applyContentFilterToDisplayItems(
            settings = settings,
            database = contentFilterDatabase,
            items = foregroundItems,
            contentDetailProvider = ContentDetailProvider(::fetchContentDetail),
            semanticMatcher = desktopKeywordSemanticMatcher,
        )
        return HomeFeedFilterResult(
            foregroundItems = foregroundItems,
            filteredItems = filteredItems,
            reverseBlock = settings.reverseBlock,
        )
    }

    private suspend fun fetchContentDetail(destination: NavDestination): DataHolder.Content? =
        ContentDetailCache.getOrFetch(destination) { navDestination ->
            when (navDestination) {
                is Article -> fetchDesktopArticleContentDetail(navDestination)
                is Question -> fetchDesktopQuestionContentDetail(navDestination)
                is Pin -> fetchDesktopPinContentDetail(navDestination)
                else -> null
            }
        }

    private suspend fun fetchDesktopArticleContentDetail(article: Article): DataHolder.Content? = runCatching {
        val jo = store.signedFetchJson(
            when (article.type) {
                ArticleType.Article -> "https://www.zhihu.com/api/v4/articles/${article.id}?include=content,topics,paid_info,can_comment,excerpt,thanks_count,voteup_count,comment_count,visited_count,relationship,ip_info,relationship.vote,author.badge_v2"
                ArticleType.Answer -> "https://www.zhihu.com/api/v4/answers/${article.id}?include=content,paid_info,can_comment,excerpt,thanks_count,voteup_count,comment_count,visited_count,attachment,reaction,ip_info,pagination_info,question.topics,reaction.relation.voting,author.badge_v2"
            },
        ) {
            method = HttpMethod.Get
        } ?: return@runCatching null
        decodeArticleContentDetail(article, jo)
    }.getOrNull()

    private suspend fun fetchDesktopQuestionContentDetail(question: Question): DataHolder.Question? = runCatching {
        val jo = store.signedFetchJson("https://www.zhihu.com/api/v4/questions/${question.questionId}?include=read_count,visit_count,answer_count,voteup_count,comment_count,follower_count,detail,excerpt,author,relationship.is_following,topics")
            ?: return@runCatching null
        decodeQuestionContentDetail(jo)
    }.getOrNull()

    private suspend fun fetchDesktopPinContentDetail(pin: Pin): DataHolder.Pin? = runCatching {
        val json = store.signedFetchJson("https://www.zhihu.com/api/v4/pins/${pin.id}") {
            method = HttpMethod.Get
        } ?: return@runCatching null
        decodePinContentDetail(json)
    }.getOrNull()

    override suspend fun sendFeedReadStatus(feed: Feed) {
        val payloadItem = zhihuLastReadTouchItem(feed, "read") ?: return
        postDesktopLastReadTouch(listOf(payloadItem))
    }

    override suspend fun recordContentInteraction(feed: Feed) {
        val settings = settingsStore.toFeedFilterSettings()
        recordFeedContentInteraction(settings, contentFilterDatabase, feed)
    }

    override suspend fun markItemsAsTouched(items: Set<Pair<String, String>>): Set<Pair<String, String>> {
        if (items.isEmpty()) return emptySet()
        val payload = zhihuLastReadTouchItems(items, "touch")
        return if (postDesktopLastReadTouch(payload)) {
            items
        } else {
            emptySet()
        }
    }

    override suspend fun clearAllHistory() {
        historyStorage.clearAndSave()
        if (store.load().cookies["d_c0"] == null) return
        val bodyText = encodeZhihuClearOnlineHistoryBody()
        store.signedFetchJson(ZHIHU_CLEAR_ONLINE_HISTORY_URL) {
            contentType(KtorContentType.Application.Json)
            setBody(bodyText)
            method = HttpMethod.Post
        }
    }

    override fun localRecommendationEngine(): LocalRecommendationEngine = localRecommendationEngine

    override fun hasImageExportPermission(): Boolean = true

    override fun requiresHtmlExportPermission(): Boolean = false

    override fun requestImageExportPermission() = Unit

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

    override suspend fun exportCollectionItemsToHtmlZip(
        collectionTitle: String,
        items: List<CollectionItem>,
        includeImages: Boolean,
        onProgress: suspend (CollectionHtmlExportProgress) -> Unit,
    ): CollectionHtmlExportResult {
        val timestampMillis = System.currentTimeMillis()
        val stagingDir = File(
            desktopCollectionExportCacheDir(),
            "collection_html_export_${sanitizeArticleExportFileNamePart(collectionTitle).ifBlank { "collection" }}_$timestampMillis",
        )
        if (stagingDir.exists()) {
            stagingDir.deleteRecursively()
        }
        if (!stagingDir.mkdirs()) {
            throw IllegalStateException("无法创建导出缓存目录")
        }

        val outputDir = desktopCollectionExportOutputDir()
        val exportHttpClient = accountHttpClient()

        var processedCount = 0
        var successCount = 0
        var skippedCount = 0
        var failedCount = 0
        var currentTitle = ""

        suspend fun emitProgress() {
            onProgress(
                CollectionHtmlExportProgress(
                    totalCount = items.size,
                    processedCount = processedCount,
                    successCount = successCount,
                    skippedCount = skippedCount,
                    failedCount = failedCount,
                    currentTitle = currentTitle,
                ),
            )
        }

        emitProgress()

        try {
            items.forEach { item ->
                currentTitle = item.content.title
                try {
                    val content = item.resolveDesktopExportContent(this)
                    if (content == null) {
                        skippedCount++
                    } else {
                        val htmlContent = if (includeImages) {
                            buildOfflineArticleExportHtml(
                                content = content,
                                includeAppAttribution = true,
                                httpClient = exportHttpClient,
                            )
                        } else {
                            buildArticleExportHtml(
                                content = content,
                                includeAppAttribution = true,
                                extraSectionsHtml = "",
                            )
                        }
                        File(stagingDir, buildArticleExportFileName(content, "html")).writeText(htmlContent)
                        successCount++
                    }
                } catch (e: Exception) {
                    if (e is CancellationException) throw e
                    failedCount++
                    Log.e("CollectionContentViewModel", "Failed to export collection item: ${item.content.title}", e)
                } finally {
                    processedCount++
                    emitProgress()
                }
            }
        } finally {
            exportHttpClient.close()
        }

        val zipFile = if (successCount > 0) {
            if (!outputDir.exists() && !outputDir.mkdirs()) {
                throw IllegalStateException("无法创建导出 ZIP 目录")
            }
            File(outputDir, buildDesktopCollectionExportZipFileName(collectionTitle, timestampMillis)).also { file ->
                if (file.exists()) {
                    file.delete()
                }
                zipDirectoryContents(stagingDir, file)
            }
        } else {
            null
        }

        return CollectionHtmlExportResult(
            totalCount = items.size,
            successCount = successCount,
            skippedCount = skippedCount,
            failedCount = failedCount,
            zipFilePath = zipFile?.absolutePath,
        )
    }

    override suspend fun handleCollectionExportFailure(error: Exception) {
        Log.e("CollectionContentViewModel", "Failed to export collection HTML zip", error)
    }

    private fun createLocalRecommendationEngine(): LocalRecommendationEngine {
        val databaseFile = desktopZhihuDataFile("local-content.db")
        databaseFile.parentFile?.mkdirs()
        val dao = getLocalContentDatabase(databaseFile).contentDao()
        return buildLocalRecommendationEngine(
            dao = dao,
            fetchFeedArray = { url -> fetchDesktopLocalFeedArray(url) },
            logWarning = { message -> Log.w("LocalRecommendationEngine", message) },
            logError = { message, throwable -> Log.e("LocalRecommendationEngine", message, throwable) },
        )
    }

    private suspend fun fetchDesktopLocalFeedArray(url: String): JsonArray =
        store
            .signedFetchJson(url)
            ?.get("data")
            ?.jsonArray ?: JsonArray(emptyList())

    private suspend fun postDesktopLastReadTouch(payload: List<List<String>>): Boolean {
        if (store.load().cookies["d_c0"] == null) return false
        return store.signedWithResponse(
            url = ZHIHU_LAST_READ_TOUCH_URL,
            block = {
                method = HttpMethod.Post
                header("x-requested-with", "fetch")
                setBody(
                    MultiPartFormDataContent(
                        formData {
                            append("items", encodeZhihuLastReadTouchItems(payload))
                        },
                    ),
                )
            },
        ) { response ->
            if (response.status.isSuccess()) {
                true
            } else {
                Log.e("Browse-Touch", response.bodyAsText())
                false
            }
        }
    }
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

private suspend fun CollectionItem.resolveDesktopExportContent(
    environment: DesktopPaginationEnvironment,
): DataHolder.Content? {
    val destination = content.navDestination as? Article ?: return null
    return environment.getContentDetail(destination)
}

private fun desktopCollectionExportCacheDir(): File =
    desktopZhihuDataFile("collection-html-export-cache").also { directory ->
        if (!directory.exists()) {
            directory.mkdirs()
        }
    }

private fun desktopCollectionExportOutputDir(): File =
    desktopZhihuDownloadsDir("无法创建导出 ZIP 目录")

private fun buildDesktopCollectionExportZipFileName(
    collectionTitle: String,
    timestampMillis: Long,
): String {
    val safeTitle = sanitizeArticleExportFileNamePart(collectionTitle).ifBlank { "收藏夹" }
    val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date(timestampMillis))
    return "zhihu++_${safeTitle}_$timestamp.zip"
}

private suspend fun zipDirectoryContents(
    sourceDir: File,
    zipFile: File,
) = withContext(Dispatchers.IO) {
    ZipOutputStream(zipFile.outputStream().buffered()).use { outputStream ->
        sourceDir
            .listFiles()
            ?.sortedBy { it.name }
            ?.forEach { file ->
                addFileToZip(
                    file = file,
                    entryPrefix = "",
                    outputStream = outputStream,
                )
            }
    }
}

private fun addFileToZip(
    file: File,
    entryPrefix: String,
    outputStream: ZipOutputStream,
) {
    if (file.isDirectory) {
        val nextPrefix = if (entryPrefix.isBlank()) file.name else "$entryPrefix/${file.name}"
        file
            .listFiles()
            ?.sortedBy { it.name }
            ?.forEach { child ->
                addFileToZip(
                    file = child,
                    entryPrefix = nextPrefix,
                    outputStream = outputStream,
                )
            }
        return
    }

    val entryName = if (entryPrefix.isBlank()) file.name else "$entryPrefix/${file.name}"
    outputStream.putNextEntry(ZipEntry(entryName))
    file.inputStream().buffered().use { inputStream ->
        inputStream.copyTo(outputStream)
    }
    outputStream.closeEntry()
}

@Composable
actual fun rememberPaginationEnvironment(allowGuestAccess: Boolean): PaginationEnvironment =
    remember(allowGuestAccess) { DesktopPaginationEnvironment() }
