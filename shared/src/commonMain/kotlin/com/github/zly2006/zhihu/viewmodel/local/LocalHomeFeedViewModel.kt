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

package com.github.zly2006.zhihu.viewmodel.local

import androidx.lifecycle.viewModelScope
import com.github.zly2006.zhihu.data.Feed
import com.github.zly2006.zhihu.data.FeedDisplayItem
import com.github.zly2006.zhihu.viewmodel.ContentInteractionEnvironment
import com.github.zly2006.zhihu.viewmodel.LocalRecommendationEnvironment
import com.github.zly2006.zhihu.viewmodel.PaginationEnvironment
import com.github.zly2006.zhihu.viewmodel.feed.BaseFeedViewModel
import com.github.zly2006.zhihu.viewmodel.feed.HomeFeedInteractionViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class LocalHomeFeedViewModel :
    BaseFeedViewModel(),
    HomeFeedInteractionViewModel {
    private lateinit var recommendationEngine: LocalRecommendationEngine

    // 存整个 entry 而不只是 result：负反馈要按 LocalFeed.id 回写 userFeedback，只有 entry 带得到这个 id。
    private val recommendationEntries = mutableMapOf<String, LocalRecommendationEntry>()

    override val initialUrl: String
        get() = error("LocalHomeFeedViewModel should not be used directly. Use LocalFeedViewModel instead.")

    override fun loadMore(environment: PaginationEnvironment) {
        if (displayItems.isEmpty()) {
            super.loadMore(environment)
        }
    }

    override suspend fun fetchFeeds(environment: PaginationEnvironment) {
        try {
            val engine = ensureEngine(environment)
            val recommendations = engine.generateRecommendations(20)
            recommendationEntries.clear()

            if (recommendations.isEmpty()) {
                generateFallbackContent()
            } else {
                val loadedItems = recommendations.map { entry ->
                    createLocalFeedDisplayItem(entry).also { item ->
                        recommendationEntries[item.stableKey] = entry
                    }
                }
                addDisplayItems(loadedItems)
                latestLoadedDisplayItems.value = loadedItems
            }
        } catch (e: Exception) {
            environment.handleLocalRecommendationFailure(e)
            if (e.message?.contains("does not exist. Is Room annotation processor correctly configured?") == true) {
                environment.showLocalRecommendationDatabaseError()
            }
            generateFallbackContent()
        } finally {
            isLoading = false
        }
    }

    fun onLocalItemOpened(item: FeedDisplayItem) {
        val result = recommendationEntries[item.stableKey]?.result ?: return
        if (!::recommendationEngine.isInitialized) {
            return
        }
        viewModelScope.launch(Dispatchers.Default) {
            recommendationEngine.recordContentOpened(result.contentId, result.reason)
        }
    }

    /**
     * 记录用户对本地推荐条目的显式反馈（miuix 信息流卡片的上滑喜欢 / 下滑不喜欢）。
     *
     * 负反馈同时把卡片移出当前列表，否则用户滑完还要继续看到它。
     */
    fun onLocalItemFeedback(item: FeedDisplayItem, feedback: Double) {
        val entry = recommendationEntries[item.stableKey] ?: return
        if (!::recommendationEngine.isInitialized) {
            return
        }
        viewModelScope.launch(Dispatchers.Default) {
            recommendationEngine.recordRecommendationFeedback(
                feedId = entry.feed.id,
                contentId = entry.result.contentId,
                reason = entry.result.reason,
                feedback = feedback,
            )
            if (feedback < 0) {
                withContext(Dispatchers.Main) {
                    displayItems.remove(item)
                }
            }
        }
    }

    private suspend fun ensureEngine(environment: LocalRecommendationEnvironment): LocalRecommendationEngine {
        if (!::recommendationEngine.isInitialized) {
            recommendationEngine = environment.localRecommendationEngine()
                ?: error("LocalRecommendationEngine is required for local home feed")
        }
        recommendationEngine.initialize()
        return recommendationEngine
    }

    private suspend fun generateFallbackContent() {
        val fallbackItems = listOf(
            FeedDisplayItem(
                title = "本地推荐正在建立候选池",
                summary = "系统会先抓取关注动态、热门内容和相关话题，再根据你的点击逐步调整排序。",
                details = "本地推荐 · 冷启动",
                feed = null,
                isFiltered = false,
            ),
            FeedDisplayItem(
                title = "你的行为只在本地学习",
                summary = "点开内容会影响后续排序，但这些学习信号不会作为推荐特征上传到服务器。",
                details = "本地推荐 · 隐私优先",
                feed = null,
                isFiltered = false,
            ),
        )

        fallbackItems.forEach { item ->
            if (displayItems.none { existing -> existing.stableKey == item.stableKey }) {
                displayItems.add(item)
            }
            delay(300)
        }
        latestLoadedDisplayItems.value = fallbackItems
    }

    override suspend fun recordContentInteraction(
        environment: ContentInteractionEnvironment,
        feed: Feed,
    ) = Unit

    override fun onUiContentClick(environment: ContentInteractionEnvironment, feed: Feed, item: FeedDisplayItem) = Unit
}
