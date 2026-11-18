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
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

const val ZHIHU_ANDROID_IDENTITY_USER_AGENT =
    "com.zhihu.android/Futureve/11.2.0 Mozilla/5.0 (Linux; Android 12; sdk_gphone64_arm64 " +
        "Build/SE1A.220630.001.A1; wv) AppleWebKit/537.36 (KHTML, like Gecko) Version/4.0 " +
        "Chrome/57.0.1000.10 Mobile Safari/537.36"

@Serializable
data class ZhihuIdentityAccount(
    val id: String,
    val urlToken: String? = null,
    val name: String,
    val avatarUrl: String? = null,
    val isActive: Boolean = false,
    val canCreateSubAccount: Boolean = false,
    val accountType: Int = 0,
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

internal fun io.ktor.client.request.HttpRequestBuilder.applyIdentityHeaders(session: ZhihuAccountSession) {
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

internal suspend fun HttpResponse.identitySuccessBody(operation: String): String {
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
internal data class ZhihuIdentityAccountListResponse(
    val data: List<ZhihuIdentityAccount> = emptyList(),
)

@Serializable
internal data class SwitchAccountRequest(
    @SerialName("target_user_id")
    val targetUserId: String,
)

@Serializable
internal data class ZhihuIdentityToken(
    val uid: String = "",
    val userId: Long? = null,
    val tokenType: String = "bearer",
    val accessToken: String,
    val refreshToken: String? = null,
    val expiresIn: Long? = null,
    val cookie: Map<String, String> = emptyMap(),
    val expiresAt: Long? = null,
)

@Serializable
internal data class ZhihuIdentityProfile(
    val id: String = "",
    val name: String = "",
    val urlToken: String? = null,
    val userType: String = "",
    val avatarUrl: String? = null,
    val canCreateSubAccount: Boolean = false,
    val accountType: Int = 0,
    val subAccountControlStatus: Int = 0,
)

/** 验证身份切换签发的凭据；调用方决定是否将结果设为应用当前账号。 */
internal suspend fun ZhihuAccountSession.resolveIdentityChange(
    body: String,
    expectedAccountId: String? = null,
    knownProfile: ZhihuAccountProfileSnapshot? = null,
): ZhihuIdentityChangeResult {
    val token = ZhihuJson.decodeJson<ZhihuIdentityToken>(ZhihuJson.json.parseToJsonElement(body))
    check(token.accessToken.isNotBlank()) { "服务器未返回新账号凭证" }
    check(token.cookie["z_c0"].isNullOrBlank().not()) { "服务器未返回新账号 Cookie" }
    val newCookies = this.cookies.toMutableMap().apply { putAll(token.cookie) }
    val client = createAccountHttpClient(newCookies, userAgent)
    return try {
        val rawProfile = if (knownProfile == null) {
            val response = client.get("https://api.zhihu.com/people/self") {
                applyIdentityHeaders(
                    this@resolveIdentityChange.copy(
                        mobileAccessToken = token.accessToken,
                        mobileTokenType = token.tokenType,
                    ),
                )
            }
            ZhihuJson.json
                .parseToJsonElement(
                    response.identitySuccessBody("初始化新账号"),
                ).jsonObject
        } else {
            check(token.uid == knownProfile.id) { "服务器返回的凭据与目标账号不一致" }
            null
        }
        val profile = rawProfile?.let { ZhihuJson.decodeJson<ZhihuIdentityProfile>(it) }
            ?: ZhihuIdentityProfile(
                id = requireNotNull(knownProfile).id,
                name = knownProfile.name,
                urlToken = knownProfile.urlToken,
                userType = knownProfile.userType,
                avatarUrl = knownProfile.avatarUrl,
            )
        check(profile.id.isNotBlank() && profile.name.isNotBlank()) { "服务器返回的账号资料不完整" }
        check(expectedAccountId == null || profile.id == expectedAccountId) { "服务器返回的账号与目标账号不一致" }
        val nextSession = this.copy(
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
            self = rawProfile,
            mobileAccessToken = token.accessToken,
            mobileRefreshToken = token.refreshToken,
            mobileTokenType = token.tokenType,
            mobileTokenExpiresAt = token.expiresAt,
        )
        ZhihuIdentityChangeResult(
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

internal suspend fun ZhihuAccountSession.switchCommentIdentity(target: ZhihuAccountProfileSnapshot): ZhihuAccountSession {
    val body = createAccountHttpClient(cookies.toMutableMap(), userAgent).use { client ->
        client
            .post("https://api.zhihu.com/account/switch") {
                applyIdentityHeaders(this@switchCommentIdentity)
                contentType(ContentType.Application.Json)
                setBody(SwitchAccountRequest(target.id))
            }.identitySuccessBody("切换评论身份")
    }
    return resolveIdentityChange(body, target.id, knownProfile = target).session
}
