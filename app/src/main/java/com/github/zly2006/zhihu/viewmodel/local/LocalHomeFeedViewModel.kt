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

import android.app.AlertDialog
import android.content.Context
import android.util.Log
import androidx.lifecycle.viewModelScope
import com.github.zly2006.zhihu.R
import com.github.zly2006.zhihu.data.Feed
import com.github.zly2006.zhihu.ui.IHomeFeedViewModel
import com.github.zly2006.zhihu.viewmodel.feed.BaseFeedViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class LocalHomeFeedViewModel :
    BaseFeedViewModel(),
    IHomeFeedViewModel {
    private lateinit var recommendationEngine: LocalRecommendationEngine

    override val initialUrl: String
        get() = error("LocalHomeFeedViewModel should not be used directly. Use LocalFeedViewModel instead.")

    override fun loadMore(context: Context) {
        if (displayItems.isEmpty()) {
            super.loadMore(context)
        }
    }

    override suspend fun fetchFeeds(context: Context) {
        try {
            val engine = ensureEngine(context)
            val recommendations = engine.generateRecommendations(20)

            if (recommendations.isEmpty()) {
                generateFallbackContent(context)
            } else {
                addDisplayItems(recommendations.map(::createLocalFeedDisplayItem))
            }
        } catch (e: Exception) {
            Log.e("LocalHomeFeedViewModel", "Error fetching local feeds", e)
            if (e.message?.contains("does not exist. Is Room annotation processor correctly configured?") == true) {
                withContext(Dispatchers.Main) {
                    AlertDialog
                        .Builder(context)
                        .setTitle(context.getString(R.string.database_error))
                        .setMessage(context.getString(R.string.local_database_error_desc))
                        .setPositiveButton(context.getString(R.string.ok)) { dialog, _ -> dialog.dismiss() }
                        .show()
                }
            }
            generateFallbackContent(context)
        } finally {
            isLoading = false
        }
    }

    fun onLocalItemOpened(context: Context, item: FeedDisplayItem) {
        val contentId = item.localContentId ?: return
        val reason = item.localReason?.let { runCatching { CrawlingReason.valueOf(it) }.getOrNull() } ?: return
        viewModelScope.launch(Dispatchers.IO) {
            ensureEngine(context).recordContentOpened(contentId, reason)
        }
    }

    fun onLocalItemFeedback(context: Context, item: FeedDisplayItem, feedback: Double) {
        val contentId = item.localContentId ?: return
        val reason = item.localReason?.let { runCatching { CrawlingReason.valueOf(it) }.getOrNull() } ?: return
        viewModelScope.launch(Dispatchers.IO) {
            ensureEngine(context).recordRecommendationFeedback(
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

    private suspend fun ensureEngine(context: Context): LocalRecommendationEngine {
        if (!::recommendationEngine.isInitialized) {
            recommendationEngine = LocalRecommendationEngine(context)
        }
        recommendationEngine.initialize()
        return recommendationEngine
    }

    private fun createLocalFeedDisplayItem(entry: LocalRecommendationEngine.LocalRecommendationEntry): FeedDisplayItem = FeedDisplayItem(
        title = entry.feed.title,
        summary = entry.feed.summary,
        details = entry.feed.reasonDisplay,
        feed = null,
        navDestination = entry.navDestination,
        isFiltered = false,
        localContentId = entry.result.contentId,
        localFeedId = entry.feed.id,
        localReason = entry.result.reason.name,
    )

    private suspend fun generateFallbackContent(context: Context) {
        val fallbackItems = listOf(
            FeedDisplayItem(
                title = context.getString(R.string.local_fallback_pool_title),
                summary = context.getString(R.string.local_fallback_pool_summary),
                details = context.getString(R.string.local_fallback_pool_details),
                feed = null,
                isFiltered = false,
            ),
            FeedDisplayItem(
                title = context.getString(R.string.local_fallback_privacy_title),
                summary = context.getString(R.string.local_fallback_privacy_summary),
                details = context.getString(R.string.local_fallback_privacy_details),
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
        context: Context,
        feed: Feed,
    ) {
    }

    override fun onUiContentClick(context: Context, feed: Feed, item: FeedDisplayItem) {
    }
}
