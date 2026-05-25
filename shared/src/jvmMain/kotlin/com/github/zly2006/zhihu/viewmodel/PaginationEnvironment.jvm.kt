package com.github.zly2006.zhihu.viewmodel

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.github.zly2006.zhihu.navigation.NavDestination
import com.github.zly2006.zhihu.shared.data.Feed
import com.github.zly2006.zhihu.shared.data.FeedDisplayItem
import com.github.zly2006.zhihu.shared.data.target
import com.github.zly2006.zhihu.shared.desktop.DesktopAccountStore
import com.github.zly2006.zhihu.shared.desktop.DesktopHistoryStorage
import com.github.zly2006.zhihu.shared.platform.SettingsStore
import com.github.zly2006.zhihu.shared.util.signZhihuFetchRequest
import com.github.zly2006.zhihu.viewmodel.filter.ContentDetailProvider
import com.github.zly2006.zhihu.viewmodel.filter.ContentType
import com.github.zly2006.zhihu.viewmodel.filter.KeywordSemanticMatcher
import com.github.zly2006.zhihu.viewmodel.filter.applyContentFilterToDisplayItems
import com.github.zly2006.zhihu.viewmodel.filter.applyForegroundReadFilterToDisplayItems
import com.github.zly2006.zhihu.viewmodel.filter.getContentFilterDatabase
import com.github.zly2006.zhihu.viewmodel.filter.recordContentInteraction
import com.github.zly2006.zhihu.viewmodel.filter.toFeedFilterSettings
import com.github.zly2006.zhihu.viewmodel.local.CrawlingExecutor
import com.github.zly2006.zhihu.viewmodel.local.FeedGenerator
import com.github.zly2006.zhihu.viewmodel.local.LocalContentInitializer
import com.github.zly2006.zhihu.viewmodel.local.LocalRecommendationEngine
import com.github.zly2006.zhihu.viewmodel.local.TaskScheduler
import com.github.zly2006.zhihu.viewmodel.local.UserBehaviorAnalyzer
import com.github.zly2006.zhihu.viewmodel.local.getLocalContentDatabase
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonArray
import java.io.File
import java.util.Properties

class DesktopPaginationEnvironment(
    private val store: DesktopAccountStore = DesktopAccountStore(),
) : PaginationEnvironment {
    private val settingsStore = desktopSettingsStore()
    private val historyStorage = DesktopHistoryStorage()
    private val contentFilterDatabase = getContentFilterDatabase(
        File(System.getProperty("user.home"), ".zhihu-plus/content-filter.db"),
    )
    private val localRecommendationEngine by lazy { createLocalRecommendationEngine() }

    override fun httpClient(): HttpClient = store.createHttpClient(store.load().cookies)

    override suspend fun fetchJson(
        url: String,
        include: String,
    ): JsonObject {
        val account = store.load()
        return httpClient().use { client ->
            client
                .get(url) {
                    if (include.isNotEmpty()) {
                        parameter("include", include)
                    }
                    account.cookies["d_c0"]?.let { dc0 ->
                        signZhihuFetchRequest(dc0 = dc0)
                    }
                }.body()
        }
    }

    override fun logDecodeFailure(
        tag: String?,
        item: JsonElement,
        error: Exception,
    ) {
        println("${tag ?: "PaginationViewModel"} failed to decode item: $error")
    }

    override suspend fun handleFetchFailure(
        tag: String?,
        error: Exception,
    ) {
        println("${tag ?: "PaginationViewModel"} failed to fetch feeds: ${error.message}")
    }

    override fun feedDisplaySettings(): FeedDisplaySettings = FeedDisplaySettings(
        enableQualityFilter = false,
        reverseBlock = settingsStore.toFeedFilterSettings().reverseBlock,
    )

    override fun localHistory(): List<NavDestination> =
        historyStorage.history

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
            contentDetailProvider = ContentDetailProvider { null },
            semanticMatcher = desktopKeywordSemanticMatcher,
        )
        return HomeFeedFilterResult(
            foregroundItems = foregroundItems,
            filteredItems = filteredItems,
            reverseBlock = settings.reverseBlock,
        )
    }

    override suspend fun recordContentInteraction(feed: Feed) {
        val settings = settingsStore.toFeedFilterSettings()
        when (val target = feed.target) {
            is Feed.AnswerTarget -> recordContentInteraction(
                settings,
                contentFilterDatabase,
                ContentType.ANSWER,
                target.id.toString(),
            )
            is Feed.ArticleTarget -> recordContentInteraction(
                settings,
                contentFilterDatabase,
                ContentType.ARTICLE,
                target.id.toString(),
            )
            is Feed.QuestionTarget -> recordContentInteraction(
                settings,
                contentFilterDatabase,
                ContentType.QUESTION,
                target.id.toString(),
            )
            is Feed.PinTarget -> recordContentInteraction(
                settings,
                contentFilterDatabase,
                ContentType.PIN,
                target.id.toString(),
            )
            else -> Unit
        }
    }

    override suspend fun clearAllHistory() {
        historyStorage.clearAndSave()
    }

    override fun localRecommendationEngine(): LocalRecommendationEngine = localRecommendationEngine

    private fun createLocalRecommendationEngine(): LocalRecommendationEngine {
        val databaseFile = File(System.getProperty("user.home"), ".zhihu-plus/local-content.db")
        databaseFile.parentFile?.mkdirs()
        val dao = getLocalContentDatabase(databaseFile).contentDao()
        val crawlingExecutor = CrawlingExecutor(
            dao = dao,
            fetchFeedArray = { url -> fetchDesktopLocalFeedArray(url) },
        )
        val taskScheduler = TaskScheduler(
            dao = dao,
            executeTask = { task -> crawlingExecutor.executeTask(task) },
        )
        val contentInitializer = LocalContentInitializer(dao)
        return LocalRecommendationEngine(
            dao = dao,
            feedGenerator = FeedGenerator(dao),
            userBehaviorAnalyzer = UserBehaviorAnalyzer(dao),
            initializeContentIfNeeded = { contentInitializer.initializeIfNeeded() },
            startScheduling = { taskScheduler.startScheduling() },
            stopScheduling = { taskScheduler.stopScheduling() },
            executeTask = { task -> crawlingExecutor.executeTask(task) },
            logWarning = { message -> println("LocalRecommendationEngine warning: $message") },
            logError = { message, throwable -> println("LocalRecommendationEngine error: $message: $throwable") },
        )
    }

    private suspend fun fetchDesktopLocalFeedArray(url: String): JsonArray {
        val account = store.load()
        return store.createHttpClient(account.cookies).use { client ->
            client
                .get(url) {
                    account.cookies["d_c0"]?.let { dc0 ->
                        signZhihuFetchRequest(dc0 = dc0)
                    }
                }.body<JsonObject>()["data"]
                ?.jsonArray ?: JsonArray(emptyList())
        }
    }
}

