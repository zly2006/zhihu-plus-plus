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

import android.Manifest
import android.app.Activity
import android.app.AlertDialog
import android.content.ClipData
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import com.github.zly2006.zhihu.data.AccountData
import com.github.zly2006.zhihu.data.ContentDetailCache
import com.github.zly2006.zhihu.data.HistoryStorage
import com.github.zly2006.zhihu.data.getContentDetail
import com.github.zly2006.zhihu.data.getOrFetch
import com.github.zly2006.zhihu.navigation.AndroidAnswerNavigatorRepository
import com.github.zly2006.zhihu.navigation.AnswerNavigatorRepository
import com.github.zly2006.zhihu.navigation.NavDestination
import com.github.zly2006.zhihu.shared.aigc.AIGC_MARKING_ENABLED_PREFERENCE_KEY
import com.github.zly2006.zhihu.shared.aigc.AigcVoteClient
import com.github.zly2006.zhihu.shared.aigc.AigcVoteVoter
import com.github.zly2006.zhihu.shared.data.DataHolder
import com.github.zly2006.zhihu.shared.data.Feed
import com.github.zly2006.zhihu.shared.data.FeedDisplayItem
import com.github.zly2006.zhihu.shared.data.ZHIHU_CLEAR_ONLINE_HISTORY_URL
import com.github.zly2006.zhihu.shared.data.ZHIHU_LAST_READ_TOUCH_URL
import com.github.zly2006.zhihu.shared.data.ZhihuJson.json
import com.github.zly2006.zhihu.shared.data.buildZhihuClearOnlineHistoryBody
import com.github.zly2006.zhihu.shared.data.encodeZhihuLastReadTouchItems
import com.github.zly2006.zhihu.shared.data.navDestination
import com.github.zly2006.zhihu.shared.data.zhihuLastReadTouchItem
import com.github.zly2006.zhihu.shared.data.zhihuLastReadTouchItems
import com.github.zly2006.zhihu.shared.filter.ContentOpenEventSupport
import com.github.zly2006.zhihu.shared.notification.NotificationSettingsStore
import com.github.zly2006.zhihu.shared.platform.androidSettingsStore
import com.github.zly2006.zhihu.shared.platform.androidUserMessageSink
import com.github.zly2006.zhihu.shared.util.HttpStatusException
import com.github.zly2006.zhihu.ui.articleHost
import com.github.zly2006.zhihu.util.ResolvedCollectionHtmlExportItem
import com.github.zly2006.zhihu.util.buildArticleExportFileName
import com.github.zly2006.zhihu.util.buildOfflineArticleExportHtml
import com.github.zly2006.zhihu.util.clipboardManager
import com.github.zly2006.zhihu.util.exportCollectionItemsToZip
import com.github.zly2006.zhihu.util.saveBitmapToGallery
import com.github.zly2006.zhihu.util.signFetchRequest
import com.github.zly2006.zhihu.viewmodel.CollectionItem
import com.github.zly2006.zhihu.viewmodel.filter.AndroidContentFilterRuntime
import com.github.zly2006.zhihu.viewmodel.filter.ContentDetailProvider
import com.github.zly2006.zhihu.viewmodel.filter.contentFilterSettings
import com.github.zly2006.zhihu.viewmodel.filter.createBlocklistManager
import com.github.zly2006.zhihu.viewmodel.filter.filterFeedDisplayItems
import com.github.zly2006.zhihu.viewmodel.filter.filterForegroundReadItems
import com.github.zly2006.zhihu.viewmodel.filter.getContentFilterDatabase
import com.github.zly2006.zhihu.viewmodel.filter.recordFeedContentInteraction
import com.github.zly2006.zhihu.viewmodel.local.LocalRecommendationEngine
import io.ktor.client.HttpClient
import io.ktor.client.plugins.UserAgent
import io.ktor.client.plugins.api.createClientPlugin
import io.ktor.client.plugins.cache.HttpCache
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.cookies.HttpCookies
import io.ktor.client.request.forms.MultiPartFormDataContent
import io.ktor.client.request.forms.formData
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpMethod
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import io.ktor.serialization.kotlinx.json.json
import io.ktor.util.appendAll
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.io.File
import java.util.UUID
import com.github.zly2006.zhihu.navigation.Article as ArticleDestination
import com.github.zly2006.zhihu.util.buildArticleExportHtml as buildAndroidArticleExportHtml
import io.ktor.http.ContentType as KtorContentType

