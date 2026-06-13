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
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ZhihuReadHistoryClientTest {
    @Test
    fun buildsReadHistoryBody() {
        assertEquals(
            """{"content_token":"123","content_type":"answer"}""",
            buildZhihuReadHistoryBody(
                contentToken = "123",
                contentType = "answer",
            ),
        )
    }

    @Test
    fun postReadHistoryUsesSignedJsonRequest() = runTest {
        var requested = false
        val client = HttpClient(
            MockEngine { request ->
                requested = true
                assertEquals(ZHIHU_READ_HISTORY_ADD_URL, request.url.toString())
                assertEquals(HttpMethod.Post, request.method)
                assertTrue(request.headers["x-zse-96"].orEmpty().startsWith("2.0_"))
                assertEquals("fetch", request.headers["x-requested-with"])
                assertEquals("application/json", request.body.contentType?.toString())
                respond(
                    content = "{}",
                    status = HttpStatusCode.OK,
                    headers = headersOf(HttpHeaders.ContentType, "application/json"),
                )
            },
        ) {
            installZhihuCommonClientConfig(
                cookies = mutableMapOf("d_c0" to "token"),
                userAgent = "test-agent",
            )
        }

        postZhihuReadHistory(
            client = client,
            cookies = mapOf("d_c0" to "token"),
            contentToken = "123",
            contentTypeName = "answer",
            lastRefreshMillis = 0L,
            updateLastRefreshMillis = {},
        )

        assertTrue(requested)
    }

    @Test
    fun postReadHistorySkipsGuestSession() = runTest {
        var requestCount = 0
        val client = HttpClient(
            MockEngine {
                requestCount++
                respond(
                    content = "{}",
                    status = HttpStatusCode.OK,
                    headers = headersOf(HttpHeaders.ContentType, "application/json"),
                )
            },
        )

        postZhihuReadHistory(
            client = client,
            cookies = emptyMap(),
            contentToken = "123",
            contentTypeName = "answer",
            lastRefreshMillis = 0L,
            updateLastRefreshMillis = {},
        )

        assertEquals(0, requestCount)
    }
}
