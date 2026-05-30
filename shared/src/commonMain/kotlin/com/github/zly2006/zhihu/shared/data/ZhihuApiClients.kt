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
import com.github.zly2006.zhihu.shared.util.ZHIHU_WEB_ZSE93
import com.github.zly2006.zhihu.shared.util.raiseForStatus
import com.github.zly2006.zhihu.shared.util.signZhihuFetchRequest
import com.github.zly2006.zhihu.util.ZhihuCredentialRefresher
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.cookies.CookiesStorage
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.request.post
import io.ktor.client.request.request
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.http.ContentType
import io.ktor.http.Cookie
import io.ktor.http.CookieEncoding
import io.ktor.http.HttpStatusCode
import io.ktor.http.Url
import io.ktor.http.contentType
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.plus
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.put
import kotlin.time.Clock

const val ZHIHU_ONLINE_HISTORY_URL = "https://api.zhihu.com/unify-consumption/read_history"
const val ZHIHU_CLEAR_ONLINE_HISTORY_URL = "https://api.zhihu.com/read_history/batch_del"

@Serializable
data class OnlineHistoryPage(
    val data: List<OnlineHistoryItem>,
    val paging: ZhihuPaging? = null,
)

fun zhihuOnlineHistoryUrl(
    offset: Int = 0,
    limit: Int = 10,
): String = "https://api.zhihu.com/unify-consumption/read_history?offset=$offset&limit=$limit"

fun buildZhihuClearOnlineHistoryBody(): JsonObject = buildJsonObject {
    put("pairs", JsonArray(emptyList()))
    put("clear", true)
}

fun encodeZhihuClearOnlineHistoryBody(): String =
    ZhihuJson.json.encodeToString(JsonObject.serializer(), buildZhihuClearOnlineHistoryBody())

suspend fun fetchOnlineHistoryPage(
    client: HttpClient,
    url: String = zhihuOnlineHistoryUrl(),
): OnlineHistoryPage = decodeOnlineHistoryPage(client.get(url).body())

fun decodeOnlineHistoryPage(response: JsonObject): OnlineHistoryPage {
    val data = response["data"]
        ?.jsonArray
        ?.let(::decodeOnlineHistoryItems)
        .orEmpty()
    val paging = response["paging"]?.let { ZhihuJson.decodeJson<ZhihuPaging>(it.jsonObject) }
    return OnlineHistoryPage(data = data, paging = paging)
}

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

const val ZHIHU_HOT_LIST_TOTAL_URL = "https://www.zhihu.com/api/v3/feed/topstory/hot-lists/total"
const val ZHIHU_HOT_LIST_INCLUDE = "data[*].content,excerpt,headline,target.author.badge_v2"

fun zhihuHotListUrl(
    limit: Int = 50,
    mobile: Boolean = true,
): String = "https://www.zhihu.com/api/v3/feed/topstory/hot-lists/total?limit=$limit&mobile=$mobile"

data class ZhihuHotListPage(
    val data: List<Feed>,
    val rawData: JsonArray,
    val paging: ZhihuPaging?,
)

suspend fun fetchHotListPage(
    client: HttpClient,
    cookies: Map<String, String> = emptyMap(),
    url: String = zhihuHotListUrl(),
    include: String = ZHIHU_HOT_LIST_INCLUDE,
): ZhihuHotListPage {
    val response = client
        .get(url.replace("http://", "https://")) {
            if (include.isNotEmpty()) {
                parameter("include", include)
            }
            cookies["d_c0"]?.let { dc0 ->
                signZhihuFetchRequest(dc0 = dc0)
            }
        }.body<JsonObject>()

    val rawData = response["data"]?.jsonArray ?: JsonArray(emptyList())
    return ZhihuHotListPage(
        data = decodeHotListFeeds(rawData),
        rawData = rawData,
        paging = response["paging"]?.let { ZhihuJson.decodeJson<ZhihuPaging>(it) },
    )
}

fun decodeHotListFeeds(
    data: JsonArray,
    ignoreInvalid: Boolean = true,
): List<Feed> = data.mapNotNull { item ->
    runCatching {
        ZhihuJson.decodeJson<Feed>(item)
    }.getOrElse { error ->
        if (ignoreInvalid) {
            null
        } else {
            throw error
        }
    }
}

const val ZHIHU_READ_HISTORY_ADD_URL = "https://www.zhihu.com/api/v4/read_history/add"
const val ZHIHU_LAST_READ_TOUCH_URL = "https://www.zhihu.com/lastread/touch"

fun buildZhihuReadHistoryBody(
    contentToken: String,
    contentType: String,
): String = buildJsonObject {
    put("content_token", contentToken)
    put("content_type", contentType)
}.toString()

fun encodeZhihuLastReadTouchItems(items: List<List<String>>): String =
    ZhihuJson.json.encodeToString(items)

