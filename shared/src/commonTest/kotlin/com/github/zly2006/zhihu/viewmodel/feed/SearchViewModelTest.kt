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

import com.github.zly2006.zhihu.shared.data.CommonFeed
import com.github.zly2006.zhihu.shared.data.Feed
import com.github.zly2006.zhihu.shared.data.Person
import com.github.zly2006.zhihu.shared.data.target
import com.github.zly2006.zhihu.viewmodel.PaginationEnvironment
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.http.Url
import kotlinx.serialization.json.JsonArray
import kotlin.test.Test
import kotlin.test.assertEquals
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
