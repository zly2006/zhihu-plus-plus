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

import com.github.zly2006.zhihu.shared.data.ZhihuJson
import io.ktor.client.HttpClient
import io.ktor.client.request.accept
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

private const val ZHIHU_API_BASE_URL = "https://api.zhihu.com"
private const val IDENTITY_ACCOUNT_LIST_PATH = "/people/account/list"
private const val CREATE_SUB_ACCOUNT_PATH = "/account/sub/register"
private const val SWITCH_ACCOUNT_PATH = "/account/switch"
private const val CURRENT_ACCOUNT_PATH = "/people/self"

const val ZHIHU_ANDROID_IDENTITY_USER_AGENT =
    "com.zhihu.android/Futureve/11.2.0 Mozilla/5.0 (Linux; Android 12; sdk_gphone64_arm64 " +
        "Build/SE1A.220630.001.A1; wv) AppleWebKit/537.36 (KHTML, like Gecko) Version/4.0 " +
        "Chrome/57.0.1000.10 Mobile Safari/537.36"

@Serializable
data class ZhihuIdentityAccount(
    val id: String,
    @SerialName("url_token")
    val urlToken: String? = null,
    val name: String,
    @SerialName("avatar_url")
    val avatarUrl: String? = null,
    @SerialName("is_active")
    val isActive: Boolean = false,
    @SerialName("can_create_sub_account")
    val canCreateSubAccount: Boolean = false,
    @SerialName("account_type")
    val accountType: Int = 0,
    @SerialName("sub_account_control_status")
    val subAccountControlStatus: Int = 0,
)

data class ZhihuIdentityChangeResult(
    val account: ZhihuIdentityAccount,
    val session: ZhihuAccountSession,
)

class ZhihuIdentityApiException(
    message: String,
    val statusCode: Int,
) : IllegalStateException(message)

/**
 * 知乎移动端“身份管理”协议客户端。
 *
 * `/account/sub/register` 与 `/account/switch` 都会直接签发目标身份的新 token 和 cookie。收到响应后必须先用新凭证
 * 请求 `/people/self`，再一次性保存完整会话；仅替换用户 ID 会让后续推荐和互动请求继续落在旧身份。
 */
class ZhihuIdentityClient(
    private val currentClient: () -> HttpClient,
    private val temporaryClient: (MutableMap<String, String>) -> HttpClient,
    private val currentSession: () -> ZhihuAccountSession,
    private val saveSession: (ZhihuAccountSession) -> Unit,
) {
    suspend fun listAccounts(): List<ZhihuIdentityAccount> {
        val response = currentClient().get("$ZHIHU_API_BASE_URL$IDENTITY_ACCOUNT_LIST_PATH") {
            applyIdentityHeaders(currentSession())
        }
        val body = response.successBody("获取身份列表")
        return ZhihuJson.json.decodeFromString<ZhihuIdentityAccountListResponse>(body).data
    }

    suspend fun createSubAccount(): ZhihuIdentityChangeResult {
        val response = currentClient().post("$ZHIHU_API_BASE_URL$CREATE_SUB_ACCOUNT_PATH") {
            applyIdentityHeaders(currentSession())
        }
        return applyIssuedToken(response.successBody("创建新账号"))
    }

    suspend fun switchAccount(targetUserId: String): ZhihuIdentityChangeResult {
        require(targetUserId.isNotBlank()) { "目标账号不能为空" }
        val response = currentClient().post("$ZHIHU_API_BASE_URL$SWITCH_ACCOUNT_PATH") {
            applyIdentityHeaders(currentSession())
            contentType(ContentType.Application.Json)
            setBody(SwitchAccountRequest(targetUserId))
        }
        return applyIssuedToken(
            body = response.successBody("切换账号"),
            expectedAccountId = targetUserId,
        )
    }

    private suspend fun applyIssuedToken(
        body: String,
        expectedAccountId: String? = null,
    ): ZhihuIdentityChangeResult {
        val token = ZhihuJson.json.decodeFromString<ZhihuIdentityToken>(body)
        check(token.accessToken.isNotBlank()) { "服务器未返回新账号凭证" }
        check(token.cookie["z_c0"].isNullOrBlank().not()) { "服务器未返回新账号 Cookie" }

        val oldSession = currentSession()
        val newCookies = oldSession.cookies.toMutableMap().apply {
            putAll(token.cookie)
        }
        val client = temporaryClient(newCookies)
        try {
            val response = client.get("$ZHIHU_API_BASE_URL$CURRENT_ACCOUNT_PATH") {
                applyIdentityHeaders(
                    oldSession.copy(
                        mobileAccessToken = token.accessToken,
                        mobileTokenType = token.tokenType,
                    ),
                )
            }
            val profileBody = response.successBody("初始化新账号")
            val rawProfile = ZhihuJson.json.parseToJsonElement(profileBody).jsonObject
            val convertedProfile = ZhihuJson.snakeCaseToCamelCase(rawProfile)
            val profile = ZhihuJson.json.decodeFromJsonElement<ZhihuIdentityProfile>(convertedProfile)
            check(profile.id.isNotBlank() && profile.name.isNotBlank()) { "服务器返回的账号资料不完整" }
            check(expectedAccountId == null || profile.id == expectedAccountId) {
                "服务器返回的账号与目标账号不一致"
            }

            val nextSession = oldSession.copy(
                login = true,
                username = profile.name,
                cookies = newCookies,
                profile = ZhihuAccountProfileSnapshot(
                    id = profile.id,
                    name = profile.name,
                    urlToken = profile.urlToken,
                    userType = profile.userType,
                    avatarUrl = profile.avatarUrl,
                ),
                self = convertedProfile,
                mobileAccessToken = token.accessToken,
                mobileRefreshToken = token.refreshToken,
                mobileTokenType = token.tokenType,
                mobileTokenExpiresAt = token.expiresAt,
            )
            saveSession(nextSession)
            return ZhihuIdentityChangeResult(
                account = ZhihuIdentityAccount(
                    id = profile.id,
                    urlToken = profile.urlToken,
                    name = profile.name,
                    avatarUrl = profile.avatarUrl,
                    isActive = true,
                    canCreateSubAccount = profile.canCreateSubAccount,
                    accountType = profile.accountType,
                    subAccountControlStatus = profile.subAccountControlStatus,
                ),
                session = nextSession,
            )
        } finally {
            client.close()
        }
    }
}

