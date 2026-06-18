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

package com.github.zly2006.zhihu.viewmodel

import com.github.zly2006.zhihu.shared.data.installZhihuCommonClientConfig
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.Url
import io.ktor.http.headersOf
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlin.reflect.typeOf
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class PaginationViewModelTest {
    @Test
    fun fetchJsonAddsIncludeForInitialUrl() = runTest {
        val requestedUrls = mutableListOf<String>()
        val environment = mockApiEnvironment(requestedUrls)

        environment.fetchJson(
            url = "https://www.zhihu.com/api/v4/members/test/answers?sort_by=voteups",
            include = "data[*].content",
        )

        val includes = Url(requestedUrls.single()).parameters.getAll("include")
        assertEquals(listOf("data[*].content"), includes)
    }

    @Test
    fun fetchJsonDoesNotDuplicateIncludeFromPagingNext() = runTest {
        val requestedUrls = mutableListOf<String>()
        val environment = mockApiEnvironment(requestedUrls)

        environment.fetchJson(
            url = "https://www.zhihu.com/api/v4/members/test/answers?include=data[*].content&limit=20&offset=20",
            include = "data[*].content",
        )

        val includes = Url(requestedUrls.single()).parameters.getAll("include")
        assertEquals(listOf("data[*].content"), includes)
    }

    @Test
    fun fetchFeedsReportsMissingDataInsteadOfThrowingNullPointerException() = runTest {
        val environment = CapturingPaginationEnvironment(
            response = buildJsonObject {
                put("paging", buildJsonObject {})
                put("error", "risk control")
            },
        )

        TestPaginationViewModel().fetchOnce(environment)

        val error = assertNotNull(environment.recordedError)
        assertTrue(error is MissingFeedDataException)
        assertTrue(error.message.orEmpty().contains("topLevelKeys=[error, paging]"))
        assertTrue(error.message.orEmpty().contains("bodyPrefix="))
    }

    private fun mockApiEnvironment(requestedUrls: MutableList<String>) = object : ZhihuApiEnvironment {
        private val client = HttpClient(
            MockEngine { request ->
                requestedUrls += request.url.toString()
                respond(
                    content = """{"data":[]}""",
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

        override fun httpClient(): HttpClient = client

        override fun authenticatedCookies(): Map<String, String> = emptyMap()

        override suspend fun handleFetchFailure(
            tag: String?,
            error: Exception,
        ): Unit = throw error
    }

    private class TestPaginationViewModel : PaginationViewModel<String>(typeOf<String>()) {
        override val initialUrl: String = "https://www.zhihu.com/api/v4/test"

        suspend fun fetchOnce(environment: PaginationEnvironment) {
            fetchFeeds(environment)
        }
    }

    private class CapturingPaginationEnvironment(
        private val response: JsonObject?,
    ) : PaginationEnvironment {
        var recordedError: Exception? = null

        override fun httpClient(): HttpClient = error("not used")

        override fun authenticatedCookies(): Map<String, String> = emptyMap()

        override suspend fun fetchJson(
            url: String,
            include: String,
        ): JsonObject? = response

        override suspend fun handleFetchFailure(
            tag: String?,
            error: Exception,
        ) {
            recordedError = error
        }
    }
}
