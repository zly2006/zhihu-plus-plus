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

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.github.zly2006.zhihu.navigation.Article
import com.github.zly2006.zhihu.navigation.NavDestination
import com.github.zly2006.zhihu.shared.data.DataHolder
import com.github.zly2006.zhihu.shared.data.Feed
import com.github.zly2006.zhihu.shared.data.FeedDisplayItem
import com.github.zly2006.zhihu.shared.data.navDestination
import com.github.zly2006.zhihu.shared.data.target
import com.github.zly2006.zhihu.shared.desktop.DesktopAccountStore
import com.github.zly2006.zhihu.shared.desktop.DesktopHistoryStorage
import com.github.zly2006.zhihu.shared.desktop.copyDesktopPlainText
import com.github.zly2006.zhihu.shared.desktop.desktopZhihuDataFile
import com.github.zly2006.zhihu.shared.desktop.desktopZhihuDownloadsDir
import com.github.zly2006.zhihu.shared.filter.ContentOpenEventSupport
import com.github.zly2006.zhihu.shared.filter.ContentOpenFrom
import com.github.zly2006.zhihu.shared.filter.TrackedContentIdentity
import com.github.zly2006.zhihu.shared.notification.NotificationSettingsStore
import com.github.zly2006.zhihu.shared.notification.desktopNotificationSettingsStore
import com.github.zly2006.zhihu.shared.platform.desktopSettingsStore
import com.github.zly2006.zhihu.shared.util.Log
import com.github.zly2006.zhihu.ui.ArticleAnswerSwitchState
import com.github.zly2006.zhihu.util.buildArticleExportFileName
import com.github.zly2006.zhihu.util.buildCollectionExportZipFileName
import com.github.zly2006.zhihu.util.sanitizeArticleExportFileNamePart
import com.github.zly2006.zhihu.viewmodel.CollectionItem
import com.github.zly2006.zhihu.viewmodel.filter.BlockedKeywordService
import com.github.zly2006.zhihu.viewmodel.filter.BlockedUser
import com.github.zly2006.zhihu.viewmodel.filter.ContentDetailProvider
import com.github.zly2006.zhihu.viewmodel.filter.ContentFilterManager
import com.github.zly2006.zhihu.viewmodel.filter.ContentType
import com.github.zly2006.zhihu.viewmodel.filter.FeedContentFilterPipeline
import com.github.zly2006.zhihu.viewmodel.filter.FeedDisplayFilterPipeline
import com.github.zly2006.zhihu.viewmodel.filter.ForegroundReadFilterPipeline
import com.github.zly2006.zhihu.viewmodel.filter.desktopContentFilterDatabaseFile
import com.github.zly2006.zhihu.viewmodel.filter.desktopKeywordSemanticMatcher
import com.github.zly2006.zhihu.viewmodel.filter.getContentFilterDatabase
import com.github.zly2006.zhihu.viewmodel.filter.toFeedFilterSettings
import com.github.zly2006.zhihu.viewmodel.local.LocalRecommendationEngine
import com.github.zly2006.zhihu.viewmodel.local.buildLocalRecommendationEngine
import com.github.zly2006.zhihu.viewmodel.local.getLocalContentDatabase
import io.ktor.client.HttpClient
import io.ktor.client.request.setBody
import io.ktor.http.contentType
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.put
import java.awt.image.BufferedImage
import java.io.File
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import javax.imageio.IIOImage
import javax.imageio.ImageIO
import javax.imageio.ImageWriteParam
import javax.swing.JEditorPane
import javax.swing.SwingUtilities
import com.github.zly2006.zhihu.util.buildArticleExportHtml as buildSharedArticleExportHtml
import com.github.zly2006.zhihu.util.buildOfflineArticleExportHtml as buildSharedOfflineArticleExportHtml
import io.ktor.http.ContentType as KtorContentType

