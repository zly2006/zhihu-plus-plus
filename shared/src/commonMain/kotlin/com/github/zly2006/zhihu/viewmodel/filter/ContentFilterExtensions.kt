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

import com.github.zly2006.zhihu.shared.data.FeedDisplayItem
import com.github.zly2006.zhihu.shared.platform.SettingsStore

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
    ): List<FeedDisplayItem> = database.filterFeedDisplayItems(
        settings = settings,
        items = items,
        contentDetailProvider = contentDetailProvider,
        semanticMatcher = semanticMatcher,
        onNlpBlocked = onNlpBlocked,
        onDetailFetchFailed = onDetailFetchFailed,
        onDetailsKeywordFiltered = onDetailsKeywordFiltered,
    )
}
