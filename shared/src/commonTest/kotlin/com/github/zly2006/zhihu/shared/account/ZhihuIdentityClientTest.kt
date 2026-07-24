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

package com.github.zly2006.zhihu.shared.account

import com.github.zly2006.zhihu.shared.data.installZhihuCommonClientConfig
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.content.TextContent
import io.ktor.http.headersOf
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ZhihuIdentityClientTest {
    @Test
    fun decodesIdentityAccountListWithMobileHeaders() = runTest {
        val session = ZhihuAccountSession(
            login = true,
            username = "current",
            cookies = mutableMapOf("z_c0" to "current-cookie"),
            mobileAccessToken = "current-access",
            mobileTokenType = "bearer",
        )
        val currentClient = identityTestClient(
            MockEngine { request ->
                assertEquals(HttpMethod.Get, request.method)
                assertEquals("/people/account/list", request.url.encodedPath)
                assertEquals("bearer current-access", request.headers[HttpHeaders.Authorization])
                assertEquals("11.2.0", request.headers["x-app-version"])
                respondJson(
                    """
                    {
                      "data": [
                        {
                          "id": "parent-id",
                          "url_token": "parent-token",
                          "name": "主账号",
                          "avatar_url": "https://example.com/parent.png",
                          "is_active": true,
                          "can_create_sub_account": true,
                          "account_type": 1,
                          "sub_account_control_status": 0
                        },
                        {
                          "id": "child-id",
                          "url_token": "child-token",
                          "name": "马甲号",
                          "avatar_url": "https://example.com/child.png",
                          "is_active": true,
                          "can_create_sub_account": true,
                          "account_type": 2,
                          "sub_account_control_status": 0
                        }
                      ]
                    }
                    """.trimIndent(),
                )
            },
            session.cookies,
        )
        val client = ZhihuIdentityClient(
            currentClient = { currentClient },
            temporaryClient = { error("temporary client should not be created") },
            currentSession = { session },
            saveSession = { error("list should not save a session") },
        )

        val accounts = client.listAccounts()

        assertEquals(listOf(1, 2), accounts.map { it.accountType })
        assertEquals(listOf("parent-id", "child-id"), accounts.map { it.id })
        assertTrue(accounts.first().canCreateSubAccount)
        currentClient.close()
    }

    @Test
    fun switchSendsTargetUserIdAndAtomicallySavesIssuedSession() = runTest {
        var savedSession = ZhihuAccountSession(
            login = true,
            username = "主账号",
            cookies = mutableMapOf(
                "d_c0" to "device-cookie",
                "z_c0" to "old-account-cookie",
            ),
            mobileAccessToken = "old-access",
            mobileRefreshToken = "old-refresh",
            mobileTokenType = "bearer",
        )
        val currentClient = identityTestClient(
            MockEngine { request ->
                assertEquals(HttpMethod.Post, request.method)
                assertEquals("/account/switch", request.url.encodedPath)
                assertEquals(
                    """{"target_user_id":"child-id"}""",
                    (request.body as TextContent).text,
                )
                respondJson(issuedTokenJson())
            },
            savedSession.cookies,
        )
        val temporaryEngine = MockEngine { request ->
            assertEquals(HttpMethod.Get, request.method)
            assertEquals("/people/self", request.url.encodedPath)
            assertEquals("bearer new-access", request.headers[HttpHeaders.Authorization])
            assertTrue(request.headers[HttpHeaders.Cookie].orEmpty().contains("z_c0=new-account-cookie"))
            respondJson(childProfileJson())
        }
        val client = ZhihuIdentityClient(
            currentClient = { currentClient },
            temporaryClient = { cookies -> identityTestClient(temporaryEngine, cookies) },
            currentSession = { savedSession },
            saveSession = { savedSession = it },
        )

        val result = client.switchAccount("child-id")

        assertEquals("child-id", result.account.id)
        assertEquals(2, result.account.accountType)
        assertEquals("马甲号", savedSession.username)
        assertEquals("device-cookie", savedSession.cookies["d_c0"])
        assertEquals("new-account-cookie", savedSession.cookies["z_c0"])
        assertEquals("new-q-cookie", savedSession.cookies["q_c0"])
        assertEquals("new-access", savedSession.mobileAccessToken)
        assertEquals("new-refresh", savedSession.mobileRefreshToken)
        assertEquals(123456789L, savedSession.mobileTokenExpiresAt)
        assertEquals("child-id", savedSession.profile?.id)
        assertTrue(savedSession.login)
        currentClient.close()
    }

    @Test
    fun createSubAccountPostsWithoutRenameOrProfileMutationBody() = runTest {
        var savedSession = ZhihuAccountSession(
            login = true,
            username = "主账号",
            cookies = mutableMapOf("z_c0" to "old-account-cookie"),
        )
        val currentClient = identityTestClient(
            MockEngine { request ->
                assertEquals(HttpMethod.Post, request.method)
                assertEquals("/account/sub/register", request.url.encodedPath)
                assertFalse(request.headers.contains(HttpHeaders.ContentType))
                assertFalse(request.body is TextContent)
                respondJson(issuedTokenJson())
            },
            savedSession.cookies,
        )
        val client = ZhihuIdentityClient(
            currentClient = { currentClient },
            temporaryClient = { cookies ->
                identityTestClient(
                    MockEngine { request ->
                        assertEquals("/people/self", request.url.encodedPath)
                        respondJson(childProfileJson())
                    },
                    cookies,
                )
            },
            currentSession = { savedSession },
            saveSession = { savedSession = it },
        )

        val result = client.createSubAccount()

        assertEquals("child-id", result.account.id)
        assertEquals("马甲号", savedSession.username)
        currentClient.close()
    }

    private fun identityTestClient(
        engine: MockEngine,
        cookies: MutableMap<String, String>,
    ): HttpClient = HttpClient(engine) {
        installZhihuCommonClientConfig(
            cookies = cookies,
            userAgent = "test-agent",
        )
    }

    private fun io.ktor.client.engine.mock.MockRequestHandleScope.respondJson(body: String) = respond(
        content = body,
        status = HttpStatusCode.OK,
        headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString()),
    )

    private fun issuedTokenJson(): String =
        """
        {
          "uid": "child-id",
          "user_id": 42,
          "token_type": "bearer",
          "access_token": "new-access",
          "refresh_token": "new-refresh",
          "expires_in": 2591999,
          "cookie": {
            "q_c0": "new-q-cookie",
            "z_c0": "new-account-cookie"
          },
          "expires_at": 123456789
        }
        """.trimIndent()

    private fun childProfileJson(): String =
        """
        {
          "id": "child-id",
          "url_token": "child-token",
          "name": "马甲号",
          "user_type": "people",
          "avatar_url": "https://example.com/child.png",
          "can_create_sub_account": true,
          "account_type": 2,
          "sub_account_control_status": 0
        }
        """.trimIndent()
}
