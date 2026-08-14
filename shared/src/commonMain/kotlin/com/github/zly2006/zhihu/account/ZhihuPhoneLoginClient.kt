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

import com.github.zly2006.zhihu.data.ZhihuJson
import com.github.zly2006.zhihu.data.toCookieHeaderString
import com.github.zly2006.zhihu.util.ZhihuMessageBodyEncryptor
import com.github.zly2006.zhihu.util.hmacSha1Hex
import io.ktor.client.HttpClient
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.accept
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.Parameters
import io.ktor.http.contentType
import io.ktor.http.formUrlEncode
import io.ktor.http.isSuccess
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

private const val PHONE_LOGIN_API_BASE_URL = "https://api.zhihu.com"
private const val DEVICE_GUEST_INIT_PATH = "/api/account/prod/init/udid_guest"
private const val CAPTCHA_PATH = "/captcha"
private const val AUTH_DIGITS_PATH = "/api/account/prod/auth/digits"
private const val SIGN_IN_PATH = "/api/account/prod/sign_in"

private const val MOBILE_CLIENT_ID = "8d5227e0aaaa4797a763ac64e0c3b8"
private const val MOBILE_CLIENT_SECRET = "ecbefbf6b17e47ecb9035107866380"
private const val MOBILE_SOURCE = "com.zhihu.android"
private const val DIGITS_GRANT_TYPE = "digits"
private const val CLOUD_APP_ID = "1355"
private const val CLOUD_APP_SECRET = "dd49a835-56e7-4a0f-95b5-efd51ea5397f"
private const val CLOUD_SIGN_VERSION = "2"

const val ZHIHU_ANDROID_PHONE_LOGIN_USER_AGENT =
    "com.zhihu.android/Futureve/11.4.0 Mozilla/5.0 (Linux; Android 12; Android SDK built for arm64 " +
        "Build/SE1A.220621.001; wv) AppleWebKit/537.36 (KHTML, like Gecko) Version/4.0 " +
        "Chrome/57.0.1000.10 Mobile Safari/537.36"

data class ZhihuMobileLoginToken(
    val accessToken: String,
    val refreshToken: String?,
    val tokenType: String,
    val expiresAt: Long?,
    val cookies: Map<String, String>,
)

data class ZhihuPhoneLoginDeviceInfo(
    val timezoneOffsetSeconds: Long,
    val appInstallTimeMillis: Long,
    val notificationEnabled: Boolean,
    val bluetoothAvailable: Boolean,
    val phoneBrand: String,
    val phoneModel: String,
    val androidRelease: String,
    val cpuType: String,
    val cpuCount: Int,
    val cpuUsage: String,
    val totalMemoryMegabytes: Int,
    val freeMemoryMegabytes: Int,
    val totalStorageMegabytes: Int,
    val freeStorageMegabytes: Int,
) {
    internal fun formParameters(): Parameters = Parameters.build {
        append("app_build", "40408")
        append("app_install_time", appInstallTimeMillis.toString())
        append("app_ticket", "interface is empty")
        append("app_version", "11.4.0")
        append("bt_ck", if (bluetoothAvailable) "1" else "0")
        append("bundle_id", MOBILE_SOURCE)
        append("cp_ct", cpuCount.toString())
        append("cp_tp", cpuType)
        append("cp_us", cpuUsage)
        append("d_n", phoneModel)
        append("fr_mem", freeMemoryMegabytes.toString())
        append("fr_st", freeStorageMegabytes.toString())
        append("latitude", "0.0")
        append("longitude", "0.0")
        append("nt_st", if (notificationEnabled) "1" else "0")
        append("ph_br", phoneBrand)
        append("ph_md", phoneModel)
        append("ph_os", "Android $androidRelease")
        append("pre_install", "InterfaceIsNull")
        append("tt_mem", totalMemoryMegabytes.toString())
        append("tt_st", totalStorageMegabytes.toString())
        append("tz_of", timezoneOffsetSeconds.toString())
        append("zx_expired", "0")
    }
}

sealed interface ZhihuPhoneDigitsResult {
    data object Sent : ZhihuPhoneDigitsResult

    data class CaptchaRequired(
        val imageBase64: String?,
    ) : ZhihuPhoneDigitsResult
}

