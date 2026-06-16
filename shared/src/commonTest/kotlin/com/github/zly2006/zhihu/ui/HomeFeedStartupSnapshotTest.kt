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
import com.github.zly2006.zhihu.shared.data.CommonFeed
import com.github.zly2006.zhihu.shared.data.DataHolder
import com.github.zly2006.zhihu.shared.data.Feed
import com.github.zly2006.zhihu.shared.data.FeedDisplayItem
import com.github.zly2006.zhihu.shared.data.Person
import com.github.zly2006.zhihu.shared.data.SegmentInfoMark
import com.github.zly2006.zhihu.shared.data.SegmentInfoMeta
import com.github.zly2006.zhihu.shared.data.SegmentInfoParagraph
import com.github.zly2006.zhihu.shared.data.ZhihuJson
import com.github.zly2006.zhihu.shared.data.toFeedDisplayItemNavDestinationJson
import kotlinx.serialization.decodeFromString
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class HomeFeedStartupSnapshotTest {
    @Test
    fun snapshotRoundTripKeepsCompleteFeedDisplayItem() {
        val question = Feed.QuestionTarget(
            id = 1,
            _title = "缓存问题",
            url = "https://www.zhihu.com/question/1",
            type = "question",
            answerCount = 12,
            followerCount = 34,
        )
        val author = Person(
            id = "author-1",
            url = "https://www.zhihu.com/people/author-1",
            userType = "people",
            urlToken = "author-1",
            name = "作者",
            headline = "签名",
            avatarUrl = "https://example.com/avatar.jpg",
            followersCount = 99,
        )
        val feed = CommonFeed(
            id = "feed-1",
            target = Feed.AnswerTarget(
                id = 2,
                url = "https://www.zhihu.com/question/1/answer/2",
                author = author,
                voteupCount = 42,
                commentCount = 5,
                question = question,
                excerpt = "原始回答摘要",
                content = "原始回答正文",
            ),
            actionText = "朋友赞同",
        )
        val item = FeedDisplayItem(
            title = "离线首页条目",
            summary = "上次退出前看到的摘要",
            details = "作者 · 42 赞同",
            feed = feed,
            navDestinationJson = Search(query = "home-cache").toFeedDisplayItemNavDestinationJson(),
            avatarSrc = "https://example.com/avatar.jpg",
            authorName = "作者",
            authorBadgeV2 = DataHolder.BadgeV2(title = "认证"),
            isFiltered = false,
            content = "https://example.com/content",
            raw = DataHolder.DummyContent,
            localContentId = "content-1",
            localFeedId = "home-cache-1",
            localReason = "离线推荐",
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

        val payload = assertNotNull(encodeHomeFeedStartupSnapshot(listOf(item)))
        val restored = ZhihuJson.json.decodeFromString<List<FeedDisplayItem>>(payload)

        assertEquals(1, restored.size)
        assertEquals(item.title, restored.single().title)
        assertEquals(item.summary, restored.single().summary)
        assertEquals(item.details, restored.single().details)
        assertEquals(item.feed, restored.single().feed)
        assertEquals(item.navDestinationJson, restored.single().navDestinationJson)
        assertEquals(item.avatarSrc, restored.single().avatarSrc)
        assertEquals(item.authorName, restored.single().authorName)
        assertEquals(item.authorBadgeV2, restored.single().authorBadgeV2)
        assertEquals(item.isFiltered, restored.single().isFiltered)
        assertEquals(item.content, restored.single().content)
        assertEquals(item.raw, restored.single().raw)
        assertEquals(item.localContentId, restored.single().localContentId)
        assertEquals(item.localFeedId, restored.single().localFeedId)
        assertEquals(item.localReason, restored.single().localReason)
        assertEquals(item.sourceLabel, restored.single().sourceLabel)
        assertEquals(item.segmentInfos, restored.single().segmentInfos)
        assertEquals(item.segmentSourceUrl, restored.single().segmentSourceUrl)
    }

    @Test
    fun snapshotIsBoundedForFileStorage() {
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

        assertEquals(10, restored.size)
        assertEquals("item-0", restored.first().localFeedId)
        assertEquals("item-9", restored.last().localFeedId)
    }

    @Test
    fun invalidSnapshotReturnsEmptyList() {
        assertEquals(emptyList(), decodeHomeFeedStartupSnapshot("{bad json"))
    }

    @Test
    fun emptySnapshotDoesNotProduceCachePayload() {
        assertNull(encodeHomeFeedStartupSnapshot(emptyList()))
    }

    @Test
    fun cacheFallbackDoesNotReplaceNewFeed() {
        val freshItems = mutableListOf(feedItem("fresh"))
        val cachedItems = listOf(feedItem("cached"))
        if (freshItems.isEmpty() && cachedItems.isNotEmpty()) {
            freshItems.addAll(cachedItems)
        }

        assertEquals(listOf("fresh"), freshItems.map { it.localFeedId })
    }

    @Test
    fun cacheFallbackRestoresOnlyWhenNoFreshFeedExists() {
        val items = mutableListOf<FeedDisplayItem>()
        val cachedItems = listOf(feedItem("cached"))
        if (items.isEmpty() && cachedItems.isNotEmpty()) {
            items.addAll(cachedItems)
        }

        assertEquals(listOf("cached"), items.map { it.localFeedId })
    }

    private fun feedItem(id: String): FeedDisplayItem = FeedDisplayItem(
        title = id,
        summary = null,
        details = "详情",
        feed = null,
        localFeedId = id,
    )
}
