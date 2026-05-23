package com.github.zly2006.zhihu.viewmodel.filter

import com.github.zly2006.zhihu.shared.data.FeedDisplayItem
import com.github.zly2006.zhihu.shared.platform.SettingsStore

/**
 * Shared feed filtering entry points.
 *
 * Platform source sets only provide settings, database builders, detail providers,
 * semantic matcher, and message/log callbacks.
 */
suspend fun recordContentDisplay(
    settings: FeedFilterSettings,
    database: ContentFilterDatabase,
    targetType: String,
    targetId: String,
) {
    database.recordContentDisplay(settings, targetType, targetId)
}

suspend fun recordContentInteraction(
    settings: FeedFilterSettings,
    database: ContentFilterDatabase,
    targetType: String,
    targetId: String,
) {
    database.recordContentInteraction(settings, targetType, targetId)
}

suspend fun performMaintenanceCleanup(
    settings: FeedFilterSettings,
    database: ContentFilterDatabase,
) {
    database.performContentFilterMaintenanceCleanup(settings)
}

suspend fun applyForegroundReadFilterToDisplayItems(
    settings: FeedFilterSettings,
    database: ContentFilterDatabase,
    items: List<FeedDisplayItem>,
): List<FeedDisplayItem> = database.filterForegroundReadItems(settings, items)

suspend fun applyContentFilterToDisplayItems(
    settings: FeedFilterSettings,
    database: ContentFilterDatabase,
    items: List<FeedDisplayItem>,
    contentDetailProvider: ContentDetailProvider,
    semanticMatcher: KeywordSemanticMatcher,
    onNlpBlocked: suspend (List<FilterableContent>) -> Unit = {},
    onDetailFetchFailed: (FeedDisplayItem) -> Unit = {},
    onDetailsKeywordFiltered: (FeedDisplayItem, String) -> Unit = { _, _ -> },
): List<FeedDisplayItem> = database.filterFeedDisplayItems(
    settings = settings,
    items = items,
    contentDetailProvider = contentDetailProvider,
    semanticMatcher = semanticMatcher,
    onNlpBlocked = onNlpBlocked,
    onDetailFetchFailed = onDetailFetchFailed,
    onDetailsKeywordFiltered = onDetailsKeywordFiltered,
)

fun isContentFilterEnabled(settings: FeedFilterSettings): Boolean = settings.enableContentFilter

fun isKeywordBlockingEnabled(settings: FeedFilterSettings): Boolean = settings.enableKeywordBlocking

fun isNLPBlockingEnabled(settings: FeedFilterSettings): Boolean = settings.enableNlpBlocking

fun getNLPSimilarityThreshold(settings: FeedFilterSettings): Double = settings.nlpSimilarityThreshold

fun isUserBlockingEnabled(settings: FeedFilterSettings): Boolean = settings.enableUserBlocking

fun isTopicBlockingEnabled(settings: FeedFilterSettings): Boolean = settings.enableTopicBlocking

fun getTopicBlockingThreshold(settings: FeedFilterSettings): Int = settings.topicBlockingThreshold

fun SettingsStore.isContentFilterEnabled(): Boolean = isContentFilterEnabled(toFeedFilterSettings())

fun SettingsStore.isKeywordBlockingEnabled(): Boolean = isKeywordBlockingEnabled(toFeedFilterSettings())

fun SettingsStore.isNLPBlockingEnabled(): Boolean = isNLPBlockingEnabled(toFeedFilterSettings())

fun SettingsStore.getNLPSimilarityThreshold(): Double = getNLPSimilarityThreshold(toFeedFilterSettings())

fun SettingsStore.isUserBlockingEnabled(): Boolean = isUserBlockingEnabled(toFeedFilterSettings())

fun SettingsStore.isTopicBlockingEnabled(): Boolean = isTopicBlockingEnabled(toFeedFilterSettings())

fun SettingsStore.getTopicBlockingThreshold(): Int = getTopicBlockingThreshold(toFeedFilterSettings())
