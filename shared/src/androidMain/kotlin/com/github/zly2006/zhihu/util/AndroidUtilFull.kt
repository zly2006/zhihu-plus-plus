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

package com.github.zly2006.zhihu.util
import android.content.Context
import android.content.Intent
import android.content.pm.ResolveInfo
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.os.SystemClock
import android.provider.MediaStore
import android.provider.MediaStore.MediaColumns
import android.provider.Settings
import android.util.Base64
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.browser.customtabs.CustomTabColorSchemeParams
import androidx.browser.customtabs.CustomTabsIntent
import androidx.compose.foundation.Image
import androidx.compose.foundation.text.InlineTextContent
import androidx.compose.foundation.text.contextmenu.data.TextContextMenuItem
import androidx.compose.foundation.text.contextmenu.modifier.filterTextContextMenuComponents
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.text.Placeholder
import androidx.compose.ui.text.PlaceholderVerticalAlign
import androidx.compose.ui.unit.em
import androidx.core.content.FileProvider
import androidx.core.net.toUri
import androidx.lifecycle.lifecycleScope
import com.github.zly2006.zhihu.data.AccountData
import com.github.zly2006.zhihu.navigation.Article
import com.github.zly2006.zhihu.navigation.ArticleType
import com.github.zly2006.zhihu.navigation.NavDestination
import com.github.zly2006.zhihu.shared.data.Collection
import com.github.zly2006.zhihu.shared.data.DataHolder
import com.github.zly2006.zhihu.shared.data.ZhihuJson
import com.github.zly2006.zhihu.shared.data.ZhihuJson.json
import com.github.zly2006.zhihu.shared.platform.androidSettingsStore
import com.github.zly2006.zhihu.shared.platform.androidUserMessageSink
import com.github.zly2006.zhihu.shared.util.ContinuousUsageReminderPolicy
import com.github.zly2006.zhihu.shared.util.ZHIHU_WEB_ZSE93
import com.github.zly2006.zhihu.shared.util.signZhihuFetchRequest
import com.github.zly2006.zhihu.util.signFetchRequest
import io.ktor.client.HttpClient
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.client.statement.readRawBytes
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.Url
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.serializer
import java.io.ByteArrayInputStream
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLConnection
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

fun buildArticleExportHtml(
    context: Context,
    exportData: ArticleExportData,
    extraSectionsHtml: String = "",
): String = renderArticleExportHtml(
    template = loadArticleExportTemplate(context),
    exportData = exportData,
    extraSectionsHtml = extraSectionsHtml,
)

suspend fun buildOfflineArticleExportHtml(
    context: Context,
    exportData: ArticleExportData,
    httpClient: HttpClient,
    includeImages: Boolean = true,
    extraSectionsHtml: String = "",
): String {
    val htmlContent = buildArticleExportHtml(
        context = context,
        exportData = exportData,
        extraSectionsHtml = extraSectionsHtml,
    )
    if (!includeImages) {
        return htmlContent
    }

    return inlineArticleExportImagesInHtml(htmlContent) { imageUrl ->
        fetchArticleExportImageDataUrl(httpClient, imageUrl)
    }
}

suspend fun buildOfflineArticleExportHtml(
    context: Context,
    content: DataHolder.Content,
    includeAppAttribution: Boolean,
    httpClient: HttpClient,
    includeImages: Boolean = true,
    extraSectionsHtml: String = "",
): String = buildOfflineArticleExportHtml(
    context = context,
    exportData = buildArticleExportData(
        content = content,
        includeAppAttribution = includeAppAttribution,
    ),
    httpClient = httpClient,
    includeImages = includeImages,
    extraSectionsHtml = extraSectionsHtml,
)

private fun loadArticleExportTemplate(context: Context): String = context.assets.open(ARTICLE_EXPORT_TEMPLATE_ASSET).use { inputStream ->
    inputStream.bufferedReader().use { reader ->
        reader.readText()
    }
}