@Composable
actual fun rememberPaginationEnvironment(allowGuestAccess: Boolean): PaginationEnvironment =
    remember(allowGuestAccess) { DesktopPaginationEnvironment() }

private val desktopKeywordSemanticMatcher = KeywordSemanticMatcher { text, blockedPhrases, threshold ->
    val normalizedText = text.lowercase()
    val textTokens = extractDesktopSemanticTokens(normalizedText).toSet()
    blockedPhrases.mapNotNull { phrase ->
        val normalizedPhrase = phrase.lowercase().trim()
        val similarity = when {
            normalizedPhrase.isBlank() -> 0.0
            normalizedText.contains(normalizedPhrase) -> 1.0
            else -> {
                val phraseTokens = extractDesktopSemanticTokens(normalizedPhrase)
                if (phraseTokens.isEmpty()) {
                    0.0
                } else {
                    phraseTokens.count { it in textTokens }.toDouble() / phraseTokens.size.toDouble()
                }
            }
        }
        if (similarity >= threshold) phrase to similarity else null
    }
}

private fun extractDesktopSemanticTokens(text: String): List<String> =
    Regex("[\\p{L}\\p{N}_\\u4e00-\\u9fff]{2,}")
        .findAll(text)
        .map { it.value.trim() }
        .filter { it.length >= 2 }
        .toList()

private fun desktopSettingsStore(): SettingsStore {
    val settingsFile = File(System.getProperty("user.home"), ".zhihu-plus/settings.properties")
    val properties = Properties()

    fun load() {
        if (settingsFile.isFile) {
            settingsFile.inputStream().use(properties::load)
        }
    }

    fun save() {
        settingsFile.parentFile?.mkdirs()
        settingsFile.outputStream().use { output ->
            properties.store(output, "Zhihu++ desktop settings")
        }
    }

    load()

    return SettingsStore(
        getBoolean = { key, defaultValue ->
            properties.getProperty(key)?.toBooleanStrictOrNull() ?: defaultValue
        },
        putBoolean = { key, value ->
            properties.setProperty(key, value.toString())
            save()
        },
        getString = { key, defaultValue ->
            properties.getProperty(key) ?: defaultValue
        },
        putString = { key, value ->
            properties.setProperty(key, value)
            save()
        },
        getStringOrNull = { key ->
            properties.getProperty(key)
        },
        putStringSet = { key, value ->
            properties.setProperty(key, value.joinToString("\u001F"))
            save()
        },
        getStringSet = { key, defaultValue ->
            properties
                .getProperty(key)
                ?.split("\u001F")
                ?.filter { it.isNotEmpty() }
                ?.toSet() ?: defaultValue
        },
        getInt = { key, defaultValue ->
            properties.getProperty(key)?.toIntOrNull() ?: defaultValue
        },
        putInt = { key, value ->
            properties.setProperty(key, value.toString())
            save()
        },
        getFloat = { key, defaultValue ->
            properties.getProperty(key)?.toFloatOrNull() ?: defaultValue
        },
        putFloat = { key, value ->
            properties.setProperty(key, value.toString())
            save()
        },
        remove = { key ->
            properties.remove(key)
            save()
        },
    )
}
