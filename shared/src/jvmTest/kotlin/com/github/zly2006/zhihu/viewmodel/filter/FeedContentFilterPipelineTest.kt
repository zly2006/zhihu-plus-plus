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
import kotlinx.coroutines.test.runTest
import kotlin.io.path.createTempDirectory
import kotlin.test.Test
import kotlin.test.assertEquals

class FeedContentFilterPipelineTest {
    @Test
    fun filtersByUserKeywordNlpAndTopicInOrder() = runTest {
        val database = getContentFilterDatabase(
            createTempDirectory("feed-content-filter-pipeline").resolve("content-filter.db").toFile(),
        )
        val blocklistService = BlocklistService(
            keywordDao = database.blockedKeywordDao(),
            userDao = database.blockedUserDao(),
            topicDao = database.blockedTopicDao(),
            mcnOrganizationDao = database.blockedMcnOrganizationDao(),
            mcnAuthorCacheDao = database.mcnAuthorCacheDao(),
        )
        blocklistService.addBlockedUser(userId = "blocked-user", userName = "Blocked")
        blocklistService.addBlockedKeyword("blocked keyword")
        blocklistService.addBlockedTopic(topicId = "blocked-topic", topicName = "Blocked Topic")

        val keywordService = BlockedKeywordService(
            keywordDao = database.blockedKeywordDao(),
            recordDao = database.blockedContentRecordDao(),
            semanticMatcher = KeywordSemanticMatcher { text, phrases, _ ->
                phrases.filter { text.contains("nlp hit") }.map { it to 0.95 }
            },
        )
        keywordService.addNLPPhrase("nlp phrase")

        val notified = mutableListOf<List<FilterableContent>>()
        val result = FeedContentFilterPipeline(
            settings = FeedFilterSettings(),
            blocklistService = blocklistService,
            blockedKeywordService = keywordService,
            htmlToText = { it },
            onNlpBlocked = { notified.add(it) },
        ).filter(
            listOf(
                filterable("keep", authorId = "ok-user"),
                filterable("by user", authorId = "blocked-user"),
                filterable("blocked keyword title", authorId = "ok-user"),
                filterable("nlp hit", authorId = "ok-user"),
                filterable("topic", authorId = "ok-user", topicId = "blocked-topic"),
            ),
        )

        assertEquals(listOf("keep"), result.kept.map { it.title })
        assertEquals(
            listOf(
                "屏蔽作者：author",
                "关键词屏蔽",
                "NLP语义屏蔽：nlp phrase",
                "屏蔽主题：Blocked Topic",
            ),
            result.blocked.map { it.second },
        )
        assertEquals(listOf(listOf("nlp hit")), notified.map { list -> list.map { it.title } })
        database.close()
    }

    @Test
    fun nlpFilteringUsesPlainTextFromHtmlByDefault() = runTest {
        val database = getContentFilterDatabase(
            createTempDirectory("feed-content-filter-html").resolve("content-filter.db").toFile(),
        )
        val keywordService = BlockedKeywordService(
            keywordDao = database.blockedKeywordDao(),
            recordDao = database.blockedContentRecordDao(),
            semanticMatcher = KeywordSemanticMatcher { text, phrases, _ ->
                phrases
                    .filter { text.contains("blocked phrase") && !text.contains("<strong>") }
                    .map { it to 0.95 }
            },
        )
        keywordService.addNLPPhrase("nlp phrase")

        val result = FeedContentFilterPipeline(
            settings = FeedFilterSettings(),
            blocklistService = BlocklistService(
                keywordDao = database.blockedKeywordDao(),
                userDao = database.blockedUserDao(),
                topicDao = database.blockedTopicDao(),
                mcnOrganizationDao = database.blockedMcnOrganizationDao(),
                mcnAuthorCacheDao = database.mcnAuthorCacheDao(),
            ),
            blockedKeywordService = keywordService,
        ).filter(
            listOf(
                filterable(
                    title = "html",
                    content = "<p>blocked <strong>phrase</strong></p>",
                    authorId = "ok-user",
                ),
            ),
        )

        assertEquals(emptyList(), result.kept)
        assertEquals(listOf("NLP语义屏蔽：nlp phrase"), result.blocked.map { it.second })
        database.close()
    }

