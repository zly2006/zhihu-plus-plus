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

fun Map<String, String>.toCookieHeaderString(): String =
    entries
        .asSequence()
        .mapNotNull { (name, value) ->
            value.takeIf { it.isNotBlank() }?.let { "$name=$it" }
        }.joinToString("; ")

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
    try {
        val refreshToken = ZhihuCredentialRefresher.fetchRefreshToken(client)
        ZhihuCredentialRefresher.refreshZhihuToken(refreshToken, client)
    } catch (e: kotlin.coroutines.cancellation.CancellationException) {
        throw e
    } catch (e: Exception) {
        // token 刷新失败（过期/响应异常/缺字段等），给出明确提示而非崩溃或 NPE。
        throw IllegalStateException("登录状态已失效，请重新登录", e)
    }
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
    // 知乎在最后一页/部分 api.zhihu.com 端点会省略 next，缺省值避免 MissingFieldException 直接崩掉分页
    val next: String = "",
    val prev: String? = null,
)

@Serializable
data class ZhihuVotersResponse(
    val paging: ZhihuPaging,
    val data: List<DataHolder.Author> = emptyList(),
)
