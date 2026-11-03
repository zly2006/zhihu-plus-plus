package com.github.zly2006.zhihu.account

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.v2.runDesktopComposeUiTest
import com.github.zly2006.zhihu.data.installZhihuCommonClientConfig
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import java.util.concurrent.atomic.AtomicInteger
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@OptIn(ExperimentalTestApi::class)
class QrLoginPaneTest {
    @Test
    fun refreshUsesTheSameCookieStorageAsTheRequestHeaders() {
        val cookies = mutableMapOf("_xsrf" to "initial")
        val attempts = AtomicInteger()
        val requests = AtomicInteger()
        val xsrfHeaders = mutableListOf<String?>()
        val client = HttpClient(
            MockEngine { request ->
                requests.incrementAndGet()
                if (request.url.encodedPath.endsWith("/scan_info")) {
                    respond("""{"status":2}""", HttpStatusCode.OK, headersOf(HttpHeaders.ContentType, "application/json"))
                } else if (request.url.encodedPath.endsWith("/qrcode")) {
                    val attempt = attempts.incrementAndGet()
                    synchronized(xsrfHeaders) { xsrfHeaders += request.headers["x-xsrftoken"] }
                    respond(
                        """{"token":"test-$attempt","link":"https://www.zhihu.com/test-qr","expires_at":120}""",
                        HttpStatusCode.OK,
                        headersOf(
                            HttpHeaders.ContentType to listOf("application/json"),
                            HttpHeaders.SetCookie to listOf("_xsrf=attempt-$attempt; Path=/; Domain=.zhihu.com"),
                        ),
                    )
                } else {
                    respond("{}", HttpStatusCode.OK, headersOf(HttpHeaders.ContentType, "application/json"))
                }
            },
        ) {
            installZhihuCommonClientConfig(cookies, ZHIHU_DESKTOP_USER_AGENT)
        }
        try {
            runDesktopComposeUiTest(width = 480, height = 640) {
                setContent {
                    MaterialTheme {
                        SharedQrLoginPane(
                            client = client,
                            cookies = cookies,
                            generateQrBitmap = { ImageBitmap(1, 1) },
                            onLoginSuccess = { error("过期二维码不能验证为登录成功") },
                        )
                    }
                }
                waitUntil(timeoutMillis = 10_000) {
                    requests.get() >= 5 && onAllNodesWithText("二维码已过期，请重试").fetchSemanticsNodes().size == 1
                }
                onNodeWithTag("qr_login_status").assertTextEquals("二维码已过期，请重试")
                onNodeWithTag("qr_login_retry").performClick()
                waitUntil(timeoutMillis = 10_000) {
                    requests.get() >= 10 && onAllNodesWithText("二维码已过期，请重试").fetchSemanticsNodes().size == 1
                }
                onNodeWithTag("qr_login_status").assertTextEquals("二维码已过期，请重试")
                assertEquals(listOf("initial", "attempt-1"), synchronized(xsrfHeaders) { xsrfHeaders.toList() })
                assertEquals("attempt-2", cookies["_xsrf"])
            }
        } finally {
            client.close()
        }
    }

    @Test
    fun malformedLoginResponseDoesNotExposeCredentialsInStatus() {
        val client = HttpClient(
            MockEngine { request ->
                respond(
                    if (request.url.encodedPath.endsWith("/qrcode")) {
                        """{"token":{},"cookie":"z_c0=private-test-value","link":"https://www.zhihu.com/test-qr"}"""
                    } else {
                        "{}"
                    },
                    HttpStatusCode.OK,
                    headersOf(HttpHeaders.ContentType, "application/json"),
                )
            },
        )
        try {
            runDesktopComposeUiTest(width = 480, height = 640) {
                setContent {
                    MaterialTheme {
                        SharedQrLoginPane(client = client, cookies = mutableMapOf(), onLoginSuccess = { false })
                    }
                }
                waitUntil(timeoutMillis = 10_000) {
                    onAllNodesWithText("知乎登录响应格式异常，请重试").fetchSemanticsNodes().size == 1
                }
                onNodeWithTag("qr_login_status").assertTextEquals("知乎登录响应格式异常，请重试")
            }
        } finally {
            client.close()
        }
    }

    @Test
    fun apiErrorMessageIsShownInsteadOfExpiredQrCode() {
        val requests = AtomicInteger()
        val client = HttpClient(
            MockEngine { request ->
                requests.incrementAndGet()
                respond(
                    if (request.url.encodedPath.endsWith("/qrcode")) {
                        """{"error":{"code":40362,"message":"异常请求，请稍后再试"}}"""
                    } else {
                        "{}"
                    },
                    HttpStatusCode.OK,
                    headersOf(HttpHeaders.ContentType, "application/json"),
                )
            },
        )
        try {
            runDesktopComposeUiTest(width = 480, height = 640) {
                setContent {
                    MaterialTheme {
                        SharedQrLoginPane(
                            client = client,
                            cookies = mutableMapOf(),
                            onLoginSuccess = { error("错误响应不能验证为登录成功") },
                        )
                    }
                }
                waitUntil(timeoutMillis = 10_000) {
                    onAllNodesWithText("异常请求，请稍后再试").fetchSemanticsNodes().size == 1
                }
                onNodeWithTag("qr_login_status").assertTextEquals("异常请求，请稍后再试")
                assertTrue(requests.get() == 4)
            }
        } finally {
            client.close()
        }
    }
}
