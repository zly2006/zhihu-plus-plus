package com.github.zly2006.zhihu.util

import com.github.zly2006.zhihu.ui.raiseForStatus
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.cookies.HttpCookies
import io.ktor.client.plugins.cookies.get
import io.ktor.client.plugins.pluginOrNull
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.Url
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

/**
 * 知乎 Token 刷新业务逻辑
 */
object ZhihuCredentialRefresher {
    private const val CLIENT_ID = "c3cef7c66a1843f8b3a9e6a1e3160e20"
    private const val CLIENT_SECRET = "d1b964811afb40118a12068ff74a12f4"
    private const val GRANT_TYPE = "refresh_token"
    private const val SOURCE = "com.zhihu.web"

    private fun hmacSha1(key: String, message: String): String {
        val signingKey = SecretKeySpec(key.toByteArray(StandardCharsets.UTF_8), "HmacSHA1")
        val mac = Mac.getInstance("HmacSHA1")
        mac.init(signingKey)
        val rawHmac = mac.doFinal(message.toByteArray(StandardCharsets.UTF_8))
        // Hex encode
        return rawHmac.joinToString("") { "%02x".format(it) }
    }

    private fun generateRefreshPayload(refreshToken: String, timestamp: Long): Map<String, String> {
        val message = "$GRANT_TYPE$CLIENT_ID$SOURCE$timestamp"
        val signature = hmacSha1(CLIENT_SECRET, message)

        return mapOf(
            "client_id" to CLIENT_ID,
            "grant_type" to GRANT_TYPE,
            "timestamp" to timestamp.toString(),
            "source" to SOURCE,
            "signature" to signature,
            "refresh_token" to refreshToken,
        )
    }

    suspend fun fetchRefreshToken(httpClient: HttpClient): String {
        val jojo = httpClient
            .post("https://www.zhihu.com/api/account/prod/token/refresh") {
                header("Content-Type", "application/x-www-form-urlencoded;charset=UTF-8")
                header("Origin", "https://www.zhihu.com")
                header("Referer", "https://www.zhihu.com/signin")
                header("x-requested-with", "fetch")
            }.raiseForStatus()
            .body<JsonObject>()
        return jojo["refresh_token"]!!.jsonPrimitive.content
    }

    /**
     * 执行刷新 Token 操作
     */
    suspend fun refreshZhihuToken(refreshToken: String, httpClient: HttpClient): String {
        httpClient.pluginOrNull(HttpCookies)?.get(Url("https://www.zhihu.com/"))?.get("z_c0")
            ?: throw IllegalArgumentException("刷新失败：缺失关键 cookie z_c0，请重新登录")

        val timestamp = System.currentTimeMillis()
        val payloadMap = generateRefreshPayload(refreshToken, timestamp)
        println("请求原始数据: $payloadMap")

        val formData = payloadMap.entries.joinToString("&") {
            "${URLEncoder.encode(it.key, "UTF-8")}=${URLEncoder.encode(it.value, "UTF-8")}"
        }
        val encryptedData = ZseSigner.encryptZseV4(formData)

        val jojo = httpClient
            .post("https://www.zhihu.com/api/v3/oauth/sign_in") {
                header("Content-Type", "application/x-www-form-urlencoded;charset=UTF-8")
                header("Origin", "https://www.zhihu.com")
                header("Referer", "https://www.zhihu.com/signin")
                header("x-zse-83", "3_3.0")
                header("x-requested-with", "fetch")
                setBody(encryptedData.toByteArray(StandardCharsets.UTF_8))
            }.raiseForStatus()
            .body<JsonObject>()

        return jojo["access_token"]!!.jsonPrimitive.content
    }
}