fun zhihuLastReadTouchItem(
    feed: Feed,
    action: String,
): List<String>? = when (val target = feed.target) {
    is Feed.AnswerTarget -> listOf("answer", target.id.toString(), action)
    is Feed.ArticleTarget -> listOf("article", target.id.toString(), action)
    is Feed.PinTarget -> listOf("pin", target.id.toString(), action)
    else -> null
}

fun zhihuLastReadTouchItems(
    items: Set<Pair<String, String>>,
    action: String,
): List<List<String>> = items.map { (type, id) ->
    listOf(type, id, action)
}

suspend fun addZhihuReadHistory(
    client: HttpClient,
    contentToken: String,
    contentType: String,
    dc0: String,
    zse93: String = ZHIHU_WEB_ZSE93,
) {
    val body = buildZhihuReadHistoryBody(contentToken, contentType)
    client.post(ZHIHU_READ_HISTORY_ADD_URL) {
        contentType(ContentType.Application.Json)
        setBody(body)
        signZhihuFetchRequest(
            zse93 = zse93,
            dc0 = dc0,
            body = body,
        )
    }
}

const val ZHIHU_NOTIFICATION_RECENT_URL = "https://www.zhihu.com/api/v4/notifications/v2/recent"
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

fun zhihuNotificationRecentUrl(
    limit: Int = 20,
): String = "https://www.zhihu.com/api/v4/notifications/v2/recent?limit=$limit"


fun decodeZhihuMeNotifications(response: JsonObject): ZhihuMeNotifications =
    ZhihuJson.decodeJson(response)

suspend fun fetchZhihuUnreadNotificationCount(
    client: HttpClient,
    configureRequest: HttpRequestBuilder.() -> Unit = {},
): Int {
    val response = client.get(ZHIHU_ME_URL, configureRequest).body<JsonObject>()
    return decodeZhihuMeNotifications(response).totalCount
}

suspend fun markAllZhihuNotificationsAsRead(
    client: HttpClient,
    configureRequest: HttpRequestBuilder.() -> Unit = {},
) {
    ZHIHU_NOTIFICATION_READ_ALL_URLS.forEach { url ->
        client.post(url, configureRequest)
    }
}

const val ZHIHU_DAILY_LATEST_URL = "https://news-at.zhihu.com/api/4/stories/latest"

suspend fun fetchLatestDailyStories(client: HttpClient): DailyStoriesResponse =
    client.get(ZHIHU_DAILY_LATEST_URL).body()

suspend fun fetchDailyStoriesBefore(
    client: HttpClient,
    date: String,
): DailyStoriesResponse = client.get("https://news-at.zhihu.com/api/4/stories/before/$date").body()

fun zhihuDailyBeforeUrl(date: String): String = "https://news-at.zhihu.com/api/4/stories/before/$date"

suspend fun fetchDailyStoriesForDate(
    client: HttpClient,
    date: String,
): DailyStoriesResponse = fetchDailyStoriesBefore(client, nextDailyApiDate(date))

fun nextDailyApiDate(date: String): String {
    require(date.length == 8 && date.all { it.isDigit() }) {
        "date must use yyyyMMdd format"
    }
    val localDate = LocalDate.parse("${date.substring(0, 4)}-${date.substring(4, 6)}-${date.substring(6, 8)}")
    val nextDate = localDate.plus(1, DateTimeUnit.DAY)
    return nextDate.toString().replace("-", "")
}

suspend fun executeZhihuAuthenticatedRequest(
    client: HttpClient,
    url: String,
    lastRefreshMillis: Long,
    updateLastRefreshMillis: (Long) -> Unit,
    nowMillis: () -> Long = { Clock.System.now().toEpochMilliseconds() },
    block: suspend HttpRequestBuilder.() -> Unit = {},
): HttpResponse {
    val response = client.request(url) {
        block()
    }
    if (response.status != HttpStatusCode.Unauthorized) return response

    if (nowMillis() - lastRefreshMillis < 10_000) {
        return response
    }
    val refreshToken = ZhihuCredentialRefresher.fetchRefreshToken(client)
    ZhihuCredentialRefresher.refreshZhihuToken(refreshToken, client)
    val refreshedAt = nowMillis()
    updateLastRefreshMillis(refreshedAt)
    return client
        .request(url) {
            block()
        }.raiseForStatus()
}

suspend fun fetchZhihuAuthenticatedJson(
    client: HttpClient,
    url: String,
    lastRefreshMillis: Long,
    updateLastRefreshMillis: (Long) -> Unit,
    nowMillis: () -> Long = { Clock.System.now().toEpochMilliseconds() },
    block: suspend HttpRequestBuilder.() -> Unit = {},
): JsonObject? {
    val response = executeZhihuAuthenticatedRequest(
        client = client,
        url = url,
        lastRefreshMillis = lastRefreshMillis,
        updateLastRefreshMillis = updateLastRefreshMillis,
        nowMillis = nowMillis,
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
