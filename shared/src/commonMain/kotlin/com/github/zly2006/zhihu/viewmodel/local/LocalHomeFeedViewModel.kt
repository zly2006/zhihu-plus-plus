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

package com.github.zly2006.zhihu.viewmodel.local

import androidx.lifecycle.viewModelScope
import com.github.zly2006.zhihu.shared.data.Feed
import com.github.zly2006.zhihu.shared.data.FeedDisplayItem
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

            if (recommendations.isEmpty()) {
                generateFallbackContent()
            } else {
                addDisplayItems(recommendations.map(::createLocalFeedDisplayItem))
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
        val contentId = item.localContentId ?: return
        val reason = item.localReason?.let { runCatching { CrawlingReason.valueOf(it) }.getOrNull() } ?: return
        if (!::recommendationEngine.isInitialized) {
            return
        }
        viewModelScope.launch(Dispatchers.Default) {
            recommendationEngine.recordContentOpened(contentId, reason)
        }
    }

    fun onLocalItemFeedback(item: FeedDisplayItem, feedback: Double) {
        val contentId = item.localContentId ?: return
        val reason = item.localReason?.let { runCatching { CrawlingReason.valueOf(it) }.getOrNull() } ?: return
        if (!::recommendationEngine.isInitialized) {
            return
        }
        viewModelScope.launch(Dispatchers.Default) {
            recommendationEngine.recordRecommendationFeedback(
                feedId = item.localFeedId,
                contentId = contentId,
                reason = reason,
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
                summary = "系统会先抓取关注动态、热门内容和相关话题，再根据你的点击与反馈逐步调整排序。",
                details = "本地推荐 · 冷启动",
                feed = null,
                isFiltered = false,
            ),
            FeedDisplayItem(
                title = "你的行为只在本地学习",
                summary = "点开、喜欢、不喜欢都会影响后续排序，但这些学习信号不会作为推荐特征上传到服务器。",
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
    }

    override suspend fun recordContentInteraction(
        environment: ContentInteractionEnvironment,
        feed: Feed,
    ) = Unit

    override fun onUiContentClick(environment: ContentInteractionEnvironment, feed: Feed, item: FeedDisplayItem) = Unit
}
