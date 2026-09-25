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

package com.github.zly2006.zhihu.viewmodel.filter

import com.github.zly2006.zhihu.data.CommonFeed
import com.github.zly2006.zhihu.data.Feed
import com.github.zly2006.zhihu.data.FeedDisplayItem
import com.github.zly2006.zhihu.data.Person
import com.github.zly2006.zhihu.data.toFeedDisplayItemNavDestinationJson
import com.github.zly2006.zhihu.navigation.Article
import com.github.zly2006.zhihu.navigation.ArticleType
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.io.path.createTempDirectory
import kotlin.test.Test
import kotlin.test.assertEquals

class ForegroundReadFilterPipelineTest {
    @Test
    fun disabledOrReverseBlockReturnsItemsWithoutRecording() = runTest {
        val fixture = fixture(settings = FeedFilterSettings(enableContentFilter = false))
        val item = item("item", 1)

        assertEquals(listOf(item), fixture.pipeline().filter(listOf(item)))
        assertEquals(0, fixture.database.contentFilterDao().getRecordCount())
        fixture.database.close()
    }

    @Test
    fun keepsUnviewedNormalItemAndRecordsView() = runTest {
        val fixture = fixture()
        val item = item("item", 1, details = "文章 · 100 赞")

        val result = fixture.pipeline().filter(listOf(item))

        assertEquals(listOf("item"), result.map { it.title })
        assertEquals(1, fixture.database.contentFilterDao().getRecordCount())
        assertEquals(
            emptyList(),
            fixture.database
                .blockedFeedRecordDao()
                .observeAll()
                .first(),
        )
        fixture.database.close()
    }

    @Test
    fun blocksAlreadyViewedUnfollowedItemAndStoresHistory() = runTest {
        val fixture = fixture()
        val item = item("item", 1)
        fixture.manager.recordContentView("article", "1")

        val result = fixture.pipeline().filter(listOf(item))

        assertEquals(emptyList(), result)
        assertEquals(
            listOf("已读过且未关注作者"),
            fixture.database
                .blockedFeedRecordDao()
                .observeAll()
                .first()
                .map { it.blockedReason },
        )
        fixture.database.close()
    }

    @Test
    fun blocksRemotelyOpenedItemWithoutPretendingItWasExposedLocally() = runTest {
        val fixture = fixture()
        val item = item("remote", 2)
        fixture.database.contentOpenEventDao().insert(
            ContentOpenEvent(contentType = "article", contentId = "2", openFrom = "remote_sync"),
        )

        assertEquals(
            listOf("article:2"),
            fixture.database.contentOpenEventDao().getRemotelySyncedContentKeysByKeys(listOf("article:2")),
        )
        assertEquals(emptyList(), fixture.pipeline().filter(listOf(item)))
        assertEquals(0, fixture.database.contentFilterDao().getRecordCount())
        fixture.database.close()
    }

    @Test
    fun openedKeysAreDistinctAndKeepTypesAndSourcesSeparate() = runTest {
        val fixture = fixture()
        try {
            val dao = fixture.database.contentOpenEventDao()
            repeat(2) { dao.insert(ContentOpenEvent(contentType = "article", contentId = "2", openFrom = "remote_sync")) }
            dao.insert(ContentOpenEvent(contentType = "answer", contentId = "2", openFrom = "home_feed"))
            val keys = listOf("article:2", "answer:2", "article:3")
            assertEquals(listOf("answer:2", "article:2"), dao.getOpenedContentKeysByKeys(keys).sorted())
            assertEquals(listOf("article:2"), dao.getRemotelySyncedContentKeysByKeys(keys))
            assertEquals(emptyList(), dao.getOpenedContentKeysByKeys(emptyList()))
            assertEquals(
                listOf("article:2"),
                dao.getOpenedContentKeysByKeys((1..1100).map { "article:$it" }),
            )
        } finally {
            fixture.database.close()
        }
    }

    @Test
    fun keepsFollowedItemEvenWhenAlreadyViewedOrLowQuality() = runTest {
        val fixture = fixture()
        val item = item("followed", 1, details = "1 分钟前", isFollowing = true)
        fixture.manager.recordContentView("article", "1")

        val result = fixture.pipeline().filter(listOf(item))

        assertEquals(listOf("followed"), result.map { it.title })
        assertEquals(
            emptyList(),
            fixture.database
                .blockedFeedRecordDao()
                .observeAll()
                .first(),
        )
        fixture.database.close()
    }

    @Test
    fun contentExposureRecorderKeepsOldReadsAndMarksInteraction() = runTest {
        val fixture = fixture()

        fixture.pipeline().filter(listOf(item("item", 1, details = "文章 · 100 赞")))
        fixture.manager.recordContentInteraction("article", "1")

        val record = fixture.database.contentFilterDao().getViewRecord("article:1")
        assertEquals(true, record?.hasInteraction)
        fixture.database.contentFilterDao().insertOrUpdateViewRecord(
            ContentViewRecord(
                id = "article:99",
                targetType = "article",
                targetId = "99",
                firstViewTime = 0L,
                lastViewTime = 0L,
            ),
        )
        assertEquals(
            0L,
            fixture.database
                .contentFilterDao()
                .getViewRecord("article:99")
                ?.lastViewTime,
        )
        fixture.database.close()
    }

    private fun fixture(settings: FeedFilterSettings = FeedFilterSettings()): Fixture {
        val database = getContentFilterDatabase(
            createTempDirectory("foreground-read-filter-pipeline").resolve("content-filter.db").toFile(),
        )
        return Fixture(database, settings)
    }

    private class Fixture(
        val database: ContentFilterDatabase,
        val settings: FeedFilterSettings,
    ) {
        val manager = ContentFilterManager(database.contentFilterDao())

        fun pipeline(): ForegroundReadFilterPipeline = ForegroundReadFilterPipeline(
            settings = settings,
            contentFilterManager = manager,
            contentOpenEventDao = database.contentOpenEventDao(),
            blockedFeedRecordDao = database.blockedFeedRecordDao(),
        )
    }

    private fun item(
        title: String,
        id: Long,
        details: String = "",
        isFollowing: Boolean = false,
    ): FeedDisplayItem = FeedDisplayItem(
        title = title,
        summary = null,
        details = details,
        feed = CommonFeed(
            target = Feed.ArticleTarget(
                id = id,
                url = "",
                author = person(isFollowing),
                title = title,
            ),
        ),
        navDestinationJson = Article(type = ArticleType.Article, id = id).toFeedDisplayItemNavDestinationJson(),
    )

    private fun person(isFollowing: Boolean): Person = Person(
        id = "author-id",
        url = "",
        userType = "people",
        name = "author",
        headline = "",
        avatarUrl = "",
        isFollowing = isFollowing,
    )
}
