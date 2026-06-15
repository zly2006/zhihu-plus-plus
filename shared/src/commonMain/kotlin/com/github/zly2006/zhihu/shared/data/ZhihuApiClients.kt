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

package com.github.zly2006.zhihu.shared.data

import com.github.zly2006.zhihu.shared.util.raiseForStatus
import com.github.zly2006.zhihu.util.ZhihuCredentialRefresher
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.cookies.CookiesStorage
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.request
import io.ktor.client.statement.HttpResponse
import io.ktor.http.Cookie
import io.ktor.http.CookieEncoding
import io.ktor.http.HttpStatusCode
import io.ktor.http.Url
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlin.time.Clock

private var lastRefreshMillis: Long = 0

internal fun resetZhihuAuthenticatedRequestRefreshThrottleForTesting() {
    lastRefreshMillis = 0
}

suspend fun executeZhihuAuthenticatedRequest(
    client: HttpClient,
    url: String,
    block: suspend HttpRequestBuilder.() -> Unit = {},
): HttpResponse {
    val response = client.request(url) {
        block()
    }
    if (response.status != HttpStatusCode.Unauthorized) return response

    if (Clock.System.now().toEpochMilliseconds() - lastRefreshMillis < 10_000) {
        return response
    }
    val refreshToken = ZhihuCredentialRefresher.fetchRefreshToken(client)
    ZhihuCredentialRefresher.refreshZhihuToken(refreshToken, client)
    lastRefreshMillis = Clock.System.now().toEpochMilliseconds()
    return client
        .request(url) {
            block()
        }.raiseForStatus()
}

suspend fun fetchZhihuAuthenticatedJson(
    client: HttpClient,
    url: String,
    block: suspend HttpRequestBuilder.() -> Unit = {},
): JsonObject? {
    val response = executeZhihuAuthenticatedRequest(
        client = client,
        url = url,
        block = block,
    )
    if (response.status == HttpStatusCode.NoContent) {
        return null
    }
    val body = response.body<JsonElement>()
    return body as? JsonObject
}

class ZhihuCookieStorage(
    private val cookies: MutableMap<String, String>,
    private val onCookieChanged: (() -> Unit)? = null,
) : CookiesStorage {
    override suspend fun addCookie(requestUrl: Url, cookie: Cookie) {
        // https://github.com/zly2006/zhihu-plus-plus/issues/25#issuecomment-3311926550
        if (cookie.name == "z_c0" && cookie.value.isBlank()) {
            return
        }
        if (cookie.domain?.endsWith("zhihu.com") != false) {
            cookies[cookie.name] = cookie.value
            onCookieChanged?.invoke()
        }
    }

    override suspend fun get(requestUrl: Url): List<Cookie> = cookies.map {
        Cookie(it.key, it.value, CookieEncoding.RAW, domain = "www.zhihu.com")
    }

    override fun close() = Unit
}

@Serializable
data class ZhihuPaging(
    val page: Int = -1,
    val isEnd: Boolean = false,
    val isStart: Boolean = false,
    val previous: String? = null,
    val totals: Int = 0,
    val next: String,
    val prev: String? = null,
)

@Serializable
data class ZhihuVotersResponse(
    val paging: ZhihuPaging,
    val data: List<DataHolder.Author> = emptyList(),
)
