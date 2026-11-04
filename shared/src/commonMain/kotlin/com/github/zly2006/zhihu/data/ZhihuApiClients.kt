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

package com.github.zly2006.zhihu.data

import com.github.zly2006.zhihu.util.json
import com.github.zly2006.zhihu.util.raiseForStatus
import com.github.zly2006.zhihu.util.zhihuAccountClientKey
import io.ktor.client.HttpClient
import io.ktor.client.plugins.cookies.CookiesStorage
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.request
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.client.statement.request
import io.ktor.http.Cookie
import io.ktor.http.CookieEncoding
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.Url
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

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

    val accountClient = client.attributes.getOrNull(zhihuAccountClientKey) ?: return response
    if (!accountClient.isCurrent) return response
    val body = response.bodyAsText()
    val ticketExpired = runCatching {
        val error = ZhihuJson.json
            .parseToJsonElement(body)
            .jsonObject["error"]
            ?.jsonObject
        error?.get("code")?.jsonPrimitive?.intOrNull == 100 &&
            error["message"]?.jsonPrimitive?.content in setOf("ERR_LOGIN_TICKET_EXPIRED", "ERR_TICKET_NOT_EXIST")
    }.getOrDefault(false)
    if (!ticketExpired) return response
    val rejectedCookie = response.request.headers[HttpHeaders.Cookie]
        ?.split(';')
        ?.firstOrNull { it.trim().startsWith("z_c0=") }
        ?.trim()
        ?.substringAfter('=')
    if (!accountClient.credentialRefresher.refresh(force = true, rejectedCookie = rejectedCookie) || !accountClient.isCurrent) return response
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
    return response.json() as? JsonObject
}

class ZhihuCookieStorage(
    private val cookies: MutableMap<String, String>,
    private val onCookieChanged: () -> Unit = {},
) : CookiesStorage {
    override suspend fun addCookie(requestUrl: Url, cookie: Cookie) {
        // https://github.com/zly2006/zhihu-plus-plus/issues/25#issuecomment-3311926550
        if (cookie.name == "z_c0" && cookie.value.isBlank()) {
            return
        }
        if (cookie.domain?.endsWith("zhihu.com") != false) {
            cookies[cookie.name] = cookie.value
            onCookieChanged()
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
