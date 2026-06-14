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

import com.fleeksoft.ksoup.Ksoup
import com.github.zly2006.zhihu.navigation.Article
import com.github.zly2006.zhihu.navigation.NavDestination
import com.github.zly2006.zhihu.navigation.Pin
import com.github.zly2006.zhihu.shared.data.AdvertisementFeed
import com.github.zly2006.zhihu.shared.data.DataHolder
import com.github.zly2006.zhihu.shared.data.Feed
import com.github.zly2006.zhihu.shared.data.FeedDisplayItem
import com.github.zly2006.zhihu.shared.data.navDestination
import com.github.zly2006.zhihu.shared.data.target
import com.github.zly2006.zhihu.shared.filter.ContentOpenEventSupport
import com.github.zly2006.zhihu.shared.platform.SettingsStore
import kotlinx.serialization.json.Json

suspend fun ContentFilterDatabase.recordContentInteraction(
    settings: FeedFilterSettings,
    targetType: String,
    targetId: String,
) {
    if (settings.enableContentFilter) {
        ContentFilterManager(contentFilterDao()).recordContentInteraction(targetType, targetId)
    }
}

suspend fun ContentFilterDatabase.performContentFilterMaintenanceCleanup(
    settings: FeedFilterSettings,
) {
    if (settings.enableContentFilter) {
        ContentFilterManager(contentFilterDao()).cleanupOldData()
    }
}

suspend fun ContentFilterDatabase.filterForegroundReadItems(
    settings: FeedFilterSettings,
    items: List<FeedDisplayItem>,
): List<FeedDisplayItem> = ForegroundReadFilterPipeline(
    settings = settings,
    contentFilterManager = ContentFilterManager(contentFilterDao()),
    blockedFeedRecordDao = blockedFeedRecordDao(),
).filter(items)

class ForegroundReadFilterPipeline(
    private val settings: FeedFilterSettings,
    private val contentFilterManager: ContentFilterManager,
    private val blockedFeedRecordDao: BlockedFeedRecordDao,
) {
    suspend fun filter(items: List<FeedDisplayItem>): List<FeedDisplayItem> {
        if (settings.reverseBlock || !settings.enableContentFilter) {
            return items
        }

        val itemIdentityPairs = items.map { item -> item to item.resolveContentIdentity() }
        val viewedContentIds = contentFilterManager.getAlreadyViewedContentIds(
            itemIdentityPairs.map { (_, identity) -> identity.type to identity.id },
        )

        val keptItems = mutableListOf<FeedDisplayItem>()
        val blockedItems = mutableListOf<Pair<FilterableContent, String>>()

        itemIdentityPairs.forEach { (item, identity) ->
            val isViewed = ContentViewRecord.generateId(identity.type, identity.id) in viewedContentIds
            val isFollowing = item.feed
                ?.target
                ?.author
                ?.isFollowing ?: false
            val isLowQualityAndroidFeed = isLowQualityForegroundFeed(item)

            if (isFollowing || (!isViewed && !isLowQualityAndroidFeed)) {
                keptItems.add(item)
                contentFilterManager.recordContentView(identity.type, identity.id)
            } else {
                blockedItems.add(
                    item.toFilterableContent(identity, DataHolder.DummyContent) to "已读过且未关注作者",
                )
            }
        }

        if (blockedItems.isNotEmpty()) {
            saveBlockedFeedRecords(blockedFeedRecordDao, blockedItems)
        }

        return keptItems
    }
}

private fun isLowQualityForegroundFeed(item: FeedDisplayItem): Boolean =
    item.details.contains("小时前") ||
        item.details.contains("分钟前") ||
        item.details.contains("浏览")

data class FeedContentFilterResult(
    val kept: List<FilterableContent>,
    val blocked: List<Pair<FilterableContent, String>>,
)