private val desktopContentFilterDb = getContentFilterDatabase(desktopContentFilterDatabaseFile())
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
    NotificationEnvironment {
    private val settingsStore = desktopSettingsStore()
    private val historyStorage = DesktopHistoryStorage()
    private val contentFilterDb = desktopContentFilterDb
    private val localRecommendationEngine by lazy { createLocalRecommendationEngine() }

    override fun httpClient(): HttpClient = store.httpClient()

    override fun xsrfToken(): String = store.load().cookies["_xsrf"] ?: ""

    override fun authenticatedCookies(): Map<String, String> = store.load().cookies

    override suspend fun <T> withAuthenticatedClient(
        block: suspend (client: HttpClient, cookies: Map<String, String>) -> T,
    ): T = store.withAuthenticatedClient(block)

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

    override fun articleAnswerSwitchState(): ArticleAnswerSwitchState? = desktopArticleAnswerSwitchState

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
        contentFilterDb.blockedUserDao().isUserBlocked(userId)

    override fun blockedUserIds(): Set<String> =
        runBlocking {
            contentFilterDb
                .blockedUserDao()
                .getAllUsers()
                .map { it.userId }
                .toSet()
        }

    override suspend fun addBlockedUser(
        userId: String,
        userName: String,
        urlToken: String?,
        avatarUrl: String?,
    ) {
        contentFilterDb.blockedUserDao().insertUser(
            BlockedUser(
                userId = userId,
                userName = userName,
                urlToken = urlToken,
                avatarUrl = avatarUrl,
            ),
        )
    }

    override suspend fun removeBlockedUser(userId: String) {
        contentFilterDb.blockedUserDao().deleteUserById(userId)
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
            database = contentFilterDb,
            destination = destination,
            questionId = questionId,
            openFrom = resolvedOpenFrom.ifBlank { "unknown" },
        )
    }

    override suspend fun recordOpenEvent(
        destination: Article,
        questionId: Long?,
    ) {
        recordContentOpenEvent(destination, questionId)
    }

    override suspend fun applyHomeFeedFilters(items: List<FeedDisplayItem>): HomeFeedFilterResult {
        val settings = settingsStore.toFeedFilterSettings()
        val foregroundItems = ForegroundReadFilterPipeline(
            settings = settings,
            contentFilterManager = ContentFilterManager(contentFilterDb.contentFilterDao()),
            blockedFeedRecordDao = contentFilterDb.blockedFeedRecordDao(),
        ).filter(items)
        val filteredItems = FeedDisplayFilterPipeline(
            settings = settings,
            contentDetailProvider = ContentDetailProvider(::getOrFetchContentDetail),
            contentFilterPipeline = FeedContentFilterPipeline(
                settings = settings,
                blockedKeywordDao = contentFilterDb.blockedKeywordDao(),
                blockedUserDao = contentFilterDb.blockedUserDao(),
                blockedTopicDao = contentFilterDb.blockedTopicDao(),
                blockedKeywordService = BlockedKeywordService(
                    keywordDao = contentFilterDb.blockedKeywordDao(),
                    recordDao = contentFilterDb.blockedContentRecordDao(),
                    semanticMatcher = desktopKeywordSemanticMatcher,
                ),
            ),
            blockedFeedRecordDao = contentFilterDb.blockedFeedRecordDao(),
        ).filter(foregroundItems)
        return HomeFeedFilterResult(
            foregroundItems = foregroundItems,
            filteredItems = filteredItems,
            reverseBlock = settings.reverseBlock,
        )
    }

    override suspend fun recordContentInteraction(feed: Feed) {
        val settings = settingsStore.toFeedFilterSettings()
        if (!settings.enableContentFilter) return
        val target = feed.target ?: return
        val (targetType, targetId) = when (target) {
            is Feed.AnswerTarget -> ContentType.ANSWER to target.id.toString()
            is Feed.ArticleTarget -> ContentType.ARTICLE to target.id.toString()
            is Feed.QuestionTarget -> ContentType.QUESTION to target.id.toString()
            is Feed.PinTarget -> ContentType.PIN to target.id.toString()
            else -> return
        }
        ContentFilterManager(contentFilterDb.contentFilterDao()).recordContentInteraction(targetType, targetId)
    }

    override suspend fun clearAllHistory() {
        historyStorage.clearAndSave()
        if (store.load().cookies["d_c0"] == null) return
        postSigned("https://api.zhihu.com/read_history/batch_del") {
            contentType(KtorContentType.Application.Json)
            setBody(
                buildJsonObject {
                    put("pairs", JsonArray(emptyList()))
                    put("clear", true)
                }.toString(),
            )
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
    ): String = buildSharedArticleExportHtml(
        loadAssetText = ::loadExportAssetText,
        content = content,
        includeAppAttribution = includeAppAttribution,
        extraSectionsHtml = extraSectionsHtml,
    )

    override suspend fun buildOfflineArticleExportHtml(
        content: DataHolder.Content,
        includeAppAttribution: Boolean,
        httpClient: HttpClient,
    ): String = buildSharedOfflineArticleExportHtml(
        loadAssetText = ::loadExportAssetText,
        content = content,
        includeAppAttribution = includeAppAttribution,
        httpClient = httpClient,
        useOriginalOnImageFetchFailure = true,
    )

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
        writeJpegImage(file, (bitmap as BufferedImage).toJpegImage())
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
            desktopZhihuDataFile("collection-html-export-cache").also { directory ->
                if (!directory.exists()) {
                    directory.mkdirs()
                }
            },
            "collection_html_export_${sanitizeArticleExportFileNamePart(collectionTitle).ifBlank { "collection" }}_$timestampMillis",
        )
        if (stagingDir.exists()) {
            stagingDir.deleteRecursively()
        }
        if (!stagingDir.mkdirs()) {
            throw IllegalStateException("无法创建导出缓存目录")
        }

        val outputDir = desktopZhihuDownloadsDir("无法创建导出 ZIP 目录")
        val exportHttpClient = httpClient()

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

        items.forEach { item ->
            currentTitle = item.content.title
            try {
                val content = item.resolveDesktopExportContent(this)
                if (content == null) {
                    skippedCount++
                } else {
                    val htmlContent = buildSharedOfflineArticleExportHtml(
                        loadAssetText = ::loadExportAssetText,
                        content = content,
                        includeAppAttribution = true,
                        httpClient = exportHttpClient,
                        includeImages = includeImages,
                        useOriginalOnImageFetchFailure = true,
                    )
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

        val zipFile = if (successCount > 0) {
            if (!outputDir.exists() && !outputDir.mkdirs()) {
                throw IllegalStateException("无法创建导出 ZIP 目录")
            }
            File(outputDir, buildCollectionExportZipFileName(collectionTitle, timestampMillis)).also { file ->
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
            fetchFeedArray = { url ->
                fetchJson(url, "")
                    ?.get("data")
                    ?.jsonArray ?: JsonArray(emptyList())
            },
            logWarning = { message -> Log.w("LocalRecommendationEngine", message) },
            logError = { message, throwable -> Log.e("LocalRecommendationEngine", message, throwable) },
        )
    }
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

private fun BufferedImage.toJpegImage(): BufferedImage {
    if (type == BufferedImage.TYPE_INT_RGB) {
        return this
    }
    return BufferedImage(width, height, BufferedImage.TYPE_INT_RGB).also { image ->
        val graphics = image.createGraphics()
        try {
            graphics.color = java.awt.Color.WHITE
            graphics.fillRect(0, 0, width, height)
            graphics.drawImage(this, 0, 0, null)
        } finally {
            graphics.dispose()
        }
    }
}

private fun writeJpegImage(file: File, image: BufferedImage) {
    val writers = ImageIO.getImageWritersByFormatName("jpg")
    if (!writers.hasNext()) {
        throw IllegalStateException("No JPEG writer available")
    }
    val writer = writers.next()
    ImageIO.createImageOutputStream(file).use { output ->
        writer.output = output
        try {
            val params = writer.defaultWriteParam
            if (params.canWriteCompressed()) {
                params.compressionMode = ImageWriteParam.MODE_EXPLICIT
                params.compressionQuality = 0.80f
            }
            writer.write(null, IIOImage(image, null, null), params)
        } finally {
            writer.dispose()
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
    return environment.fetchContentDetail(destination)
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