suspend fun fetchArticleExportImageDataUrl(
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

    return "data:$mimeType;base64,${android.util.Base64.encodeToString(bytes, android.util.Base64.NO_WRAP)}"
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

actual fun hmacSha1Hex(key: String, message: String): String {
    val signingKey = SecretKeySpec(key.toByteArray(StandardCharsets.UTF_8), "HmacSHA1")
    val mac = Mac.getInstance("HmacSHA1")
    mac.init(signingKey)
    val rawHmac = mac.doFinal(message.toByteArray(StandardCharsets.UTF_8))
    return rawHmac.joinToString("") { "%02x".format(it) }
}

@Serializable
private data class EmojiApiResponse(
    val data: EmojiData? = null,
)

@Serializable
private data class EmojiData(
    val stickers: List<StickerItem> = emptyList(),
)

@Serializable
private data class StickerItem(
    val id: String,
    val placeholder: String,
    @SerialName("static_image_url") val staticImageUrl: String,
)

object EmojiManager {
    private const val TAG = "EmojiManager"
    private const val EMOJI_API_URL = "https://www.zhihu.com/api/v4/sticker-groups/1114161698310770688"
    private const val EMOJI_MAPPING_FILE = "emoji_mapping.json"
    private const val EMOJI_CACHE_VERSION_FILE = "emoji_version.txt"
    private const val CURRENT_EMOJI_VERSION = "1" // 增加版本号可强制重新下载

    // emoji占位符到文件名的映射
    private val emojiMapping = mutableMapOf<String, String>()

    // emoji文件名到本地缓存路径的映射
    private val emojiCache = mutableMapOf<String, String>()

    private var isInitialized = false

    /**
     * 初始化emoji管理器，从Zhihu API动态下载emoji数据
     */
    suspend fun initialize(context: Context) = withContext(Dispatchers.IO) {
        if (isInitialized) return@withContext

        try {
            val emojiDir = File(context.filesDir, "emojis")
            if (!emojiDir.exists()) {
                emojiDir.mkdirs()
            }

            val mappingFile = File(context.filesDir, EMOJI_MAPPING_FILE)
            val versionFile = File(context.filesDir, EMOJI_CACHE_VERSION_FILE)

            // 检查是否需要重新下载
            val needsDownload = !mappingFile.exists() ||
                !versionFile.exists() ||
                versionFile.readText().trim() != CURRENT_EMOJI_VERSION

            if (needsDownload) {
                Log.i(TAG, "Downloading emoji data from Zhihu API...")
                downloadAndCacheEmojis(emojiDir, mappingFile, versionFile)
            } else {
                Log.i(TAG, "Loading emoji data from cache...")
                loadEmojiMapping(mappingFile, emojiDir)
            }

            Log.i(TAG, "Emoji manager initialized with ${emojiMapping.size} emojis")
            isInitialized = true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize emoji manager", e)
            // 尝试加载本地缓存
            try {
                val mappingFile = File(context.filesDir, EMOJI_MAPPING_FILE)
                val emojiDir = File(context.filesDir, "emojis")
                if (mappingFile.exists()) {
                    loadEmojiMapping(mappingFile, emojiDir)
                    isInitialized = true
                    Log.i(TAG, "Loaded emoji from cache after download failure")
                }
            } catch (e2: Exception) {
                Log.e(TAG, "Failed to load emoji from cache", e2)
            }
        }
    }

    /**
     * 从Zhihu API下载emoji数据并缓存到本地
     */
    private fun downloadAndCacheEmojis(
        emojiDir: File,
        mappingFile: File,
        versionFile: File,
    ) {
        val url = URL(EMOJI_API_URL)
        val connection = url.openConnection() as HttpURLConnection
        connection.requestMethod = "GET"
        connection.connectTimeout = 10000
        connection.readTimeout = 10000

        try {
            val responseCode = connection.responseCode
            if (responseCode != HttpURLConnection.HTTP_OK) {
                throw Exception("HTTP error code: $responseCode")
            }

            val response = connection.inputStream.bufferedReader().use { it.readText() }
            val json = Json { ignoreUnknownKeys = true }
            val stickers = json.decodeFromString<EmojiApiResponse>(response).data?.stickers
                ?: throw Exception("Invalid response format")

            val newMapping = mutableMapOf<String, String>()
            var downloadCount = 0

            stickers.forEach { sticker ->
                val fileName = "emoji_${sticker.id}.png"
                newMapping[sticker.placeholder] = fileName

                // 解析base64图片数据
                if (sticker.staticImageUrl.startsWith("data:image/png;base64,")) {
                    val base64Data = sticker.staticImageUrl.substring("data:image/png;base64,".length)
                    val imageBytes = Base64.decode(base64Data, Base64.DEFAULT)

                    val targetFile = File(emojiDir, fileName)
                    targetFile.outputStream().use { it.write(imageBytes) }

                    emojiCache[fileName] = targetFile.absolutePath
                    downloadCount++
                }
            }

            // 保存映射文件
            val mappingJson = json.encodeToString(
                kotlinx.serialization.serializer<Map<String, String>>(),
                newMapping,
            )
            mappingFile.writeText(mappingJson)

            // 保存版本文件
            versionFile.writeText(CURRENT_EMOJI_VERSION)

            emojiMapping.clear()
            emojiMapping.putAll(newMapping)

            Log.i(TAG, "Downloaded and cached $downloadCount emojis")
        } finally {
            connection.disconnect()
        }
    }

    /**
     * 从本地缓存加载emoji映射
     */
    private fun loadEmojiMapping(mappingFile: File, emojiDir: File) {
        val json = Json { ignoreUnknownKeys = true }
        val mappingJson = mappingFile.readText()
        val mapping = json.decodeFromString<Map<String, String>>(mappingJson)

        emojiMapping.clear()
        emojiMapping.putAll(mapping)

        emojiCache.clear()
        mapping.values.forEach { fileName ->
            val file = File(emojiDir, fileName)
            if (file.exists()) {
                emojiCache[fileName] = file.absolutePath
            }
        }
    }

    /**
     * 根据emoji占位符获取本地文件路径
     * @param placeholder emoji占位符
     * @return 本地文件路径，如果emoji不存在返回null
     */
    fun getEmojiPath(placeholder: String): String? {
        val fileName = emojiMapping[placeholder] ?: return null
        return emojiCache[fileName]
    }

    /**
     * 根据emoji文件名获取本地文件路径
     * @param fileName emoji文件名，例如 "emoji_1114211823741685761.png"
     * @return 本地文件路径，如果emoji不存在返回null
     */
    fun getEmojiPathByFileName(fileName: String): String? = emojiCache[fileName]

    /**
     * 创建InlineTextContent映射，用于Text组件显示emoji
     */
    fun createInlineContentMap(emojis: Set<String>): Map<String, InlineTextContent> {
        return emojis
            .mapNotNull { emojiKey ->
                val path = getEmojiPathByFileName(emojiKey.removePrefix("emoji_")) ?: return@mapNotNull null

                emojiKey to InlineTextContent(
                    placeholder = Placeholder(
                        width = 1.3.em,
                        height = 1.3.em,
                        placeholderVerticalAlign = PlaceholderVerticalAlign.TextCenter,
                    ),
                ) {
                    val bitmap = BitmapFactory.decodeFile(path)
                    if (bitmap != null) {
                        Image(
                            bitmap = bitmap.asImageBitmap(),
                            contentDescription = emojiKey,
                            modifier = Modifier,
                        )
                    }
                }
            }.toMap()
    }
}

data class ResolvedCollectionHtmlExportItem(
    val htmlFileName: String,
    val htmlContent: String,
)

data class CollectionHtmlZipExportProgress(
    val totalCount: Int,
    val processedCount: Int,
    val successCount: Int,
    val skippedCount: Int,
    val failedCount: Int,
    val currentTitle: String = "",
)

data class CollectionHtmlZipExportResult(
    val totalCount: Int,
    val successCount: Int,
    val skippedCount: Int,
    val failedCount: Int,
    val stagingDir: File,
    val zipFile: File?,
)

fun buildCollectionExportZipFileName(
    collectionTitle: String,
    timestampMillis: Long = System.currentTimeMillis(),
): String {
    val safeTitle = sanitizeArticleExportFileNamePart(collectionTitle).ifBlank { "收藏夹" }
    val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date(timestampMillis))
    return "zhihu++_${safeTitle}_$timestamp.zip"
}

