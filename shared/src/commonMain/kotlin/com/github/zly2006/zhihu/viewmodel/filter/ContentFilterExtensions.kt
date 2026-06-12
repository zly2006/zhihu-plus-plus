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
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.serialization.json.Json

suspend fun ContentFilterDatabase.recordContentDisplay(
    settings: FeedFilterSettings,
    targetType: String,
    targetId: String,
) {
    createContentExposureRecorder(settings).recordDisplay(targetType, targetId)
}

suspend fun ContentFilterDatabase.recordContentInteraction(
    settings: FeedFilterSettings,
    targetType: String,
    targetId: String,
) {
    createContentExposureRecorder(settings).recordInteraction(targetType, targetId)
}

suspend fun ContentFilterDatabase.performContentFilterMaintenanceCleanup(
    settings: FeedFilterSettings,
) {
    createContentExposureRecorder(settings).performMaintenanceCleanup()
}

suspend fun ContentFilterDatabase.filterForegroundReadItems(
    settings: FeedFilterSettings,
    items: List<FeedDisplayItem>,
): List<FeedDisplayItem> = createForegroundReadFilterPipeline(settings).filter(items)

class ContentExposureRecorder(
    private val settings: FeedFilterSettings,
    private val contentFilterManager: ContentFilterManager,
) {
    suspend fun recordDisplay(
        targetType: String,
        targetId: String,
    ) {
        if (settings.enableContentFilter) {
            contentFilterManager.recordContentView(targetType, targetId)
        }
    }

    suspend fun recordInteraction(
        targetType: String,
        targetId: String,
    ) {
        if (settings.enableContentFilter) {
            contentFilterManager.recordContentInteraction(targetType, targetId)
        }
    }

    suspend fun performMaintenanceCleanup() {
        if (settings.enableContentFilter) {
            contentFilterManager.cleanupOldData()
        }
    }
}

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
    private val htmlToText: (String) -> String = ::htmlToPlainText,
    private val onNlpBlocked: suspend (List<FilterableContent>) -> Unit = {},
    private val mcnCompanyProvider: McnCompanyProvider = NoopMcnCompanyProvider,
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

        if (settings.enableMcnBlocking && (settings.blockAllMcnAuthors || blocklistService.hasBlockedMcnOrganizations())) {
            val mcnCompanyByToken = resolveMcnCompanies(filteredContents)
            val kept = mutableListOf<FilterableContent>()
            for (content in filteredContents) {
                val mcnCompany = content.authorUrlToken?.let(mcnCompanyByToken::get)
                val shouldBlock = !mcnCompany.isNullOrBlank() &&
                    (settings.blockAllMcnAuthors || blocklistService.isMcnOrganizationBlocked(mcnCompany))
                if (shouldBlock) {
                    blocked.add(content to "屏蔽MCN机构：$mcnCompany")
                } else {
                    kept.add(content)
                }
            }
            filteredContents = kept
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

    private suspend fun resolveMcnCompanies(contents: List<FilterableContent>): Map<String, String?> = coroutineScope {
        val authorNamesByToken = mutableMapOf<String, String?>()
        contents.forEach { content ->
            val token = content.authorUrlToken?.takeIf { it.isNotBlank() } ?: return@forEach
            if (token !in authorNamesByToken) {
                authorNamesByToken[token] = content.authorName
            }
        }

        val resolvedCompanies = mutableMapOf<String, String?>()
        val tokensToFetch = mutableListOf<String>()
        authorNamesByToken.keys.forEach { token ->
            val cachedAuthor = blocklistService.getCachedMcnAuthor(token)
            if (cachedAuthor != null) {
                resolvedCompanies[token] = cachedAuthor.mcnCompany.normalizeMcnCompany()
            } else {
                tokensToFetch.add(token)
            }
        }

        val semaphore = Semaphore(MCN_LOOKUP_CONCURRENCY)
        val fetchedCompanies = tokensToFetch
            .map { token ->
                async {
                    token to semaphore.withPermit {
                        runCatching {
                            mcnCompanyProvider
                                .getMcnCompany(token)
                                .normalizeMcnCompany()
                                .also { company ->
                                    blocklistService.cacheMcnCompany(token, authorNamesByToken[token], company)
                                }
                        }.getOrNull()
                    }
                }
            }.awaitAll()

        resolvedCompanies + fetchedCompanies
    }
}

private const val MCN_LOOKUP_CONCURRENCY = 4

