package com.github.zly2006.zhihu.shared.data

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class ZhihuDailyClientTest {
    @Test
    fun nextDailyApiDateHandlesMonthAndLeapYearBoundaries() {
        assertEquals("20260301", nextDailyApiDate("20260228"))
        assertEquals("20240229", nextDailyApiDate("20240228"))
        assertEquals("20250101", nextDailyApiDate("20241231"))
    }

    @Test
    fun nextDailyApiDateRejectsInvalidFormat() {
        assertFailsWith<IllegalArgumentException> {
            nextDailyApiDate("2026-05-20")
        }
    }

    @Test
    fun fetchDailyStoriesForDateRequestsNextDayBeforeEndpoint() = runTest {
        val client = dailyMockClient { url ->
            assertEquals(zhihuDailyBeforeUrl("20260521"), url)
        }

        val response = fetchDailyStoriesForDate(client, "20260520")

        assertEquals("20260520", response.date)
        assertEquals(1L, response.stories.single().id)
    }

    @Test
    fun fetchLatestDailyStoriesRequestsLatestEndpoint() = runTest {
        val client = dailyMockClient { url ->
            assertEquals(ZHIHU_DAILY_LATEST_URL, url)
        }

        val response = fetchLatestDailyStories(client)

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
