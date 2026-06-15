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

import com.github.zly2006.zhihu.navigation.Search
import com.github.zly2006.zhihu.shared.data.DataHolder
import com.github.zly2006.zhihu.shared.data.FeedDisplayItem
import com.github.zly2006.zhihu.shared.data.SegmentInfoMark
import com.github.zly2006.zhihu.shared.data.SegmentInfoMeta
import com.github.zly2006.zhihu.shared.data.SegmentInfoParagraph
import com.github.zly2006.zhihu.shared.data.toFeedDisplayItemNavDestinationJson
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class HomeFeedStartupSnapshotTest {
    @Test
    fun snapshotRoundTripKeepsDisplayFieldsWithoutRuntimePayloads() {
        val item = FeedDisplayItem(
            title = "离线首页条目",
            summary = "上次退出前看到的摘要",
            details = "作者 · 42 赞同",
            feed = null,
            navDestinationJson = Search(query = "home-cache").toFeedDisplayItemNavDestinationJson(),
            avatarSrc = "https://example.com/avatar.jpg",
            authorName = "作者",
            authorBadgeV2 = DataHolder.BadgeV2(title = "认证"),
            isFiltered = false,
            content = "https://example.com/content",
            raw = DataHolder.DummyContent,
            localFeedId = "home-cache-1",
            sourceLabel = "朋友赞同",
            segmentInfos = listOf(
                SegmentInfoParagraph(
                    pid = "0",
                    text = "段评文本",
                    marks = listOf(
                        SegmentInfoMark(
                            startIndex = 0,
                            endIndex = 2,
                            segInfo = SegmentInfoMeta(segIds = listOf("seg-1"), likeCount = 3),
                        ),
                    ),
                ),
            ),
            segmentSourceUrl = "https://www.zhihu.com/question/1/answer/2",
        )

        val restored = decodeHomeFeedStartupSnapshot(assertNotNull(encodeHomeFeedStartupSnapshot(listOf(item))))

        assertEquals(1, restored.size)
        assertEquals(item.title, restored.single().title)
        assertEquals(item.summary, restored.single().summary)
        assertEquals(item.details, restored.single().details)
        assertEquals(item.navDestinationJson, restored.single().navDestinationJson)
        assertEquals(item.avatarSrc, restored.single().avatarSrc)
        assertEquals(item.authorName, restored.single().authorName)
        assertEquals(item.authorBadgeV2, restored.single().authorBadgeV2)
        assertEquals(item.localFeedId, restored.single().localFeedId)
        assertEquals(item.sourceLabel, restored.single().sourceLabel)
        assertEquals(item.segmentInfos, restored.single().segmentInfos)
        assertEquals(item.segmentSourceUrl, restored.single().segmentSourceUrl)
        assertNull(restored.single().feed)
        assertNull(restored.single().raw)
    }

    @Test
    fun snapshotIsBoundedForPreferenceStorage() {
        val items = List(120) { index ->
            FeedDisplayItem(
                title = "条目 $index",
                summary = null,
                details = "详情",
                feed = null,
                localFeedId = "item-$index",
            )
        }

        val restored = decodeHomeFeedStartupSnapshot(assertNotNull(encodeHomeFeedStartupSnapshot(items)))

        assertEquals(80, restored.size)
        assertEquals("item-0", restored.first().localFeedId)
        assertEquals("item-79", restored.last().localFeedId)
    }

    @Test
    fun invalidSnapshotReturnsEmptyList() {
        assertEquals(emptyList(), decodeHomeFeedStartupSnapshot("{bad json"))
    }
}
