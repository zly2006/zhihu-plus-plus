package com.github.zly2006.zhihu.viewmodel

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.github.zly2006.zhihu.data.ContentDetailCache
import com.github.zly2006.zhihu.data.decodeArticleContentDetail
import com.github.zly2006.zhihu.data.decodePinContentDetail
import com.github.zly2006.zhihu.data.zhihuArticleContentDetailUrl
import com.github.zly2006.zhihu.data.zhihuPinContentDetailUrl
import com.github.zly2006.zhihu.navigation.Article
import com.github.zly2006.zhihu.navigation.NavDestination
import com.github.zly2006.zhihu.navigation.Pin
import com.github.zly2006.zhihu.shared.data.Collection
import com.github.zly2006.zhihu.shared.data.CollectionItem
import com.github.zly2006.zhihu.shared.data.DataHolder
import com.github.zly2006.zhihu.shared.data.Feed
import com.github.zly2006.zhihu.shared.data.FeedDisplayItem
import com.github.zly2006.zhihu.shared.data.ZHIHU_CLEAR_ONLINE_HISTORY_URL
import com.github.zly2006.zhihu.shared.data.ZHIHU_LAST_READ_TOUCH_URL
import com.github.zly2006.zhihu.shared.data.decodeZhihuCollection
import com.github.zly2006.zhihu.shared.data.encodeZhihuClearOnlineHistoryBody
import com.github.zly2006.zhihu.shared.data.encodeZhihuLastReadTouchItems
import com.github.zly2006.zhihu.shared.data.navDestination
import com.github.zly2006.zhihu.shared.data.zhihuLastReadTouchItem
import com.github.zly2006.zhihu.shared.data.zhihuLastReadTouchItems
import com.github.zly2006.zhihu.shared.desktop.DesktopAccountStore
import com.github.zly2006.zhihu.shared.desktop.DesktopHistoryStorage
import com.github.zly2006.zhihu.shared.desktop.desktopZhihuDataFile
import com.github.zly2006.zhihu.shared.desktop.desktopZhihuDownloadsDir
import com.github.zly2006.zhihu.shared.desktop.signedFetchJson
import com.github.zly2006.zhihu.shared.desktop.signedWithResponse
import com.github.zly2006.zhihu.shared.platform.UserMessageSink
import com.github.zly2006.zhihu.shared.platform.desktopSettingsStore
import com.github.zly2006.zhihu.shared.util.Log
import com.github.zly2006.zhihu.util.buildArticleExportFileName
import com.github.zly2006.zhihu.util.sanitizeArticleExportFileNamePart
import com.github.zly2006.zhihu.viewmodel.filter.ContentDetailProvider
import com.github.zly2006.zhihu.viewmodel.filter.applyContentFilterToDisplayItems
import com.github.zly2006.zhihu.viewmodel.filter.applyForegroundReadFilterToDisplayItems
import com.github.zly2006.zhihu.viewmodel.filter.desktopContentFilterDatabaseFile
import com.github.zly2006.zhihu.viewmodel.filter.desktopKeywordSemanticMatcher
import com.github.zly2006.zhihu.viewmodel.filter.getContentFilterDatabase
import com.github.zly2006.zhihu.viewmodel.filter.recordFeedContentInteraction
import com.github.zly2006.zhihu.viewmodel.filter.toFeedFilterSettings
import com.github.zly2006.zhihu.viewmodel.local.LocalRecommendationEngine
import com.github.zly2006.zhihu.viewmodel.local.buildLocalRecommendationEngine
import com.github.zly2006.zhihu.viewmodel.local.getLocalContentDatabase
import io.ktor.client.HttpClient
import io.ktor.client.request.forms.MultiPartFormDataContent
import io.ktor.client.request.forms.formData
import io.ktor.client.request.header
import io.ktor.client.request.parameter
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpMethod
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonArray
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import io.ktor.http.ContentType as KtorContentType

class DesktopPaginationEnvironment(
    private val store: DesktopAccountStore = DesktopAccountStore(),
) : PaginationEnvironment,
    CollectionContentEnvironment {
    private val settingsStore = desktopSettingsStore()
    private val historyStorage = DesktopHistoryStorage()
    private val contentFilterDatabase = getContentFilterDatabase(desktopContentFilterDatabaseFile())
    private val localRecommendationEngine by lazy { createLocalRecommendationEngine() }

    override fun httpClient(): HttpClient = store.createHttpClient(store.load().cookies)

    override suspend fun fetchJson(
        url: String,
        include: String,
    ): JsonObject? = store.signedFetchJson(url) {
        if (include.isNotEmpty()) {
            parameter("include", include)
        }
        method = HttpMethod.Get
    }

    override fun logDecodeFailure(
        tag: String?,
        item: JsonElement,
        error: Exception,
    ) {
        Log.e(tag ?: "PaginationViewModel", "Failed to decode item: $item", error)
    }

    override suspend fun handleFetchFailure(
        tag: String?,
        error: Exception,
    ) {
        Log.e(tag ?: "PaginationViewModel", "Failed to fetch feeds", error)
    }

    override fun feedDisplaySettings(): FeedDisplaySettings = FeedDisplaySettings(
        enableQualityFilter = false,
        reverseBlock = settingsStore.toFeedFilterSettings().reverseBlock,
    )

    override fun localHistory(): List<NavDestination> =
        historyStorage.history

    override suspend fun addReadHistory(
        contentToken: String,
        contentTypeName: String,
    ) {
        store.addReadHistory(
            contentToken = contentToken,
            contentTypeName = contentTypeName,
        )
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
                is Pin -> fetchDesktopPinContentDetail(navDestination)
                else -> null
            }
        }

    private suspend fun fetchDesktopArticleContentDetail(article: Article): DataHolder.Content? = runCatching {
        val jo = store.signedFetchJson(zhihuArticleContentDetailUrl(article)) {
            method = HttpMethod.Get
        } ?: return@runCatching null
        decodeArticleContentDetail(article, jo)
    }.getOrNull()

    private suspend fun fetchDesktopPinContentDetail(pin: Pin): DataHolder.Pin? = runCatching {
        val json = store.signedFetchJson(zhihuPinContentDetailUrl(pin)) {
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

    override suspend fun fetchCollection(collectionId: String): Collection {
        val json = fetchJson("https://www.zhihu.com/api/v4/collections/$collectionId", "")
            ?: throw IllegalStateException("收藏夹信息加载失败")
        return decodeZhihuCollection(json["collection"] ?: throw IllegalStateException("收藏夹信息为空"))
    }

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
        val articleRuntime = DesktopArticleViewModelRuntime(
            store = store,
            userMessages = UserMessageSink(
                showShortMessage = { message -> Log.i("CollectionContentViewModel", message) },
                showLongMessage = { message -> Log.i("CollectionContentViewModel", message) },
            ),
        )
        val exportHttpClient = articleRuntime.accountHttpClient()

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
                    val content = item.resolveDesktopExportContent(articleRuntime)
                    if (content == null) {
                        skippedCount++
                    } else {
                        val htmlContent = if (includeImages) {
                            articleRuntime.buildOfflineArticleExportHtml(
                                content = content,
                                includeAppAttribution = true,
                                httpClient = exportHttpClient,
                            )
                        } else {
                            articleRuntime.buildArticleExportHtml(
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

private suspend fun CollectionItem.resolveDesktopExportContent(
    articleRuntime: DesktopArticleViewModelRuntime,
): DataHolder.Content? {
    val destination = content.navDestination as? Article ?: return null
    return articleRuntime.getContentDetail(destination)
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