    @Test
    fun filtersByBlockedMcnOrganization() = runTest {
        val database = getContentFilterDatabase(
            createTempDirectory("feed-content-filter-mcn").resolve("content-filter.db").toFile(),
        )
        val blocklistService = BlocklistService(
            keywordDao = database.blockedKeywordDao(),
            userDao = database.blockedUserDao(),
            topicDao = database.blockedTopicDao(),
            mcnOrganizationDao = database.blockedMcnOrganizationDao(),
            mcnAuthorCacheDao = database.mcnAuthorCacheDao(),
        )
        blocklistService.addBlockedMcnOrganization("杭州亚序")

        val result = FeedContentFilterPipeline(
            settings = FeedFilterSettings(),
            blocklistService = blocklistService,
            blockedKeywordService = BlockedKeywordService(
                keywordDao = database.blockedKeywordDao(),
                recordDao = database.blockedContentRecordDao(),
                semanticMatcher = KeywordSemanticMatcher { _, _, _ -> emptyList() },
            ),
            mcnCompanyProvider = McnCompanyProvider { token ->
                when (token) {
                    "blocked-token" -> "杭州亚序"
                    "ok-token" -> "其他机构"
                    else -> null
                }
            },
        ).filter(
            listOf(
                filterable("blocked mcn", authorId = "blocked-user", authorUrlToken = "blocked-token"),
                filterable("other mcn", authorId = "ok-user", authorUrlToken = "ok-token"),
                filterable("no token", authorId = "missing-token", authorUrlToken = null),
            ),
        )

        assertEquals(listOf("other mcn", "no token"), result.kept.map { it.title })
        assertEquals(listOf("屏蔽MCN机构：杭州亚序"), result.blocked.map { it.second })
        database.close()
    }

    @Test
    fun keepsBlockedMcnOrganizationWhenMcnBlockingDisabled() = runTest {
        val database = getContentFilterDatabase(
            createTempDirectory("feed-content-filter-mcn-disabled").resolve("content-filter.db").toFile(),
        )
        val blocklistService = BlocklistService(
            keywordDao = database.blockedKeywordDao(),
            userDao = database.blockedUserDao(),
            topicDao = database.blockedTopicDao(),
            mcnOrganizationDao = database.blockedMcnOrganizationDao(),
            mcnAuthorCacheDao = database.mcnAuthorCacheDao(),
        )
        blocklistService.addBlockedMcnOrganization("杭州亚序")

        val result = FeedContentFilterPipeline(
            settings = FeedFilterSettings(enableMcnBlocking = false),
            blocklistService = blocklistService,
            blockedKeywordService = BlockedKeywordService(
                keywordDao = database.blockedKeywordDao(),
                recordDao = database.blockedContentRecordDao(),
                semanticMatcher = KeywordSemanticMatcher { _, _, _ -> emptyList() },
            ),
            mcnCompanyProvider = McnCompanyProvider { "杭州亚序" },
        ).filter(
            listOf(
                filterable("blocked mcn", authorId = "blocked-user", authorUrlToken = "blocked-token"),
            ),
        )

        assertEquals(listOf("blocked mcn"), result.kept.map { it.title })
        assertEquals(emptyList(), result.blocked)
        database.close()
    }

    @Test
    fun skipsMcnLookupWhenNoMcnRuleIsEnabled() = runTest {
        val database = getContentFilterDatabase(
            createTempDirectory("feed-content-filter-no-mcn-rules").resolve("content-filter.db").toFile(),
        )
        var calls = 0
        val result = FeedContentFilterPipeline(
            settings = FeedFilterSettings(),
            blocklistService = BlocklistService(
                keywordDao = database.blockedKeywordDao(),
                userDao = database.blockedUserDao(),
                topicDao = database.blockedTopicDao(),
                mcnOrganizationDao = database.blockedMcnOrganizationDao(),
                mcnAuthorCacheDao = database.mcnAuthorCacheDao(),
            ),
            blockedKeywordService = BlockedKeywordService(
                keywordDao = database.blockedKeywordDao(),
                recordDao = database.blockedContentRecordDao(),
                semanticMatcher = KeywordSemanticMatcher { _, _, _ -> emptyList() },
            ),
            mcnCompanyProvider = McnCompanyProvider {
                calls += 1
                "杭州亚序"
            },
        ).filter(
            listOf(filterable("normal", authorId = "normal-user", authorUrlToken = "normal-token")),
        )

        assertEquals(listOf("normal"), result.kept.map { it.title })
        assertEquals(emptyList(), result.blocked)
        assertEquals(0, calls)
        database.close()
    }

