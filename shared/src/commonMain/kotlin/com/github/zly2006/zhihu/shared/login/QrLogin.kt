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

package com.github.zly2006.zhihu.shared.login
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.github.zly2006.zhihu.shared.data.ZHIHU_ME_URL
import com.github.zly2006.zhihu.shared.data.ZhihuJson
import com.github.zly2006.zhihu.shared.util.ZHIHU_WEB_ZSE93
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.decodeFromJsonElement
import kotlin.time.Clock

const val ZHIHU_HOME_URL = "https://www.zhihu.com/"
const val ZHIHU_SIGNIN_URL = "https://www.zhihu.com/signin?next=%2F"
const val ZHIHU_SIGNIN_REFERER_URL = "https://www.zhihu.com/signin"
const val ZHIHU_RISK_CONTROL_URL = "https://www.zhihu.com/account/risk_control/"
const val ZHIHU_DESKTOP_USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/145.0.0.0 Safari/537.36"

private const val UDID_URL = "https://www.zhihu.com/udid"
private const val CAPTCHA_V2_URL = "https://www.zhihu.com/api/v3/oauth/captcha/v2?type=captcha_sign_in"
private const val QRCODE_URL = "https://www.zhihu.com/api/v3/account/api/login/qrcode"
private const val DESKTOP_SEC_CH_UA = "\"Not:A-Brand\";v=\"99\", \"Google Chrome\";v=\"145\", \"Chromium\";v=\"145\""
private const val DESKTOP_SEC_CH_UA_MOBILE = "?0"
private const val DESKTOP_SEC_CH_UA_PLATFORM = "\"Windows\""

@Serializable
data class ZhihuQrCodeResponse(
    val expiresAt: Long? = null,
    val link: String? = null,
    val token: String? = null,
    val qrcodeToken: String? = null,
)

@Serializable
data class ZhihuQrScanError(
    val needLogin: Boolean? = null,
    val redirect: String? = null,
    val code: Int? = null,
    val message: String? = null,
)

@Serializable
data class ZhihuQrScanInfo(
    val status: Int? = null,
    val cookie: String? = null,
    val cookies: String? = null,
    val zC0: String? = null,
    val userId: String? = null,
    val accessToken: String? = null,
    val success: Boolean? = null,
    val loggedIn: Boolean? = null,
    val loginStatus: String? = null,
    val error: ZhihuQrScanError? = null,
)

suspend fun prefetchQrLoginContext(
    client: HttpClient,
    cookies: Map<String, String>,
) {
    client.get(ZHIHU_SIGNIN_URL) {
        createDesktopHeaders(ZHIHU_HOME_URL).forEach { (key, value) ->
            header(key, value)
        }
    }

    runCatching {
        client.post(UDID_URL) {
            createZhihuLoginHeaders(cookies, ZHIHU_SIGNIN_REFERER_URL).forEach { (key, value) ->
                header(key, value)
            }
            setBody("{}")
        }
    }

    runCatching {
        client.get(CAPTCHA_V2_URL) {
            createZhihuLoginHeaders(cookies, ZHIHU_SIGNIN_REFERER_URL).forEach { (key, value) ->
                header(key, value)
            }
        }
    }
}

suspend fun requestQrCode(
    client: HttpClient,
    cookies: Map<String, String>,
): ZhihuQrCodeResponse {
    val response = client.post(QRCODE_URL) {
        createZhihuLoginHeaders(cookies, ZHIHU_SIGNIN_REFERER_URL).forEach { (key, value) ->
            header(key, value)
        }
        setBody("{}")
    }
    val body = response.bodyAsText()
    val result = decodeZhihuLoginJsonTyped(
        ZhihuQrCodeResponse.serializer(),
        ZhihuJson.json.parseToJsonElement(body),
    )
    val token = result.token ?: result.qrcodeToken
    if (response.status.value >= 400 || token.isNullOrBlank() || result.link.isNullOrBlank()) {
        throw IllegalStateException("二维码获取失败：${response.status.value}")
    }
    return result.copy(token = token)
}

