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

package com.github.zly2006.zhihu.shared.data

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.MockRequestHandleScope
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.util.network.UnresolvedAddressException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class ZhihuDailyClientTest {
    @Test
    fun fetchDailyStoriesForDateRequestsNextDayBeforeEndpoint() = runTest {
        val client = dailyMockClient { url ->
            assertEquals("https://news-at.zhihu.com/api/4/stories/before/20260521", url)
        }

        val response = client.fetchDailyStoriesBefore("20260521")

        assertEquals("20260520", response.date)
        assertEquals(1L, response.stories.single().id)
    }

    @Test
    fun fetchLatestDailyStoriesRequestsLatestEndpoint() = runTest {
        val client = dailyMockClient { url ->
            assertEquals("https://news-at.zhihu.com/api/4/stories/latest", url)
        }

        val response = client.fetchLatestDailyStories()

        assertEquals("20260520", response.date)
    }

    @Test
    fun fetchLatestDailyStoriesFallsBackToDailyHostWhenPrimaryHostCannotResolve() = runTest {
        val requestedUrls = mutableListOf<String>()
        val client = HttpClient(
            MockEngine { request ->
                requestedUrls += request.url.toString()
                if (request.url.host == "news-at.zhihu.com") {
                    throw UnresolvedAddressException()
                }
                respondDailyStories()
            },
        ) {
            installZhihuCommonClientConfig(
                cookies = mutableMapOf(),
                userAgent = "test-agent",
            )
        }

        val response = client.fetchLatestDailyStories()

        assertEquals("20260520", response.date)
        assertEquals(
            listOf(
                "https://news-at.zhihu.com/api/4/stories/latest",
                "https://daily.zhihu.com/api/4/stories/latest",
            ),
            requestedUrls,
        )
    }

    @Test
    fun fetchLatestDailyStoriesFallsBackForTopLevelHostResolutionMessage() = runTest {
        val requestedUrls = mutableListOf<String>()
        val client = HttpClient(
            MockEngine { request ->
                requestedUrls += request.url.toString()
                if (request.url.host == "news-at.zhihu.com") {
                    throw Exception("Unable to resolve host news-at.zhihu.com")
                }
                respondDailyStories()
            },
        ) {
            installZhihuCommonClientConfig(
                cookies = mutableMapOf(),
                userAgent = "test-agent",
            )
        }

        val response = client.fetchLatestDailyStories()

        assertEquals("20260520", response.date)
        assertEquals(
            listOf(
                "https://news-at.zhihu.com/api/4/stories/latest",
                "https://daily.zhihu.com/api/4/stories/latest",
            ),
            requestedUrls,
        )
    }

    @Test
    fun fetchDailyStoriesBeforeFallsBackToDailyHostWhenPrimaryHostCannotResolve() = runTest {
        val requestedUrls = mutableListOf<String>()
        val client = HttpClient(
            MockEngine { request ->
                requestedUrls += request.url.toString()
                if (request.url.host == "news-at.zhihu.com") {
                    throw UnresolvedAddressException()
                }
                respondDailyStories()
            },
        ) {
            installZhihuCommonClientConfig(
                cookies = mutableMapOf(),
                userAgent = "test-agent",
            )
        }

        val response = client.fetchDailyStoriesBefore("20260521")

        assertEquals("20260520", response.date)
        assertEquals(
            listOf(
                "https://news-at.zhihu.com/api/4/stories/before/20260521",
                "https://daily.zhihu.com/api/4/stories/before/20260521",
            ),
            requestedUrls,
        )
    }

    @Test
    fun fetchDailyStoriesBeforeDoesNotFallbackForNestedHostResolutionMessageOnly() = runTest {
        val requestedUrls = mutableListOf<String>()
        val client = HttpClient(
            MockEngine { request ->
                requestedUrls += request.url.toString()
                throw Exception(
                    "daily api unavailable",
                    Exception("Unable to resolve host news-at.zhihu.com"),
                )
            },
        ) {
            installZhihuCommonClientConfig(
                cookies = mutableMapOf(),
                userAgent = "test-agent",
            )
        }

        assertFailsWith<Exception> {
            client.fetchDailyStoriesBefore("20260521")
        }
        assertEquals(
            listOf("https://news-at.zhihu.com/api/4/stories/before/20260521"),
            requestedUrls,
        )
    }

    @Test
    fun fetchLatestDailyStoriesDoesNotFallbackForNonHostFailures() = runTest {
        val requestedUrls = mutableListOf<String>()
        val client = HttpClient(
            MockEngine { request ->
                requestedUrls += request.url.toString()
                throw IllegalStateException("daily api unavailable")
            },
        ) {
            installZhihuCommonClientConfig(
                cookies = mutableMapOf(),
                userAgent = "test-agent",
            )
        }

        assertFailsWith<IllegalStateException> {
            client.fetchLatestDailyStories()
        }
        assertEquals(
            listOf("https://news-at.zhihu.com/api/4/stories/latest"),
            requestedUrls,
        )
    }

    @Test
    fun fetchLatestDailyStoriesPropagatesCancellationWithoutFallback() = runTest {
        val requestedUrls = mutableListOf<String>()
        val client = HttpClient(
            MockEngine { request ->
                requestedUrls += request.url.toString()
                throw CancellationException("request cancelled")
            },
        ) {
            installZhihuCommonClientConfig(
                cookies = mutableMapOf(),
                userAgent = "test-agent",
            )
        }

        assertFailsWith<CancellationException> {
            client.fetchLatestDailyStories()
        }
        assertEquals(
            listOf("https://news-at.zhihu.com/api/4/stories/latest"),
            requestedUrls,
        )
    }

    @Test
    fun fetchLatestDailyStoriesPropagatesFallbackFailureWhenBothHostsCannotResolve() = runTest {
        val requestedUrls = mutableListOf<String>()
        val client = HttpClient(
            MockEngine { request ->
                requestedUrls += request.url.toString()
                throw UnresolvedAddressException()
            },
        ) {
            installZhihuCommonClientConfig(
                cookies = mutableMapOf(),
                userAgent = "test-agent",
            )
        }

        assertFailsWith<UnresolvedAddressException> {
            client.fetchLatestDailyStories()
        }
        assertEquals(
            listOf(
                "https://news-at.zhihu.com/api/4/stories/latest",
                "https://daily.zhihu.com/api/4/stories/latest",
            ),
            requestedUrls,
        )
    }

    private fun dailyMockClient(assertUrl: (String) -> Unit): HttpClient = HttpClient(
        MockEngine { request ->
            assertUrl(request.url.toString())
            respondDailyStories()
        },
    ) {
        installZhihuCommonClientConfig(
            cookies = mutableMapOf(),
            userAgent = "test-agent",
        )
    }

    private fun MockRequestHandleScope.respondDailyStories() = respond(
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
}
