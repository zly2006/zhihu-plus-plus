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

package com.github.zly2006.zhihu.viewmodel.filter

import com.github.zly2006.zhihu.navigation.Article
import com.github.zly2006.zhihu.navigation.NavDestination
import com.github.zly2006.zhihu.navigation.Pin
import com.github.zly2006.zhihu.shared.data.AdvertisementFeed
import com.github.zly2006.zhihu.shared.data.DataHolder
import com.github.zly2006.zhihu.shared.data.FeedDisplayItem
import com.github.zly2006.zhihu.shared.data.navDestination
import com.github.zly2006.zhihu.shared.data.target

fun interface ContentDetailProvider {
    suspend fun get(navDestination: NavDestination): DataHolder.Content?
}

fun ContentFilterDatabase.createContentExposureRecorder(
    settings: FeedFilterSettings,
): ContentExposureRecorder = ContentExposureRecorder(
    settings = settings,
    contentFilterManager = ContentFilterManager(contentFilterDao()),
)

fun ContentFilterDatabase.createForegroundReadFilterPipeline(
    settings: FeedFilterSettings,
): ForegroundReadFilterPipeline = ForegroundReadFilterPipeline(
    settings = settings,
    contentFilterManager = ContentFilterManager(contentFilterDao()),
    blockedFeedRecordDao = blockedFeedRecordDao(),
)

fun ContentFilterDatabase.createFeedDisplayFilterPipeline(
    settings: FeedFilterSettings,
    contentDetailProvider: ContentDetailProvider,
    semanticMatcher: KeywordSemanticMatcher,
    onNlpBlocked: suspend (List<FilterableContent>) -> Unit = {},
    onDetailFetchFailed: (FeedDisplayItem) -> Unit = {},
    onDetailsKeywordFiltered: (FeedDisplayItem, String) -> Unit = { _, _ -> },
): FeedDisplayFilterPipeline = FeedDisplayFilterPipeline(
    settings = settings,
    contentDetailProvider = contentDetailProvider,
    contentFilterPipeline = FeedContentFilterPipeline(
        settings = settings,
        blocklistService = BlocklistService(
            keywordDao = blockedKeywordDao(),
            userDao = blockedUserDao(),
            topicDao = blockedTopicDao(),
        ),
        blockedKeywordService = BlockedKeywordService(
            keywordDao = blockedKeywordDao(),
            recordDao = blockedContentRecordDao(),
            semanticMatcher = semanticMatcher,
        ),
        onNlpBlocked = onNlpBlocked,
    ),
    blockedFeedRecordDao = blockedFeedRecordDao(),
    onDetailFetchFailed = onDetailFetchFailed,
    onDetailsKeywordFiltered = onDetailsKeywordFiltered,
)

suspend fun ContentFilterDatabase.filterFeedDisplayItems(
    settings: FeedFilterSettings,
    items: List<FeedDisplayItem>,
    contentDetailProvider: ContentDetailProvider,
    semanticMatcher: KeywordSemanticMatcher,
    onNlpBlocked: suspend (List<FilterableContent>) -> Unit = {},
    onDetailFetchFailed: (FeedDisplayItem) -> Unit = {},
    onDetailsKeywordFiltered: (FeedDisplayItem, String) -> Unit = { _, _ -> },
): List<FeedDisplayItem> = createFeedDisplayFilterPipeline(
    settings = settings,
    contentDetailProvider = contentDetailProvider,
    semanticMatcher = semanticMatcher,
    onNlpBlocked = onNlpBlocked,
    onDetailFetchFailed = onDetailFetchFailed,
    onDetailsKeywordFiltered = onDetailsKeywordFiltered,
).filter(items)

