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

import android.content.Context
import android.content.SharedPreferences
import android.os.Build
import android.util.Log
import android.widget.Toast
import com.github.zly2006.zhihu.data.ContentDetailCache
import com.github.zly2006.zhihu.navigation.Article
import com.github.zly2006.zhihu.navigation.Pin
import com.github.zly2006.zhihu.nlp.NlpServiceKeywordSemanticMatcher
import com.github.zly2006.zhihu.shared.data.AdvertisementFeed
import com.github.zly2006.zhihu.shared.data.DataHolder
import com.github.zly2006.zhihu.shared.data.FeedDisplayItem
import com.github.zly2006.zhihu.shared.data.navDestination
import com.github.zly2006.zhihu.shared.data.target
import com.github.zly2006.zhihu.ui.PREFERENCE_NAME
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jsoup.Jsoup

/**
 * Feed 过滤扩展工具。
 * 只负责对 [FeedDisplayItem] 列表编排过滤流程、补齐过滤所需上下文，并写入 feed 级屏蔽历史。
 * 这里不负责定义内容级规则本身，也不负责详情页打开事件；那些逻辑分别在 blocklist/NLP 仓库和已读事件支持类里。
 */
object ContentFilterExtensions {
    /** 检查是否启用了 feed 已读/低质过滤总开关。 */
    fun isContentFilterEnabled(context: Context): Boolean {
        val preferences = context.getSharedPreferences(PREFERENCE_NAME, Context.MODE_PRIVATE)
        return preferences.toFeedFilterSettings().enableContentFilter
    }

    /**
     * 检查是否启用了关键词屏蔽功能
     */
    fun isKeywordBlockingEnabled(context: Context): Boolean {
        val preferences = context.getSharedPreferences(PREFERENCE_NAME, Context.MODE_PRIVATE)
        return preferences.toFeedFilterSettings().enableKeywordBlocking
    }

    /**
     * 检查是否启用了NLP语义屏蔽功能
     */
    fun isNLPBlockingEnabled(context: Context): Boolean {
        val preferences = context.getSharedPreferences(PREFERENCE_NAME, Context.MODE_PRIVATE)
        return preferences.toFeedFilterSettings().enableNlpBlocking
    }

    /**
     * 获取NLP相似度阈值
     */
    fun getNLPSimilarityThreshold(context: Context): Double {
        val preferences = context.getSharedPreferences(PREFERENCE_NAME, Context.MODE_PRIVATE)
        return preferences.toFeedFilterSettings().nlpSimilarityThreshold
    }

    /**
     * 检查是否启用了用户屏蔽功能
     */
    fun isUserBlockingEnabled(context: Context): Boolean {
        val preferences = context.getSharedPreferences(PREFERENCE_NAME, Context.MODE_PRIVATE)
        return preferences.toFeedFilterSettings().enableUserBlocking
    }

    /**
     * 检查是否启用了主题屏蔽功能
     */
    fun isTopicBlockingEnabled(context: Context): Boolean {
        val preferences = context.getSharedPreferences(PREFERENCE_NAME, Context.MODE_PRIVATE)
        return preferences.toFeedFilterSettings().enableTopicBlocking
    }

    /**
     * 获取主题屏蔽阈值
     */
    fun getTopicBlockingThreshold(context: Context): Int {
        val preferences = context.getSharedPreferences(PREFERENCE_NAME, Context.MODE_PRIVATE)
        return preferences.toFeedFilterSettings().topicBlockingThreshold
    }

    /**
     * 在 feed 中记录某个内容身份被展示了一次。
     * 这里记录的是“内容在 feed 中曝光”，不是内容详情页被打开。
     */
    suspend fun recordContentDisplay(context: Context, targetType: String, targetId: String) {
        if (!isContentFilterEnabled(context)) return

        withContext(Dispatchers.IO) {
            try {
                val filterManager = ContentFilterManager(getContentFilterDatabase(context).contentFilterDao())
                filterManager.recordContentView(targetType, targetId)
            } catch (e: Exception) {
                Log.e("ContentFilterExtensions", "Failed to record content display", e)
            }
        }
    }