suspend fun pollQrCodeLogin(
    client: HttpClient,
    cookies: MutableMap<String, String>,
    token: String,
    deadline: Long,
    onScanned: () -> Unit,
    onRiskControl: (String?, String?) -> Unit,
): Boolean {
    var hasPromptedConfirm = false
    var hasPromptedRiskControl = false

    while (currentEpochMillis() <= deadline) {
        currentCoroutineContext().ensureActive()

        try {
            val response = client.get("$QRCODE_URL/$token/scan_info") {
                createZhihuLoginHeaders(cookies, ZHIHU_SIGNIN_URL, isPolling = true).forEach { (key, value) ->
                    header(key, value)
                }
            }
            if (response.status == HttpStatusCode.Forbidden) {
                onRiskControl(
                    "知乎限制了当前网络环境的登录请求，请先完成网络环境验证。",
                    ZHIHU_RISK_CONTROL_URL,
                )
                return false
            }
            val body = response.bodyAsText()
            val scanInfo = runCatching {
                decodeZhihuLoginJsonTyped(
                    ZhihuQrScanInfo.serializer(),
                    ZhihuJson.json.parseToJsonElement(body),
                )
            }.getOrDefault(ZhihuQrScanInfo())

            syncCookiesFromScanInfo(cookies, scanInfo)

            if (isRiskControlResponse(response.status.value, scanInfo)) {
                if (!hasPromptedRiskControl) {
                    hasPromptedRiskControl = true
                    onRiskControl(scanInfo.error?.message, scanInfo.error?.redirect)
                }
                return false
            }

            if (scanInfo.status == 1 && !hasPromptedConfirm) {
                hasPromptedConfirm = true
                onScanned()
            }

            val loginSucceeded = isQrLoginSuccessful(scanInfo)
            if (loginSucceeded || cookies.containsKey("z_c0")) {
                if (!cookies.containsKey("z_c0")) {
                    runCatching {
                        client.get(ZHIHU_ME_URL) {
                            createZhihuLoginHeaders(cookies, ZHIHU_SIGNIN_URL, isPolling = true).forEach { (key, value) ->
                                header(key, value)
                            }
                        }
                    }
                }
                if (loginSucceeded || cookies.containsKey("z_c0")) {
                    return true
                }
            }
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            // 临时网络抖动时继续轮询
        }

        delay(500)
    }

    return false
}

fun createDesktopHeaders(referer: String? = null): MutableMap<String, String> {
    val headers = mutableMapOf(
        "accept-encoding" to "gzip",
        "Accept" to "application/json, text/plain, */*",
        "Accept-Language" to "en-US,en;q=0.9",
        "User-Agent" to ZHIHU_DESKTOP_USER_AGENT,
        "sec-ch-ua" to DESKTOP_SEC_CH_UA,
        "sec-ch-ua-mobile" to DESKTOP_SEC_CH_UA_MOBILE,
        "sec-ch-ua-platform" to DESKTOP_SEC_CH_UA_PLATFORM,
    )
    if (!referer.isNullOrBlank()) {
        headers["Referer"] = referer
    }
    return headers
}

fun createZhihuLoginHeaders(
    cookies: Map<String, String>,
    referer: String,
    isPolling: Boolean = false,
): Map<String, String> {
    val headers = createDesktopHeaders(referer)
    headers["Origin"] = ZHIHU_HOME_URL.removeSuffix("/")
    headers["x-requested-with"] = "fetch"
    headers["content-type"] = "application/json;charset=UTF-8"
    if (isPolling) {
        headers["Accept"] = "*/*"
        headers["sec-fetch-dest"] = "empty"
        headers["sec-fetch-mode"] = "cors"
        headers["sec-fetch-site"] = "same-origin"
        headers["x-zse-93"] = ZHIHU_WEB_ZSE93
    }
    cookies["_xsrf"]?.let {
        headers["x-xsrftoken"] = it
    }
    return headers
}

