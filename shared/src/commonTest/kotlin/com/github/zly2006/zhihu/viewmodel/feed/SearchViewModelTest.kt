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
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SearchViewModelTest {
    @Test
    fun searchRequestKeepsMemberAndTabContractsSeparate() {
        val hiddenGeneralFilters = SearchFilters(
            sort = SearchSortOption.Latest,
            contentType = SearchContentType.Answer,
            timeRange = SearchTimeRange.Week,
        )
        val member = Url(zhihuSearchUrl("用户创作", restrictedMemberHashId = "member-hash-id"))

        assertEquals("member", member.parameters["restricted_scene"])
        assertEquals("member_hash_id", member.parameters["restricted_field"])
        assertEquals("member-hash-id", member.parameters["restricted_value"])
        listOf(SearchTab.People, SearchTab.Topic).forEach { tab ->
            val request = Url(zhihuSearchUrl("kmp 搜索", tab, hiddenGeneralFilters))
            assertEquals(tab.parameter, request.parameters["t"])
            assertEquals("Normal", request.parameters["search_source"])
            assertTrue(listOf("sort", "vertical", "vertical_info", "time_interval").none { it in request.parameters })
        }
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