    /**
     * 在 feed 中记录用户对某个内容身份发生过交互。
     * 这里的交互用于放宽已读/重复曝光过滤，不等同于详情页打开事件表。
     */
    suspend fun recordContentInteraction(context: Context, targetType: String, targetId: String) {
        if (!isContentFilterEnabled(context)) return

        withContext(Dispatchers.IO) {
            try {
                val filterManager = ContentFilterManager(getContentFilterDatabase(context).contentFilterDao())
                filterManager.recordContentInteraction(targetType, targetId)
            } catch (e: Exception) {
                Log.e("ContentFilterExtensions", "Failed to record content interaction", e)
            }
        }
    }

    /**
     * 定期清理过期数据（建议在应用启动时调用）
     */
    suspend fun performMaintenanceCleanup(context: Context) {
        if (!isContentFilterEnabled(context)) return

        withContext(Dispatchers.IO) {
            try {
                val filterManager = ContentFilterManager(getContentFilterDatabase(context).contentFilterDao())
                filterManager.cleanupOldData()
            } catch (e: Exception) {
                Log.e("ContentFilterExtensions", "Failed to perform maintenance cleanup", e)
            }
        }
    }

    /**
     * 对首页前台 feed 应用“已读/低质”过滤。
     * 这里只看本地曝光记录和当前卡片信息，不做关键词/NLP 等内容级规则判断。
     */
    suspend fun applyForegroundReadFilterToDisplayItems(
        context: Context,
        items: List<FeedDisplayItem>,
    ): List<FeedDisplayItem> = withContext(Dispatchers.IO) {
        try {
            val preferences = context.getSharedPreferences(PREFERENCE_NAME, Context.MODE_PRIVATE)
            val settings = preferences.toFeedFilterSettings()
            if (settings.reverseBlock || !settings.enableContentFilter) {
                return@withContext items
            }

            val filterManager = ContentFilterManager(getContentFilterDatabase(context).contentFilterDao())
            val itemIdentityPairs = items.map { item -> item to item.resolveContentIdentity() }
            val viewedContentIds = filterManager.getAlreadyViewedContentIds(
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
                // 手机版特供垃圾，根本没人点赞那种。
                // 没人点赞，你乎就只能拿时间和浏览量来招笑了。
                val isLowQualityAndroidFeed = item.details.contains("小时前") || item.details.contains("分钟前") || item.details.contains("浏览")

                if (isFollowing || (!isViewed && !isLowQualityAndroidFeed)) {
                    keptItems.add(item)
                    filterManager.recordContentView(identity.type, identity.id)
                } else {
                    blockedItems.add(
                        item.toFilterableContent(identity, DataHolder.DummyContent) to "已读过且未关注作者",
                    )
                }
            }

            if (blockedItems.isNotEmpty()) {
                saveBlockedFeedRecords(context, blockedItems)
            }

            keptItems
        } catch (e: Exception) {
            Log.e("ContentFilterExtensions", "Failed to apply foreground read filter", e)
            items
        }
    }

