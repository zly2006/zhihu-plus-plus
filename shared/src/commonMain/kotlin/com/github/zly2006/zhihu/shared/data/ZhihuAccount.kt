package com.github.zly2006.zhihu.shared.data

import com.github.zly2006.zhihu.shared.account.DEFAULT_ZHIHU_USER_AGENT
import com.github.zly2006.zhihu.shared.account.ZhihuAccountProfileSnapshot
import com.github.zly2006.zhihu.shared.account.ZhihuAccountSession
import io.ktor.client.HttpClient
import io.ktor.client.HttpClientConfig
import io.ktor.client.call.body
import io.ktor.client.engine.HttpClientEngineConfig
import io.ktor.client.plugins.UserAgent
import io.ktor.client.plugins.cache.HttpCache
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.cookies.HttpCookies
import io.ktor.client.request.get
import io.ktor.http.HttpStatusCode
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject

const val ZHIHU_ME_URL = "https://www.zhihu.com/api/v4/me"

@Serializable
data class ZhihuAccountProfile(
    val id: String = "",
    val name: String = "",
    val urlToken: String? = null,
    val userType: String = "",
)

fun <T : HttpClientEngineConfig> HttpClientConfig<T>.installZhihuCommonClientConfig(
    cookies: MutableMap<String, String>,
    userAgent: String,
    onCookieChanged: (() -> Unit)? = null,
    enableHttpCache: Boolean = false,
) {
    if (enableHttpCache) {
        install(HttpCache)
    }
    install(HttpCookies) {
        storage = ZhihuCookieStorage(cookies, onCookieChanged)
    }
    install(ContentNegotiation) {
        json(ZhihuJson.json)
    }
    install(UserAgent) {
        agent = userAgent
    }
}

suspend fun fetchVerifiedZhihuAccount(client: HttpClient): JsonObject? {
    val response = client.get(ZHIHU_ME_URL)
    if (response.status != HttpStatusCode.OK) {
        return null
    }
    return response.body<JsonObject>()
}

suspend fun fetchVerifiedZhihuProfile(client: HttpClient): ZhihuAccountProfile? =
    fetchVerifiedZhihuAccount(client)?.let { ZhihuJson.decodeJson<ZhihuAccountProfile>(it) }

suspend fun fetchVerifiedZhihuSession(
    client: HttpClient,
    cookies: Map<String, String>,
    userAgent: String = DEFAULT_ZHIHU_USER_AGENT,
): ZhihuAccountSession? {
    val account = fetchVerifiedZhihuAccount(client) ?: return null
    val profile = ZhihuJson.decodeJson<ZhihuAccountProfile>(account)
    return ZhihuAccountSession(
        login = true,
        username = profile.name,
        cookies = cookies.toMutableMap(),
        userAgent = userAgent,
        profile = ZhihuAccountProfileSnapshot(
            id = profile.id,
            name = profile.name,
            urlToken = profile.urlToken,
            userType = profile.userType,
        ),
        self = account,
    )
}
