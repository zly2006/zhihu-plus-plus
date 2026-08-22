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

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.github.zly2006.zhihu.account.NativeAccountStore
import com.github.zly2006.zhihu.account.NativeHistoryStorage
import com.github.zly2006.zhihu.data.Feed
import com.github.zly2006.zhihu.data.FeedDisplayItem
import com.github.zly2006.zhihu.data.target
import com.github.zly2006.zhihu.filter.ContentOpenEventSupport
import com.github.zly2006.zhihu.filter.ContentOpenFrom
import com.github.zly2006.zhihu.filter.TrackedContentIdentity
import com.github.zly2006.zhihu.navigation.Article
import com.github.zly2006.zhihu.navigation.NavDestination
import com.github.zly2006.zhihu.notification.NotificationSettingsStore
import com.github.zly2006.zhihu.notification.nativeNotificationSettingsStore
import com.github.zly2006.zhihu.platform.copyNativePlainText
import com.github.zly2006.zhihu.platform.nativeSettingsStore
import com.github.zly2006.zhihu.platform.requestNativeQrLogin
import com.github.zly2006.zhihu.ui.ArticleAnswerSwitchState
import com.github.zly2006.zhihu.util.Log
import com.github.zly2006.zhihu.viewmodel.filter.BlockedKeywordService
import com.github.zly2006.zhihu.viewmodel.filter.BlockedQuestionAuthor
import com.github.zly2006.zhihu.viewmodel.filter.BlockedUser
import com.github.zly2006.zhihu.viewmodel.filter.ContentDetailProvider
import com.github.zly2006.zhihu.viewmodel.filter.ContentFilterManager
import com.github.zly2006.zhihu.viewmodel.filter.ContentType
import com.github.zly2006.zhihu.viewmodel.filter.FeedContentFilterPipeline
import com.github.zly2006.zhihu.viewmodel.filter.FeedDisplayFilterPipeline
import com.github.zly2006.zhihu.viewmodel.filter.ForegroundReadFilterPipeline
import com.github.zly2006.zhihu.viewmodel.filter.getContentFilterDatabase
import com.github.zly2006.zhihu.viewmodel.filter.toFeedFilterSettings
import com.github.zly2006.zhihu.viewmodel.local.LocalRecommendationEngine
import com.github.zly2006.zhihu.viewmodel.local.buildLocalRecommendationEngine
import com.github.zly2006.zhihu.viewmodel.local.getNativeLocalContentDatabase
import io.ktor.client.HttpClient
import io.ktor.client.request.setBody
import io.ktor.http.contentType
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.put
import io.ktor.http.ContentType as KtorContentType

internal val nativeArticleAnswerSwitchState = ArticleAnswerSwitchData()
private var nativePendingContentOpenIdentity: TrackedContentIdentity? = null
private var nativePendingContentOpenFrom: String? = null

internal fun prepareNativePendingContentOpen(
    target: NavDestination,
    currentMainTabOpenFrom: String?,
    source: NavDestination?,
) {
    val identity = ContentOpenEventSupport.toTrackedContentIdentity(target)
    if (identity == null) {
        nativePendingContentOpenIdentity = null
        nativePendingContentOpenFrom = null
        return
    }
    nativePendingContentOpenIdentity = identity
    nativePendingContentOpenFrom = currentMainTabOpenFrom
        ?: ContentOpenEventSupport.inferOpenFrom(source, target)
}

private fun consumeNativePendingContentOpenFrom(destination: NavDestination): String {
    val identity = ContentOpenEventSupport.toTrackedContentIdentity(destination) ?: return ContentOpenFrom.UNKNOWN
    if (identity != nativePendingContentOpenIdentity) return ContentOpenFrom.UNKNOWN
    val openFrom = nativePendingContentOpenFrom ?: ContentOpenFrom.UNKNOWN
    nativePendingContentOpenIdentity = null
    nativePendingContentOpenFrom = null
    return openFrom
}

@Composable
actual fun rememberPaginationEnvironment(allowGuestAccess: Boolean): PaginationEnvironment =
    remember(allowGuestAccess) { NativePaginationEnvironment() }

