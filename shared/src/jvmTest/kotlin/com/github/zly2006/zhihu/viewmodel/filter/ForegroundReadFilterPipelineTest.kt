package com.github.zly2006.zhihu.viewmodel.filter

import com.github.zly2006.zhihu.navigation.Article
import com.github.zly2006.zhihu.navigation.ArticleType
import com.github.zly2006.zhihu.shared.data.CommonFeed
import com.github.zly2006.zhihu.shared.data.Feed
import com.github.zly2006.zhihu.shared.data.FeedDisplayItem
import com.github.zly2006.zhihu.shared.data.Person
import com.github.zly2006.zhihu.shared.data.toFeedDisplayItemNavDestinationJson
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
        assertEquals(emptyList(), fixture.database.blockedFeedRecordDao().getRecent())
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
                .getRecent()
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
        assertEquals(emptyList(), fixture.database.blockedFeedRecordDao().getRecent())
        fixture.database.close()
    }

    @Test
    fun contentExposureRecorderMarksInteractionAndCleanup() = runTest {
        val fixture = fixture()

        fixture.database.recordContentDisplay(FeedFilterSettings(), "article", "1")
        fixture.database.recordContentInteraction(FeedFilterSettings(), "article", "1")

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
        fixture.database.performContentFilterMaintenanceCleanup(FeedFilterSettings())
        assertEquals(null, fixture.database.contentFilterDao().getViewRecord("article:old"))
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
