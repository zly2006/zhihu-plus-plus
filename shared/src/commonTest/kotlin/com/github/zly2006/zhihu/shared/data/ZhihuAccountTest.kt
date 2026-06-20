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
import io.ktor.client.call.body
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class ZhihuAccountTest {
    @BeforeTest
    fun resetAuthRefreshThrottle() {
        resetZhihuAuthenticatedRequestRefreshThrottleForTesting()
    }

    @Test
    fun fetchVerifiedSessionReturnsNullForUnauthorizedResponse() = runTest {
        val client = mockClient(
            status = HttpStatusCode.Unauthorized,
            body = """{"error":"unauthorized"}""",
        )

        assertNull(fetchVerifiedZhihuSession(client, emptyMap(), "test-agent"))
    }

    @Test
    fun fetchVerifiedSessionKeepsProfileAndRawSelf() = runTest {
        val cookies = mutableMapOf("z_c0" to "token", "d_c0" to "dc0")
        val client = mockClient(
            status = HttpStatusCode.OK,
            body = """{"id":"1","name":"Alice","url_token":"alice-token","user_type":"people","avatar_url":"https://example.com/avatar.jpg"}""",
            cookies = cookies,
        )

        val session = fetchVerifiedZhihuSession(client, cookies, "test-agent")

        requireNotNull(session)
        assertEquals(true, session.login)
        assertEquals("Alice", session.username)
        assertEquals(cookies, session.cookies)
        assertEquals("test-agent", session.userAgent)
        assertEquals("1", session.profile?.id)
        assertEquals("alice-token", session.profile?.urlToken)
        assertEquals("https://example.com/avatar.jpg", session.profile?.avatarUrl)
        assertEquals(
            "https://example.com/avatar.jpg",
            session.self
                ?.jsonObject
                ?.get("avatar_url")
                ?.jsonPrimitive
                ?.content,
        )
    }

    @Test
    fun fetchAuthenticatedJsonRefreshesUnauthorizedRequestOnce() = runTest {
        val requests = mutableListOf<String>()
        val cookies = mutableMapOf("z_c0" to "token")
        val client = HttpClient(
            MockEngine { request ->
                requests += "${request.method.value} ${request.url.encodedPath}"
                when (requests.size) {
                    1 -> respond(
                        content = """{"error":"unauthorized"}""",
                        status = HttpStatusCode.Unauthorized,
                        headers = headersOf(HttpHeaders.ContentType, "application/json"),
                    )
                    2 -> respond(
                        content = """{"refresh_token":"refresh-token"}""",
                        status = HttpStatusCode.OK,
                        headers = headersOf(HttpHeaders.ContentType, "application/json"),
                    )
                    3 -> respond(
                        content = """{"access_token":"access-token"}""",
                        status = HttpStatusCode.OK,
                        headers = headersOf(HttpHeaders.ContentType, "application/json"),
                    )
                    else -> respond(
                        content = """{"ok":true}""",
                        status = HttpStatusCode.OK,
                        headers = headersOf(HttpHeaders.ContentType, "application/json"),
                    )
                }
            },
        ) {
            installZhihuCommonClientConfig(
                cookies = cookies,
                userAgent = "test-agent",
            )
        }

        val response = fetchZhihuAuthenticatedJson(
            client = client,
            url = "https://www.zhihu.com/api/v4/test",
        ) {
            method = HttpMethod.Get
        }

        assertEquals("true", response?.get("ok")?.jsonPrimitive?.content)
        assertEquals(
            listOf(
                "GET /api/v4/test",
                "POST /api/account/prod/token/refresh",
                "POST /api/v3/oauth/sign_in",
                "GET /api/v4/test",
            ),
            requests,
        )
    }

    @Test
    fun executeAuthenticatedRequestKeepsCallerOwnedResponseBody() = runTest {
        val requests = mutableListOf<String>()
        val cookies = mutableMapOf("z_c0" to "token")
        val client = HttpClient(
            MockEngine { request ->
                requests += "${request.method.value} ${request.url.encodedPath}"
                when (requests.size) {
                    1 -> respond(
                        content = """{"error":"unauthorized"}""",
                        status = HttpStatusCode.Unauthorized,
                        headers = headersOf(HttpHeaders.ContentType, "application/json"),
                    )
                    2 -> respond(
                        content = """{"refresh_token":"refresh-token"}""",
                        status = HttpStatusCode.OK,
                        headers = headersOf(HttpHeaders.ContentType, "application/json"),
                    )
                    3 -> respond(
                        content = """{"access_token":"access-token"}""",
                        status = HttpStatusCode.OK,
                        headers = headersOf(HttpHeaders.ContentType, "application/json"),
                    )
                    else -> respond(
                        content = """{"follower_count":42}""",
                        status = HttpStatusCode.OK,
                        headers = headersOf(HttpHeaders.ContentType, "application/json"),
                    )
                }
            },
        ) {
            installZhihuCommonClientConfig(
                cookies = cookies,
                userAgent = "test-agent",
            )
        }

        val response = executeZhihuAuthenticatedRequest(
            client = client,
            url = "https://www.zhihu.com/api/v4/members/alice/followers",
        ) {
            method = HttpMethod.Post
        }

        assertEquals(HttpStatusCode.OK, response.status)
        assertEquals(
            "42",
            response.body<kotlinx.serialization.json.JsonObject>()["follower_count"]?.jsonPrimitive?.content,
        )
        assertEquals(
            listOf(
                "POST /api/v4/members/alice/followers",
                "POST /api/account/prod/token/refresh",
                "POST /api/v3/oauth/sign_in",
                "POST /api/v4/members/alice/followers",
            ),
            requests,
        )
    }

    private fun mockClient(
        status: HttpStatusCode,
        body: String,
        cookies: MutableMap<String, String> = mutableMapOf("_xsrf" to "token"),
    ): HttpClient = HttpClient(
        MockEngine { request ->
            assertEquals(ZHIHU_ME_URL, request.url.toString())
            respond(
                content = body,
                status = status,
                headers = headersOf(HttpHeaders.ContentType, "application/json"),
            )
        },
    ) {
        installZhihuCommonClientConfig(
            cookies = cookies,
            userAgent = "test-agent",
        )
    }
}