private fun io.ktor.client.request.HttpRequestBuilder.applyIdentityHeaders(session: ZhihuAccountSession) {
    accept(ContentType.Application.Json)
    header(HttpHeaders.UserAgent, ZHIHU_ANDROID_IDENTITY_USER_AGENT)
    header("x-api-version", "3.0.93")
    header("x-app-version", "11.2.0")
    header("x-app-build", "release")
    header("x-app-bundleid", "com.zhihu.android")
    header("x-app-flavor", "zhihuwap64")
    header(
        "x-app-za",
        "OS=Android&Release=12&Model=sdk_gphone64_arm64&VersionName=11.2.0&VersionCode=40210&" +
            "Product=com.zhihu.android&Width=1440&Height=2952&Installer=Market&DeviceType=AndroidPhone&Brand=google",
    )
    header("x-network-type", "WiFi")
    header("x-zse-93", "101_1_1.0")
    session.mobileAccessToken
        ?.takeIf { it.isNotBlank() }
        ?.let { accessToken ->
            val tokenType = session.mobileTokenType?.takeIf { it.isNotBlank() } ?: "bearer"
            header(HttpHeaders.Authorization, "$tokenType $accessToken")
        }
}

private suspend fun HttpResponse.successBody(operation: String): String {
    val body = bodyAsText()
    if (status == HttpStatusCode.OK) return body

    val serverMessage = runCatching {
        val json = ZhihuJson.json.parseToJsonElement(body).jsonObject
        json["error"]
            ?.jsonObject
            ?.get("message")
            ?.jsonPrimitive
            ?.content
            ?: json["message"]?.jsonPrimitive?.content
    }.getOrNull()
    throw ZhihuIdentityApiException(
        message = serverMessage?.let { "$operation 失败：$it" } ?: "$operation 失败（HTTP ${status.value}）",
        statusCode = status.value,
    )
}

@Serializable
private data class ZhihuIdentityAccountListResponse(
    val data: List<ZhihuIdentityAccount> = emptyList(),
)

@Serializable
private data class SwitchAccountRequest(
    @SerialName("target_user_id")
    val targetUserId: String,
)

@Serializable
private data class ZhihuIdentityToken(
    val uid: String = "",
    @SerialName("user_id")
    val userId: Long? = null,
    @SerialName("token_type")
    val tokenType: String = "bearer",
    @SerialName("access_token")
    val accessToken: String,
    @SerialName("refresh_token")
    val refreshToken: String? = null,
    @SerialName("expires_in")
    val expiresIn: Long? = null,
    val cookie: Map<String, String> = emptyMap(),
    @SerialName("expires_at")
    val expiresAt: Long? = null,
)

@Serializable
private data class ZhihuIdentityProfile(
    val id: String = "",
    val name: String = "",
    val urlToken: String? = null,
    val userType: String = "",
    val avatarUrl: String? = null,
    val canCreateSubAccount: Boolean = false,
    val accountType: Int = 0,
    val subAccountControlStatus: Int = 0,
)