fun syncCookiesFromScanInfo(
    cookies: MutableMap<String, String>,
    scanInfo: ZhihuQrScanInfo,
) {
    val rawCookie = listOf(scanInfo.cookie, scanInfo.cookies)
        .filterIsInstance<String>()
        .joinToString(";")
    cookies.putAll(parseCookieAssignments(rawCookie))
    scanInfo.zC0
        ?.takeIf { it.isNotBlank() }
        ?.let { cookies["z_c0"] = it }
}

fun parseCookieAssignments(rawCookie: String): Map<String, String> {
    val skipCookieAttributes = setOf("Domain", "Path", "Expires", "Max-Age", "HttpOnly", "Secure", "SameSite")
    return rawCookie
        .split(";")
        .mapNotNull { item ->
            val trimmed = item.trim()
            if (!trimmed.contains("=")) {
                return@mapNotNull null
            }
            val name = trimmed.substringBefore("=").trim()
            if (name.isBlank() || skipCookieAttributes.contains(name)) {
                return@mapNotNull null
            }
            val value = trimmed.substringAfter("=").trim()
            if (value.isBlank()) {
                return@mapNotNull null
            }
            name to value
        }.toMap()
}

fun isQrLoginSuccessful(scanInfo: ZhihuQrScanInfo): Boolean {
    if (scanInfo.userId != null || !scanInfo.accessToken.isNullOrBlank() || scanInfo.success == true || scanInfo.loggedIn == true) {
        return true
    }
    val loginStatus = scanInfo.loginStatus.orEmpty().uppercase()
    return loginStatus in setOf("CONFIRMED", "LOGIN_SUCCESS", "SUCCESS", "OK", "LOGGED_IN")
}

fun isRiskControlResponse(
    statusCode: Int,
    scanInfo: ZhihuQrScanInfo,
): Boolean {
    val errorBody = scanInfo.error ?: return false
    return statusCode == 403 && (errorBody.code == 40352 || errorBody.needLogin == true)
}

fun normalizeDeadline(expiresAt: Long?): Long {
    if (expiresAt == null || expiresAt <= 0) {
        return currentEpochMillis() + 120_000
    }
    return if (expiresAt < 10_000_000_000L) {
        expiresAt * 1000
    } else {
        expiresAt
    }
}

private fun currentEpochMillis(): Long = Clock.System.now().toEpochMilliseconds()

private fun <T> decodeZhihuLoginJsonTyped(serializer: KSerializer<T>, json: JsonElement): T {
    val normalized = ZhihuJson.snakeCaseToCamelCase(json)
    try {
        return ZhihuJson.json.decodeFromJsonElement(serializer, normalized)
    } catch (e: SerializationException) {
        throw SerializationException("Failed to parse QR login JSON: ${e.message}\n\n$normalized", e)
    }
}

