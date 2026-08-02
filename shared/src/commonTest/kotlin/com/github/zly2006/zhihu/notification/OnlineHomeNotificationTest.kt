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

package com.github.zly2006.zhihu.notification

import com.github.zly2006.zhihu.data.ZhihuJson
import com.github.zly2006.zhihu.platform.SettingsStore
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonObject
import kotlin.test.Test
import kotlin.test.assertEquals

class OnlineHomeNotificationTest {
    @Test
    fun repositoryCachesForThreeHours() = runTest {
        val values = mutableMapOf<String, Any>()
        val repository = OnlineHomeNotificationRepository(mapBackedSettingsStore(values))
        val notification = OnlineHomeNotification(
            uuid = "da811fe3-858a-4655-afd4-a63024b74dbb",
            expiresAt = 500,
            title = "在线通知",
            accept = OnlineHomeNotificationAccept("打开", HOME_NOTIFICATION_ACTION_OPEN_URL, JsonPrimitive("https://example.com")),
            dismiss = "关闭",
        )
        var requests = 0
        val client = HttpClient(
            MockEngine {
                requests++
                respond(
                    content = ZhihuJson.json.encodeToString(OnlineHomeNotificationResponse(listOf(notification))),
                    status = HttpStatusCode.OK,
                    headers = headersOf(HttpHeaders.ContentType, "application/json"),
                )
            },
        ) {
            install(ContentNegotiation) {
                json(ZhihuJson.json)
            }
        }

        assertEquals(listOf(notification), repository.load("0.26", client, now = 1_000))
        assertEquals(listOf(notification), repository.load("0.26", client, now = 1_000 + HOME_NOTIFICATION_CHECK_INTERVAL_MILLIS - 1))
        assertEquals(1, requests)
        assertEquals(listOf(notification), repository.load("0.26", client, now = 1_000 + HOME_NOTIFICATION_CHECK_INTERVAL_MILLIS))
        assertEquals(2, requests)
    }

    @Test
    fun readStateUsesUuidIndependentlyAndKeepsExpiresAtAsMetadata() {
        val values = mutableMapOf<String, Any>()
        val repository = OnlineHomeNotificationRepository(mapBackedSettingsStore(values))
        val readNotification = OnlineHomeNotification(
            uuid = "da811fe3-858a-4655-afd4-a63024b74dbb",
            expiresAt = 500,
            title = "已读通知",
            dismiss = "关闭",
        )
        val unreadNotification = OnlineHomeNotification(
            uuid = "2c9fe00a-80f3-4b8f-89a1-b96dbe411791",
            expiresAt = 400,
            title = "未读通知",
            dismiss = "关闭",
        )
        values[HOME_NOTIFICATION_CACHE_PREFERENCE_KEY] = ZhihuJson.json.encodeToString(
            OnlineHomeNotificationResponse(listOf(readNotification, unreadNotification)),
        )

        repository.markRead(readNotification)

        assertEquals(listOf(unreadNotification), repository.cachedNotifications())
        val readState = ZhihuJson.json
            .parseToJsonElement(values.getValue(HOME_NOTIFICATION_READ_STATE_PREFERENCE_KEY).toString())
            .jsonObject
            .getValue("expiresAtByUuid")
            .jsonObject
        assertEquals(JsonPrimitive(readNotification.expiresAt), readState[readNotification.uuid])
    }

    @Test
    fun actionDecodesGenericJsonValue() {
        val response = ZhihuJson.json.decodeFromString<OnlineHomeNotificationResponse>(
            """
            {
              "notifications": [{
                "uuid": "da811fe3-858a-4655-afd4-a63024b74dbb",
                "expiresAt": 500,
                "title": "在线通知",
                "accept": {
                  "text": "执行",
                  "key": "future_action",
                  "value": {"enabled": true, "count": 3}
                },
                "dismiss": "关闭"
              }]
            }
            """.trimIndent(),
        )

        val action = response.notifications.single().accept!!
        val value = action.value!!.jsonObject
        assertEquals("future_action", action.key)
        assertEquals(JsonPrimitive(true), value["enabled"])
        assertEquals(JsonPrimitive(3), value["count"])
    }

    private fun mapBackedSettingsStore(values: MutableMap<String, Any>): SettingsStore = SettingsStore(
        getBoolean = { key, default -> values[key] as? Boolean ?: default },
        putBoolean = { key, value -> values[key] = value },
        getString = { key, default -> values[key] as? String ?: default },
        putString = { key, value -> values[key] = value },
        getStringOrNull = { key -> values[key] as? String },
        putStringSet = { key, value -> values[key] = value },
        getStringSet = { key, default ->
            @Suppress("UNCHECKED_CAST")
            (values[key] as? Set<String> ?: default)
        },
        getInt = { key, default -> values[key] as? Int ?: default },
        putInt = { key, value -> values[key] = value },
        getLong = { key, default -> values[key] as? Long ?: default },
        putLong = { key, value -> values[key] = value },
        getFloat = { key, default -> values[key] as? Float ?: default },
        putFloat = { key, value -> values[key] = value },
        remove = values::remove,
    )
}
