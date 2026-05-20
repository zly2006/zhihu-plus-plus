package com.github.zly2006.zhihu.shared.data

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlin.test.Test
import kotlin.test.assertEquals

class ZhihuNotificationClientTest {
    @Test
    fun buildsRecentNotificationUrl() {
        assertEquals(
            "https://www.zhihu.com/api/v4/notifications/v2/recent?limit=20",
            zhihuNotificationRecentUrl(),
        )
        assertEquals(
            "https://www.zhihu.com/api/v4/notifications/v2/recent?limit=50",
            zhihuNotificationRecentUrl(limit = 50),
        )
    }

    @Test
    fun decodesUnreadNotificationCountsFromSnakeCasePayload() {
        val notifications = decodeZhihuMeNotifications(
            buildJsonObject {
                put("default_notifications_count", 1)
                put("follow_notifications_count", 2)
                put("vote_thank_notifications_count", 3)
            },
        )

        assertEquals(1, notifications.defaultNotificationsCount)
        assertEquals(2, notifications.followNotificationsCount)
        assertEquals(3, notifications.voteThankNotificationsCount)
        assertEquals(6, notifications.totalCount)
    }

    @Test
    fun fetchUnreadNotificationCountUsesMeEndpoint() = runTest {
        val client = HttpClient(
            MockEngine { request ->
                assertEquals(ZHIHU_ME_URL, request.url.toString())
                respond(
                    content =
                        """
                        {
                          "default_notifications_count": 4,
                          "follow_notifications_count": 5,
                          "vote_thank_notifications_count": 6
                        }
                        """.trimIndent(),
                    status = HttpStatusCode.OK,
                    headers = headersOf(HttpHeaders.ContentType, "application/json"),
                )
            },
        ) {
            installZhihuCommonClientConfig(
                cookies = mutableMapOf(),
                userAgent = "test-agent",
            )
        }

        assertEquals(15, fetchZhihuUnreadNotificationCount(client))
    }

    @Test
    fun markAllNotificationsAsReadPostsEveryCategoryInOrder() = runTest {
        val requestedUrls = mutableListOf<String>()
        val client = HttpClient(
            MockEngine { request ->
                assertEquals(HttpMethod.Post, request.method)
                requestedUrls += request.url.toString()
                respond(
                    content = "{}",
                    status = HttpStatusCode.OK,
                    headers = headersOf(HttpHeaders.ContentType, "application/json"),
                )
            },
        ) {
            installZhihuCommonClientConfig(
                cookies = mutableMapOf(),
                userAgent = "test-agent",
            )
        }

        markAllZhihuNotificationsAsRead(client)

        assertEquals(ZHIHU_NOTIFICATION_READ_ALL_URLS, requestedUrls)
    }
}
