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

import com.github.zly2006.zhihu.shared.data.DataHolder
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
        database.blockedUserDao().insertUser(BlockedUser(userId = "blocked-user", userName = "Blocked"))
        database.blockedKeywordDao().insertKeyword(BlockedKeyword(keyword = "blocked keyword"))
        database.blockedTopicDao().insertTopic(BlockedTopic(topicId = "blocked-topic", topicName = "Blocked Topic"))

        val keywordService = BlockedKeywordService(
            keywordDao = database.blockedKeywordDao(),
            recordDao = database.blockedContentRecordDao(),
            semanticMatcher = KeywordSemanticMatcher { text, phrases, _ ->
                phrases.filter { text.contains("nlp hit") }.map { it to 0.95 }
            },
        )
        database.blockedKeywordDao().insertKeyword(
            BlockedKeyword(
                keyword = "nlp phrase",
                keywordType = KeywordType.NLP_SEMANTIC.name,
            ),
        )

        val notified = mutableListOf<List<FilterableContent>>()
        val result = FeedContentFilterPipeline(
            settings = FeedFilterSettings(),
            blockedKeywordDao = database.blockedKeywordDao(),
            blockedUserDao = database.blockedUserDao(),
            blockedTopicDao = database.blockedTopicDao(),
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
        database.blockedKeywordDao().insertKeyword(
            BlockedKeyword(
                keyword = "nlp phrase",
                keywordType = KeywordType.NLP_SEMANTIC.name,
            ),
        )

        val result = FeedContentFilterPipeline(
            settings = FeedFilterSettings(),
            blockedKeywordDao = database.blockedKeywordDao(),
            blockedUserDao = database.blockedUserDao(),
            blockedTopicDao = database.blockedTopicDao(),
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

    private fun filterable(
        title: String,
        content: String = title,
        authorId: String,
        topicId: String? = null,
    ): FilterableContent = FilterableContent(
        title = title,
        summary = null,
        content = content,
        authorName = "author",
        authorId = authorId,
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
