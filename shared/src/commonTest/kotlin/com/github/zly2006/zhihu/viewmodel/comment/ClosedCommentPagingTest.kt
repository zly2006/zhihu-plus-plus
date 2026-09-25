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

package com.github.zly2006.zhihu.viewmodel.comment

import com.github.zly2006.zhihu.data.ZhihuJson
import com.github.zly2006.zhihu.navigation.Article
import com.github.zly2006.zhihu.navigation.ArticleType
import com.github.zly2006.zhihu.viewmodel.PaginationEnvironment
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.JsonObject
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ClosedCommentPagingTest {
    @Test
    fun closedCommentSectionDoesNotFollowEmptyCursor() = runTest {
        val requests = mutableListOf<String>()
        val environment = closedCommentEnvironment(requests)
        val viewModel = RootCommentViewModel(
            Article(type = ArticleType.Answer, id = 66419687738),
        )

        viewModel.fetchFeeds(environment)
        if (!viewModel.isEnd) {
            viewModel.fetchFeeds(environment)
        }

        assertEquals(1, requests.size)
        assertEquals("评论区已关闭", viewModel.commentClosedMessage)
        assertTrue(viewModel.isEnd)
        assertTrue(viewModel.allData.isEmpty())
    }

    @Test
    fun openCommentSectionKeepsPaging() = runTest {
        val requests = mutableListOf<String>()
        val environment = object : PaginationEnvironment {
            override fun httpClient() = HttpClient(MockEngine { error("Unexpected HTTP request") })

            override fun authenticatedCookies() = emptyMap<String, String>()

            override suspend fun fetchJson(url: String, include: String): JsonObject {
                requests += url
                return ZhihuJson.json.parseToJsonElement(
                    """
                    {
                      "comment_status": {"type": 0, "text": "", "induce_text": ""},
                      "data": [],
                      "paging": {
                        "is_end": false,
                        "next": "https://www.zhihu.com/api/v4/comment_v5/answers/1/root_comment?offset=next"
                      }
                    }
                    """.trimIndent(),
                ) as JsonObject
            }

            override suspend fun handleFetchFailure(tag: String?, error: Exception) = Unit
        }
        val viewModel = RootCommentViewModel(Article(type = ArticleType.Answer, id = 1))

        viewModel.fetchFeeds(environment)

        assertNull(viewModel.commentClosedMessage)
        assertEquals(false, viewModel.isEnd)
        assertEquals(1, requests.size)
    }
}

private fun closedCommentEnvironment(requests: MutableList<String>): PaginationEnvironment =
    object : PaginationEnvironment {
        override fun httpClient() = HttpClient(MockEngine { error("Unexpected HTTP request") })

        override fun authenticatedCookies() = emptyMap<String, String>()

        override suspend fun fetchJson(url: String, include: String): JsonObject {
            requests += url
            val offset = requests.size
            return ZhihuJson.json.parseToJsonElement(
                """
                {
                  "comment_status": {"type": 1, "text": "评论区已关闭", "induce_text": ""},
                  "edit_status": {"can_reply": false, "toast": "评论区已关闭"},
                  "data": [],
                  "paging": {
                    "is_end": false,
                    "is_start": ${offset == 1},
                    "next": "https://www.zhihu.com/api/v4/comment_v5/answers/66419687738/root_comment?limit=10&offset=cursor-$offset&order_by=score",
                    "totals": 164
                  }
                }
                """.trimIndent(),
            ) as JsonObject
        }

        override suspend fun handleFetchFailure(tag: String?, error: Exception) = Unit
    }
