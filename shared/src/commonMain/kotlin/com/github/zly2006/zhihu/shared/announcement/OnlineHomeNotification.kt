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

package com.github.zly2006.zhihu.shared.announcement

import com.github.zly2006.zhihu.shared.data.ZhihuJson
import com.github.zly2006.zhihu.shared.platform.SettingsStore
import com.github.zly2006.zhihu.shared.util.raiseForStatus
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import kotlinx.coroutines.CancellationException
import kotlinx.io.buffered
import kotlinx.io.files.Path
import kotlinx.io.files.SystemFileSystem
import kotlinx.io.readString
import kotlinx.io.writeString
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonPrimitive
import kotlin.time.Clock

const val ZHIHU_PLUS_PLUS_HOME_NOTIFICATIONS_URL = "https://redenmc.com/api/zhihu/notifications"
const val HOME_NOTIFICATION_LAST_CHECK_PREFERENCE_KEY = "onlineHomeNotificationLastCheck"
const val HOME_NOTIFICATION_CACHE_FILE_NAME = "online_home_notifications.json"
const val HOME_NOTIFICATION_CHECK_INTERVAL_MILLIS = 3 * 60 * 60 * 1000L

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

@Serializable
private data class OnlineHomeNotificationCache(
    val notifications: List<OnlineHomeNotification> = emptyList(),
    val readUuids: Set<String> = emptySet(),
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
    private val cacheFile: Path,
) {
    private var cache: OnlineHomeNotificationCache? = null

    fun cachedNotifications(): List<OnlineHomeNotification> {
        val currentCache = readCache()
        return currentCache.notifications
            .asSequence()
            .filter { it.isValid() }
            .distinctBy { it.uuid }
            .filterNot { it.uuid in currentCache.readUuids }
            .toList()
    }

    suspend fun load(
        versionName: String,
        httpClient: HttpClient,
        now: Long = Clock.System.now().toEpochMilliseconds(),
    ): List<OnlineHomeNotification> {
        val cached = cachedNotifications()
        val lastCheck = settings.getLong(HOME_NOTIFICATION_LAST_CHECK_PREFERENCE_KEY, 0)
        if (lastCheck > 0 && now - lastCheck in 0 until HOME_NOTIFICATION_CHECK_INTERVAL_MILLIS) {
            return cached
        }

        // 失败也计入本轮检查，避免首页反复进入时持续重试并造成额外请求。
        settings.putLong(HOME_NOTIFICATION_LAST_CHECK_PREFERENCE_KEY, now)
        val response = try {
            fetchOnlineHomeNotifications(httpClient, versionName)
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            return cached
        }

        val notifications = response.notifications
            .asSequence()
            .filter { it.isValid() }
            .distinctBy { it.uuid }
            .toList()
        writeCache(readCache().copy(notifications = notifications))
        return cachedNotifications()
    }

    fun markRead(notification: OnlineHomeNotification) {
        val currentCache = readCache()
        writeCache(currentCache.copy(readUuids = currentCache.readUuids + notification.uuid))
    }

    private fun readCache(): OnlineHomeNotificationCache {
        cache?.let { return it }
        return runCatching {
            if (!SystemFileSystem.exists(cacheFile)) return@runCatching OnlineHomeNotificationCache()
            SystemFileSystem.source(cacheFile).buffered().use { source ->
                ZhihuJson.json.decodeFromString<OnlineHomeNotificationCache>(source.readString())
            }
        }.getOrDefault(OnlineHomeNotificationCache()).also { cache = it }
    }

    private fun writeCache(updatedCache: OnlineHomeNotificationCache) {
        cache = updatedCache
        SystemFileSystem.sink(cacheFile).buffered().use { sink ->
            sink.writeString(ZhihuJson.json.encodeToString(updatedCache))
        }
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
