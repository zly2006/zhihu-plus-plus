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

package com.github.zly2006.zhihu.account

import com.github.zly2006.zhihu.data.installZhihuCommonClientConfig
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.time.Clock

class QrLoginClientTest {
    @Test
    fun initializationStopsAtEachForbiddenResponse() = runTest {
        for (blockedRequest in 1..3) {
            var requests = 0
            val engine = MockEngine {
                requests += 1
                respond(
                    content = if (requests == blockedRequest) {
                        """{"error":{"code":40352,"message":"请完成安全验证","redirect":"https://www.zhihu.com/account/risk_control/"}}"""
                    } else {
                        "{}"
                    },
                    status = if (requests == blockedRequest) HttpStatusCode.Forbidden else HttpStatusCode.OK,
                    headers = headersOf(HttpHeaders.ContentType, "application/json"),
                )
            }
            val client = HttpClient(engine)
            try {
                val error = assertFailsWith<IllegalStateException> {
                    prefetchQrLoginContext(client, emptyMap())
                }
                assertTrue(error.message.orEmpty().contains("请完成安全验证"))
                assertEquals(blockedRequest, requests)
            } finally {
                client.close()
            }
        }
    }

    @Test
    fun qrCreationPreservesForbiddenMessage() = runTest {
        val client = HttpClient(
            MockEngine {
                respond(
                    """{"error":{"code":40352,"message":"请完成安全验证","redirect":"https://www.zhihu.com/account/risk_control/"}}""",
                    HttpStatusCode.Forbidden,
                    headersOf(HttpHeaders.ContentType, "application/json"),
                )
            },
        )
        try {
            val error = assertFailsWith<QrLoginRiskControlException> {
                requestQrCode(client, emptyMap())
            }
            assertTrue(error.message.orEmpty().contains("请完成安全验证"))
            assertEquals(ZHIHU_RISK_CONTROL_URL, error.redirectUrl)
        } finally {
            client.close()
        }
    }

    @Test
    fun errorParsingDoesNotDependOnUnrelatedResponseFields() = runTest {
        val client = HttpClient(
            MockEngine {
                respond(
                    """{"status":{},"error":{"code":40352,"message":"请完成安全验证","redirect":"https://www.zhihu.com/account/risk_control/?next=scan"}}""",
                    HttpStatusCode.Forbidden,
                    headersOf(HttpHeaders.ContentType, "application/json"),
                )
            },
        )
        try {
            val error = assertFailsWith<QrLoginRiskControlException> {
                requestQrCode(client, emptyMap())
            }
            assertEquals("请完成安全验证", error.message)
            assertEquals("https://www.zhihu.com/account/risk_control/?next=scan", error.redirectUrl)
        } finally {
            client.close()
        }
    }

    @Test
    fun pollingDoesNotSwallowApiErrorReturnedWithOkStatus() = runBlocking {
        var requests = 0
        val client = HttpClient(
            MockEngine {
                requests += 1
                respond(
                    if (requests == 1) {
                        """{"error":{"code":40362,"message":"异常请求，请稍后再试"}}"""
                    } else {
                        """{"status":2}"""
                    },
                    HttpStatusCode.OK,
                    headersOf(HttpHeaders.ContentType, "application/json"),
                )
            },
        )
        try {
            val error = assertFailsWith<IllegalStateException> {
                pollQrCodeLogin(
                    client,
                    mutableMapOf(),
                    "test-token",
                    normalizeDeadline(null),
                    onScanned = { error("服务端错误不应进入扫码确认") },
                    onRiskControl = { _, _ -> error("没有验证地址的错误不应猜测短信验证") },
                )
            }
            assertTrue(error.message.orEmpty().contains("异常请求，请稍后再试"))
            assertEquals(1, requests)
        } finally {
            client.close()
        }
    }