    @Test
    fun filtersKnownMcnAuthorsByTheirOrganizations() = runTest {
        val knownAuthors = mapOf(
            "jiang-nan-mao-mao-chong" to "知加传媒（深圳）有限公司",
            "lihuawei" to "杭州亚序科技有限公司",
            "qbtu-zi" to "杭州亚序科技有限公司",
            "allen-xu-3" to "杭州亚序科技有限公司",
            "dao-shu-43" to "杭州亚序科技有限公司",
            "wang-yu-ting-29-74" to "杭州亚序科技有限公司",
            "xin-yuan-jia-de-xiao-ling-lai" to "杭州亚序科技有限公司",
            "duncanzhang" to "杭州亚序科技有限公司",
            "dream-boy-60-5" to "杭州亚序科技有限公司",
            "ju-bei-yao-jiu-jing-gu-du-81-39" to "知加传媒（深圳）有限公司",
            "yuan-bei-bei-14" to "杭州含章文化传播有限公司",
            "yang-xi-yu-25-5" to "知加传媒（深圳）有限公司",
            "jin-xiao-cun-wei-yi-bao-49" to "上海友芝士文化传播有限公司",
            "allen-63-88" to "深圳知外文化传播有限公司",
            "mu-cun-shang-chun-shu" to "极合（北京）科技有限公司",
            "lee-lee-1998" to "杭州亚序科技有限公司",
        )
        val database = getContentFilterDatabase(
            createTempDirectory("feed-content-filter-known-mcn").resolve("content-filter.db").toFile(),
        )
        val blocklistService = BlocklistService(
            keywordDao = database.blockedKeywordDao(),
            userDao = database.blockedUserDao(),
            topicDao = database.blockedTopicDao(),
            mcnOrganizationDao = database.blockedMcnOrganizationDao(),
            mcnAuthorCacheDao = database.mcnAuthorCacheDao(),
        )
        knownAuthors.values.distinct().forEach { organization ->
            blocklistService.addBlockedMcnOrganization(organization)
        }

        val result = FeedContentFilterPipeline(
            settings = FeedFilterSettings(),
            blocklistService = blocklistService,
            blockedKeywordService = BlockedKeywordService(
                keywordDao = database.blockedKeywordDao(),
                recordDao = database.blockedContentRecordDao(),
                semanticMatcher = KeywordSemanticMatcher { _, _, _ -> emptyList() },
            ),
            mcnCompanyProvider = McnCompanyProvider { token -> knownAuthors[token] },
        ).filter(
            knownAuthors.keys.map { token ->
                filterable(token, authorId = token, authorUrlToken = token)
            },
        )

        assertEquals(emptyList(), result.kept)
        assertEquals(knownAuthors.size, result.blocked.size)
        assertEquals(
            knownAuthors.values.map { organization -> "屏蔽MCN机构：$organization" }.toSet(),
            result.blocked.map { it.second }.toSet(),
        )
        database.close()
    }

    @Test
    fun filtersAllMcnAuthorsWhenEnabled() = runTest {
        val database = getContentFilterDatabase(
            createTempDirectory("feed-content-filter-all-mcn").resolve("content-filter.db").toFile(),
        )
        val result = FeedContentFilterPipeline(
            settings = FeedFilterSettings(blockAllMcnAuthors = true),
            blocklistService = BlocklistService(
                keywordDao = database.blockedKeywordDao(),
                userDao = database.blockedUserDao(),
                topicDao = database.blockedTopicDao(),
                mcnOrganizationDao = database.blockedMcnOrganizationDao(),
                mcnAuthorCacheDao = database.mcnAuthorCacheDao(),
            ),
            blockedKeywordService = BlockedKeywordService(
                keywordDao = database.blockedKeywordDao(),
                recordDao = database.blockedContentRecordDao(),
                semanticMatcher = KeywordSemanticMatcher { _, _, _ -> emptyList() },
            ),
            mcnCompanyProvider = McnCompanyProvider { token ->
                if (token == "mcn-token") "任意机构" else null
            },
        ).filter(
            listOf(
                filterable("mcn", authorId = "mcn-user", authorUrlToken = "mcn-token"),
                filterable("normal", authorId = "normal-user", authorUrlToken = "normal-token"),
            ),
        )

        assertEquals(listOf("normal"), result.kept.map { it.title })
        assertEquals(listOf("屏蔽MCN机构：任意机构"), result.blocked.map { it.second })
        database.close()
    }