class FeedDisplayFilterPipeline(
    private val settings: FeedFilterSettings,
    private val contentDetailProvider: ContentDetailProvider,
    private val contentFilterPipeline: FeedContentFilterPipeline,
    private val blockedFeedRecordDao: BlockedFeedRecordDao,
    private val onDetailFetchFailed: (FeedDisplayItem) -> Unit = {},
    private val onDetailsKeywordFiltered: (FeedDisplayItem, String) -> Unit = { _, _ -> },
) {
    suspend fun filter(items: List<FeedDisplayItem>): List<FeedDisplayItem> {
        val (followedUserItems, otherItems) = if (!settings.filterFollowedUserContent) {
            items.partition { item ->
                item.feed
                    ?.target
                    ?.author
                    ?.isFollowing == true
            }
        } else {
            emptyList<FeedDisplayItem>() to items
        }

        val itemToFilterableMap = mutableMapOf<FeedDisplayItem, FilterableContent>()

        otherItems.forEach { item ->
            val identity = item.resolveContentIdentity()
            val rawContent = resolveRawContent(item)

            if (rawContent is DataHolder.DummyContent) {
                onDetailFetchFailed(item)
            }

            itemToFilterableMap[item] = item.toFilterableContent(identity, rawContent)
        }

        val filterableContents = itemToFilterableMap.values.toList()

        if (settings.reverseBlock) {
            val adIds = filterableContents
                .filter(::isFeedAdOrPaidContent)
                .map { it.contentId }
                .toSet()
            return items.filter { item ->
                item.resolveContentIdentity().id in adIds
            } + items.filter { it.feed is AdvertisementFeed }
        }

        val adBlockedContents = mutableListOf<Pair<FilterableContent, String>>()
        val nonAdContents = filterableContents.filter { content ->
            val blockReason = getFeedAdBlockReason(content, settings.adBlockSettings)
            if (blockReason != null) adBlockedContents.add(content to blockReason)
            blockReason == null
        }

        val contentFilterResult = contentFilterPipeline.filter(nonAdContents)
        val filteredContentIds = contentFilterResult.kept.map { it.contentId }.toSet()

        val itemToRawMap = itemToFilterableMap.entries.associate { (item, filterable) ->
            filterable.contentId to Pair(item, filterable.raw)
        }

        val filteredOtherItems = otherItems.mapNotNull { item ->
            val contentId = item.resolveContentIdentity().id
            if (contentId in filteredContentIds) {
                val (_, raw) = itemToRawMap[contentId] ?: (null to null)
                item.copy(raw = raw)
            } else {
                null
            }
        }

        val allBlocked = adBlockedContents + contentFilterResult.blocked
        if (allBlocked.isNotEmpty()) {
            saveBlockedFeedRecords(blockedFeedRecordDao, allBlocked)
        }

        return (followedUserItems + filteredOtherItems).filterDetailsKeywords()
    }

    private suspend fun resolveRawContent(item: FeedDisplayItem): DataHolder.Content = when (val dest = item.navDestination) {
        is Article -> contentDetailProvider.get(dest) ?: DataHolder.DummyContent
        is Pin -> contentDetailProvider.get(dest) ?: DataHolder.DummyContent
        else -> DataHolder.DummyContent
    }

    private fun List<FeedDisplayItem>.filterDetailsKeywords(): List<FeedDisplayItem> = filter { item ->
        detailsPostFilterKeywords.none { keyword ->
            val shouldFilter = item.details.contains(keyword)
            if (shouldFilter) {
                onDetailsKeywordFiltered(item, keyword)
            }
            shouldFilter
        }
    }
}

suspend fun saveBlockedFeedRecords(
    dao: BlockedFeedRecordDao,
    blocked: List<Pair<FilterableContent, String>>,
) {
    blocked.forEach { (content, reason) ->
        dao.insert(
            BlockedFeedRecord(
                title = content.title,
                questionId = content.questionId,
                authorName = content.authorName,
                authorId = content.authorId,
                url = content.url,
                content = content.content,
                blockedReason = reason,
                navDestinationJson = content.navDestinationJson,
                feedJson = content.feedJson,
            ),
        )
    }
    dao.maintainLimit()
}

private val detailsPostFilterKeywords = listOf("感兴趣", "购买")
