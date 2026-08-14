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

package com.github.zly2006.zhihu.viewmodel.feed

import com.github.zly2006.zhihu.data.CommonFeed
import com.github.zly2006.zhihu.data.Feed
import com.github.zly2006.zhihu.data.Person
import com.github.zly2006.zhihu.data.SearchResult
import com.github.zly2006.zhihu.data.ZhihuJson
import com.github.zly2006.zhihu.data.target
import com.github.zly2006.zhihu.viewmodel.PaginationEnvironment
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.Url
import io.ktor.http.headersOf
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonPrimitive
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull

class SearchViewModelTest {
    @Test
    fun globalSearchUrlDoesNotCarryMemberRestriction() {
        val url = Url(SearchViewModel("kmp 搜索").initialRequestUrl)

        assertEquals("kmp 搜索", url.parameters["q"])
        assertNull(url.parameters["restricted_scene"])
        assertNull(url.parameters["restricted_field"])
        assertNull(url.parameters["restricted_value"])
    }

    @Test
    fun memberScopedSearchUrlCarriesZhihuRestrictionFields() {
        val url = Url(SearchViewModel("用户创作", restrictedMemberHashId = "member-hash-id").initialRequestUrl)

        assertEquals("用户创作", url.parameters["q"])
        assertEquals("member", url.parameters["restricted_scene"])
        assertEquals("member_hash_id", url.parameters["restricted_field"])
        assertEquals("member-hash-id", url.parameters["restricted_value"])
        assertEquals("Normal", url.parameters["search_source"])
        assertEquals("0", url.parameters["lc_idx"])
    }

    @Test
    fun topicSearchUsesOfficialVerticalAndShowAllTopics() {
        val url = Url(
            zhihuSearchUrl(
                query = "AI",
                searchTab = SearchTab.Topic,
            ),
        )

        assertEquals("topic", url.parameters["t"])
        assertEquals("1", url.parameters["show_all_topics"])
        assertEquals("AI", url.parameters["q"])
    }

    @Test
    fun decodesObservedTopicSearchShapeThroughZhihuJson() {
        val result = decodeTopicSearchResult(
            ZhihuJson.json.parseToJsonElement(
                """{"type":"search_result","object":{"id":"20106982","name":"<em>AI</em>技术","url":"https://api.zhihu.com/topics/20106982","type":"topic","excerpt":"人工智能（Artificial Intelligence），英文缩写为<em>AI</em>","avatar_url":"https://pic.example/topic.jpg","is_following":false,"visit_count":3359904456,"top_answer_count":1890565}}""",
            ),
        )!!

        assertEquals("20106982", result.topic.id)
        assertEquals("AI技术", result.topic.name)
        assertEquals("人工智能（Artificial Intelligence），英文缩写为AI", result.excerpt)
        assertEquals(3_359_904_456, result.visitCount)
        assertEquals(1_890_565, result.discussCount)
        assertFalse(result.isFollowing)
    }

    @Test
    fun malformedTopicSearchEntriesAreRejectedWithoutThrowing() {
        assertNull(decodeTopicSearchResult(JsonPrimitive("not-an-object")))
        assertNull(
            decodeTopicSearchResult(
                ZhihuJson.json.parseToJsonElement("""{"type":"search_result","object":null}"""),
            ),
        )
    }

    @Test
    fun topicSearchFollowFailureRollsBackRelationship() = runTest {
        val viewModel = SearchViewModel("AI")
        viewModel.topicResults += TopicSearchResult(
            topic = com.github.zly2006.zhihu.data.DataHolder
                .Topic("1", "topic", "zhihu://topic/1", "AI"),
            excerpt = "",
            visitCount = 1,
            discussCount = 1,
            isFollowing = false,
        )
        val environment = object : PaginationEnvironment {
            override fun httpClient() = HttpClient(
                MockEngine {
                    respond("failed", HttpStatusCode.InternalServerError, headersOf(HttpHeaders.ContentType, "text/plain"))
                },
            )

            override fun authenticatedCookies() = mapOf("d_c0" to "test")

            override suspend fun handleFetchFailure(tag: String?, error: Exception) = Unit
        }

        assertFalse(viewModel.setTopicFollowing(environment, "1", true).isSuccess)
        assertFalse(viewModel.topicResults.single().isFollowing)
        assertFalse("1" in viewModel.changingTopicIds)
    }

    @Test
    fun searchResultsExcludeLocallyBlockedAuthors() {
        val viewModel = TestSearchViewModel()
        val blocked = answerFeed(id = 1, authorId = "blocked-user")
        val kept = answerFeed(id = 2, authorId = "kept-user")

        viewModel.process(
            environment = testEnvironment(blockedUserIds = setOf("blocked-user")),
            feeds = listOf(blocked, kept),
        )

        assertEquals(listOf<Feed>(kept), viewModel.allData)
        assertEquals(
            listOf("kept-user"),
            viewModel.displayItems.map {
                it.feed
                    ?.target
                    ?.author
                    ?.id
            },
        )
    }

    @Test
    fun decodesPeopleSearchResultAsStronglyTypedPeople() {
        val result = ZhihuJson.decodeJson<SearchResult>(
            ZhihuJson.json.parseToJsonElement(
                """
                {
                  "type": "search_result",
                  "id": 460104019,
                  "object": {
                    "id": "6733f12c60e7e98ea7491f20de46f79e",
                    "url_token": "zhouyuan",
                    "type": "people",
                    "url": "https://api.zhihu.com/people/6733f12c60e7e98ea7491f20de46f79e",
                "name": "<em>周源</em>",
                    "user_type": "people",
                    "headline": "知乎 001 号员工",
                    "gender": 1,
                    "avatar_url": "https://pic.example/avatar.jpg",
                    "follower_count": 1048300,
                    "answer_count": 371
                  }
                }
                """.trimIndent(),
            ),
        )

        assertEquals("zhouyuan", result.people?.people?.urlToken)
        assertEquals("周源", result.people?.people?.name)
        assertEquals("<em>周源</em>", result.people?.highlightedName)
        assertEquals(1048300, result.people?.people?.followerCount)
        assertNull(result.toFeed())
    }

    private class TestSearchViewModel : SearchViewModel("query") {
        fun process(
            environment: PaginationEnvironment,
            feeds: List<Feed>,
        ) {
            processResponse(environment, feeds, JsonArray(emptyList()))
        }
    }

    private fun testEnvironment(blockedUserIds: Set<String>) = object : PaginationEnvironment {
        override fun httpClient() = HttpClient(MockEngine)

        override fun authenticatedCookies() = emptyMap<String, String>()

        override fun blockedUserIds() = blockedUserIds

        override suspend fun handleFetchFailure(
            tag: String?,
            error: Exception,
        ) = Unit
    }

    private fun answerFeed(
        id: Long,
        authorId: String,
    ) = CommonFeed(
        id = id.toString(),
        verb = "SEARCH_RESULT",
        target = Feed.AnswerTarget(
            id = id,
            url = "https://www.zhihu.com/question/1/answer/$id",
            author = Person(
                id = authorId,
                url = "https://www.zhihu.com/people/$authorId",
                userType = "people",
                name = authorId,
                headline = "",
                avatarUrl = "",
            ),
            question = Feed.QuestionTarget(
                id = 1,
                _title = "问题",
                url = "https://www.zhihu.com/question/1",
                type = "question",
            ),
        ),
    )
}
