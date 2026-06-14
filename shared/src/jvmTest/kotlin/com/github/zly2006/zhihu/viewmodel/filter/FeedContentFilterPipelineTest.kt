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
import com.github.zly2006.zhihu.shared.data.OfficialBadge
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
    fun filtersByBlockedMcnOrganizationFromAuthorTokenAndCachesLookup() = runTest {
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
        blocklistService.addBlockedMcnOrganization("杭州含章文化传播有限公司")
        blocklistService.cacheMcnAuthorProfile(
            "cached-token",
            "cached author",
            McnAuthorProfile(mcnCompany = "杭州含章文化传播有限公司"),
        )

        val fetchedTokens = mutableListOf<String>()
        val result = FeedContentFilterPipeline(
            settings = FeedFilterSettings(enableMcnBlocking = true),
            blocklistService = blocklistService,
            blockedKeywordService = BlockedKeywordService(
                keywordDao = database.blockedKeywordDao(),
                recordDao = database.blockedContentRecordDao(),
                semanticMatcher = KeywordSemanticMatcher { _, _, _ -> emptyList() },
            ),
            mcnAndBadgeProvider = McnAndBadgeProvider { token ->
                fetchedTokens += token
                when (token) {
                    "network-token" -> McnAuthorProfile(mcnCompany = "杭州含章文化传播有限公司")
                    "plain-token" -> McnAuthorProfile(
                        officialBadge = OfficialBadge(
                            title = "已认证的个人",
                            description = "电脑吧评测室",
                            iconUrl = "https://pic.example/badge.png",
                        ),
                    )
                    else -> McnAuthorProfile()
                }
            },
        ).filter(
            listOf(
                filterable("cached mcn", authorId = "cached-user", authorUrlToken = "cached-token"),
                filterable("network mcn", authorId = "network-user", authorUrlToken = "network-token"),
                filterable("plain author", authorId = "plain-user", authorUrlToken = "plain-token"),
            ),
        )

        assertEquals(listOf("plain author"), result.kept.map { it.title })
        assertEquals(
            listOf("屏蔽MCN机构：杭州含章文化传播有限公司", "屏蔽MCN机构：杭州含章文化传播有限公司"),
            result.blocked.map { it.second },
        )
        assertEquals(listOf("network-token", "plain-token"), fetchedTokens)
        assertEquals(
            "https://pic.example/badge.png",
            result.kept
                .single()
                .authorOfficialBadge
                ?.iconUrl,
        )
        assertEquals("杭州含章文化传播有限公司", blocklistService.getCachedMcnAuthor("network-token")?.mcnCompany)
        assertEquals("https://pic.example/badge.png", blocklistService.getCachedMcnAuthor("plain-token")?.officialBadge?.iconUrl)
        database.close()
    }

    private fun filterable(
        title: String,
        content: String = title,
        authorId: String,
        topicId: String? = null,
        authorUrlToken: String = "author",
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
