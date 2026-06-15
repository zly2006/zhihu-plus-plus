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
import com.github.zly2006.zhihu.shared.data.AdvertisementFeed
import com.github.zly2006.zhihu.shared.data.CommonFeed
import com.github.zly2006.zhihu.shared.data.DataHolder
import com.github.zly2006.zhihu.shared.data.Feed
import com.github.zly2006.zhihu.shared.data.FeedDisplayItem
import com.github.zly2006.zhihu.shared.data.OfficialBadge
import com.github.zly2006.zhihu.shared.data.Person
import com.github.zly2006.zhihu.shared.data.toFeedDisplayItemNavDestinationJson
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.buildJsonObject
import kotlin.io.path.createTempDirectory
import kotlin.test.Test
import kotlin.test.assertEquals

class FeedDisplayFilterPipelineTest {
    @Test
    fun keepsAllowedItemsWithFetchedRawContentAndStoresBlockedRecords() = runTest {
        val fixture = fixture()
        val paidItem = item("paid", 1)
        val keptItem = item("kept", 2)

        val result = fixture
            .pipeline(
                detailProvider = provider(
                    1L to article("paid", paid = true),
                    2L to article("kept"),
                ),
            ).filter(listOf(paidItem, keptItem))

        assertEquals(listOf("kept"), result.map { it.title })
        assertEquals("kept", (result.single().raw as DataHolder.Article).title)
        assertEquals(
            listOf("知乎盐选付费内容"),
            fixture.database
                .blockedFeedRecordDao()
                .observeAll()
                .first()
                .map { it.blockedReason },
        )
        fixture.database.close()
    }

