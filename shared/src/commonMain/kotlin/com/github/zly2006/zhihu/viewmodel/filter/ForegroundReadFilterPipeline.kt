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

import com.github.zly2006.zhihu.shared.data.DataHolder
import com.github.zly2006.zhihu.shared.data.FeedDisplayItem
import com.github.zly2006.zhihu.shared.data.target

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