suspend fun <T> exportCollectionItemsToZip(
    collectionTitle: String,
    items: List<T>,
    cacheDir: File,
    outputDir: File,
    timestampMillis: Long = System.currentTimeMillis(),
    displayTitle: (T) -> String,
    resolveItem: suspend (T) -> ResolvedCollectionHtmlExportItem?,
    onProgress: suspend (CollectionHtmlZipExportProgress) -> Unit = {},
): CollectionHtmlZipExportResult {
    val stagingDir = File(
        cacheDir,
        "collection_html_export_${sanitizeArticleExportFileNamePart(collectionTitle).ifBlank { "collection" }}_$timestampMillis",
    )
    if (stagingDir.exists()) {
        stagingDir.deleteRecursively()
    }
    if (!stagingDir.mkdirs()) {
        throw IllegalStateException("无法创建导出缓存目录")
    }

    val totalCount = items.size
    var processedCount = 0
    var successCount = 0
    var skippedCount = 0
    var failedCount = 0
    var currentTitle = ""

    suspend fun emitProgress() {
        onProgress(
            CollectionHtmlZipExportProgress(
                totalCount = totalCount,
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
        currentTitle = displayTitle(item)
        try {
            val resolvedItem = resolveItem(item)
            if (resolvedItem == null) {
                skippedCount++
            } else {
                val htmlFile = File(stagingDir, resolvedItem.htmlFileName)
                htmlFile.parentFile?.mkdirs()
                htmlFile.writeText(resolvedItem.htmlContent)
                successCount++
            }
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            failedCount++
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

    return CollectionHtmlZipExportResult(
        totalCount = totalCount,
        successCount = successCount,
        skippedCount = skippedCount,
        failedCount = failedCount,
        stagingDir = stagingDir,
        zipFile = zipFile,
    )
}

private fun zipDirectoryContents(
    sourceDir: File,
    zipFile: File,
) {
    ZipOutputStream(FileOutputStream(zipFile).buffered()).use { outputStream ->
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

class ContinuousUsageReminderManager(
    private val activity: ComponentActivity,
) {
    private val settingsStore by lazy { androidSettingsStore(activity) }

    private var policy = ContinuousUsageReminderPolicy(loadIntervalMinutes())
    private var sessionAccumulatedForegroundMs =
        settingsStore.getLong(KEY_SESSION_ACCUMULATED_FOREGROUND_MS, 0L).coerceAtLeast(0L)
    private var foregroundStartElapsedMs: Long? = null
    private var checkJob: Job? = null
    private var reminderDialog: AlertDialog? = null

    fun onAppForeground() {
        if (foregroundStartElapsedMs == null) {
            restoreSessionForForegroundStart()
            foregroundStartElapsedMs = SystemClock.elapsedRealtime()
        }
        val elapsedForegroundMs = currentElapsedForegroundMs()
        syncIntervalWithPreferences(elapsedForegroundMs, forceUpdatePolicyBucket = true)
        ensureCheckLoop()
        checkAndShowReminder()
    }

    fun onAppBackground() {
        checkJob?.cancel()
        checkJob = null

        foregroundStartElapsedMs?.let { startElapsed ->
            val foregroundDuration = SystemClock.elapsedRealtime() - startElapsed
            sessionAccumulatedForegroundMs += foregroundDuration
            sessionAccumulatedForegroundMs = sessionAccumulatedForegroundMs.coerceAtLeast(0L)
            settingsStore.putLong(KEY_SESSION_ACCUMULATED_FOREGROUND_MS, sessionAccumulatedForegroundMs)
            settingsStore.putLong(KEY_SESSION_LAST_BACKGROUND_WALL_CLOCK_MS, System.currentTimeMillis())
        }
        foregroundStartElapsedMs = null

        reminderDialog?.dismiss()
        reminderDialog = null
    }

    fun onDestroy() {
        checkJob?.cancel()
        checkJob = null
        reminderDialog?.dismiss()
        reminderDialog = null
    }

    private fun ensureCheckLoop() {
        if (checkJob?.isActive == true) return
        checkJob = activity.lifecycleScope.launch {
            while (isActive) {
                delay(CHECK_INTERVAL_MS)
                checkAndShowReminder()
            }
        }
    }

    private fun checkAndShowReminder() {
        val elapsedForegroundMs = currentElapsedForegroundMs()
        if (elapsedForegroundMs <= 0L) return

        syncIntervalWithPreferences(elapsedForegroundMs)

        val reminder = policy.consumeReminder(elapsedForegroundMs) ?: return
        if (activity.isFinishing || activity.isDestroyed) return

        if (reminderDialog?.isShowing == true) return
        reminderDialog = AlertDialog
            .Builder(activity)
            .setTitle("连续浏览提醒")
            .setMessage("你已经连续浏览知乎 ${reminder.durationText}\n\n休息一下吧")
            .setPositiveButton("知道了", null)
            .show()
    }

    private fun restoreSessionForForegroundStart() {
        val now = System.currentTimeMillis()
        val lastBackgroundWallClockMs = settingsStore.getLong(KEY_SESSION_LAST_BACKGROUND_WALL_CLOCK_MS, 0L)
        val shouldContinueSession = shouldContinueSession(lastBackgroundWallClockMs, now)

        if (shouldContinueSession) {
            sessionAccumulatedForegroundMs =
                settingsStore.getLong(KEY_SESSION_ACCUMULATED_FOREGROUND_MS, 0L).coerceAtLeast(0L)
        } else {
            sessionAccumulatedForegroundMs = 0L
            policy.resetSession()
            settingsStore.putLong(KEY_SESSION_ACCUMULATED_FOREGROUND_MS, 0L)
        }

        settingsStore.putLong(KEY_SESSION_LAST_BACKGROUND_WALL_CLOCK_MS, 0L)
    }

    fun currentElapsedForegroundMs(): Long {
        val startElapsed = foregroundStartElapsedMs ?: return sessionAccumulatedForegroundMs
        return sessionAccumulatedForegroundMs + (SystemClock.elapsedRealtime() - startElapsed)
    }

    private fun syncIntervalWithPreferences(
        elapsedForegroundMs: Long = 0L,
        forceUpdatePolicyBucket: Boolean = false,
    ) {
        val interval = loadIntervalMinutes()
        if (!forceUpdatePolicyBucket && interval == policy.intervalMinutes) return
        policy.updateInterval(intervalMinutes = interval, elapsedForegroundMs = elapsedForegroundMs)
    }

    private fun loadIntervalMinutes(): Int {
        val storedInterval = settingsStore.getInt(KEY_CONTINUOUS_USAGE_REMINDER_INTERVAL_MINUTES, 0)
        return ContinuousUsageReminderPolicy.normalizeIntervalMinutes(storedInterval)
    }

    companion object {
        const val KEY_CONTINUOUS_USAGE_REMINDER_INTERVAL_MINUTES = "continuousUsageReminderIntervalMinutes"
        private const val KEY_SESSION_ACCUMULATED_FOREGROUND_MS = "continuousUsageReminderSessionAccumulatedMs"
        private const val KEY_SESSION_LAST_BACKGROUND_WALL_CLOCK_MS = "continuousUsageReminderLastBackgroundWallClockMs"
        private const val CHECK_INTERVAL_MS = 10_000L
        private const val CONTINUITY_GRACE_MS = 5 * 60_000L

        internal fun shouldContinueSession(lastBackgroundWallClockMs: Long, nowWallClockMs: Long): Boolean {
            if (lastBackgroundWallClockMs <= 0L) return false
            if (nowWallClockMs < lastBackgroundWallClockMs) return false
            return nowWallClockMs - lastBackgroundWallClockMs <= CONTINUITY_GRACE_MS
        }
    }
}

fun createEmojiInlineContent(emojiKeys: Set<String>): Map<String, InlineTextContent> {
    return emojiKeys
        .filter { it.startsWith("emoji_") }
        .mapNotNull { emojiKey ->
            val fileName = emojiKey.removePrefix("emoji_")
            val path = EmojiManager.getEmojiPathByFileName(fileName) ?: return@mapNotNull null

            emojiKey to InlineTextContent(
                placeholder = Placeholder(
                    width = 1.3.em,
                    height = 1.3.em,
                    placeholderVerticalAlign = PlaceholderVerticalAlign.TextCenter,
                ),
            ) {
                val bitmap = android.graphics.BitmapFactory.decodeFile(path)
                if (bitmap != null) {
                    Image(
                        bitmap = bitmap.asImageBitmap(),
                        contentDescription = emojiKey,
                        modifier = Modifier,
                    )
                }
            }
        }.toMap()
}

/**
 * 保存图片到相册
 */
suspend fun saveImageToGallery(
    context: Context,
    httpClient: HttpClient,
    imageUrl: String,
) {
    val userMessages = androidUserMessageSink(context)
    try {
        val response = httpClient.get(imageUrl)
        val bytes = response.readRawBytes()
        val fileName = imageUrl.toUri().lastPathSegment ?: "downloaded_image.jpg"

        val contentValues = android.content.ContentValues().apply {
            put(MediaColumns.DISPLAY_NAME, fileName)
            put(MediaColumns.MIME_TYPE, "image/jpeg")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaColumns.RELATIVE_PATH, android.os.Environment.DIRECTORY_PICTURES)
                put(MediaColumns.IS_PENDING, 1)
            }
        }

        val resolver = context.contentResolver
        val collection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            MediaStore.Images.Media
                .getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
        } else {
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI
        }

        val imageUri = resolver.insert(collection, contentValues)
        if (imageUri != null) {
            resolver.openOutputStream(imageUri).use { os ->
                os?.write(bytes)
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                contentValues.clear()
                contentValues.put(MediaColumns.IS_PENDING, 0)
                resolver.update(imageUri, contentValues, null, null)
            }

            userMessages.showShortMessage("图片已保存到相册")
        }
    } catch (e: Exception) {
        userMessages.showShortMessage("保存失败: ${e.message}")
    }
}

/**
 * 分享图片
 * 图片临时保存到 externalCacheDir/share_images/，应用启动时自动清空
 */
suspend fun shareImage(
    context: Context,
    httpClient: HttpClient,
    imageUrl: String,
) {
    val userMessages = androidUserMessageSink(context)
    try {
        val response = httpClient.get(imageUrl)
        val bytes = response.readRawBytes()
        val shareDir = java.io.File(context.externalCacheDir, "share_images").apply { mkdirs() }
        val file = java.io.File(shareDir, "share_${System.currentTimeMillis()}.jpg")
        file.writeBytes(bytes)
        val imageUri = FileProvider.getUriForFile(context, "${context.packageName}.provider", file)
        val shareIntent = Intent().apply {
            action = Intent.ACTION_SEND
            putExtra(Intent.EXTRA_STREAM, imageUri)
            type = "image/jpeg"
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(shareIntent, "分享图片"))
    } catch (e: Exception) {
        userMessages.showShortMessage("分享失败: ${e.message}")
    }
}

/**
 * 清空分享图片缓存目录
 */
fun clearShareImageCache(context: Context) {
    java.io.File(context.externalCacheDir, "share_images").deleteRecursively()
}

fun HttpRequestBuilder.signFetchRequest() {
    val body = if (contentType() == ContentType.Application.Json) {
        body as? String
            ?: bodyType?.kotlinType?.let { type ->
                json.encodeToString(serializer(type), body)
            }
    } else {
        null
    }
    signZhihuFetchRequest(
        zse93 = ZHIHU_WEB_ZSE93,
        dc0 = AccountData.data.cookies["d_c0"] ?: "",
        body = body,
    )
}

@OptIn(DelicateCoroutinesApi::class)
fun telemetry(context: Context, usage: String) {
    require(usage in listOf("start", "login")) {
        "Usage must be either 'start' or 'login', but was '$usage'."
    }
    val settings = androidSettingsStore(context)
    val data = AccountData.loadData(context)
    if (settings.getBoolean("allowTelemetry", true)) {
        val versionName = runCatching {
            context.packageManager.getPackageInfo(context.packageName, 0).versionName
        }.getOrNull() ?: "unknown"
        GlobalScope.launch {
            @OptIn(ExperimentalStdlibApi::class)
            runCatching {
                val hash = MessageDigest
                    .getInstance("MD5")
                    .apply {
                        data.self!!
                            .userType
                            .toByteArray()
                            .let(this::update)
                        data.self.urlToken
                            ?.toByteArray()
                            ?.let(this::update)
                    }.digest(data.self!!.id.toByteArray())
                    .toHexString()
                AccountData
                    .httpClient(context)
                    .post("https://redenmc.com/api/zhihu/usage?client_hash=$hash&usage=$usage") {
                        contentType(ContentType.Application.Json)
                        header(
                            HttpHeaders.UserAgent,
                            "Zhihu++/$versionName",
                        )
                    }
            }
        }
    }
}

/**
 * 洛天依主题浏览器打开
 */
fun luoTianYiUrlLauncher(context: Context, uri: Uri) {
    if (uri.host == "link.zhihu.com") {
        Url(uri.toString()).parameters["target"]?.let {
            luoTianYiUrlLauncher(context, it.toUri())
            return
        }
    }
    val color = androidSettingsStore(context).getInt("luotianyi_color", 0xff_66CCFF.toInt())
    val intent = CustomTabsIntent
        .Builder()
        .setDefaultColorSchemeParams(
            CustomTabColorSchemeParams
                .Builder()
                .setToolbarColor(color)
                .build(),
        ).build()
    intent.launchUrl(context, uri)
}

val Context.clipboardManager
    get() = getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager

object OpenInBrowser {
    suspend fun openUrlInBrowser(context: Context, destination: NavDestination): Boolean {
        val urlToken = AccountData.data.self?.urlToken ?: return false
        val jojo = AccountData.fetchGet(context, "https://www.zhihu.com/api/v4/people/$urlToken/collections?limit=50") { signFetchRequest() }!!
        val collection = ZhihuJson
            .decodeJson<List<Collection>>(jojo["data"]!!)
            .firstOrNull { it.description == "com.github.zly2006.zhplus.openinbrowser" }
            ?: ZhihuJson.decodeJson<Collection>(
                AccountData.fetchPost(context, "https://www.zhihu.com/api/v4/collections") {
                    signFetchRequest()
                    contentType(ContentType.Application.Json)
                    setBody(
                        buildJsonObject {
                            put("title", "Zhihu++: 要在浏览器中打开的内容")
                            put("description", "com.github.zly2006.zhplus.openinbrowser")
                            put("is_public", false)
                        },
                    )
                }!!["collection"]!!,
            )
        if (destination is Article) {
            val contentType = when (destination.type) {
                ArticleType.Answer -> "answer"
                ArticleType.Article -> "article"
            }
            val url = "https://api.zhihu.com/collections/contents/$contentType/${destination.id}"
            val body = "add_collections=${collection.id}"
            return AccountData
                .httpClient(context)
                .put(url) {
                    signFetchRequest()
                    contentType(ContentType.Application.FormUrlEncoded)
                    setBody(body)
                }.status
                .isSuccess()
        }
        return false
    }
}

val blacklist = listOf(
    "com.baidu.BaiduMap",
    "com.hihonor.",
    "com.madness.collision",
)

fun Modifier.fuckHonorService(): Modifier = this.filterTextContextMenuComponents {
    // 过滤傻逼荣耀手机的 AI 帮写 选项
    if (it !is TextContextMenuItem) {
        return@filterTextContextMenuComponents true
    }
    try {
        val fields = it.onClick.javaClass.declaredFields
        val resolveInfo = fields.firstOrNull { field -> field.type === ResolveInfo::class.java }
        resolveInfo ?: return@filterTextContextMenuComponents true
        resolveInfo.isAccessible = true
        val info = resolveInfo.get(it.onClick) as ResolveInfo
        val packageName = info.activityInfo.packageName
        if (blacklist.any { it in packageName }) {
            // 黑名单过滤
            return@filterTextContextMenuComponents false
        }
        return@filterTextContextMenuComponents true
    } catch (_: Exception) {
        return@filterTextContextMenuComponents true
    }
}

enum class PowerSaveModeCompat {
    NORMAL,
    POWER_SAVE,
    HUAWEI_POWER_SAVE,
    ;

    val isPowerSaveMode: Boolean
        get() = this != NORMAL

    companion object {
        fun getPowerSaveMode(context: Context): PowerSaveModeCompat {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && context.getSystemService(PowerManager::class.java).isPowerSaveMode) {
                return POWER_SAVE
            }
            if (Settings.System.getInt(context.contentResolver, "SmartModeStatus", 0) == 4) {
                return HUAWEI_POWER_SAVE
            }
            return NORMAL
        }
    }
}

@Suppress("NOTHING_TO_INLINE")
inline fun ComponentActivity.enableEdgeToEdgeCompat() {
    enableEdgeToEdge()
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        // Fix for three-button nav not properly going edge-to-edge.
        // TODO: https://issuetracker.google.com/issues/298296168
        window.isNavigationBarContrastEnforced = false
    }
}
