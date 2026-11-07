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

package com.github.zly2006.zhihu.data

import com.github.zly2006.zhihu.account.ZhihuAccountProfileSnapshot
import com.github.zly2006.zhihu.account.ZhihuAccountRepository
import com.github.zly2006.zhihu.account.ZhihuAccountSession
import com.github.zly2006.zhihu.account.ZhihuAccountSessionStore
import com.github.zly2006.zhihu.account.ZhihuAccountStore
import com.github.zly2006.zhihu.account.accountHttpClientEngineForTesting
import com.github.zly2006.zhihu.account.phoneLoginNowEpochSecondsForTesting
import com.github.zly2006.zhihu.util.jsonObject
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class ZhihuAccountTest {
    @Test
    fun capturedExpiredMeRequestRenewsAndRestoresTheSameAccount() = runTest {
        val requests = mutableListOf<String>()
        val engine = MockEngine { request ->
            requests += "${request.method.value} ${request.url.host}${request.url.encodedPath}"
            when (request.url.encodedPath) {
                "/api/account/prod/sign_in" -> respond(
                    CAPTURED_MOBILE_REFRESH_BODY,
                    headers = headersOf(HttpHeaders.ContentType to listOf("application/json"), HttpHeaders.SetCookie to CAPTURED_MOBILE_REFRESH_COOKIES),
                )
                "/api/v4/me" -> if (request.headers[HttpHeaders.Cookie].orEmpty().contains("z_c0=captured-z-c0")) {
                    respond(CAPTURED_RESTORED_ME_BODY, headers = headersOf(HttpHeaders.ContentType, "application/json"))
                } else {
                    respond(CAPTURED_EXPIRED_TICKET_BODY, HttpStatusCode.Unauthorized, headersOf(HttpHeaders.ContentType, "application/json"))
                }
                else -> error("Unrecorded request: ${request.url.host}${request.url.encodedPath}")
            }
        }
        var saved: String? = null
        val repository = ZhihuAccountRepository(object : ZhihuAccountSessionStore {
            override fun readText() = saved

            override fun writeText(text: String) {
                saved = text
            }

            override fun delete() {
                saved = null
            }
        })
        repository.save(
            ZhihuAccountSession(
                login = true,
                username = "Captured User",
                profile = ZhihuAccountProfileSnapshot(id = "captured-person", name = "Captured User"),
                cookies = mutableMapOf("z_c0" to "expired-cookie", "d_c0" to "captured-device-cookie"),
                mobileAccessToken = "expired-access-token",
                mobileRefreshToken = "saved-refresh-token",
                mobileTokenExpiresAt = 1_793_756_790L,
            ),
        )
        accountHttpClientEngineForTesting = engine
        phoneLoginNowEpochSecondsForTesting = { 1_793_779_695L }
        val store = ZhihuAccountStore(repository)
        try {
            val response = store.client.get(ZHIHU_ME_URL).jsonObject()
            assertEquals("captured-person", response?.get("id")?.jsonPrimitive?.content)
            assertEquals(listOf("GET www.zhihu.com/api/v4/me", "POST api.zhihu.com/api/account/prod/sign_in", "GET www.zhihu.com/api/v4/me"), requests)
            val restored = repository.load()
            assertEquals("captured-refresh-token", restored.mobileRefreshToken)
            assertEquals("captured-access-token", restored.mobileAccessToken)
            assertEquals(1796371695L, restored.mobileTokenExpiresAt)
            assertEquals("captured-device-cookie", restored.cookies["d_c0"])
            assertEquals("captured-z-c0", restored.cookies["z_c0"])
            assertEquals("captured-q-c0", restored.cookies["q_c0"])
            assertEquals("captured-xsrf", restored.cookies["_xsrf"])
            assertEquals("captured-BEC", restored.cookies["BEC"])
        } finally {
            store.close()
            accountHttpClientEngineForTesting = null
            phoneLoginNowEpochSecondsForTesting = null
            engine.close()
        }
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

// Captured 2026-11-04 from /api/v4/me and Android prod/sign_in; see docs/mobile-session-refresh.md.
// Every response field and cookie attribute is retained; only identity and credential values are deidentified.
internal const val CAPTURED_EXPIRED_TICKET_BODY = """{"error":{"code":100,"name":"AuthenticationInvalidRequest","message":"ERR_LOGIN_TICKET_EXPIRED"}}
"""
internal const val CAPTURED_MOBILE_REFRESH_BODY = """{"uid":"captured-person","user_id":123456789,"token_type":"bearer","access_token":"captured-access-token","refresh_token":"captured-refresh-token","expires_in":2591999,"cookie":{"q_c0":"captured-q-c0","z_c0":"captured-z-c0"},"unlock_ticket":null,"lock_in":null,"verification":null,"expires_at":1796371695}
"""
internal const val CAPTURED_RESTORED_ME_BODY = """{"id":"captured-person","url_token":"captured-user","name":"Captured User","use_default_avatar":false,"avatar_url":"https://picx.zhimg.com/v2-00000000000000000000000000000000_xl.jpg?source=32738c0c&needBackground=1","avatar_url_template":"https://picx.zhimg.com/v2-00000000000000000000000000000000_l.jpg?source=32738c0c&needBackground=1","is_org":false,"type":"people","url":"https://www.zhihu.com/api/v4/people/scanmenge","user_type":"people","headline":"Captured profile","headline_render":"Captured profile","gender":0,"is_advertiser":false,"ip_info":"已脱敏","vip_info":{"is_vip":false,"vip_type":0,"rename_days":"0","entrance_v2":null,"rename_frequency":0,"rename_await_days":42},"kvip_info":{"is_vip":false},"answer_count":35,"question_count":1,"articles_count":5,"columns_count":0,"zvideo_count":0,"favorite_count":5,"pins_count":15,"voteup_count":4882,"thanked_count":16279,"line_comment_count":722,"line_like_count":0,"line_comment_only_count":0,"available_medals_count":0,"org_verify_status":null,"uid":"123456789","default_notifications_count":0,"follow_notifications_count":0,"vote_thank_notifications_count":0,"messages_count":0,"creation_count":56,"is_realname":true,"has_applying_column":false,"has_add_baike_summary_permission":false,"editor_info":["bio","topic"],"ai_assistant_info":null,"can_create_sub_account":true,"account_type":1,"sub_account_control_status":0,"extra":null}
"""
internal const val CAPTURED_INVALID_REFRESH_BODY = """{"error":{"code":100008,"name":"ERR_INVALID_REFRESH_TOKEN","message":"refresh_token 已失效"}}
"""
internal val CAPTURED_MOBILE_REFRESH_COOKIES = listOf(
    "z_c0=captured-z-c0; Path=/; Domain=zhihu.com; Expires=Fri, 04 Dec 2026 08:08:15 GMT; HttpOnly; Secure",
    "_xsrf=captured-xsrf; path=/; domain=zhihu.com; expires=Sun, 22-Apr-29 08:08:16 GMT",
    "BEC=captured-BEC;Path=/;Max-Age=3600",
)