@Composable
fun SharedQrLoginPane(
    createClient: (MutableMap<String, String>) -> HttpClient,
    onLoginSuccess: suspend (Map<String, String>) -> Boolean,
    modifier: Modifier = Modifier,
    generateQrBitmap: (String) -> ImageBitmap = ::generateQrLoginBitmap,
    initialCookies: Map<String, String> = emptyMap(),
    qrReadyMessage: String = "请打开知乎++ App 扫一扫",
    onQrReady: () -> Unit = {},
    readRiskControlCookies: (String) -> Map<String, String> = { emptyMap() },
    riskControlContent: (
        @Composable (
            url: String,
            cookies: Map<String, String>,
            onCookiesChanged: (Map<String, String>) -> Unit,
        ) -> Unit
    )? = null,
) {
    var refreshKey by rememberSaveable { mutableIntStateOf(0) }
    var qrBitmap by remember { mutableStateOf<ImageBitmap?>(null) }
    var statusText by remember { mutableStateOf("正在获取二维码") }
    var sessionCookies by remember { mutableStateOf(initialCookies.toMap()) }
    var riskControlUrl by remember { mutableStateOf<String?>(null) }
    var riskControlMessage by remember { mutableStateOf<String?>(null) }
    var isWorking by remember { mutableStateOf(true) }

    LaunchedEffect(refreshKey) {
        val cookies = sessionCookies.toMutableMap()
        val client = createClient(cookies)
        qrBitmap = null
        statusText = "正在获取二维码"
        riskControlUrl = null
        isWorking = true

        try {
            prefetchQrLoginContext(client, cookies)
            val qrCode = requestQrCode(client, cookies)
            sessionCookies = cookies.toMap()
            val qrLink = qrCode.link ?: throw IllegalStateException("知乎没有返回二维码链接")
            val qrToken = qrCode.token ?: qrCode.qrcodeToken ?: throw IllegalStateException("知乎没有返回二维码 token")
            qrBitmap = generateQrBitmap(qrLink)
            statusText = qrReadyMessage
            onQrReady()

            val success = pollQrCodeLogin(
                client = client,
                cookies = cookies,
                token = qrToken,
                deadline = normalizeDeadline(qrCode.expiresAt),
                onScanned = {
                    statusText = "请在知乎 App 上确认登录"
                },
                onRiskControl = { message, redirectUrl ->
                    sessionCookies = cookies.toMap()
                    riskControlMessage = message ?: "知乎需要验证当前网络环境"
                    riskControlUrl = redirectUrl ?: ZHIHU_RISK_CONTROL_URL
                    statusText = riskControlMessage ?: "知乎需要验证当前网络环境"
                },
            )

            if (success) {
                statusText = "正在验证登录"
                isWorking = false
                statusText = if (onLoginSuccess(cookies)) {
                    "登录成功"
                } else {
                    "登录结果验证失败，请重试"
                }
            } else if (!riskControlUrl.isNullOrBlank()) {
                isWorking = false
            } else {
                statusText = "二维码已过期，请重试"
                isWorking = false
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            statusText = e.message ?: "二维码获取失败，请重试"
            isWorking = false
        } finally {
            client.close()
        }
    }

    val currentRiskControlUrl = riskControlUrl
    if (!currentRiskControlUrl.isNullOrBlank()) {
        Column(
            modifier = modifier
                .fillMaxSize()
                .testTag("qr_risk_control_content"),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = riskControlMessage ?: "请先完成知乎的网络环境验证",
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedButton(
                onClick = {
                    sessionCookies = sessionCookies + readRiskControlCookies(currentRiskControlUrl)
                    riskControlUrl = null
                    riskControlMessage = null
                    refreshKey += 1
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("qr_risk_control_continue"),
            ) {
                Text("完成验证后继续扫码")
            }
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
            ) {
                riskControlContent?.invoke(
                    currentRiskControlUrl,
                    sessionCookies,
                ) { updatedCookies ->
                    sessionCookies = sessionCookies + updatedCookies
                } ?: Text(
                    text = "当前被知乎风控，请过几个小时再试",
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    textAlign = TextAlign.Center,
                )
            }
        }
        return
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .testTag("qr_login_content"),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        if (qrBitmap != null) {
            Image(
                bitmap = qrBitmap!!,
                contentDescription = "知乎登录二维码",
                modifier = Modifier
                    .size(260.dp)
                    .testTag("qr_login_image"),
            )
            Spacer(modifier = Modifier.size(16.dp))
        } else if (isWorking) {
            CircularProgressIndicator(
                modifier = Modifier.testTag("qr_login_loading"),
            )
            Spacer(modifier = Modifier.size(16.dp))
        }

        Text(
            text = statusText,
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .testTag("qr_login_status"),
        )

        if (!riskControlMessage.isNullOrBlank()) {
            Spacer(modifier = Modifier.size(12.dp))
            Text(
                text = riskControlMessage.orEmpty(),
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
        }

        Spacer(modifier = Modifier.size(20.dp))

        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            OutlinedButton(
                onClick = {
                    riskControlUrl = null
                    riskControlMessage = null
                    refreshKey += 1
                },
                modifier = Modifier.testTag("qr_login_retry"),
            ) {
                Text("刷新二维码")
            }
        }
    }
}

expect fun generateQrLoginBitmap(content: String): ImageBitmap
