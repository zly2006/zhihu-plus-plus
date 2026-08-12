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
import com.github.zly2006.zhihu.util.ZhihuMessageBodyEncryptor
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.MockRequestHandleScope
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
import kotlin.test.assertFailsWith
import kotlin.test.assertIs
import kotlin.test.assertTrue

class ZhihuPhoneLoginClientTest {
    @Test
    fun guestCaptchaCheckAndDigitsRequestMatchOfficialProtocol() = runTest {
        val cookies = mutableMapOf<String, String>()
        var requestIndex = 0
        val httpClient = phoneLoginTestClient(
            MockEngine { request ->
                requestIndex++
                when (requestIndex) {
                    1 -> {
                        assertEquals(HttpMethod.Post, request.method)
                        assertEquals("/api/account/prod/init/udid_guest", request.url.encodedPath)
                        assertTrue(request.headers[HttpHeaders.Authorization].orEmpty().startsWith("oauth "))
                        assertEquals("1355", request.headers["x-app-id"])
                        assertEquals("2", request.headers["x-sign-version"])
                        assertEquals("1700000000", request.headers["x-req-ts"])
                        assertEquals(
                            "32a3795d4beccbacb11cb806d67e155158e3566a",
                            request.headers["x-req-signature"],
                        )
                        assertEquals("11.4.0", request.headers["x-app-version"])
                        assertEquals("44", request.headers["x-page-id"])
                        assertEquals("101_1_1.0", request.headers["x-zse-93"])
                        assertTrue(
                            request.headers[HttpHeaders.UserAgent]
                                .orEmpty()
                                .contains("Android SDK built for arm64"),
                        )
                        assertTrue(request.headers["x-app-za"].orEmpty().contains("Width=1080&Height=2274"))
                        assertEncryptedBody(DEVICE_FORM, request.body)
                        respondJson(
                            """
                            {
                              "udid": "device-id",
                              "guest": {
                                "access_token": "guest-access",
                                "token_type": "bearer",
                                "cookie": {"q_c0": "guest-cookie"}
                              }
                            }
                            """.trimIndent(),
                        )
                    }

                    2 -> {
                        assertEquals(HttpMethod.Get, request.method)
                        assertEquals("/captcha", request.url.encodedPath)
                        assertEquals("bearer guest-access", request.headers[HttpHeaders.Authorization])
                        assertEquals("device-id", request.headers["x-udid"])
                        assertTrue(request.headers[HttpHeaders.Cookie].orEmpty().contains("q_c0=guest-cookie"))
                        respondJson("""{"show_captcha":false}""")
                    }

                    3 -> {
                        assertEquals(HttpMethod.Post, request.method)
                        assertEquals("/api/account/prod/auth/digits", request.url.encodedPath)
                        assertEquals("bearer guest-access", request.headers[HttpHeaders.Authorization])
                        assertEncryptedBody(
                            "username=%2B8613800138000&client_id=8d5227e0aaaa4797a763ac64e0c3b8",
                            request.body,
                        )
                        respondJson("""{"success":true}""")
                    }

                    else -> error("Unexpected request #$requestIndex")
                }
            },
            cookies,
        )
        val client = ZhihuPhoneLoginClient(httpClient, cookies, DEVICE_INFO) { 1_700_000_000L }

        val result = client.requestDigits("138 0013 8000")

        assertIs<ZhihuPhoneDigitsResult.Sent>(result)
        assertEquals("guest-cookie", cookies["q_c0"])
        assertEquals(3, requestIndex)
        httpClient.close()
    }