interface AndroidContextPaginationEnvironment : PaginationEnvironment {
    val context: Context
}

private val ZHIHU_PP_ANDROID_HEADERS = createClientPlugin("ZhihuPPAndroidHeaders", { }) {
    onRequest { request, _ ->
        request.headers.appendAll(AccountData.ANDROID_HEADERS)
    }
}

private const val AIGC_VOTE_CLIENT_ID_KEY = "aigcVoteClientId"
private const val AIGC_VOTE_SERVER_URL_KEY = "aigcVoteServerUrl"
private const val DEFAULT_ANDROID_AIGC_VOTE_SERVER_URL = "https://aigc-vote.ai.fintechedu.cn"

open class SharedAndroidPaginationEnvironment(
    override val context: Context,
    private val allowGuestAccess: Boolean,
) : AndroidContextPaginationEnvironment,
    CollectionContentEnvironment {
    private val localRecommendationEngine by lazy { LocalRecommendationEngine(context) }
    private val settingsStore by lazy { androidSettingsStore(context) }
    private val userMessageSink by lazy { androidUserMessageSink(context) }
    private val aigcVoteHttpClient by lazy {
        HttpClient {
            install(ContentNegotiation) {
                json(json)
            }
        }
    }
    private val aigcVoteClient by lazy {
        AigcVoteClient(
            httpClient = aigcVoteHttpClient,
            baseUrl = aigcVoteServerUrl(),
            clientId = aigcVoteClientId(),
        )
    }
    private var lastAuthRefreshMillis = 0L

    override fun httpClient(): HttpClient {
        val loginForRecommendation = settingsStore.getBoolean("loginForRecommendation", true)
        if (allowGuestAccess && !loginForRecommendation) {
            return HttpClient {
                install(HttpCache)
                install(ContentNegotiation) {
                    json(json)
                }
                install(UserAgent) {
                    agent = AccountData.data.userAgent
                }
            }
        }
        return AccountData.httpClient(context)
    }

    override fun mobileHomeFeedHttpClient(): HttpClient {
        val loginForRecommendation = settingsStore.getBoolean("loginForRecommendation", true)

        return HttpClient {
            install(ContentNegotiation) {
                json(json)
            }
            install(UserAgent) {
                agent = AccountData.ANDROID_USER_AGENT
            }
            install(ZHIHU_PP_ANDROID_HEADERS)
            if (loginForRecommendation) {
                install(HttpCookies) {
                    storage = AccountData.cookieStorage(context, null)
                }
            }
        }
    }

    override fun aigcVoteClient(): AigcVoteClient? =
        if (settingsStore.getBoolean(AIGC_MARKING_ENABLED_PREFERENCE_KEY, false)) {
            aigcVoteClient
        } else {
            null
        }

    override fun aigcVoteVoter(): AigcVoteVoter? =
        AccountData.data.self?.let { self ->
            AigcVoteVoter(
                id = self.id,
                name = self.name,
                urlToken = self.urlToken,
                avatarUrl = self.avatarUrl,
            )
        }

    override fun authenticatedCookies(): Map<String, String> {
        val loginForRecommendation = settingsStore.getBoolean("loginForRecommendation", true)
        return if (allowGuestAccess && !loginForRecommendation) {
            emptyMap()
        } else {
            AccountData.data.cookies
        }
    }

    override fun lastAuthRefreshMillis(): Long = lastAuthRefreshMillis

    override fun updateLastAuthRefreshMillis(value: Long) {
        lastAuthRefreshMillis = value
    }

    private fun aigcVoteServerUrl(): String =
        settingsStore
            .getString(AIGC_VOTE_SERVER_URL_KEY, DEFAULT_ANDROID_AIGC_VOTE_SERVER_URL)
            .ifBlank { DEFAULT_ANDROID_AIGC_VOTE_SERVER_URL }

    private fun aigcVoteClientId(): String {
        settingsStore.getStringOrNull(AIGC_VOTE_CLIENT_ID_KEY)?.takeIf { it.isNotBlank() }?.let {
            return it
        }
        val id = UUID.randomUUID().toString()
        settingsStore.putString(AIGC_VOTE_CLIENT_ID_KEY, id)
        return id
    }

    override suspend fun handleFetchFailure(
        tag: String?,
        error: Exception,
    ) {
        if (error is HttpStatusException) {
            Log.e(tag, "Response: ${error.bodyText}", error)
            if (tryShowLoginExpiredDialog(error)) {
                return
            }
            showDebugErrorDialog(error)
        }
        Log.e(tag, "Failed to fetch feeds", error)
        // 缺少 d_c0 设备标识时 zse96 签名必然非法（知乎回 10003 请升级客户端），给出可操作提示而非看不懂的报错
        val hint = if (AccountData.data.cookies["d_c0"].isNullOrBlank()) {
            "（登录缺少设备标识 d_c0，网页接口无法签名，请重新登录）"
        } else {
            ""
        }
        context.mainExecutor.execute {
            userMessageSink.showShortMessage("加载失败: ${error.message}$hint")
        }
    }

    override suspend fun handleMobileHomeFeedFailure(error: Exception) {
        Log.e("AndroidHomeFeedViewModel", "Failed to fetch feeds", error)
        context.mainExecutor.execute {
            userMessageSink.showShortMessage("安卓端推荐加载失败: ${error.message}")
        }
    }

    override fun feedDisplaySettings(): FeedDisplaySettings = FeedDisplaySettings(
        enableQualityFilter = settingsStore.getBoolean("enableQualityFilter", true),
        reverseBlock = settingsStore.getBoolean("reverseBlock", false),
    )

    override fun localHistory(): List<NavDestination> = HistoryStorage(context).history

    override suspend fun addReadHistory(
        contentToken: String,
        contentTypeName: String,
    ) {
        AccountData.addReadHistory(context, contentToken, contentTypeName)
    }

    override suspend fun postHistoryDestination(destination: NavDestination) {
        HistoryStorage(context).add(destination)
    }

    override suspend fun isUserBlocked(userId: String): Boolean =
        getContentFilterDatabase(context).createBlocklistManager().isUserBlocked(userId)

    override fun blockedUserIds(): Set<String> =
        kotlinx.coroutines.runBlocking {
            getContentFilterDatabase(context)
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
        getContentFilterDatabase(context).createBlocklistManager().addBlockedUser(
            userId = userId,
            userName = userName,
            urlToken = urlToken,
            avatarUrl = avatarUrl,
        )
    }

    override suspend fun removeBlockedUser(userId: String) {
        getContentFilterDatabase(context).createBlocklistManager().removeBlockedUser(userId)
    }

    override suspend fun recordContentOpenEvent(
        destination: NavDestination,
        questionId: Long?,
        openFrom: String,
    ) {
        val resolvedOpenFrom = openFrom.ifBlank {
            context.articleHost()?.consumePendingContentOpenFrom(destination) ?: ""
        }
        ContentOpenEventSupport.recordOpenEvent(
            database = getContentFilterDatabase(context),
            destination = destination,
            questionId = questionId,
            openFrom = resolvedOpenFrom.ifBlank { "unknown" },
        )
    }

    override suspend fun getContentDetail(article: ArticleDestination): DataHolder.Content? =
        DataHolder.getContentDetail(context, article)

    override suspend fun followQuestion(
        questionId: Long,
        follow: Boolean,
    ) {
        val url = "https://www.zhihu.com/api/v4/questions/$questionId/followers"
        AccountData.fetch(context, url) {
            signFetchRequest()
            method = if (follow) HttpMethod.Post else HttpMethod.Delete
        }
    }

    override suspend fun applyHomeFeedFilters(items: List<FeedDisplayItem>): HomeFeedFilterResult {
        val settings = feedDisplaySettings()
        val filterSettings = context.contentFilterSettings()
        val filterDatabase = getContentFilterDatabase(context)
        val foregroundItems = filterDatabase.filterForegroundReadItems(
            settings = filterSettings,
            items = items,
        )
        val filteredItems = filterDatabase.filterFeedDisplayItems(
            settings = filterSettings,
            items = foregroundItems,
            contentDetailProvider = ContentDetailProvider { ContentDetailCache.getOrFetch(context, it) },
            semanticMatcher = AndroidContentFilterRuntime.semanticMatcher,
            onNlpBlocked = { blockedThisRound ->
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    context.mainExecutor.execute {
                        userMessageSink.showShortMessage("NLP 已屏蔽 ${blockedThisRound.first().title.take(10)}... 等 ${blockedThisRound.size} 条内容")
                    }
                }
            },
            onDetailFetchFailed = { item ->
                Log.w("ContentFilterExtensions", "Failed to fetch content details for item '${item.title}'. Using dummy content for filtering.")
            },
            onDetailsKeywordFiltered = { item, keyword ->
                Log.e("ContentFilterExtensions", "Filtered item '${item.title}' due to keyword '$keyword' in details: ${item.content}")
            },
        )
        return HomeFeedFilterResult(
            foregroundItems = foregroundItems,
            filteredItems = filteredItems,
            reverseBlock = settings.reverseBlock,
        )
    }

    override suspend fun sendFeedReadStatus(feed: Feed) {
        val payloadItem = zhihuLastReadTouchItem(feed, "read") ?: return
        AccountData.fetchPost(context, ZHIHU_LAST_READ_TOUCH_URL) {
            signFetchRequest()
            header("x-requested-with", "fetch")
            setBody(
                MultiPartFormDataContent(
                    formData {
                        append("items", encodeZhihuLastReadTouchItems(listOf(payloadItem)))
                    },
                ),
            )
        }
    }

    override suspend fun recordContentInteraction(feed: Feed) {
        val settings = context.contentFilterSettings()
        val database = getContentFilterDatabase(context)
        recordFeedContentInteraction(settings, database, feed)
    }

    override suspend fun markItemsAsTouched(items: Set<Pair<String, String>>): Set<Pair<String, String>> {
        if (items.isEmpty()) return emptySet()
        val response = AccountData.httpClient(context).post(ZHIHU_LAST_READ_TOUCH_URL) {
            header("x-requested-with", "fetch")
            signFetchRequest()
            setBody(
                MultiPartFormDataContent(
                    formData {
                        val payload = zhihuLastReadTouchItems(items, "touch")
                        append("items", encodeZhihuLastReadTouchItems(payload))
                    },
                ),
            )
        }
        return if (response.status.isSuccess()) {
            items
        } else {
            Log.e("Browse-Touch", response.bodyAsText())
            emptySet()
        }
    }

    override suspend fun clearAllHistory() {
        HistoryStorage(context).clearAndSave()
        AccountData.fetchPost(context, ZHIHU_CLEAR_ONLINE_HISTORY_URL) {
            signFetchRequest()
            contentType(KtorContentType.Application.Json)
            setBody(buildZhihuClearOnlineHistoryBody())
        }
    }

    override fun localRecommendationEngine(): LocalRecommendationEngine = localRecommendationEngine

    override suspend fun handleLocalRecommendationFailure(error: Exception) {
        Log.e("LocalHomeFeedViewModel", "Error fetching local feeds", error)
    }

    override suspend fun showLocalRecommendationDatabaseError() {
        withContext(Dispatchers.Main) {
            AlertDialog
                .Builder(context)
                .setTitle("数据库错误")
                .setMessage("本地推荐系统的数据库未正确初始化。请尝试重启应用或清除应用数据。")
                .setPositiveButton("确定") { dialog, _ -> dialog.dismiss() }
                .show()
        }
    }

    override fun answerNavigatorRepository(): AnswerNavigatorRepository = AndroidAnswerNavigatorRepository(context)

    override fun articleAnswerSwitchState() = context.articleHost()?.articleAnswerSwitchState

    override suspend fun exportCollectionItemsToHtmlZip(
        collectionTitle: String,
        items: List<CollectionItem>,
        includeImages: Boolean,
        onProgress: suspend (CollectionHtmlExportProgress) -> Unit,
    ): CollectionHtmlExportResult {
        val outputDir = context.getExternalFilesDir(null)
            ?: throw IllegalStateException("外部文件目录不可用")
        val exportHttpClient = httpClient()
        val result = withContext(Dispatchers.IO) {
            exportCollectionItemsToZip(
                collectionTitle = collectionTitle,
                items = items,
                cacheDir = context.cacheDir,
                outputDir = outputDir,
                displayTitle = { item ->
                    item.content.title.ifBlank { item.content.description() }
                },
                resolveItem = { item ->
                    resolveCollectionItemForHtmlExport(
                        item = item,
                        exportHttpClient = exportHttpClient,
                        includeImages = includeImages,
                    )
                },
                onProgress = { progress ->
                    withContext(Dispatchers.Main) {
                        onProgress(
                            CollectionHtmlExportProgress(
                                totalCount = progress.totalCount,
                                processedCount = progress.processedCount,
                                successCount = progress.successCount,
                                skippedCount = progress.skippedCount,
                                failedCount = progress.failedCount,
                                currentTitle = progress.currentTitle,
                            ),
                        )
                    }
                },
            )
        }
        return CollectionHtmlExportResult(
            totalCount = result.totalCount,
            successCount = result.successCount,
            skippedCount = result.skippedCount,
            failedCount = result.failedCount,
            zipFilePath = result.zipFile?.absolutePath,
        )
    }

    override suspend fun handleCollectionExportFailure(error: Exception) {
        Log.e("CollectionContentViewModel", "Failed to export collection HTML zip", error)
        context.mainExecutor.execute {
            userMessageSink.showShortMessage("导出失败: ${error.message}")
        }
    }

    private suspend fun resolveCollectionItemForHtmlExport(
        item: CollectionItem,
        exportHttpClient: HttpClient,
        includeImages: Boolean,
    ): ResolvedCollectionHtmlExportItem? {
        val navDestination = item.content.navDestination as? ArticleDestination ?: return null
        val content = ContentDetailCache.getOrFetch(context, navDestination)
            ?: throw IllegalStateException("无法加载「${item.content.title}」详情")
        if (content !is DataHolder.Answer && content !is DataHolder.Article) {
            return null
        }

        return ResolvedCollectionHtmlExportItem(
            htmlFileName = buildArticleExportFileName(content, "html"),
            htmlContent = buildOfflineArticleExportHtml(
                context = context,
                content = content,
                includeAppAttribution = true,
                httpClient = exportHttpClient,
                includeImages = includeImages,
            ),
        )
    }

    private fun tryShowLoginExpiredDialog(error: HttpStatusException): Boolean {
        try {
            val body = json.parseToJsonElement(error.bodyText).jsonObject
            val errorBody = body["error"]?.jsonObject ?: return false
            if (errorBody["code"]?.jsonPrimitive?.int == 100 &&
                errorBody["message"]?.jsonPrimitive?.content == "ERR_TICKET_NOT_EXIST"
            ) {
                context.mainExecutor.execute {
                    if (context.canSafelyShowDialog()) {
                        AlertDialog
                            .Builder(context)
                            .setTitle("登录已过期")
                            .setMessage("请重新登录以继续使用完整功能。")
                            .setPositiveButton("重新登录") { _, _ ->
                                AccountData.delete(context)
                                context.startActivity(
                                    Intent().setClassName(
                                        context.packageName,
                                        "com.github.zly2006.zhihu.LoginActivity",
                                    ),
                                )
                            }.setNegativeButton("取消", null)
                            .show()
                    }
                }
                return true
            }
        } catch (_: Exception) {
        }
        return false
    }

    private fun showDebugErrorDialog(error: HttpStatusException) {
        context.mainExecutor.execute {
            if (context.canSafelyShowDialog()) {
                AlertDialog
                    .Builder(context)
                    .setTitle("错误 ${error.status}")
                    .setMessage(error.bodyText)
                    .setNeutralButton("复制curl") { _, _ ->
                        val curl = error.dumpedCurlRequest
                        context.clipboardManager
                            .setPrimaryClip(
                                ClipData.newPlainText(
                                    "curl",
                                    curl,
                                ),
                            )
                        userMessageSink.showShortMessage("已复制到剪贴板")
                    }.show()
            }
        }
    }

    // Export methods
    override fun setPlainTextClipboard(
        label: String,
        text: String,
    ) {
        context.clipboardManager.setPrimaryClip(ClipData.newPlainText(label, text))
    }

    override fun buildArticleExportHtml(
        content: DataHolder.Content,
        includeAppAttribution: Boolean,
        extraSectionsHtml: String,
    ): String = buildAndroidArticleExportHtml(
        context = context,
        content = content,
        includeAppAttribution = includeAppAttribution,
        extraSectionsHtml = extraSectionsHtml,
    )

    override suspend fun buildOfflineArticleExportHtml(
        content: DataHolder.Content,
        includeAppAttribution: Boolean,
        httpClient: HttpClient,
    ): String = buildOfflineArticleExportHtml(
        context = context,
        content = content,
        includeAppAttribution = includeAppAttribution,
        httpClient = httpClient,
    )

    override fun saveImageToMediaStore(
        displayName: String,
        bitmap: Any,
    ) = saveBitmapToGallery(context, displayName, bitmap as android.graphics.Bitmap)

    override fun saveHtmlToDownloads(
        displayName: String,
        htmlContent: String,
    ): String = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        saveHtmlToDownloadsWithMediaStore(displayName, htmlContent)
    } else {
        saveHtmlToLegacyDownloads(displayName, htmlContent)
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    private fun saveHtmlToDownloadsWithMediaStore(
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
    private fun saveHtmlToLegacyDownloads(
        displayName: String,
        htmlContent: String,
    ): String {
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

    override fun articleImageExportRenderer(loadAssetText: (String) -> String): ArticleImageExportRenderer =
        AndroidArticleExportRenderer(context, loadAssetText)

    override fun hasImageExportPermission(): Boolean =
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_EXTERNAL_STORAGE) ==
            PackageManager.PERMISSION_GRANTED

    override fun requiresHtmlExportPermission(): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.Q

    override fun requestImageExportPermission() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            ActivityCompat.requestPermissions(
                context as Activity,
                arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE),
                1001,
            )
        }
    }

    override fun loadExportAssetText(fileName: String): String =
        context.assets.open(fileName).use { inputStream ->
            inputStream.bufferedReader().use { reader ->
                reader.readText()
            }
        }
}

