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

import com.github.zly2006.zhihu.platform.SettingsStore
import com.github.zly2006.zhihu.util.raiseForStatus
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonPrimitive

const val ZHIHU_PLUS_PLUS_HOME_NOTIFICATIONS_URL = "https://redenmc.com/api/zhihu/notifications"
const val HOME_NOTIFICATION_READ_UUIDS_PREFERENCE_KEY = "onlineHomeNotificationReadUuids"
const val HOME_NOTIFICATION_REFRESH_INTERVAL_MILLIS = 3 * 60 * 60 * 1000L

const val HOME_NOTIFICATION_ACTION_OPEN_URL = "open_url"
const val HOME_NOTIFICATION_ACTION_OPEN_UPDATE_SETTINGS = "open_update_settings"
const val HOME_NOTIFICATION_ACTION_OPEN_PIN = "open_pin"
const val HOME_NOTIFICATION_ACTION_OPEN_ANSWER = "open_answer"
const val HOME_NOTIFICATION_ACTION_OPEN_ARTICLE = "open_article"
const val HOME_NOTIFICATION_ACTION_SET_SETTING = "set_setting"

private val homeNotificationUuidPattern = Regex(
    "[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[1-5][0-9a-fA-F]{3}-[89abAB][0-9a-fA-F]{3}-[0-9a-fA-F]{12}",
)

@Serializable
data class OnlineHomeNotificationResponse(
    val notifications: List<OnlineHomeNotification> = emptyList(),
)

@Serializable
data class OnlineHomeNotification(
    val uuid: String,
    val expiresAt: Long,
    val title: String,
    val content: String? = null,
    val accept: OnlineHomeNotificationAccept? = null,
    val dismiss: String,
)

@Serializable
data class OnlineHomeNotificationAccept(
    val text: String,
    val key: String,
    val value: JsonElement? = null,
)

suspend fun fetchOnlineHomeNotifications(
    client: HttpClient,
    versionName: String,
): OnlineHomeNotificationResponse = client
    .get(ZHIHU_PLUS_PLUS_HOME_NOTIFICATIONS_URL) {
        parameter("version", versionName)
    }.raiseForStatus()
    .body()

class OnlineHomeNotificationRepository(
    private val settings: SettingsStore,
) {
    suspend fun load(
        versionName: String,
        httpClient: HttpClient,
    ): List<OnlineHomeNotification> {
        val readUuids = settings.getStringSet(HOME_NOTIFICATION_READ_UUIDS_PREFERENCE_KEY, emptySet())
        return fetchOnlineHomeNotifications(httpClient, versionName)
            .notifications
            .asSequence()
            .filter { it.isValid() }
            .distinctBy { it.uuid }
            .filterNot { it.uuid in readUuids }
            .toList()
    }

    fun markRead(notification: OnlineHomeNotification) {
        val readUuids = settings.getStringSet(HOME_NOTIFICATION_READ_UUIDS_PREFERENCE_KEY, emptySet())
        settings.putStringSet(HOME_NOTIFICATION_READ_UUIDS_PREFERENCE_KEY, readUuids + notification.uuid)
    }

    private fun OnlineHomeNotification.isValid(): Boolean =
        uuid.matches(homeNotificationUuidPattern) &&
            title.isNotBlank() &&
            dismiss.isNotBlank() &&
            accept?.isValid() != false

    private fun OnlineHomeNotificationAccept.isValid(): Boolean = text.isNotBlank() &&
        when (key) {
            HOME_NOTIFICATION_ACTION_OPEN_URL ->
                (value as? JsonPrimitive)?.contentOrNull?.startsWith("https://") == true
            HOME_NOTIFICATION_ACTION_OPEN_PIN,
            HOME_NOTIFICATION_ACTION_OPEN_ANSWER,
            HOME_NOTIFICATION_ACTION_OPEN_ARTICLE,
            ->
                (value as? JsonPrimitive)?.contentOrNull?.toLongOrNull() != null
            HOME_NOTIFICATION_ACTION_OPEN_UPDATE_SETTINGS -> value == null
            HOME_NOTIFICATION_ACTION_SET_SETTING -> {
                val setting = value as? JsonObject
                val name = setting?.get("setting_name")?.jsonPrimitive?.contentOrNull
                when (setting?.get("value_type")?.jsonPrimitive?.contentOrNull) {
                    "boolean" -> name?.isNotBlank() == true && setting["value"]?.jsonPrimitive?.booleanOrNull != null
                    "string" -> name?.isNotBlank() == true && setting["value"]?.jsonPrimitive?.contentOrNull != null
                    "int" -> name?.isNotBlank() == true && setting["value"]?.jsonPrimitive?.intOrNull != null
                    else -> false
                }
            }
            else -> false
        }
}
