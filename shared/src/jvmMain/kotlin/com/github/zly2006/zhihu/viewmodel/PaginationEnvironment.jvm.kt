package com.github.zly2006.zhihu.viewmodel

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.github.zly2006.zhihu.shared.data.Feed
import com.github.zly2006.zhihu.shared.data.FeedDisplayItem
import com.github.zly2006.zhihu.shared.data.target
import com.github.zly2006.zhihu.shared.desktop.DesktopAccountStore
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
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import java.io.File
import java.util.Properties

class DesktopPaginationEnvironment(
    private val store: DesktopAccountStore = DesktopAccountStore(),
) : PaginationEnvironment {
    private val settingsStore = desktopSettingsStore()
    private val contentFilterDatabase = getContentFilterDatabase(
        File(System.getProperty("user.home"), ".zhihu-plus/content-filter.db"),
    )

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
            semanticMatcher = KeywordSemanticMatcher { _, _, _ -> emptyList() },
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
}

@Composable
actual fun rememberPaginationEnvironment(allowGuestAccess: Boolean): PaginationEnvironment =
    remember(allowGuestAccess) { DesktopPaginationEnvironment() }

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
