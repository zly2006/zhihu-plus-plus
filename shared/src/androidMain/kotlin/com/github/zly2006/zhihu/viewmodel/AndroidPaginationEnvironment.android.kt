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
import android.app.AlertDialog
import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import com.github.zly2006.zhihu.data.AccountData
import com.github.zly2006.zhihu.data.ContentDetailCache
import com.github.zly2006.zhihu.data.HistoryStorage
import com.github.zly2006.zhihu.data.getOrFetch
import com.github.zly2006.zhihu.navigation.AndroidAnswerNavigatorRepository
import com.github.zly2006.zhihu.navigation.AnswerNavigatorRepository
import com.github.zly2006.zhihu.navigation.NavDestination
import com.github.zly2006.zhihu.shared.data.Collection
import com.github.zly2006.zhihu.shared.data.CollectionItem
import com.github.zly2006.zhihu.shared.data.DataHolder
import com.github.zly2006.zhihu.shared.data.Feed
import com.github.zly2006.zhihu.shared.data.FeedDisplayItem
import com.github.zly2006.zhihu.shared.data.ZHIHU_CLEAR_ONLINE_HISTORY_URL
import com.github.zly2006.zhihu.shared.data.ZHIHU_LAST_READ_TOUCH_URL
import com.github.zly2006.zhihu.shared.data.ZhihuJson
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
import com.github.zly2006.zhihu.util.signFetchRequest
import com.github.zly2006.zhihu.viewmodel.filter.AndroidContentFilterRuntime
import com.github.zly2006.zhihu.viewmodel.filter.ContentDetailProvider
import com.github.zly2006.zhihu.viewmodel.filter.ContentFilterExtensions
import com.github.zly2006.zhihu.viewmodel.filter.contentFilterSettings
import com.github.zly2006.zhihu.viewmodel.filter.createBlocklistManager
import com.github.zly2006.zhihu.viewmodel.filter.getContentFilterDatabase
import com.github.zly2006.zhihu.viewmodel.filter.recordFeedContentInteraction
import com.github.zly2006.zhihu.viewmodel.local.LocalRecommendationEngine
import io.ktor.client.HttpClient
import io.ktor.client.plugins.UserAgent
import io.ktor.client.plugins.api.createClientPlugin
import io.ktor.client.plugins.cache.HttpCache
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.cookies.HttpCookies
import io.ktor.client.request.HttpRequestBuilder
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
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import com.github.zly2006.zhihu.navigation.Article as ArticleDestination
import io.ktor.http.ContentType as KtorContentType
import android.Manifest
import android.content.ContentValues
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.os.Environment
import android.provider.MediaStore
import androidx.annotation.RequiresApi
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

interface AndroidContextPaginationEnvironment : PaginationEnvironment {
    val context: Context
}

private val ZHIHU_PP_ANDROID_HEADERS = createClientPlugin("ZhihuPPAndroidHeaders", { }) {
    onRequest { request, _ ->
        request.headers.appendAll(AccountData.ANDROID_HEADERS)
    }
}

