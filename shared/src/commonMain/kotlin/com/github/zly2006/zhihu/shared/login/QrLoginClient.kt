package com.github.zly2006.zhihu.shared.login

import com.github.zly2006.zhihu.shared.data.ZhihuJson
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

const val ZHIHU_HOME_URL = "https://www.zhihu.com/"
const val ZHIHU_SIGNIN_URL = "https://www.zhihu.com/signin?next=%2F"
const val ZHIHU_SIGNIN_REFERER_URL = "https://www.zhihu.com/signin"
const val ZHIHU_RISK_CONTROL_URL = "https://www.zhihu.com/account/risk_control/"
const val ZHIHU_DESKTOP_USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/145.0.0.0 Safari/537.36"

private const val UDID_URL = "https://www.zhihu.com/udid"
private const val CAPTCHA_V2_URL = "https://www.zhihu.com/api/v3/oauth/captcha/v2?type=captcha_sign_in"
private const val QRCODE_URL = "https://www.zhihu.com/api/v3/account/api/login/qrcode"
private const val ME_URL = "https://www.zhihu.com/api/v4/me"
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
    val result = decodeZhihuLoginJson(
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

    while (currentTimeMillis() <= deadline) {
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
                decodeZhihuLoginJson(
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
                        client.get(ME_URL) {
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
        headers["x-zse-93"] = "101_3_3.0"
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
        return currentTimeMillis() + 120_000
    }
    return if (expiresAt < 10_000_000_000L) {
        expiresAt * 1000
    } else {
        expiresAt
    }
}

internal fun decodeZhihuLoginJson(
    serializer: KSerializer<ZhihuQrCodeResponse>,
    json: JsonElement,
): ZhihuQrCodeResponse = decodeZhihuLoginJsonTyped(serializer, json)

internal fun decodeZhihuLoginJson(
    serializer: KSerializer<ZhihuQrScanInfo>,
    json: JsonElement,
): ZhihuQrScanInfo = decodeZhihuLoginJsonTyped(serializer, json)

private fun <T> decodeZhihuLoginJsonTyped(serializer: KSerializer<T>, json: JsonElement): T {
    val normalized = ZhihuJson.snakeCaseToCamelCase(json)
    try {
        return ZhihuJson.json.decodeFromJsonElement(serializer, normalized)
    } catch (e: SerializationException) {
        throw SerializationException("Failed to parse QR login JSON: ${e.message}\n\n$normalized", e)
    }
}