/**
 * 知乎 Android 客户端手机号登录协议。
 *
 * Captcha 是服务端条件分支：正常情况下服务端会直接允许发送短信，但风控要求验证时必须先完成 `/captcha`，
 * 不能把当前抓包中没有出现验证码误解成可以永久绕过验证码。
 */
class ZhihuPhoneLoginClient(
    private val httpClient: HttpClient,
    private val cookies: MutableMap<String, String>,
    private val deviceInfo: ZhihuPhoneLoginDeviceInfo,
    private val nowEpochSeconds: () -> Long,
) {
    private var authorization = "oauth $MOBILE_CLIENT_ID"
    private var deviceId: String? = null
    private var webDeviceCookie = cookies.remove("d_c0")?.takeIf(String::isNotBlank)

    suspend fun requestDigits(phoneNumber: String): ZhihuPhoneDigitsResult {
        val username = normalizePhoneNumber(phoneNumber)
        ensureGuestToken()

        val captchaState = requestCaptchaState()
        if (captchaState.showCaptcha) {
            return ZhihuPhoneDigitsResult.CaptchaRequired(refreshCaptcha())
        }

        return sendDigits(username, recheckCaptchaWhenRequested = true)
    }

    private suspend fun sendDigits(
        username: String,
        recheckCaptchaWhenRequested: Boolean,
    ): ZhihuPhoneDigitsResult {
        val response = httpClient.post("$PHONE_LOGIN_API_BASE_URL$AUTH_DIGITS_PATH") {
            applyMobileHeaders()
            setEncryptedForm(
                "username" to username,
                "client_id" to MOBILE_CLIENT_ID,
            )
        }
        val body = response.bodyAsText()
        if (response.status.isSuccess()) {
            return ZhihuPhoneDigitsResult.Sent
        }

        val error = runCatching {
            val root = ZhihuJson.json.parseToJsonElement(body).jsonObject
            root["error"]?.jsonObject ?: root
        }.getOrNull()
        val errorCode = error
            ?.get("code")
            ?.jsonPrimitive
            ?.content
            ?.toIntOrNull()
        if (errorCode in CAPTCHA_INVALID_ERROR_CODES) {
            return ZhihuPhoneDigitsResult.CaptchaRequired(refreshCaptcha())
        }
        if (errorCode == CAPTCHA_NEEDED_ERROR_CODE && recheckCaptchaWhenRequested) {
            val captchaState = requestCaptchaState()
            return if (captchaState.showCaptcha) {
                ZhihuPhoneDigitsResult.CaptchaRequired(refreshCaptcha())
            } else {
                sendDigits(username, recheckCaptchaWhenRequested = false)
            }
        }
        val errorMessage = error?.get("message")?.jsonPrimitive?.content
        error(
            errorMessage?.let { "发送短信验证码失败：$it" }
                ?: "发送短信验证码失败（HTTP ${response.status.value}）",
        )
    }

    suspend fun refreshCaptcha(): String? {
        ensureGuestToken()
        val response = httpClient.put("$PHONE_LOGIN_API_BASE_URL$CAPTCHA_PATH") {
            applyMobileHeaders()
        }
        val body = response.bodyAsText()
        check(response.status.isSuccess()) { "获取图形验证码失败（HTTP ${response.status.value}）" }
        return ZhihuJson.decodeJson<CaptchaResponse>(ZhihuJson.json.parseToJsonElement(body)).imgBase64
    }

    suspend fun verifyCaptcha(input: String): Boolean {
        require(input.isNotBlank()) { "图形验证码不能为空" }
        ensureGuestToken()
        val response = httpClient.post("$PHONE_LOGIN_API_BASE_URL$CAPTCHA_PATH") {
            applyMobileHeaders()
            setEncryptedForm("input_text" to input.trim())
        }
        val body = response.bodyAsText()
        check(response.status.isSuccess()) { "验证图形验证码失败（HTTP ${response.status.value}）" }
        return ZhihuJson.decodeJson<CaptchaVerificationResponse>(ZhihuJson.json.parseToJsonElement(body)).success
    }

    suspend fun signIn(
        phoneNumber: String,
        digits: String,
    ): ZhihuMobileLoginToken {
        val username = normalizePhoneNumber(phoneNumber)
        require(digits.isNotBlank()) { "短信验证码不能为空" }
        ensureGuestToken()

        val timestamp = nowEpochSeconds()
        val signature = hmacSha1Hex(
            MOBILE_CLIENT_SECRET,
            "$DIGITS_GRANT_TYPE$MOBILE_CLIENT_ID$MOBILE_SOURCE$timestamp",
        )
        val response = httpClient.post("$PHONE_LOGIN_API_BASE_URL$SIGN_IN_PATH") {
            applyMobileHeaders()
            setEncryptedForm(
                "client_id" to MOBILE_CLIENT_ID,
                "digits" to digits.trim(),
                "grant_type" to DIGITS_GRANT_TYPE,
                "signature" to signature,
                "source" to MOBILE_SOURCE,
                "timestamp" to timestamp.toString(),
                "username" to username,
            )
        }
        val body = response.bodyAsText()
        if (!response.status.isSuccess()) {
            val message = runCatching {
                val root = ZhihuJson.json.parseToJsonElement(body).jsonObject
                (root["error"]?.jsonObject ?: root)["message"]?.jsonPrimitive?.content
            }.getOrNull()
            error(message?.let { "手机号登录失败：$it" } ?: "手机号登录失败（HTTP ${response.status.value}）")
        }
        val token = ZhihuJson.decodeJson<TokenResponse>(ZhihuJson.json.parseToJsonElement(body))
        check(token.accessToken.isNotBlank()) { "服务器未返回登录凭证" }
        cookies.putAll(token.cookie.filterValues(String::isNotBlank))
        if (cookies["d_c0"].isNullOrBlank()) {
            cookies["d_c0"] = checkNotNull(webDeviceCookie) {
                "服务器未返回必要的 Cookie d_c0"
            }
        }
        return ZhihuMobileLoginToken(
            accessToken = token.accessToken,
            refreshToken = token.refreshToken,
            tokenType = token.tokenType,
            expiresAt = token.expiresIn?.let(timestamp::plus),
            cookies = cookies.toMap(),
        )
    }

    private suspend fun ensureGuestToken() {
        if (!authorization.startsWith("oauth ")) return

        if (webDeviceCookie == null) {
            val mobileCookies = cookies.toMap()
            var fetchedDeviceCookie: String? = null
            val response = try {
                httpClient.post("https://www.zhihu.com/udid").also {
                    fetchedDeviceCookie = cookies["d_c0"]?.takeIf(String::isNotBlank)
                }
            } finally {
                // `d_c0` makes the mobile guest initialization return 500. Keep one cookie store,
                // but do not expose web preheating cookies to the mobile protocol before sign-in.
                cookies.clear()
                cookies.putAll(mobileCookies)
            }
            check(response.status.isSuccess()) { "初始化网页设备凭证失败（HTTP ${response.status.value}）" }
            webDeviceCookie = checkNotNull(fetchedDeviceCookie) { "服务器未返回必要的 Cookie d_c0" }
        }

        val timestamp = nowEpochSeconds().toString()
        val form = deviceInfo.formParameters().formUrlEncode()
        val signature = hmacSha1Hex(
            CLOUD_APP_SECRET,
            CLOUD_APP_ID + CLOUD_SIGN_VERSION + form + timestamp,
        )
        val response = httpClient.post("$PHONE_LOGIN_API_BASE_URL$DEVICE_GUEST_INIT_PATH") {
            applyMobileHeaders()
            header("x-app-id", CLOUD_APP_ID)
            header("x-sign-version", CLOUD_SIGN_VERSION)
            header("x-req-ts", timestamp)
            header("x-req-signature", signature)
            contentType(ContentType.Application.FormUrlEncoded)
            setBody(ZhihuMessageBodyEncryptor.encrypt(form))
        }
        val body = response.bodyAsText()
        if (!response.status.isSuccess()) {
            val message = runCatching {
                val root = ZhihuJson.json.parseToJsonElement(body).jsonObject
                (root["error"]?.jsonObject ?: root)["message"]?.jsonPrimitive?.content
            }.getOrNull()
            error(
                message?.let { "初始化手机号登录失败：$it" }
                    ?: "初始化手机号登录失败（HTTP ${response.status.value}）",
            )
        }
        val initialization = ZhihuJson.decodeJson<DeviceGuestInitializationResponse>(ZhihuJson.json.parseToJsonElement(body))
        val guest = initialization.guest
        check(guest.accessToken.isNotBlank()) { "服务器未返回访客凭证" }
        check(initialization.udid.isNotBlank()) { "服务器未返回设备凭证" }
        cookies.putAll(guest.cookie.filterValues(String::isNotBlank))
        deviceId = initialization.udid
        authorization = "${guest.tokenType.ifBlank { "bearer" }} ${guest.accessToken}"
    }

    private suspend fun requestCaptchaState(): CaptchaResponse {
        val response = httpClient.get("$PHONE_LOGIN_API_BASE_URL$CAPTCHA_PATH") {
            applyMobileHeaders()
        }
        val body = response.bodyAsText()
        check(response.status.isSuccess()) { "检查图形验证码失败（HTTP ${response.status.value}）" }
        return ZhihuJson.decodeJson(ZhihuJson.json.parseToJsonElement(body))
    }

    private fun HttpRequestBuilder.applyMobileHeaders() {
        accept(ContentType.Application.Json)
        header(HttpHeaders.UserAgent, ZHIHU_ANDROID_PHONE_LOGIN_USER_AGENT)
        header(HttpHeaders.Authorization, authorization)
        cookies.toCookieHeaderString().takeIf(String::isNotBlank)?.let {
            header(HttpHeaders.Cookie, it)
        }
        deviceId?.let { header("x-udid", it) }
        header("x-api-version", "3.0.93")
        header("x-app-version", "11.4.0")
        header("x-app-build", "release")
        header("x-app-bundleid", MOBILE_SOURCE)
        header("x-app-flavor", "honor")
        header(
            "x-app-za",
            "OS=Android&Release=12&Model=Android+SDK+built+for+arm64&VersionName=11.4.0&VersionCode=40408&" +
                "Product=com.zhihu.android&Width=1080&Height=2274&Installer=%E8%8D%A3%E8%80%80%E5%95%86%E5%BA%97&" +
                "DeviceType=AndroidPhone&Brand=Android",
        )
        header("x-network-type", "3G")
        header("x-page-id", "44")
        header("x-zse-93", "101_1_1.0")
    }

    private fun HttpRequestBuilder.setEncryptedForm(vararg entries: Pair<String, String>) {
        contentType(ContentType.Application.FormUrlEncoded)
        val form = Parameters
            .build {
                entries.forEach { (name, value) -> append(name, value) }
            }.formUrlEncode()
        setBody(ZhihuMessageBodyEncryptor.encrypt(form))
    }
}

