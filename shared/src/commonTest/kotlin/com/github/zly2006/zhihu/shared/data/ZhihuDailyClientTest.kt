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

package com.github.zly2006.zhihu.shared.data

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.request.get
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class ZhihuDailyClientTest {
    @Test
    fun fetchDailyStoriesForDateRequestsNextDayBeforeEndpoint() = runTest {
        val client = dailyMockClient { url ->
            assertEquals("https://news-at.zhihu.com/api/4/stories/before/20260521", url)
        }

        val response: DailyStoriesResponse = client
            .get("https://news-at.zhihu.com/api/4/stories/before/20260521")
            .body()

        assertEquals("20260520", response.date)
        assertEquals(1L, response.stories.single().id)
    }

    @Test
    fun fetchLatestDailyStoriesRequestsLatestEndpoint() = runTest {
        val client = dailyMockClient { url ->
            assertEquals("https://news-at.zhihu.com/api/4/stories/latest", url)
        }

        val response: DailyStoriesResponse = client
            .get("https://news-at.zhihu.com/api/4/stories/latest")
            .body()

        assertEquals("20260520", response.date)
    }

    private fun dailyMockClient(assertUrl: (String) -> Unit): HttpClient = HttpClient(
        MockEngine { request ->
            assertUrl(request.url.toString())
            respond(
                content =
                    """
                    {
                      "date": "20260520",
                      "stories": [
                        {
                          "id": 1,
                          "title": "story",
                          "url": "https://example.com/story",
                          "hint": "hint",
                          "images": [],
                          "type": 0
                        }
                      ]
                    }
                    """.trimIndent(),
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, "application/json"),
            )
        },
    ) {
        installZhihuCommonClientConfig(
            cookies = mutableMapOf(),
            userAgent = "test-agent",
        )
    }
}