class FeedContentFilterPipeline(
    private val settings: FeedFilterSettings,
    private val blocklistService: BlocklistService,
    private val blockedKeywordService: BlockedKeywordService,
    private val htmlToText: (String) -> String = { html -> Ksoup.parse(html).text() },
    private val onNlpBlocked: suspend (List<FilterableContent>) -> Unit = {},
) {
    suspend fun filter(contents: List<FilterableContent>): FeedContentFilterResult {
        val blocked = mutableListOf<Pair<FilterableContent, String>>()
        var filteredContents = contents

        if (settings.enableUserBlocking) {
            val (kept, removed) = filteredContents.partition { !blocklistService.isUserBlocked(it.authorId) }
            removed.forEach { blocked.add(it to "屏蔽作者：${it.authorName ?: it.authorId}") }
            filteredContents = kept
        }

        if (settings.enableKeywordBlocking) {
            val (kept, removed) = filteredContents.partition { content ->
                !blocklistService.containsBlockedKeyword(content.title) &&
                    !blocklistService.containsBlockedKeyword(content.summary ?: "") &&
                    !blocklistService.containsBlockedKeyword(content.content ?: "")
            }
            removed.forEach { blocked.add(it to "关键词屏蔽") }
            filteredContents = kept
        }

        if (settings.enableNlpBlocking) {
            val blockedThisRound = mutableListOf<FilterableContent>()
            val finalFilteredContents = mutableListOf<FilterableContent>()

            for (content in filteredContents) {
                val (shouldBlock, matchedKeywords) = blockedKeywordService.checkNLPBlockingWithWeight(
                    title = content.title,
                    excerpt = content.summary,
                    content = content.content?.let(htmlToText),
                    threshold = settings.nlpSimilarityThreshold,
                )

                if (!shouldBlock) {
                    finalFilteredContents.add(content)
                } else {
                    blockedKeywordService.recordBlockedContent(
                        contentId = content.contentId,
                        contentType = content.contentType,
                        title = content.title,
                        excerpt = content.summary ?: "",
                        authorName = content.authorName,
                        authorId = content.authorId,
                        matchedKeywords = matchedKeywords,
                    )
                    val keywordNames = matchedKeywords.joinToString("、") { it.keyword }
                    blocked.add(content to "NLP语义屏蔽：$keywordNames")
                    blockedThisRound.add(content)
                }
            }

            if (blockedThisRound.isNotEmpty()) {
                onNlpBlocked(blockedThisRound)
            }

            filteredContents = finalFilteredContents
        }

        if (settings.enableTopicBlocking) {
            filteredContents = filteredContents.filter { content ->
                val topicIds = extractTopicIds(content.raw)
                val kept = blocklistService.countBlockedTopics(topicIds) < settings.topicBlockingThreshold
                if (!kept) {
                    val topicName = topicIds
                        ?.first { topicId ->
                            blocklistService.isTopicBlocked(topicId)
                        }?.let { topicId ->
                            blocklistService.getTopicName(topicId)
                        }
                    blocked.add(content to "屏蔽主题：$topicName")
                }
                kept
            }
        }

        return FeedContentFilterResult(filteredContents, blocked)
    }
}

fun interface ContentDetailProvider {
    suspend fun get(navDestination: NavDestination): DataHolder.Content?
}

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
                .filter { content -> getFeedAdBlockReason(content, FeedAdBlockSettings()) != null }
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

suspend fun recordFeedContentInteraction(
    settings: FeedFilterSettings,
    database: ContentFilterDatabase,
    feed: Feed,
) {
    val target = feed.target ?: return
    val (targetType, targetId) = when (target) {
        is Feed.AnswerTarget -> ContentType.ANSWER to target.id.toString()
        is Feed.ArticleTarget -> ContentType.ARTICLE to target.id.toString()
        is Feed.QuestionTarget -> ContentType.QUESTION to target.id.toString()
        is Feed.PinTarget -> ContentType.PIN to target.id.toString()
        else -> return
    }
    database.recordContentInteraction(settings, targetType, targetId)
}

/**
 * 常见内容身份类型。
 * 用于 feed 过滤、内容打开记录和导航查询，不属于 Android 平台语义。
 */
object ContentType {
    const val ANSWER = "answer"
    const val ARTICLE = "article"
    const val QUESTION = "question"
    const val TOPIC = "topic"
    const val COLUMN = "column"
    const val VIDEO = "video"
    const val PIN = "pin"
}

/**
 * 从 feed item 提炼出的内容快照。
 * 这个结构只在 feed 过滤流水线内部流转，用来承接关键词、NLP、作者、主题等内容级规则。
 */
data class FilterableContent(
    val title: String,
    val summary: String?,
    val content: String?,
    val authorName: String?,
    val authorId: String?,
    val contentId: String,
    val contentType: String,
    val raw: DataHolder.Content,
    val isFollowing: Boolean = false,
    val questionId: Long? = null,
    val url: String? = null,
    val feedJson: String? = null,
    val navDestinationJson: String? = null,
)

data class FeedContentIdentity(
    val type: String,
    val id: String,
)

fun FeedDisplayItem.resolveContentIdentity(): FeedContentIdentity {
    val identity = navDestination?.let(ContentOpenEventSupport::toTrackedContentIdentity)
    return if (identity != null) {
        FeedContentIdentity(identity.type, identity.id)
    } else {
        FeedContentIdentity("unknown", navDestination.hashCode().toString())
    }
}