class SharedAndroidNotificationEnvironment(
    context: Context,
    allowGuestAccess: Boolean,
    override val notificationSettingsStore: NotificationSettingsStore,
) : SharedAndroidPaginationEnvironment(context, allowGuestAccess),
    NotificationEnvironment

fun PaginationViewModel<*>.paginationEnvironment(context: Context): AndroidContextPaginationEnvironment =
    SharedAndroidPaginationEnvironment(context, allowGuestAccess)

@Composable
actual fun rememberPaginationEnvironment(allowGuestAccess: Boolean): PaginationEnvironment {
    val context = LocalContext.current
    return remember(context, allowGuestAccess) { SharedAndroidPaginationEnvironment(context, allowGuestAccess) }
}

fun PaginationViewModel<*>.notificationEnvironment(
    context: Context,
    notificationSettingsStore: NotificationSettingsStore,
): NotificationEnvironment =
    SharedAndroidNotificationEnvironment(context, allowGuestAccess, notificationSettingsStore)

fun PaginationViewModel<*>.refresh(context: Context) {
    refresh(paginationEnvironment(context))
}

fun PaginationViewModel<*>.loadMore(context: Context) {
    loadMore(paginationEnvironment(context))
}

fun PaginationViewModel<*>.httpClient(context: Context): HttpClient =
    paginationEnvironment(context).httpClient()

private fun Context.canSafelyShowDialog(): Boolean {
    val activity = this as? Activity ?: return false
    if (activity.isFinishing || activity.isDestroyed) return false
    val lifecycleOwner = activity as? LifecycleOwner ?: return true
    return lifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)
}