    @Test
    fun ignoresInvalidMcnCompanyValues() = runTest {
        val database = getContentFilterDatabase(
            createTempDirectory("feed-content-filter-invalid-mcn").resolve("content-filter.db").toFile(),
        )
        val result = FeedContentFilterPipeline(
            settings = FeedFilterSettings(blockAllMcnAuthors = true),
            blocklistService = BlocklistService(
                keywordDao = database.blockedKeywordDao(),
                userDao = database.blockedUserDao(),
                topicDao = database.blockedTopicDao(),
                mcnOrganizationDao = database.blockedMcnOrganizationDao(),
                mcnAuthorCacheDao = database.mcnAuthorCacheDao(),
            ),
            blockedKeywordService = BlockedKeywordService(
                keywordDao = database.blockedKeywordDao(),
                recordDao = database.blockedContentRecordDao(),
                semanticMatcher = KeywordSemanticMatcher { _, _, _ -> emptyList() },
            ),
            mcnCompanyProvider = McnCompanyProvider { "false" },
        ).filter(
            listOf(filterable("normal", authorId = "normal-user", authorUrlToken = "normal-token")),
        )

        assertEquals(listOf("normal"), result.kept.map { it.title })
        assertEquals(emptyList(), result.blocked)
        database.close()
    }

    @Test
    fun cachesAuthorsWithoutMcnCompany() = runTest {
        val database = getContentFilterDatabase(
            createTempDirectory("feed-content-filter-no-mcn-cache").resolve("content-filter.db").toFile(),
        )
        var calls = 0
        val blocklistService = BlocklistService(
            keywordDao = database.blockedKeywordDao(),
            userDao = database.blockedUserDao(),
            topicDao = database.blockedTopicDao(),
            mcnOrganizationDao = database.blockedMcnOrganizationDao(),
            mcnAuthorCacheDao = database.mcnAuthorCacheDao(),
        )
        val pipeline = FeedContentFilterPipeline(
            settings = FeedFilterSettings(blockAllMcnAuthors = true),
            blocklistService = blocklistService,
            blockedKeywordService = BlockedKeywordService(
                keywordDao = database.blockedKeywordDao(),
                recordDao = database.blockedContentRecordDao(),
                semanticMatcher = KeywordSemanticMatcher { _, _, _ -> emptyList() },
            ),
            mcnCompanyProvider = McnCompanyProvider {
                calls += 1
                null
            },
        )

        pipeline.filter(listOf(filterable("first", authorId = "normal-user", authorUrlToken = "normal-token")))
        pipeline.filter(listOf(filterable("second", authorId = "normal-user", authorUrlToken = "normal-token")))

        assertEquals(1, calls)
        database.close()
    }

    @Test
    fun deduplicatesMcnLookupForSameAuthorInSingleBatch() = runTest {
        val database = getContentFilterDatabase(
            createTempDirectory("feed-content-filter-mcn-dedup").resolve("content-filter.db").toFile(),
        )
        var calls = 0
        val blocklistService = BlocklistService(
            keywordDao = database.blockedKeywordDao(),
            userDao = database.blockedUserDao(),
            topicDao = database.blockedTopicDao(),
            mcnOrganizationDao = database.blockedMcnOrganizationDao(),
            mcnAuthorCacheDao = database.mcnAuthorCacheDao(),
        )
        blocklistService.addBlockedMcnOrganization("杭州亚序")

        val result = FeedContentFilterPipeline(
            settings = FeedFilterSettings(),
            blocklistService = blocklistService,
            blockedKeywordService = BlockedKeywordService(
                keywordDao = database.blockedKeywordDao(),
                recordDao = database.blockedContentRecordDao(),
                semanticMatcher = KeywordSemanticMatcher { _, _, _ -> emptyList() },
            ),
            mcnCompanyProvider = McnCompanyProvider {
                calls += 1
                "杭州亚序"
            },
        ).filter(
            listOf(
                filterable("first", authorId = "same-user", authorUrlToken = "same-token"),
                filterable("second", authorId = "same-user", authorUrlToken = "same-token"),
            ),
        )

        assertEquals(1, calls)
        assertEquals(emptyList(), result.kept)
        assertEquals(listOf("屏蔽MCN机构：杭州亚序", "屏蔽MCN机构：杭州亚序"), result.blocked.map { it.second })
        database.close()
    }