fun FeedDisplayItem.toFilterableContent(
    identity: FeedContentIdentity,
    rawContent: DataHolder.Content,
): FilterableContent = FilterableContent(
    title = title,
    summary = summary,
    content = when (rawContent) {
        is DataHolder.Answer -> rawContent.content
        is DataHolder.Article -> rawContent.content
        is DataHolder.Pin -> rawContent.contentHtml
        else -> null
    } ?: content ?: summary,
    authorName = authorName,
    authorId = rawContent.author?.id,
    contentId = identity.id,
    contentType = identity.type,
    raw = rawContent,
    isFollowing = rawContent.author?.isFollowing ?: false,
    questionId = (rawContent as? DataHolder.Answer)?.question?.id,
    url = feed?.target?.url,
    feedJson = feed?.let { runCatching { feedFilterRecordJson.encodeToString(it) }.getOrNull() },
    navDestinationJson = navDestination?.let { runCatching { feedFilterRecordJson.encodeToString(it) }.getOrNull() },
)

/** 从内容实体中提取主题 ID 列表，供 feed 过滤阶段的主题规则使用。 */
fun extractTopicIds(raw: DataHolder.Content): List<String>? = when (raw) {
    is DataHolder.Answer -> raw.question.topics.map { it.id }
    is DataHolder.Question -> raw.topics.map { it.id }
    is DataHolder.Article -> raw.topics?.map { it.id }
    is DataHolder.Pin -> raw.topics?.map { it.id }
    else -> null
}

private val DataHolder.Content.author: DataHolder.Author?
    get() = when (this) {
        is DataHolder.Answer -> this.author
        is DataHolder.Article -> this.author
        is DataHolder.Pin -> this.author
        is DataHolder.Question -> this.author
        else -> null
    }

private val feedFilterRecordJson = Json {
    ignoreUnknownKeys = true
    encodeDefaults = false
}

data class FeedAdBlockSettings(
    val blockZhihuAdPlatform: Boolean = true,
    val blockZhihuSchool: Boolean = true,
    val blockWeChatOfficialAccount: Boolean = true,
    val blockPaidContent: Boolean = true,
)

fun getFeedAdBlockReason(
    content: FilterableContent,
    settings: FeedAdBlockSettings,
): String? = when (val raw = content.raw) {
    is DataHolder.Answer -> {
        if (settings.blockPaidContent && raw.paidInfo != null) {
            "知乎盐选付费内容"
        } else {
            getLinkBasedAdReason(raw.content, settings)
        }
    }
    is DataHolder.Article -> {
        if (settings.blockPaidContent && raw.paidInfo != null) {
            "知乎盐选付费内容"
        } else {
            getLinkBasedAdReason(raw.content, settings)
        }
    }
    is DataHolder.Pin -> getLinkBasedAdReason(raw.contentHtml, settings)
    else -> null
}

private fun getLinkBasedAdReason(
    content: String,
    settings: FeedAdBlockSettings,
): String? {
    if (settings.blockZhihuAdPlatform && "xg.zhihu.com" in content) return "知乎广告平台内容"
    if (settings.blockZhihuSchool && ("d.zhihu.com" in content || "data-edu-card-id" in content)) return "知乎学堂内容"
    if (settings.blockWeChatOfficialAccount && "mp.weixin.qq.com" in content) return "微信公众号文章"
    return null
}

data class FeedFilterSettings(
    val enableContentFilter: Boolean = true,
    val reverseBlock: Boolean = false,
    val filterFollowedUserContent: Boolean = false,
    val enableKeywordBlocking: Boolean = true,
    val enableNlpBlocking: Boolean = true,
    val nlpSimilarityThreshold: Double = 0.8,
    val enableUserBlocking: Boolean = true,
    val enableTopicBlocking: Boolean = true,
    val topicBlockingThreshold: Int = 1,
    val adBlockSettings: FeedAdBlockSettings = FeedAdBlockSettings(),
)

fun SettingsStore.toFeedFilterSettings(): FeedFilterSettings = FeedFilterSettings(
    enableContentFilter = getBoolean("enableContentFilter", true),
    reverseBlock = getBoolean("reverseBlock", false),
    filterFollowedUserContent = getBoolean("filterFollowedUserContent", false),
    enableKeywordBlocking = getBoolean("enableKeywordBlocking", true),
    enableNlpBlocking = getBoolean("enableNLPBlocking", true),
    nlpSimilarityThreshold = getFloat("nlpSimilarityThreshold", 0.8f).toDouble(),
    enableUserBlocking = getBoolean("enableUserBlocking", true),
    enableTopicBlocking = getBoolean("enableTopicBlocking", true),
    topicBlockingThreshold = getInt("topicBlockingThreshold", 1),
    adBlockSettings = FeedAdBlockSettings(
        blockZhihuAdPlatform = getBoolean("blockZhihuAdPlatform", true),
        blockZhihuSchool = getBoolean("blockZhihuSchool", true),
        blockWeChatOfficialAccount = getBoolean("blockWeChatOfficialAccount", true),
        blockPaidContent = getBoolean("blockPaidContent", true),
    ),
)
