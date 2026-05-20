package com.github.zly2006.zhihu.shared.data

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.http.URLBuilder
import io.ktor.http.appendPathSegments
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject

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
): String = URLBuilder("https://www.zhihu.com")
    .apply {
        appendPathSegments("api", "v4", "notifications", "v2", "recent")
        parameters.append("limit", limit.toString())
    }.buildString()

@Serializable
data class ZhihuMeNotifications(
    val defaultNotificationsCount: Int = 0,
    val followNotificationsCount: Int = 0,
    val voteThankNotificationsCount: Int = 0,
) {
    val totalCount: Int get() = defaultNotificationsCount + followNotificationsCount + voteThankNotificationsCount
}

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