internal class NativePaginationEnvironment(
    private val accountStore: NativeAccountStore = NativeAccountStore(),
    override val notificationSettingsStore: NotificationSettingsStore = nativeNotificationSettingsStore(),
    private val showFetchFailureMessage: ((String) -> Unit)? = null,
) : PaginationEnvironment,
    CollectionContentEnvironment,
    NotificationEnvironment {
    private val settingsStore = nativeSettingsStore("settings.properties")
    private val historyStorage = NativeHistoryStorage()
    private val contentFilterDatabase = getContentFilterDatabase()
    private val localRecommendationEngine by lazy {
        getNativeLocalContentDatabase()?.contentDao()?.let { dao ->
            buildLocalRecommendationEngine(
                dao = dao,
                fetchFeedArray = { url -> fetchJson(url, "")?.get("data")?.jsonArray ?: JsonArray(emptyList()) },
                logWarning = { message -> Log.w("LocalRecommendationEngine", message) },
                logError = { message, throwable -> Log.e("LocalRecommendationEngine", message, throwable) },
            )
        }
    }

    override fun httpClient(): HttpClient = accountStore.httpClient()

    override fun authenticatedCookies(): Map<String, String> = accountStore.load().cookies

    override suspend fun <T> withAuthenticatedClient(
        block: suspend (client: HttpClient, cookies: Map<String, String>) -> T,
    ): T = accountStore.withAuthenticatedClient(block)

    override fun xsrfToken(): String = accountStore.load().cookies["_xsrf"].orEmpty()

    override suspend fun refreshAccountProfile() {
        accountStore.refreshAndSaveProfile()
    }

    override fun requestLogin(): Boolean {
        requestNativeQrLogin()
        return true
    }

    override fun clearAccountSession() = accountStore.clear()

    override fun currentAccountId(): String = accountStore
        .load()
        .profile
        ?.id
        .orEmpty()

    override suspend fun verifyLogin(cookies: Map<String, String>): Boolean =
        accountStore.verifyAndSave(cookies.toMutableMap())

    override fun saveCookies(cookies: Map<String, String>) {
        accountStore.save(
            accountStore.load().copy(
                login = true,
                cookies = cookies.toMutableMap(),
            ),
        )
    }

    override fun logout() = clearAccountSession()

    override fun feedDisplaySettings(): FeedDisplaySettings {
        val filterSettings = settingsStore.toFeedFilterSettings()
        return FeedDisplaySettings(
            qualityFilterMode = QualityFilterMode.OFF,
            reverseBlock = filterSettings.reverseBlock,
            loadFullContentForRecommendationFiltering = filterSettings.loadFullContentForRecommendationFiltering,
        )
    }

    override fun localHistory(): List<NavDestination> = historyStorage.history

    override suspend fun postHistoryDestination(destination: NavDestination) = historyStorage.add(destination)

    override fun articleAnswerSwitchState(): ArticleAnswerSwitchState = nativeArticleAnswerSwitchState

    override fun setPlainTextClipboard(label: String, text: String) = copyNativePlainText(text)

    override suspend fun isUserBlocked(userId: String): Boolean =
        contentFilterDatabase.blockedUserDao().isUserBlocked(userId)

    override suspend fun isQuestionAuthorBlocked(userId: String): Boolean =
        contentFilterDatabase.blockedQuestionAuthorDao().isUserBlocked(userId)

    override fun blockedUserIds(): Set<String> = runBlocking {
        contentFilterDatabase
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
        contentFilterDatabase.blockedUserDao().insertUser(
            BlockedUser(
                userId = userId,
                userName = userName,
                urlToken = urlToken,
                avatarUrl = avatarUrl,
            ),
        )
    }

    override suspend fun addBlockedQuestionAuthor(
        userId: String,
        userName: String,
        urlToken: String?,
        avatarUrl: String?,
    ) {
        contentFilterDatabase.blockedQuestionAuthorDao().insertUser(
            BlockedQuestionAuthor(
                userId = userId,
                userName = userName,
                urlToken = urlToken,
                avatarUrl = avatarUrl,
            ),
        )
    }

    override suspend fun removeBlockedUser(userId: String) {
        contentFilterDatabase.blockedUserDao().deleteUserById(userId)
    }

    override suspend fun removeBlockedQuestionAuthor(userId: String) {
        contentFilterDatabase.blockedQuestionAuthorDao().deleteUserById(userId)
    }

    override suspend fun recordContentOpenEvent(
        destination: NavDestination,
        questionId: Long?,
        openFrom: String,
    ) {
        val resolvedOpenFrom = openFrom.ifBlank { consumeNativePendingContentOpenFrom(destination) }
        ContentOpenEventSupport.recordOpenEvent(
            database = contentFilterDatabase,
            destination = destination,
            questionId = questionId,
            openFrom = resolvedOpenFrom.ifBlank { ContentOpenFrom.UNKNOWN },
        )
    }

    override suspend fun recordOpenEvent(destination: Article, questionId: Long?) =
        recordContentOpenEvent(destination, questionId)

    override suspend fun applyHomeFeedFilters(
        items: List<FeedDisplayItem>,
        loadFullContent: Boolean,
        applyForegroundFilter: Boolean,
    ): HomeFeedFilterResult {
        val settings = settingsStore.toFeedFilterSettings()
        val foregroundItems = if (applyForegroundFilter) {
            ForegroundReadFilterPipeline(
                settings = settings,
                contentFilterManager = ContentFilterManager(contentFilterDatabase.contentFilterDao()),
                blockedFeedRecordDao = contentFilterDatabase.blockedFeedRecordDao(),
            ).filter(items)
        } else {
            items
        }
        val filteredItems = FeedDisplayFilterPipeline(
            settings = settings,
            contentDetailProvider = ContentDetailProvider(::getOrFetchContentDetail),
            contentFilterPipeline = FeedContentFilterPipeline(
                settings = settings,
                blockedKeywordDao = contentFilterDatabase.blockedKeywordDao(),
                blockedUserDao = contentFilterDatabase.blockedUserDao(),
                blockedQuestionAuthorDao = contentFilterDatabase.blockedQuestionAuthorDao(),
                blockedTopicDao = contentFilterDatabase.blockedTopicDao(),
                blockedKeywordService = BlockedKeywordService(
                    keywordDao = contentFilterDatabase.blockedKeywordDao(),
                    recordDao = contentFilterDatabase.blockedContentRecordDao(),
                    semanticMatcher = null,
                ),
            ),
            blockedFeedRecordDao = contentFilterDatabase.blockedFeedRecordDao(),
        ).filter(
            foregroundItems,
            loadFullContent = loadFullContent,
        )
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
        ContentFilterManager(contentFilterDatabase.contentFilterDao()).recordContentInteraction(targetType, targetId)
    }

    override suspend fun clearAllHistory() {
        historyStorage.clearAndSave()
        if (accountStore.load().cookies["d_c0"] == null) return
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

    override fun localRecommendationEngine(): LocalRecommendationEngine? = localRecommendationEngine

    override suspend fun exportCollectionItemsToHtmlZip(
        collectionTitle: String,
        items: List<CollectionItem>,
        includeImages: Boolean,
        onProgress: suspend (CollectionHtmlExportProgress) -> Unit,
    ): CollectionHtmlExportResult = error("当前平台暂不支持收藏夹 HTML 压缩包导出")

    override suspend fun handleCollectionExportFailure(error: Exception) {
        Log.e("CollectionContentViewModel", "Failed to export collection HTML zip", error)
    }

    override fun logDecodeFailure(tag: String?, item: JsonElement, error: Exception) {
        Log.e(tag ?: "PaginationViewModel", "Failed to decode item: $item", error)
    }

    override suspend fun handleFetchFailure(tag: String?, error: Exception) {
        Log.e(tag ?: "PaginationViewModel", "Failed to fetch feeds", error)
        showFetchFailureMessage?.invoke("加载失败: ${error.message}")
    }
}