    /**
     * 对 [FeedDisplayItem] 列表应用 feed 过滤流水线。
     * 输入和输出都是 feed item；其中广告、关键词、NLP、作者、主题等规则，作用在从 feed 提取出的内容快照上。
     * 已读/重复曝光过滤已在前台通过 [applyForegroundReadFilterToDisplayItems] 处理。
     *
     * 在吃💩模式下，只返回广告 feed。
     */
    suspend fun applyContentFilterToDisplayItems(
        context: Context,
        items: List<FeedDisplayItem>,
    ): List<FeedDisplayItem> = withContext(Dispatchers.IO) {
        try {
            val preferences = context.getSharedPreferences(PREFERENCE_NAME, Context.MODE_PRIVATE)
            val settings = preferences.toFeedFilterSettings()

            var filteredItems = items

            // 1. 应用关注用户过滤逻辑
            val shouldFilterFollowed = settings.filterFollowedUserContent

            val (followedUserItems, otherItems) = if (!shouldFilterFollowed) {
                // 分离已关注用户的内容
                filteredItems.partition { item ->
                    item.feed
                        ?.target
                        ?.author
                        ?.isFollowing == true
                }
            } else {
                Pair(emptyList(), filteredItems)
            }

            // 2. 转换为FilterableContent并获取完整内容，同时建立映射关系
            val itemToFilterableMap = mutableMapOf<FeedDisplayItem, FilterableContent>()

            otherItems.forEach { item ->
                val identity = item.resolveContentIdentity()

                // 获取完整内容详情
                val rawContent = when (val dest = item.navDestination) {
                    is Article -> ContentDetailCache.getOrFetch(context, dest) ?: DataHolder.DummyContent
                    is Pin -> ContentDetailCache.getOrFetch(context, dest) ?: DataHolder.DummyContent
                    else -> DataHolder.DummyContent
                }

                if (rawContent is DataHolder.DummyContent) {
                    Log.w("ContentFilterExtensions", "Failed to fetch content details for item '${item.title}' with navDestination '${item.navDestination}'. Using dummy content for filtering.")
                }

                itemToFilterableMap[item] = item.toFilterableContent(identity, rawContent)
            }

            val filterableContents = itemToFilterableMap.values.toList()

            // 3. 过滤广告和付费内容
            val adBlockedContents = mutableListOf<Pair<FilterableContent, String>>()
            if (settings.reverseBlock) {
                val ads = filterableContents.filter { content -> isFeedAdOrPaidContent(content) }
                val ids = ads.map { it.contentId }
                return@withContext items.filter { item ->
                    val contentId = item.resolveContentIdentity().id
                    contentId in ids
                } + items.filter { it.feed is AdvertisementFeed }
            }
            val nonAdContents = filterableContents.filter { content ->
                val blockReason = getFeedAdBlockReason(content, settings.adBlockSettings)
                if (blockReason != null) adBlockedContents.add(content to blockReason)
                blockReason == null
            }

            // 4. 应用关键词和NLP过滤
            val blockedContents = mutableListOf<Pair<FilterableContent, String>>()
            val filteredContents = filterContents(context, nonAdContents, blockedContents, settings)
            val filteredContentIds = filteredContents.map { it.contentId }.toSet()

            // 5. 根据过滤结果重新构建FeedDisplayItem，并附带raw信息
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

            filteredItems = followedUserItems + filteredOtherItems

            // 6. 持久化所有屏蔽记录
            val allBlocked = adBlockedContents + blockedContents
            if (allBlocked.isNotEmpty()) {
                saveBlockedFeedRecords(context, allBlocked)
            }

            filteredItems.filter {
                listOf(
                    "感兴趣",
                    "购买",
                ).none { keyword ->
                    val shouldFilter = it.details.contains(keyword)
                    if (shouldFilter) {
                        Log.e("ContentFilterExtensions", "Filtered item '${it.title}' due to keyword '$keyword' in details: ${it.content}")
                    }
                    shouldFilter
                }
            }
        } catch (e: Exception) {
            Log.e("ContentFilterExtensions", "Failed to apply content filter to display items", e)
            items
        }
    }

    /**
     * 对从 feed 提取出的内容快照应用内容级规则。
     * @param blocked 收集被屏蔽的 feed 内容快照及原因，供后续写入 feed 屏蔽历史
     */
    private suspend fun filterContents(
        context: Context,
        contents: List<FilterableContent>,
        blocked: MutableList<Pair<FilterableContent, String>>,
        settings: FeedFilterSettings,
    ): List<FilterableContent> {
        val database = getContentFilterDatabase(context)
        val pipeline = FeedContentFilterPipeline(
            settings = settings,
            blocklistService = BlocklistService(
                keywordDao = database.blockedKeywordDao(),
                userDao = database.blockedUserDao(),
                topicDao = database.blockedTopicDao(),
            ),
            blockedKeywordService = BlockedKeywordService(
                keywordDao = database.blockedKeywordDao(),
                recordDao = database.blockedContentRecordDao(),
                semanticMatcher = NlpServiceKeywordSemanticMatcher,
            ),
            htmlToText = { Jsoup.parse(it).text() },
            onNlpBlocked = { blockedThisRound ->
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    context.mainExecutor.execute {
                        Toast.makeText(context, "NLP 已屏蔽 ${blockedThisRound.first().title.take(10)}... 等 ${blockedThisRound.size} 条内容", Toast.LENGTH_SHORT).show()
                    }
                }
            },
        )
        val result = pipeline.filter(contents)
        blocked.addAll(result.blocked)
        return result.kept
    }

    private suspend fun saveBlockedFeedRecords(
        context: Context,
        blocked: List<Pair<FilterableContent, String>>,
    ) {
        try {
            val dao = getContentFilterDatabase(context).blockedFeedRecordDao()
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
        } catch (e: Exception) {
            Log.e("ContentFilterExtensions", "Failed to save blocked feed records", e)
        }
    }

}

private fun SharedPreferences.toFeedFilterSettings(): FeedFilterSettings = FeedFilterSettings(
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