    @Test
    fun captchaBranchLoadsImageAndVerifiesUserInputBeforeSendingDigits() = runTest {
        val cookies = mutableMapOf<String, String>()
        var requestIndex = 0
        val httpClient = phoneLoginTestClient(
            MockEngine { request ->
                requestIndex++
                when (requestIndex) {
                    1 -> respondJson(INITIALIZATION_RESPONSE)

                    2 -> {
                        assertEquals(HttpMethod.Get, request.method)
                        assertEquals("/captcha", request.url.encodedPath)
                        respondJson("""{"show_captcha":true}""")
                    }

                    3 -> {
                        assertEquals(HttpMethod.Put, request.method)
                        assertEquals("/captcha", request.url.encodedPath)
                        respondJson("""{"show_captcha":true,"img_base64":"image-data"}""")
                    }

                    4 -> {
                        assertEquals(HttpMethod.Post, request.method)
                        assertEquals("/captcha", request.url.encodedPath)
                        assertEncryptedBody("input_text=a7Bc", request.body)
                        respondJson("""{"success":true}""")
                    }

                    else -> error("Unexpected request #$requestIndex")
                }
            },
            cookies,
        )
        val client = ZhihuPhoneLoginClient(httpClient, cookies, DEVICE_INFO) { 1_700_000_000L }

        val result = client.requestDigits("13800138000")
        assertEquals("image-data", assertIs<ZhihuPhoneDigitsResult.CaptchaRequired>(result).imageBase64)
        assertTrue(client.verifyCaptcha(" a7Bc "))

        assertEquals(4, requestIndex)
        httpClient.close()
    }

    @Test
    fun signInUsesDigitsSignatureAndReturnsCompleteMobileSession() = runTest {
        val cookies = mutableMapOf("d_c0" to "device-cookie")
        var requestIndex = 0
        val httpClient = phoneLoginTestClient(
            MockEngine { request ->
                requestIndex++
                when (requestIndex) {
                    1 -> respondJson(
                        """
                        {
                          "udid": "device-id",
                          "guest": {
                            "access_token": "guest-access",
                            "token_type": "bearer",
                            "cookie": {"q_c0": "guest-cookie"}
                          }
                        }
                        """.trimIndent(),
                    )

                    2 -> {
                        assertEquals(HttpMethod.Post, request.method)
                        assertEquals("/api/account/prod/sign_in", request.url.encodedPath)
                        assertEquals("bearer guest-access", request.headers[HttpHeaders.Authorization])
                        assertEncryptedBody(
                            "client_id=8d5227e0aaaa4797a763ac64e0c3b8&digits=123456&grant_type=digits&" +
                                "signature=096465d8a44361e0393c87ab61b0d48a088b2cfb&source=com.zhihu.android&" +
                                "timestamp=1700000000&username=%2B8613800138000",
                            request.body,
                        )
                        respondJson(
                            """
                            {
                              "access_token": "account-access",
                              "refresh_token": "account-refresh",
                              "token_type": "bearer",
                              "expires_in": 3600,
                              "cookie": {
                                "q_c0": "account-q-cookie",
                                "z_c0": "account-z-cookie"
                              }
                            }
                            """.trimIndent(),
                        )
                    }

                    3 -> {
                        assertEquals(HttpMethod.Get, request.method)
                        assertEquals("www.zhihu.com", request.url.host)
                        assertEquals("/", request.url.encodedPath)
                        respond(
                            content = "<!doctype html>",
                            headers = headersOf(HttpHeaders.SetCookie, "d_c0=web-device-cookie; Path=/; Domain=zhihu.com"),
                        )
                    }

                    else -> error("Unexpected request #$requestIndex")
                }
            },
            cookies,
        )
        val client = ZhihuPhoneLoginClient(httpClient, cookies, DEVICE_INFO) { 1_700_000_000L }

        val token = client.signIn("+8613800138000", "123456")

        assertEquals("account-access", token.accessToken)
        assertEquals("account-refresh", token.refreshToken)
        assertEquals("bearer", token.tokenType)
        assertEquals(1_700_003_600L, token.expiresAt)
        assertEquals("web-device-cookie", token.cookies["d_c0"])
        assertEquals("account-z-cookie", token.cookies["z_c0"])
        assertEquals("account-q-cookie", token.cookies["q_c0"])
        httpClient.close()
    }

    @Test
    fun exposesServerErrorCodeForInvalidPhoneNumber() = runTest {
        val cookies = mutableMapOf<String, String>()
        var requestIndex = 0
        val httpClient = phoneLoginTestClient(
            MockEngine { request ->
                requestIndex++
                when (requestIndex) {
                    1 -> respondJson(INITIALIZATION_RESPONSE)
                    2 -> respondJson("""{"show_captcha":false}""")
                    3 -> respondJson(
                        body =
                            """
                            {
                              "error": {
                                "code": 100030,
                                "name": "ERR_BAD_PHONE_NO_FORMAT",
                                "message": "手机号格式错误"
                              }
                            }
                            """.trimIndent(),
                        status = HttpStatusCode.BadRequest,
                    )

                    else -> error("Unexpected request #$requestIndex")
                }
            },
            cookies,
        )
        val client = ZhihuPhoneLoginClient(httpClient, cookies, DEVICE_INFO) { 1_700_000_000L }

        val error = assertFailsWith<ZhihuPhoneLoginException> {
            client.requestDigits("13800138000")
        }

        assertEquals(HttpStatusCode.BadRequest.value, error.statusCode)
        assertEquals(100030, error.errorCode)
        assertTrue(error.message.orEmpty().contains("手机号格式错误"))
        httpClient.close()
    }

