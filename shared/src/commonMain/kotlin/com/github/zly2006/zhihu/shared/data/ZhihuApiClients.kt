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
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.plus
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlin.time.Clock

const val ZHIHU_CLEAR_ONLINE_HISTORY_URL = "https://api.zhihu.com/read_history/batch_del"

fun decodeOnlineHistoryItems(
    data: JsonArray,
    ignoreInvalid: Boolean = false,
): List<OnlineHistoryItem> = data.mapNotNull { item ->
    runCatching {
        ZhihuJson.decodeJson<OnlineHistoryItem>(item)
    }.getOrElse { error ->
        if (ignoreInvalid) {
            null
        } else {
            throw error
        }
    }
}

const val ZHIHU_LAST_READ_TOUCH_URL = "https://www.zhihu.com/lastread/touch"

const val ZHIHU_NOTIFICATION_DEFAULT_READ_ALL_URL =
    "https://www.zhihu.com/api/v4/notifications/v2/default/actions/readall"
const val ZHIHU_NOTIFICATION_FOLLOW_READ_ALL_URL =
    "https://www.zhihu.com/api/v4/notifications/v2/follow/actions/readall"
const val ZHIHU_NOTIFICATION_VOTE_THANK_READ_ALL_URL =
    "https://www.zhihu.com/api/v4/notifications/v2/vote_thank/actions/readall"

val ZHIHU_NOTIFICATION_READ_ALL_URLS = listOf(
    ZHIHU_NOTIFICATION_DEFAULT_READ_ALL_URL,
    ZHIHU_NOTIFICATION_FOLLOW_READ_ALL_URL,
    ZHIHU_NOTIFICATION_VOTE_THANK_READ_ALL_URL,
)

const val ZHIHU_DAILY_LATEST_URL = "https://news-at.zhihu.com/api/4/stories/latest"

fun nextDailyApiDate(date: String): String {
    require(date.length == 8 && date.all { it.isDigit() }) {
        "date must use yyyyMMdd format"
    }
    val localDate = LocalDate.parse("${date.substring(0, 4)}-${date.substring(4, 6)}-${date.substring(6, 8)}")
    val nextDate = localDate.plus(1, DateTimeUnit.DAY)
    return nextDate.toString().replace("-", "")
}

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
