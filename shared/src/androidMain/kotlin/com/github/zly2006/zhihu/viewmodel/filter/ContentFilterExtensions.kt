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
import com.github.zly2006.zhihu.shared.data.FeedDisplayItem
import com.github.zly2006.zhihu.shared.platform.SettingsStore
import com.github.zly2006.zhihu.ui.PREFERENCE_NAME
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Feed 过滤扩展工具。
 * 只负责对 [FeedDisplayItem] 列表编排过滤流程、补齐过滤所需上下文，并写入 feed 级屏蔽历史。
 * 这里不负责定义内容级规则本身，也不负责详情页打开事件；那些逻辑分别在 blocklist/NLP 仓库和已读事件支持类里。
 */
object ContentFilterExtensions {
    /** 检查是否启用了 feed 已读/低质过滤总开关。 */
    fun isContentFilterEnabled(context: Context): Boolean = context.feedFilterSettings().enableContentFilter

    /**
     * 检查是否启用了关键词屏蔽功能
     */
    fun isKeywordBlockingEnabled(context: Context): Boolean = context.feedFilterSettings().enableKeywordBlocking

    /**
     * 检查是否启用了NLP语义屏蔽功能
     */
    fun isNLPBlockingEnabled(context: Context): Boolean = context.feedFilterSettings().enableNlpBlocking

    /**
     * 获取NLP相似度阈值
     */
    fun getNLPSimilarityThreshold(context: Context): Double = context.feedFilterSettings().nlpSimilarityThreshold

    /**
     * 检查是否启用了用户屏蔽功能
     */
    fun isUserBlockingEnabled(context: Context): Boolean = context.feedFilterSettings().enableUserBlocking

    /**
     * 检查是否启用了主题屏蔽功能
     */
    fun isTopicBlockingEnabled(context: Context): Boolean = context.feedFilterSettings().enableTopicBlocking

    /**
     * 获取主题屏蔽阈值
     */
    fun getTopicBlockingThreshold(context: Context): Int = context.feedFilterSettings().topicBlockingThreshold

    /**
     * 在 feed 中记录某个内容身份被展示了一次。
     * 这里记录的是“内容在 feed 中曝光”，不是内容详情页被打开。
     */
    suspend fun recordContentDisplay(context: Context, targetType: String, targetId: String) {
        withContext(Dispatchers.IO) {
            try {
                val settings = context.feedFilterSettings()
                recordContentDisplay(settings, getContentFilterDatabase(context), targetType, targetId)
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
        withContext(Dispatchers.IO) {
            try {
                val settings = context.feedFilterSettings()
                recordContentInteraction(settings, getContentFilterDatabase(context), targetType, targetId)
            } catch (e: Exception) {
                Log.e("ContentFilterExtensions", "Failed to record content interaction", e)
            }
        }
    }

    /**
     * 定期清理过期数据（建议在应用启动时调用）
     */
    suspend fun performMaintenanceCleanup(context: Context) {
        withContext(Dispatchers.IO) {
            try {
                val settings = context.feedFilterSettings()
                performMaintenanceCleanup(settings, getContentFilterDatabase(context))
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
            val settings = context.feedFilterSettings()
            applyForegroundReadFilterToDisplayItems(settings, getContentFilterDatabase(context), items)
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
            val settings = context.feedFilterSettings()
            applyContentFilterToDisplayItems(
                settings = settings,
                database = getContentFilterDatabase(context),
                items = items,
                contentDetailProvider = ContentDetailProvider { ContentDetailCache.getOrFetch(context, it) },
                semanticMatcher = AndroidContentFilterRuntime.semanticMatcher,
                onNlpBlocked = { blockedThisRound ->
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                        context.mainExecutor.execute {
                            Toast.makeText(context, "NLP 已屏蔽 ${blockedThisRound.first().title.take(10)}... 等 ${blockedThisRound.size} 条内容", Toast.LENGTH_SHORT).show()
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
        } catch (e: Exception) {
            Log.e("ContentFilterExtensions", "Failed to apply content filter to display items", e)
            items
        }
    }
}

private fun Context.feedFilterSettings(): FeedFilterSettings =
    getSharedPreferences(PREFERENCE_NAME, Context.MODE_PRIVATE)
        .toSettingsStore()
        .toFeedFilterSettings()

private fun SharedPreferences.toSettingsStore(): SettingsStore = SettingsStore(
    getBoolean = ::getBoolean,
    putBoolean = { key, value -> edit().putBoolean(key, value).apply() },
    getString = ::getStringValue,
    putString = { key, value -> edit().putString(key, value).apply() },
    getStringOrNull = { key -> getString(key, null) },
    putStringSet = { key, value -> edit().putStringSet(key, value).apply() },
    getStringSet = { key, defaultValue -> getStringSet(key, defaultValue)?.toSet() ?: defaultValue },
    getInt = ::getInt,
    putInt = { key, value -> edit().putInt(key, value).apply() },
    getFloat = ::getFloat,
    putFloat = { key, value -> edit().putFloat(key, value).apply() },
    remove = { key -> edit().remove(key).apply() },
)

private fun SharedPreferences.getStringValue(key: String, defaultValue: String): String =
    getString(key, defaultValue) ?: defaultValue
