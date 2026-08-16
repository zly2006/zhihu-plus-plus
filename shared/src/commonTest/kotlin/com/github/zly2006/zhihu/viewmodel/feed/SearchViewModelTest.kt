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
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNull

class SearchViewModelTest {
    @Test
    fun memberRestrictionOnlyAppearsInMemberSearch() {
        val global = Url(zhihuSearchUrl("kmp 搜索"))
        val member = Url(zhihuSearchUrl("用户创作", restrictedMemberHashId = "member-hash-id"))

        assertNull(global.parameters["restricted_scene"])
        assertEquals("member", member.parameters["restricted_scene"])
        assertEquals("member_hash_id", member.parameters["restricted_field"])
        assertEquals("member-hash-id", member.parameters["restricted_value"])
    }

    @Test
    fun decodesGeneralSearchObjectDirectlyAsFeed() {
        val viewModel = TestSearchViewModel()
        val environment = environment("{}")
        val feeds = viewModel.decode(
            environment,
            SearchTab.General,
            """{"data":[{"type":"search_result","id":2,"object":{"id":2,"type":"answer","url":"https://www.zhihu.com/question/1/answer/2","question":{"id":1,"type":"question","title":"KMP","url":"https://www.zhihu.com/question/1"}}}]}""",
        )
        viewModel.process(environment, feeds)

        assertEquals(
            "KMP",
            viewModel.displayItems
                .single()
                .feed
                ?.target
                ?.title,
        )
    }

    @Test
    fun decodesPeopleSearchWithoutPolymorphicSerializer() {
        val viewModel = TestSearchViewModel()
        viewModel.decode(
            environment("{}"),
            SearchTab.People,
            """{"data":[{"type":"search_result","id":1,"object":{"id":"person-id","url_token":"zhouyuan","type":"people","url":"https://api.zhihu.com/people/person-id","name":"<em>周源</em>","headline":"知乎 001 号员工","gender":1,"avatar_url":"https://pic.example/avatar.jpg","follower_count":1048300,"answer_count":371}}]}""",
        )

        val result = assertIs<SearchEntity.Person>(viewModel.entities.single()).person
        assertEquals("zhouyuan", result.urlToken)
        assertEquals("<em>周源</em>", result.name)
        assertEquals(1_048_300, result.followerCount)
        assertEquals(371, result.answerCount)
    }

    @Test
    fun decodesTopicAndRollsBackFailedFollow() = runTest {
        val viewModel = TestSearchViewModel()
        val environment = environment("{}")
        viewModel.decode(
            environment,
            SearchTab.Topic,
            """{"data":[{"type":"search_result","object":{"id":"20106982","name":"<em>AI</em>技术","url":"https://api.zhihu.com/topics/20106982","type":"topic","excerpt":"人工智能（Artificial Intelligence），英文缩写为<em>AI</em>","avatar_url":"https://pic.example/topic.jpg","is_following":false,"visit_count":3359904456,"top_answer_count":1890565}}]}""",
        )

        val topic = assertIs<SearchEntity.Topic>(viewModel.entities.single())
        assertEquals("AI技术", topic.topic.name)
        assertEquals(3_359_904_456, topic.visitCount)
        assertEquals(1_890_565, topic.discussCount)

        assertFalse(viewModel.setTopicFollowing(environment("failed", HttpStatusCode.InternalServerError), topic.id, true).isSuccess)
        assertFalse(assertIs<SearchEntity.Topic>(viewModel.entities.single()).isFollowing)
        assertFalse(topic.id in viewModel.changingTopicIds)
    }

    @Test
    fun rejectsNonEmptyTopicPageWhenNoTopicCanBeDecoded() {
        val error = assertFailsWith<IllegalStateException> {
            TestSearchViewModel().decode(
                environment("{}"),
                SearchTab.Topic,
                """{"data":[{"type":"search_result","object":{"id":"person-id","type":"people"}}]}""",
            )
        }

        assertEquals("服务端返回了 1 条话题搜索结果，但均无法解码", error.message)
    }

    @Test
    fun searchResultsExcludeLocallyBlockedAuthors() {
        val viewModel = TestSearchViewModel()
        val blocked = answerFeed(id = 1, authorId = "blocked-user")
        val kept = answerFeed(id = 2, authorId = "kept-user")

        viewModel.process(environment("{}", blockedUserIds = setOf("blocked-user")), listOf(blocked, kept))

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

    private class TestSearchViewModel : SearchViewModel("query") {
        override fun refresh(environment: PaginationEnvironment) = Unit

        fun decode(
            environment: PaginationEnvironment,
            tab: SearchTab,
            response: String,
        ): List<Feed> {
            selectTab(environment, tab)
            val data = ZhihuJson.json
                .parseToJsonElement(response)
                .jsonObject["data"]!!
                .jsonArray
            return decodePage(environment, data)
        }

        fun process(
            environment: PaginationEnvironment,
            feeds: List<Feed>,
        ) = processResponse(environment, feeds, JsonArray(emptyList()))
    }

    private fun environment(
        response: String,
        status: HttpStatusCode = HttpStatusCode.OK,
        blockedUserIds: Set<String> = emptySet(),
    ) = object : PaginationEnvironment {
        override fun httpClient() = HttpClient(
            MockEngine {
                respond(response, status, headersOf(HttpHeaders.ContentType, "application/json"))
            },
        )

        override fun authenticatedCookies() = mapOf("d_c0" to "test")

        override fun blockedUserIds() = blockedUserIds

        override suspend fun fetchJson(
            url: String,
            include: String,
        ): JsonObject? = if (status == HttpStatusCode.OK) {
            ZhihuJson.json.parseToJsonElement(response) as? JsonObject
        } else {
            null
        }

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