    @Test
    fun refreshesExpiredMcnCacheEntries() = runTest {
        val database = getContentFilterDatabase(
            createTempDirectory("feed-content-filter-expired-mcn-cache").resolve("content-filter.db").toFile(),
        )
        var calls = 0
        val blocklistService = BlocklistService(
            keywordDao = database.blockedKeywordDao(),
            userDao = database.blockedUserDao(),
            topicDao = database.blockedTopicDao(),
            mcnOrganizationDao = database.blockedMcnOrganizationDao(),
            mcnAuthorCacheDao = database.mcnAuthorCacheDao(),
        )
        blocklistService.cacheMcnCompany("mcn-token", "author", null)
        database.mcnAuthorCacheDao().insert(
            McnAuthorCache(urlToken = "mcn-token", userName = "author", mcnCompany = null, checkedTime = 0L),
        )

        val result = FeedContentFilterPipeline(
            settings = FeedFilterSettings(blockAllMcnAuthors = true),
            blocklistService = blocklistService,
            blockedKeywordService = BlockedKeywordService(
                keywordDao = database.blockedKeywordDao(),
                recordDao = database.blockedContentRecordDao(),
                semanticMatcher = KeywordSemanticMatcher { _, _, _ -> emptyList() },
            ),
            mcnCompanyProvider = McnCompanyProvider {
                calls += 1
                "刷新后的机构"
            },
        ).filter(
            listOf(filterable("expired", authorId = "mcn-user", authorUrlToken = "mcn-token")),
        )

        assertEquals(1, calls)
        assertEquals(emptyList(), result.kept)
        assertEquals(listOf("屏蔽MCN机构：刷新后的机构"), result.blocked.map { it.second })
        assertEquals("刷新后的机构", blocklistService.getCachedMcnAuthor("mcn-token")?.mcnCompany)
        database.close()
    }

    @Test
    fun filterableContentFallsBackToFeedDisplayAuthorUrlToken() {
        val item = FeedDisplayItem(
            title = "answer card",
            summary = "summary",
            details = "details",
            feed = null,
            authorName = "MCN author",
            authorUrlToken = "mcn-token",
        )

        val content = item.toFilterableContent(
            identity = FeedContentIdentity(ContentType.ANSWER, "answer-id"),
            rawContent = DataHolder.DummyContent,
        )

        assertEquals("MCN author", content.authorName)
        assertEquals("mcn-token", content.authorUrlToken)
    }

    private fun filterable(
        title: String,
        content: String = title,
        authorId: String,
        authorUrlToken: String? = "author",
        topicId: String? = null,
    ): FilterableContent = FilterableContent(
        title = title,
        summary = null,
        content = content,
        authorName = "author",
        authorId = authorId,
        authorUrlToken = authorUrlToken,
        contentId = title,
        contentType = "article",
        raw = article(title, content, topicId),
    )

    private fun article(
        title: String,
        body: String,
        topicId: String?,
    ): DataHolder.Article = DataHolder.Article(
        id = title.hashCode().toLong(),
        author = author(),
        canComment = DataHolder.CanComment(status = true, reason = ""),
        title = title,
        content = body,
        excerpt = "",
        type = "article",
        created = 1L,
        updated = 1L,
        url = "https://www.zhihu.com/p/$title",
        voteupCount = 0,
        topics = topicId?.let { listOf(DataHolder.Topic(id = it, type = "topic", url = "", name = it)) },
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
}