open class SharedAndroidPaginationEnvironment(
    override val context: Context,
    private val allowGuestAccess: Boolean,
) : AndroidContextPaginationEnvironment,
    CollectionContentEnvironment {
    private val localRecommendationEngine by lazy { LocalRecommendationEngine(context) }
    private val settingsStore by lazy { androidSettingsStore(context) }
    private val userMessageSink by lazy { androidUserMessageSink(context) }

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

    override suspend fun fetchJson(
        url: String,
        include: String,
    ): JsonObject? =
        AccountData.fetchGet(context, url) {
            addIncludeAndSign(include)
        }

    override fun logDecodeFailure(
        tag: String?,
        item: JsonElement,
        error: Exception,
    ) {
        Log.e(tag, "Failed to decode item: $item", error)
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
        context.mainExecutor.execute {
            userMessageSink.showShortMessage("加载失败: ${error.message}")
        }
    }

    override suspend fun handleMobileHomeFeedFailure(error: Exception) {
        Log.e("AndroidHomeFeedViewModel", "Failed to fetch feeds", error)
        context.mainExecutor.execute {
            userMessageSink.showShortMessage("安卓端推荐加载失败: ${error.message}")
        }
    }

    override fun configureSignedRequest(builder: HttpRequestBuilder) {
        builder.signFetchRequest()
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
        val foregroundItems = ContentFilterExtensions.applyForegroundReadFilterToDisplayItems(
            settings = filterSettings,
            database = filterDatabase,
            items = items,
        )
        val filteredItems = ContentFilterExtensions.applyContentFilterToDisplayItems(
            settings = filterSettings,
            database = filterDatabase,
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

    override suspend fun fetchCollection(collectionId: String): Collection {
        val json = fetchJson("https://www.zhihu.com/api/v4/collections/$collectionId", "")
            ?: throw IllegalStateException("收藏夹信息加载失败")
        return ZhihuJson.decodeJson<Collection>(json["collection"] ?: throw IllegalStateException("收藏夹信息为空"))
    }

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

    override fun hasImageExportPermission(): Boolean = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        ContextCompat.checkSelfPermission(context, Manifest.permission.READ_MEDIA_IMAGES) == PackageManager.PERMISSION_GRANTED
    } else {
        ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
    }

    override fun requiresHtmlExportPermission(): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.Q

    override fun requestImageExportPermission() {
        val permissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            arrayOf(Manifest.permission.READ_MEDIA_IMAGES)
        } else {
            arrayOf(
                Manifest.permission.WRITE_EXTERNAL_STORAGE,
                Manifest.permission.READ_EXTERNAL_STORAGE,
            )
        }
        ActivityCompat.requestPermissions(context as Activity, permissions, 1001)
    }

    override fun loadExportAssetText(fileName: String): String =
        context.assets.open(fileName).use { inputStream ->
            inputStream.bufferedReader().use { reader ->
                reader.readText()
            }
        }


}

class SharedAndroidNotificationPaginationEnvironment(
    context: Context,
    allowGuestAccess: Boolean,
    override val notificationSettingsStore: NotificationSettingsStore,
) : SharedAndroidPaginationEnvironment(context, allowGuestAccess),
    NotificationPaginationEnvironment

fun PaginationViewModel<*>.paginationEnvironment(context: Context): AndroidContextPaginationEnvironment =
    SharedAndroidPaginationEnvironment(context, allowGuestAccess)

@Composable
actual fun rememberPaginationEnvironment(allowGuestAccess: Boolean): PaginationEnvironment {
    val context = LocalContext.current
    return remember(context, allowGuestAccess) {
        SharedAndroidPaginationEnvironment(context, allowGuestAccess)
    }
}

fun PaginationViewModel<*>.notificationPaginationEnvironment(
    context: Context,
    notificationSettingsStore: NotificationSettingsStore,
): NotificationPaginationEnvironment =
    SharedAndroidNotificationPaginationEnvironment(context, allowGuestAccess, notificationSettingsStore)

fun PaginationEnvironment.androidContext(): Context =
    (this as? AndroidContextPaginationEnvironment)?.context
        ?: error("Android Context is required for this pagination path")

fun PaginationViewModel<*>.refresh(context: Context) {
    refresh(paginationEnvironment(context))
}

fun PaginationViewModel<*>.loadMore(context: Context) {
    loadMore(paginationEnvironment(context))
}

fun PaginationViewModel<*>.httpClient(context: Context): HttpClient =
    httpClient(paginationEnvironment(context))

private fun HttpRequestBuilder.addIncludeAndSign(include: String) {
    url {
        if (include.isNotEmpty()) {
            parameters["include"] = include
        }
    }
    signFetchRequest()
}

private fun Context.canSafelyShowDialog(): Boolean {
    val activity = this as? Activity ?: return false
    if (activity.isFinishing || activity.isDestroyed) return false
    val lifecycleOwner = activity as? LifecycleOwner ?: return true
    return lifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)
}
