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

package com.github.zly2006.zhihu.ui

import com.github.zly2006.zhihu.shared.data.DataHolder
import com.github.zly2006.zhihu.shared.data.FeedDisplayItem
import com.github.zly2006.zhihu.shared.data.SegmentInfoParagraph
import com.github.zly2006.zhihu.shared.data.ZhihuJson
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString

const val AUTO_REFRESH_HOME_ON_STARTUP_PREFERENCE_KEY = "autoRefreshHomeOnStartup"
const val HOME_FEED_STARTUP_SNAPSHOT_PREFERENCE_KEY = "homeFeedStartupSnapshot"

private const val HOME_FEED_STARTUP_SNAPSHOT_MAX_ITEMS = 80

@Serializable
private data class HomeFeedStartupSnapshot(
    val items: List<HomeFeedStartupSnapshotItem>,
)

@Serializable
private data class HomeFeedStartupSnapshotItem(
    val title: String,
    val summary: String?,
    val details: String,
    val navDestinationJson: String?,
    val avatarSrc: String?,
    val authorName: String?,
    val authorBadgeV2: DataHolder.BadgeV2?,
    val isFiltered: Boolean,
    val content: String?,
    val localContentId: String?,
    val localFeedId: String?,
    val localReason: String?,
    val sourceLabel: String?,
    val segmentInfos: List<SegmentInfoParagraph>,
    val segmentSourceUrl: String?,
)

fun encodeHomeFeedStartupSnapshot(items: List<FeedDisplayItem>): String? {
    val snapshotItems = items
        .take(HOME_FEED_STARTUP_SNAPSHOT_MAX_ITEMS)
        .map { it.toStartupSnapshotItem() }
    if (snapshotItems.isEmpty()) return null

    return runCatching {
        ZhihuJson.json.encodeToString(HomeFeedStartupSnapshot(snapshotItems))
    }.getOrNull()
}

fun decodeHomeFeedStartupSnapshot(serialized: String?): List<FeedDisplayItem> {
    if (serialized.isNullOrBlank()) return emptyList()

    return runCatching {
        ZhihuJson
            .json
            .decodeFromString<HomeFeedStartupSnapshot>(serialized)
            .items
            .map { it.toDisplayItem() }
    }.getOrDefault(emptyList())
}

private fun FeedDisplayItem.toStartupSnapshotItem(): HomeFeedStartupSnapshotItem = HomeFeedStartupSnapshotItem(
    title = title,
    summary = summary,
    details = details,
    navDestinationJson = navDestinationJson,
    avatarSrc = avatarSrc,
    authorName = authorName,
    authorBadgeV2 = authorBadgeV2,
    isFiltered = isFiltered,
    content = content,
    localContentId = localContentId,
    localFeedId = localFeedId,
    localReason = localReason,
    sourceLabel = sourceLabel,
    segmentInfos = segmentInfos,
    segmentSourceUrl = segmentSourceUrl,
)

private fun HomeFeedStartupSnapshotItem.toDisplayItem(): FeedDisplayItem = FeedDisplayItem(
    title = title,
    summary = summary,
    details = details,
    feed = null,
    navDestinationJson = navDestinationJson,
    avatarSrc = avatarSrc,
    authorName = authorName,
    authorBadgeV2 = authorBadgeV2,
    isFiltered = isFiltered,
    content = content,
    raw = null,
    localContentId = localContentId,
    localFeedId = localFeedId,
    localReason = localReason,
    sourceLabel = sourceLabel,
    segmentInfos = segmentInfos,
    segmentSourceUrl = segmentSourceUrl,
)
