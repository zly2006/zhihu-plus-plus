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

import com.github.zly2006.zhihu.navigation.Article
import com.github.zly2006.zhihu.navigation.ArticleType
import com.github.zly2006.zhihu.navigation.Question
import com.github.zly2006.zhihu.shared.data.CommonFeed
import com.github.zly2006.zhihu.shared.data.Feed
import com.github.zly2006.zhihu.shared.data.FeedDisplayItem
import com.github.zly2006.zhihu.shared.data.Person
import com.github.zly2006.zhihu.shared.data.toFeedDisplayItemNavDestinationJson
import com.github.zly2006.zhihu.shared.filter.ContentOpenFrom
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.io.path.createTempDirectory
import kotlin.test.Test
import kotlin.test.assertEquals

class ForegroundReadFilterPipelineTest {
    @Test
    fun disabledOrReverseBlockReturnsItemsWithoutRecording() = runTest {
        val fixture = fixture(
            settings = FeedFilterSettings(
                enableContentFilter = false,
                enableRecentlyOpenedContentFilter = true,
            ),
        )
        val item = answerItem("item", 1)
        fixture.database.contentOpenEventDao().insert(opened(ContentType.ANSWER, "1", openedAt = NOW - DAY))

        assertEquals(listOf(item), fixture.pipeline().filter(listOf(item)))
        assertEquals(0, fixture.database.contentFilterDao().getRecordCount())
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
    fun blocksRecentlyOpenedAnswerAndQuestionButKeepsExpiredRecords() = runTest {
        val fixture = fixture(
            settings = FeedFilterSettings(
                enableRecentlyOpenedContentFilter = true,
                recentlyOpenedContentFilterPeriodDays = 7,
            ),
            nowMillis = NOW,
        )
        fixture.database.contentOpenEventDao().insert(opened(ContentType.ANSWER, "1", openedAt = NOW - DAY))
        fixture.database.contentOpenEventDao().insert(opened(ContentType.QUESTION, "2", openedAt = NOW - DAY))
        fixture.database.contentOpenEventDao().insert(opened(ContentType.ANSWER, "3", openedAt = NOW - 8 * DAY))

        val result = fixture.pipeline().filter(
            listOf(
                answerItem("recent answer", 1),
                questionItem("recent question", 2),
                answerItem("expired answer", 3),
            ),
        )

        assertEquals(listOf("expired answer"), result.map { it.title })
        assertEquals(
            listOf("已打开过的内容（7天内）", "已打开过的内容（7天内）"),
            fixture.database
                .blockedFeedRecordDao()
                .observeAll()
                .first()
                .map { it.blockedReason },
        )
        fixture.database.close()
    }

    @Test
    fun blocksAnswerWhenItsQuestionWasOpenedThroughAnotherAnswer() = runTest {
        val fixture = fixture(
            settings = FeedFilterSettings(enableRecentlyOpenedContentFilter = true),
            nowMillis = NOW,
        )
        fixture.database.contentOpenEventDao().insert(
            opened(ContentType.ANSWER, "10", questionId = 99, openedAt = NOW - DAY),
        )

        val result = fixture.pipeline().filter(listOf(answerFeedItem("same question answer", answerId = 11, questionId = 99)))

        assertEquals(emptyList(), result)
        assertEquals(
            listOf("已打开过的内容（7天内）"),
            fixture.database
                .blockedFeedRecordDao()
                .observeAll()
                .first()
                .map { it.blockedReason },
        )
        fixture.database.close()
    }

    @Test
    fun permanentOpenedContentFilterIgnoresOpenedAt() = runTest {
        val fixture = fixture(
            settings = FeedFilterSettings(
                enableRecentlyOpenedContentFilter = true,
                recentlyOpenedContentFilterPeriodDays = RECENTLY_OPENED_CONTENT_FILTER_PERMANENT_DAYS,
            ),
            nowMillis = NOW,
        )
        fixture.database.contentOpenEventDao().insert(opened(ContentType.ANSWER, "1", openedAt = 1L))

        val result = fixture.pipeline().filter(listOf(answerItem("old answer", 1)))

        assertEquals(emptyList(), result)
        assertEquals(
            listOf("已打开过的内容（永久）"),
            fixture.database
                .blockedFeedRecordDao()
                .observeAll()
                .first()
                .map { it.blockedReason },
        )
        fixture.database.close()
    }

    @Test
    fun contentExposureRecorderMarksInteractionAndCleanup() = runTest {
        val fixture = fixture()

        fixture.pipeline().filter(listOf(item("item", 1, details = "文章 · 100 赞")))
        fixture.manager.recordContentInteraction("article", "1")

        val record = fixture.database.contentFilterDao().getViewRecord("article:1")
        assertEquals(true, record?.hasInteraction)
        fixture.database.contentFilterDao().insertOrUpdateViewRecord(
            ContentViewRecord(
                id = "article:old",
                targetType = "article",
                targetId = "old",
                firstViewTime = 0L,
                lastViewTime = 0L,
            ),
        )
        fixture.manager.cleanupOldData()
        assertEquals(null, fixture.database.contentFilterDao().getViewRecord("article:old"))
        fixture.database.close()
    }

    private fun fixture(
        settings: FeedFilterSettings = FeedFilterSettings(),
        nowMillis: Long = NOW,
    ): Fixture {
        val database = getContentFilterDatabase(
            createTempDirectory("foreground-read-filter-pipeline").resolve("content-filter.db").toFile(),
        )
        return Fixture(database, settings, nowMillis)
    }

    private class Fixture(
        val database: ContentFilterDatabase,
        val settings: FeedFilterSettings,
        val nowMillis: Long,
    ) {
        val manager = ContentFilterManager(database.contentFilterDao())

        fun pipeline(): ForegroundReadFilterPipeline = ForegroundReadFilterPipeline(
            settings = settings,
            contentFilterManager = manager,
            blockedFeedRecordDao = database.blockedFeedRecordDao(),
            contentOpenEventDao = database.contentOpenEventDao(),
            nowMillis = { nowMillis },
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

    private fun answerItem(
        title: String,
        id: Long,
    ): FeedDisplayItem = FeedDisplayItem(
        title = title,
        summary = null,
        details = "",
        feed = null,
        navDestinationJson = Article(type = ArticleType.Answer, id = id).toFeedDisplayItemNavDestinationJson(),
    )

    private fun questionItem(
        title: String,
        id: Long,
    ): FeedDisplayItem = FeedDisplayItem(
        title = title,
        summary = null,
        details = "",
        feed = null,
        navDestinationJson = Question(questionId = id, title = title).toFeedDisplayItemNavDestinationJson(),
    )

    private fun answerFeedItem(
        title: String,
        answerId: Long,
        questionId: Long,
    ): FeedDisplayItem = FeedDisplayItem(
        title = title,
        summary = null,
        details = "",
        feed = CommonFeed(
            target = Feed.AnswerTarget(
                id = answerId,
                url = "",
                author = person(false),
                question = questionTarget(title, questionId),
            ),
        ),
    )

    private fun questionTarget(
        title: String,
        id: Long,
    ): Feed.QuestionTarget = Feed.QuestionTarget(
        id = id,
        _title = title,
        url = "",
        type = "question",
    )

    private fun opened(
        contentType: String,
        contentId: String,
        questionId: Long? = null,
        openedAt: Long,
    ): ContentOpenEvent = ContentOpenEvent(
        contentType = contentType,
        contentId = contentId,
        questionId = questionId,
        openFrom = ContentOpenFrom.HOME_FEED,
        openedAt = openedAt,
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

    private companion object {
        const val DAY = 24L * 60L * 60L * 1000L
        const val NOW = 20L * DAY
    }
}