    @Test
    fun ticketNeededErrorRechecksCaptchaAndRetriesDigitsOnce() = runTest {
        val cookies = mutableMapOf<String, String>()
        var requestIndex = 0
        val httpClient = phoneLoginTestClient(
            MockEngine { request ->
                requestIndex++
                when (requestIndex) {
                    1 -> respondJson(INITIALIZATION_RESPONSE)
                    2 -> respondJson("""{"show_captcha":false}""")
                    3 -> respondJson(
                        body = """{"error":{"code":120005,"message":"需要重新检查验证码"}}""",
                        status = HttpStatusCode.BadRequest,
                    )

                    4 -> {
                        assertEquals(HttpMethod.Get, request.method)
                        assertEquals("/captcha", request.url.encodedPath)
                        respondJson("""{"show_captcha":false}""")
                    }

                    5 -> {
                        assertEquals("/api/account/prod/auth/digits", request.url.encodedPath)
                        respondJson("""{"success":true}""")
                    }

                    else -> error("Unexpected request #$requestIndex")
                }
            },
            cookies,
        )
        val client = ZhihuPhoneLoginClient(httpClient, cookies, DEVICE_INFO) { 1_700_000_000L }

        assertIs<ZhihuPhoneDigitsResult.Sent>(client.requestDigits("13800138000"))
        assertEquals(5, requestIndex)
        httpClient.close()
    }

    private fun phoneLoginTestClient(
        engine: MockEngine,
        cookies: MutableMap<String, String>,
    ): HttpClient = HttpClient(engine) {
        installZhihuCommonClientConfig(
            cookies = cookies,
            userAgent = "test-agent",
        )
    }

    private fun MockRequestHandleScope.respondJson(
        body: String,
        status: HttpStatusCode = HttpStatusCode.OK,
    ) = respond(
        content = body,
        status = status,
        headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString()),
    )

    private fun assertEncryptedBody(
        expectedPlainText: String,
        body: Any,
    ) {
        assertEquals(
            ZhihuMessageBodyEncryptor.encrypt(expectedPlainText),
            (body as TextContent).text,
        )
    }

    private companion object {
        val DEVICE_INFO = ZhihuPhoneLoginDeviceInfo(
            timezoneOffsetSeconds = 28_800,
            appInstallTimeMillis = 1_700_000_000_000,
            notificationEnabled = false,
            bluetoothAvailable = true,
            phoneBrand = "Android",
            phoneModel = "Android SDK built for arm64",
            androidRelease = "12",
            cpuType = "aarch64",
            cpuCount = 4,
            cpuUsage = "0.0",
            totalMemoryMegabytes = 256,
            freeMemoryMegabytes = 128,
            totalStorageMegabytes = 65_536,
            freeStorageMegabytes = 32_768,
        )

        const val DEVICE_FORM =
            "app_build=40408&app_install_time=1700000000000&app_ticket=interface+is+empty&" +
                "app_version=11.4.0&bt_ck=1&bundle_id=com.zhihu.android&cp_ct=4&cp_tp=aarch64&" +
                "cp_us=0.0&d_n=Android+SDK+built+for+arm64&fr_mem=128&fr_st=32768&latitude=0.0&" +
                "longitude=0.0&nt_st=0&ph_br=Android&ph_md=Android+SDK+built+for+arm64&" +
                "ph_os=Android+12&pre_install=InterfaceIsNull&tt_mem=256&tt_st=65536&tz_of=28800&" +
                "zx_expired=0"

        const val INITIALIZATION_RESPONSE =
            """{"udid":"device-id","guest":{"access_token":"guest-access","token_type":"bearer"}}"""
    }
}