private fun htmlToPlainText(html: String): String = Ksoup.parse(html).text()

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
    mcnCompanyProvider: McnCompanyProvider = NoopMcnCompanyProvider,
): FeedDisplayFilterPipeline = FeedDisplayFilterPipeline(
    settings = settings,
    contentDetailProvider = contentDetailProvider,
    contentFilterPipeline = FeedContentFilterPipeline(
        settings = settings,
        blocklistService = BlocklistService(
            keywordDao = blockedKeywordDao(),
            userDao = blockedUserDao(),
            topicDao = blockedTopicDao(),
            mcnOrganizationDao = blockedMcnOrganizationDao(),
            mcnAuthorCacheDao = mcnAuthorCacheDao(),
        ),
        blockedKeywordService = BlockedKeywordService(
            keywordDao = blockedKeywordDao(),
            recordDao = blockedContentRecordDao(),
            semanticMatcher = semanticMatcher,
        ),
        onNlpBlocked = onNlpBlocked,
        mcnCompanyProvider = mcnCompanyProvider,
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
    mcnCompanyProvider: McnCompanyProvider = NoopMcnCompanyProvider,
): List<FeedDisplayItem> = createFeedDisplayFilterPipeline(
    settings = settings,
    contentDetailProvider = contentDetailProvider,
    semanticMatcher = semanticMatcher,
    onNlpBlocked = onNlpBlocked,
    onDetailFetchFailed = onDetailFetchFailed,
    onDetailsKeywordFiltered = onDetailsKeywordFiltered,
    mcnCompanyProvider = mcnCompanyProvider,
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
    val authorUrlToken: String? = null,
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
    authorName = authorName ?: rawContent.author?.name,
    authorId = rawContent.author?.id ?: feed?.target?.author?.id,
    authorUrlToken = rawContent.author?.urlToken ?: authorUrlToken ?: feed?.target?.author?.urlToken,
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

fun isFeedAdOrPaidContent(content: FilterableContent): Boolean = getFeedAdBlockReason(content, FeedAdBlockSettings()) != null

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
    val enableMcnBlocking: Boolean = true,
    val blockAllMcnAuthors: Boolean = false,
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
    enableMcnBlocking = getBoolean("enableMcnBlocking", true),
    blockAllMcnAuthors = getBoolean("blockAllMcnAuthors", false),
    enableTopicBlocking = getBoolean("enableTopicBlocking", true),
    topicBlockingThreshold = getInt("topicBlockingThreshold", 1),
    adBlockSettings = FeedAdBlockSettings(
        blockZhihuAdPlatform = getBoolean("blockZhihuAdPlatform", true),
        blockZhihuSchool = getBoolean("blockZhihuSchool", true),
        blockWeChatOfficialAccount = getBoolean("blockWeChatOfficialAccount", true),
        blockPaidContent = getBoolean("blockPaidContent", true),
    ),
)

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
    recordContentInteraction(settings, database, targetType, targetId)
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
    mcnCompanyProvider: McnCompanyProvider = NoopMcnCompanyProvider,
): List<FeedDisplayItem> = database.filterFeedDisplayItems(
    settings = settings,
    items = items,
    contentDetailProvider = contentDetailProvider,
    semanticMatcher = semanticMatcher,
    onNlpBlocked = onNlpBlocked,
    onDetailFetchFailed = onDetailFetchFailed,
    onDetailsKeywordFiltered = onDetailsKeywordFiltered,
    mcnCompanyProvider = mcnCompanyProvider,
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

/**
 * Feed 过滤扩展工具。
 * 只负责对 [FeedDisplayItem] 列表编排过滤流程、补齐过滤所需上下文，并写入 feed 级屏蔽历史。
 * 这里不负责定义内容级规则本身，也不负责详情页打开事件；那些逻辑分别在 blocklist/NLP 仓库和已读事件支持类里。
 */
object ContentFilterExtensions {
    /** 检查是否启用了 feed 已读/低质过滤总开关。 */
    fun isContentFilterEnabled(settings: FeedFilterSettings): Boolean = settings.enableContentFilter

    fun isContentFilterEnabled(settings: SettingsStore): Boolean =
        isContentFilterEnabled(settings.toFeedFilterSettings())

    /**
     * 检查是否启用了关键词屏蔽功能
     */
    fun isKeywordBlockingEnabled(settings: FeedFilterSettings): Boolean = settings.enableKeywordBlocking

    fun isKeywordBlockingEnabled(settings: SettingsStore): Boolean =
        isKeywordBlockingEnabled(settings.toFeedFilterSettings())

    /**
     * 检查是否启用了NLP语义屏蔽功能
     */
    fun isNLPBlockingEnabled(settings: FeedFilterSettings): Boolean = settings.enableNlpBlocking

    fun isNLPBlockingEnabled(settings: SettingsStore): Boolean =
        isNLPBlockingEnabled(settings.toFeedFilterSettings())

    /**
     * 获取NLP相似度阈值
     */
    fun getNLPSimilarityThreshold(settings: FeedFilterSettings): Double = settings.nlpSimilarityThreshold

    fun getNLPSimilarityThreshold(settings: SettingsStore): Double =
        getNLPSimilarityThreshold(settings.toFeedFilterSettings())

    /**
     * 检查是否启用了用户屏蔽功能
     */
    fun isUserBlockingEnabled(settings: FeedFilterSettings): Boolean = settings.enableUserBlocking

    fun isUserBlockingEnabled(settings: SettingsStore): Boolean =
        isUserBlockingEnabled(settings.toFeedFilterSettings())

    /**
     * 检查是否启用了主题屏蔽功能
     */
    fun isTopicBlockingEnabled(settings: FeedFilterSettings): Boolean = settings.enableTopicBlocking

    fun isTopicBlockingEnabled(settings: SettingsStore): Boolean =
        isTopicBlockingEnabled(settings.toFeedFilterSettings())

    /**
     * 获取主题屏蔽阈值
     */
    fun getTopicBlockingThreshold(settings: FeedFilterSettings): Int = settings.topicBlockingThreshold

    fun getTopicBlockingThreshold(settings: SettingsStore): Int =
        getTopicBlockingThreshold(settings.toFeedFilterSettings())

    /**
     * 在 feed 中记录某个内容身份被展示了一次。
     * 这里记录的是“内容在 feed 中曝光”，不是内容详情页被打开。
     */
    suspend fun recordContentDisplay(
        settings: FeedFilterSettings,
        database: ContentFilterDatabase,
        targetType: String,
        targetId: String,
    ) {
        database.recordContentDisplay(settings, targetType, targetId)
    }

    /**
     * 在 feed 中记录用户对某个内容身份发生过交互。
     * 这里的交互用于放宽已读/重复曝光过滤，不等同于详情页打开事件表。
     */
    suspend fun recordContentInteraction(
        settings: FeedFilterSettings,
        database: ContentFilterDatabase,
        targetType: String,
        targetId: String,
    ) {
        database.recordContentInteraction(settings, targetType, targetId)
    }

    /**
     * 定期清理过期数据（建议在应用启动时调用）
     */
    suspend fun performMaintenanceCleanup(
        settings: FeedFilterSettings,
        database: ContentFilterDatabase,
    ) {
        database.performContentFilterMaintenanceCleanup(settings)
    }

    /**
     * 对首页前台 feed 应用“已读/低质”过滤。
     * 这里只看本地曝光记录和当前卡片信息，不做关键词/NLP 等内容级规则判断。
     */
    suspend fun applyForegroundReadFilterToDisplayItems(
        settings: FeedFilterSettings,
        database: ContentFilterDatabase,
        items: List<FeedDisplayItem>,
    ): List<FeedDisplayItem> =
        database.filterForegroundReadItems(settings, items)

    /**
     * 对 [FeedDisplayItem] 列表应用 feed 过滤流水线。
     * 输入和输出都是 feed item；其中广告、关键词、NLP、作者、主题等规则，作用在从 feed 提取出的内容快照上。
     * 已读/重复曝光过滤已在前台通过 [applyForegroundReadFilterToDisplayItems] 处理。
     *
     * 在吃💩模式下，只返回广告 feed。
     */
    suspend fun applyContentFilterToDisplayItems(
        settings: FeedFilterSettings,
        database: ContentFilterDatabase,
        items: List<FeedDisplayItem>,
        contentDetailProvider: ContentDetailProvider,
        semanticMatcher: KeywordSemanticMatcher,
        onNlpBlocked: suspend (List<FilterableContent>) -> Unit = {},
        onDetailFetchFailed: (FeedDisplayItem) -> Unit = {},
        onDetailsKeywordFiltered: (FeedDisplayItem, String) -> Unit = { _, _ -> },
        mcnCompanyProvider: McnCompanyProvider = NoopMcnCompanyProvider,
    ): List<FeedDisplayItem> = database.filterFeedDisplayItems(
        settings = settings,
        items = items,
        contentDetailProvider = contentDetailProvider,
        semanticMatcher = semanticMatcher,
        onNlpBlocked = onNlpBlocked,
        onDetailFetchFailed = onDetailFetchFailed,
        onDetailsKeywordFiltered = onDetailsKeywordFiltered,
        mcnCompanyProvider = mcnCompanyProvider,
    )
}