private fun normalizePhoneNumber(phoneNumber: String): String {
    val compact = phoneNumber
        .trim()
        .replace(" ", "")
        .replace("-", "")
    val local = when {
        compact.startsWith("+86") -> compact.removePrefix("+86")
        compact.startsWith("86") && compact.length == 13 -> compact.removePrefix("86")
        else -> compact
    }
    require(local.length == 11 && local.all(Char::isDigit) && local.startsWith('1')) {
        "请输入正确的中国大陆手机号"
    }
    return "+86$local"
}

@Serializable
private data class GuestTokenResponse(
    val accessToken: String,
    val tokenType: String = "bearer",
    val cookie: Map<String, String> = emptyMap(),
)

@Serializable
private data class DeviceGuestInitializationResponse(
    val udid: String,
    val guest: GuestTokenResponse,
)

@Serializable
private data class CaptchaResponse(
    val showCaptcha: Boolean = false,
    val imgBase64: String? = null,
)

@Serializable
private data class CaptchaVerificationResponse(
    val success: Boolean = false,
)

@Serializable
private data class TokenResponse(
    val accessToken: String,
    val refreshToken: String? = null,
    val tokenType: String = "bearer",
    val expiresIn: Long? = null,
    val cookie: Map<String, String> = emptyMap(),
)

private val CAPTCHA_INVALID_ERROR_CODES = setOf(120001, 120002)
private const val CAPTCHA_NEEDED_ERROR_CODE = 120005