    @Test
    fun keepsFollowedUserItemsOutOfFilteringWhenSettingDisabled() = runTest {
        val fixture = fixture()
        var fetchCount = 0
        val followedItem = item(
            title = "followed",
            id = 1,
            feed = CommonFeed(
                target = Feed.ArticleTarget(
                    id = 1,
                    author = person(isFollowing = true),
                    title = "followed",
                    content = "",
                    excerpt = "",
                    url = "",
                    created = 1,
                    updated = 1,
                    voteupCount = 0,
                ),
            ),
        )

        val result = fixture
            .pipeline(
                detailProvider = ContentDetailProvider {
                    fetchCount++
                    article("blocked keyword")
                },
            ).filter(listOf(followedItem))

        assertEquals(listOf("followed"), result.map { it.title })
        assertEquals(0, fetchCount)
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
    fun reverseBlockReturnsDetectedAdsAndAdvertisementFeeds() = runTest {
        val fixture = fixture(settings = FeedFilterSettings(reverseBlock = true))
        val paidItem = item("paid", 1)
        val normalItem = item("normal", 2)
        val advertisementItem = FeedDisplayItem(
            title = "ad feed",
            summary = null,
            details = "广告",
            feed = AdvertisementFeed(
                ad = AdvertisementFeed.Ad(
                    creatives = listOf(AdvertisementFeed.Creative("https://ad.example", "ad", "ad")),
                ),
            ),
        )

        val result = fixture
            .pipeline(
                detailProvider = provider(
                    1L to article("paid", paid = true),
                    2L to article("normal"),
                ),
            ).filter(listOf(paidItem, normalItem, advertisementItem))

        assertEquals(listOf("paid", "ad feed"), result.map { it.title })
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
    fun detailsPostFilterDoesNotWriteBlockedHistory() = runTest {
        val fixture = fixture()
        val result = fixture
            .pipeline(
                detailProvider = provider(1L to article("buy")),
            ).filter(
                listOf(item("buy", 1, details = "购买")),
            )

        assertEquals(emptyList(), result)
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
    fun databaseHydratesCachedAuthorProfileFromFeedAuthorToken() = runTest {
        val fixture = fixture()
        fixture.database
            .createBlocklistManager()
            .cacheMcnAuthorProfile(
                urlToken = "cached-author",
                userName = "cached author",
                profile = McnAuthorProfile(
                    mcnCompany = "杭州含章文化传播有限公司",
                    officialBadge = OfficialBadge(
                        title = "优秀答主",
                        description = "数码话题下的优秀答主",
                        iconUrl = "https://pic.example/cached-badge.png",
                    ),
                ),
            )
        val feedItem = item(
            title = "cached badge",
            id = 1,
            feed = articleFeed(author = person(isFollowing = false, urlToken = "cached-author")),
        )

        val result = fixture.database.hydrateCachedAuthorProfiles(listOf(feedItem))

        assertEquals("https://pic.example/cached-badge.png", result.single().authorOfficialBadge?.iconUrl)
        assertEquals("杭州含章文化传播有限公司", result.single().authorMcnCompany)
        fixture.database.close()
    }

    @Test
    fun cachedBadgeDoesNotReplaceExistingAuthorBadge() = runTest {
        val fixture = fixture()
        fixture.database
            .createBlocklistManager()
            .cacheMcnAuthorProfile(
                urlToken = "cached-author",
                userName = "cached author",
                profile = McnAuthorProfile(
                    mcnCompany = "缓存MCN机构",
                    officialBadge = OfficialBadge(
                        title = "缓存徽章",
                        description = "缓存徽章",
                        iconUrl = "https://pic.example/cached-badge.png",
                    ),
                ),
            )
        val existingBadge = OfficialBadge(
            title = "接口徽章",
            description = "接口徽章",
            iconUrl = "https://pic.example/api-badge.png",
        )
        val feedItem = item(
            title = "existing badge",
            id = 1,
            feed = articleFeed(author = person(isFollowing = false, urlToken = "cached-author")),
        ).copy(authorOfficialBadge = existingBadge)

        val result = fixture.database.hydrateCachedAuthorProfiles(listOf(feedItem))

        assertEquals("https://pic.example/api-badge.png", result.single().authorOfficialBadge?.iconUrl)
        assertEquals("缓存MCN机构", result.single().authorMcnCompany)
        fixture.database.close()
    }

    @Test
    fun feedDisplayPipelineHydratesCachedBadgeForFollowedItemsSkippedByFilters() = runTest {
        val fixture = fixture()
        fixture.database
            .createBlocklistManager()
            .cacheMcnAuthorProfile(
                urlToken = "followed-author",
                userName = "followed author",
                profile = McnAuthorProfile(
                    mcnCompany = "关注作者MCN",
                    officialBadge = OfficialBadge(
                        title = "社区成就",
                        description = "知势榜领域影响力榜答主",
                        iconUrl = "https://pic.example/followed-badge.png",
                    ),
                ),
            )
        val followedItem = item(
            title = "followed",
            id = 1,
            feed = articleFeed(author = person(isFollowing = true, urlToken = "followed-author")),
        )
        var fetchCount = 0

        val result = fixture.database.filterFeedDisplayItems(
            settings = FeedFilterSettings(),
            items = listOf(followedItem),
            contentDetailProvider = ContentDetailProvider {
                fetchCount++
                article("unexpected")
            },
            semanticMatcher = KeywordSemanticMatcher { _, _, _ -> emptyList() },
        )

        assertEquals(0, fetchCount)
        assertEquals("https://pic.example/followed-badge.png", result.single().authorOfficialBadge?.iconUrl)
        assertEquals("关注作者MCN", result.single().authorMcnCompany)
        fixture.database.close()
    }

    @Test
    fun authorCacheWithOnlyBadgeUsesPositiveTtl() {
        val cache = McnAuthorCache(
            urlToken = "cached-author",
            badgeIconUrl = "https://pic.example/cached-badge.png",
            checkedTime = 1_000L,
        )

        assertEquals(false, cache.isExpired(nowMillis = 2L * 24 * 60 * 60 * 1000 + 1_000L))
    }

    @Test
    fun databaseFactoryWiresFeedDisplayPipelineServices() = runTest {
        val fixture = fixture()
        val keywordService = BlockedKeywordService(
            keywordDao = fixture.database.blockedKeywordDao(),
            recordDao = fixture.database.blockedContentRecordDao(),
            semanticMatcher = KeywordSemanticMatcher { text, phrases, _ ->
                phrases.filter { text.contains("semantic body") }.map { it to 0.95 }
            },
        )
        keywordService.addNLPPhrase("semantic phrase")

        val result = fixture.database
            .filterFeedDisplayItems(
                settings = FeedFilterSettings(),
                items = listOf(item("semantic", 1)),
                contentDetailProvider = provider(1L to article("semantic", content = "<p>semantic body</p>")),
                semanticMatcher = KeywordSemanticMatcher { text, phrases, _ ->
                    phrases.filter { text.contains("semantic body") }.map { it to 0.95 }
                },
            )

        assertEquals(emptyList(), result)
        assertEquals(
            listOf("NLP语义屏蔽：semantic phrase"),
            fixture.database
                .blockedFeedRecordDao()
                .observeAll()
                .first()
                .map { it.blockedReason },
        )
        fixture.database.close()
    }

    private fun fixture(settings: FeedFilterSettings = FeedFilterSettings()): Fixture {
        val database = getContentFilterDatabase(
            createTempDirectory("feed-display-filter-pipeline").resolve("content-filter.db").toFile(),
        )
        return Fixture(database, settings)
    }

    private class Fixture(
        val database: ContentFilterDatabase,
        val settings: FeedFilterSettings,
    ) {
        fun pipeline(
            detailProvider: ContentDetailProvider,
        ): FeedDisplayFilterPipeline = FeedDisplayFilterPipeline(
            settings = settings,
            contentDetailProvider = detailProvider,
            contentFilterPipeline = FeedContentFilterPipeline(
                settings = settings,
                blocklistService = BlocklistService(
                    keywordDao = database.blockedKeywordDao(),
                    userDao = database.blockedUserDao(),
                    topicDao = database.blockedTopicDao(),
                ),
                blockedKeywordService = BlockedKeywordService(
                    keywordDao = database.blockedKeywordDao(),
                    recordDao = database.blockedContentRecordDao(),
                    semanticMatcher = KeywordSemanticMatcher { _, _, _ -> emptyList() },
                ),
            ),
            blockedFeedRecordDao = database.blockedFeedRecordDao(),
        )
    }

    private fun provider(
        vararg contents: Pair<Long, DataHolder.Content>,
    ): ContentDetailProvider {
        val contentById = contents.toMap()
        return ContentDetailProvider { destination ->
            contentById[(destination as Article).id]
        }
    }

    private fun item(
        title: String,
        id: Long,
        details: String = "",
        feed: Feed? = null,
    ): FeedDisplayItem = FeedDisplayItem(
        title = title,
        summary = null,
        details = details,
        feed = feed,
        navDestinationJson = Article(type = ArticleType.Article, id = id).toFeedDisplayItemNavDestinationJson(),
    )

    private fun articleFeed(author: Person): Feed = CommonFeed(
        target = Feed.ArticleTarget(
            id = 1,
            author = author,
            title = "title",
            content = "",
            excerpt = "",
            url = "",
            created = 1,
            updated = 1,
            voteupCount = 10,
        ),
    )

    private fun article(
        title: String,
        content: String = title,
        paid: Boolean = false,
    ): DataHolder.Article = DataHolder.Article(
        id = title.hashCode().toLong(),
        author = author(),
        canComment = DataHolder.CanComment(status = true, reason = ""),
        title = title,
        content = content,
        excerpt = "",
        type = "article",
        created = 1L,
        updated = 1L,
        url = "https://www.zhihu.com/p/$title",
        voteupCount = 0,
        paidInfo = if (paid) buildJsonObject { } else null,
    )

    private fun author(): DataHolder.Author = DataHolder.Author(
        avatarUrl = "",
        gender = 0,
        headline = "",
        id = "author-id",
        isAdvertiser = false,
        isOrg = false,
        name = "author",
        type = "people",
        url = "",
        urlToken = "author",
        userType = "people",
    )

    private fun person(
        isFollowing: Boolean,
        urlToken: String = "author",
    ): Person = Person(
        id = "author-id",
        url = "",
        userType = "people",
        urlToken = urlToken,
        name = "author",
        headline = "",
        avatarUrl = "",
        isFollowing = isFollowing,
    )
}