    @Test
    fun pollingPausesAtChallengeAndUsesVerifiedCookiesOnRetry() = runBlocking {
        val cookies = mutableMapOf("_xsrf" to "before")
        var requests = 0
        val headers = mutableListOf<String>()
        val client = HttpClient(
            MockEngine { request ->
                requests += 1
                headers += request.headers[HttpHeaders.Cookie].orEmpty()
                when (requests) {
                    1 -> respond(
                        """{"error":{"code":40352,"message":"请完成短信安全验证","redirect":"https://www.zhihu.com/account/risk_control/"}}""",
                        HttpStatusCode.Forbidden,
                        headersOf(HttpHeaders.ContentType, "application/json"),
                    )
                    else -> respond(
                        """{"logged_in":true,"z_c0":"authenticated"}""",
                        HttpStatusCode.OK,
                        headersOf(HttpHeaders.ContentType, "application/json"),
                    )
                }
            },
        ) {
            installZhihuCommonClientConfig(cookies, ZHIHU_DESKTOP_USER_AGENT)
        }
        try {
            var challenge: Pair<String?, String?>? = null
            assertFalse(
                pollQrCodeLogin(
                    client,
                    cookies,
                    "test-token",
                    normalizeDeadline(null),
                    onScanned = {},
                    onRiskControl = { message, redirect -> challenge = message to redirect },
                ),
            )
            assertEquals("请完成短信安全验证" to ZHIHU_RISK_CONTROL_URL, challenge)
            assertEquals(1, requests)
            cookies["_xsrf"] = "verified"
            cookies["capsion_ticket"] = "test-ticket"
            assertTrue(
                pollQrCodeLogin(
                    client,
                    cookies,
                    "retry-token",
                    normalizeDeadline(null),
                    onScanned = {},
                    onRiskControl = { _, _ -> error("验证后的模拟请求不应进入风控") },
                ),
            )
            assertEquals(2, requests)
            assertTrue(headers.last().contains("_xsrf=verified"))
            assertTrue(headers.last().contains("capsion_ticket=test-ticket"))
            assertEquals("authenticated", cookies["z_c0"])
        } finally {
            client.close()
        }
    }

    @Test
    fun initializationDoesNotSwallowCancellation() = runTest {
        var requests = 0
        val client = HttpClient(
            MockEngine {
                requests += 1
                if (requests == 2) throw CancellationException("取消初始化")
                respond("{}", HttpStatusCode.OK, headersOf(HttpHeaders.ContentType, "application/json"))
            },
        )
        try {
            assertFailsWith<CancellationException> {
                prefetchQrLoginContext(client, emptyMap())
            }
            assertEquals(2, requests)
        } finally {
            client.close()
        }
    }

    @Test
    fun parsesAndSyncsQrCookies() {
        val cookies = parseCookieAssignments("z_c0=abc; Path=/; Domain=.zhihu.com; _xsrf=def; HttpOnly")
        assertEquals(mapOf("z_c0" to "abc", "_xsrf" to "def"), cookies)

        val syncedCookies = mutableMapOf<String, String>()
        syncCookiesFromScanInfo(syncedCookies, ZhihuQrScanInfo(zC0 = "token"))

        assertEquals("token", syncedCookies["z_c0"])
    }

    @Test
    fun recognizesQrTerminalStates() {
        assertTrue(isQrLoginSuccessful(ZhihuQrScanInfo(loginStatus = "login_success")))
        assertFalse(isQrLoginSuccessful(ZhihuQrScanInfo(loginStatus = "waiting")))
        assertTrue(isQrLoginExpired(ZhihuQrScanInfo(status = 2)))
        assertTrue(isQrLoginExpired(ZhihuQrScanInfo(loginStatus = "qr_code_expired")))
        assertFalse(isQrLoginExpired(ZhihuQrScanInfo(status = 1, loginStatus = "waiting")))
    }

    @Test
    fun normalizesQrDeadlineUnitsAndStaleValues() {
        val before = Clock.System.now().toEpochMilliseconds()
        val epochSeconds = Clock.System.now().toEpochMilliseconds() / 1000 + 600
        val epochMillis = Clock.System.now().toEpochMilliseconds() + 600_000

        listOf(600L, 600_000L).forEach { expiresAt ->
            val deadline = normalizeDeadline(expiresAt)
            assertTrue(deadline >= before + 600_000)
            assertTrue(deadline < before + 601_000)
        }
        val staleDeadline = normalizeDeadline(1_000_000_000)
        assertTrue(staleDeadline >= before + 120_000)
        assertTrue(staleDeadline < before + 121_000)
        assertEquals(epochSeconds * 1000, normalizeDeadline(epochSeconds))
        assertEquals(epochMillis, normalizeDeadline(epochMillis))
    }
}
