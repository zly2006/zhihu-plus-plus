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
import com.github.zly2006.zhihu.navigation.Account
import com.github.zly2006.zhihu.navigation.Article
import com.github.zly2006.zhihu.navigation.ArticleType
import com.github.zly2006.zhihu.navigation.NavDestination
import com.github.zly2006.zhihu.navigation.Pin
import com.github.zly2006.zhihu.platform.SettingsStore
import com.github.zly2006.zhihu.util.raiseForStatus
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.intOrNull
import kotlin.time.Clock

const val ZHIHU_PLUS_PLUS_HOME_NOTIFICATIONS_URL = "https://api.redenmc.com/api/zhihu/notifications"
const val HOME_NOTIFICATION_READ_UUIDS_PREFERENCE_KEY = "onlineHomeNotificationReadUuids"
const val HOME_NOTIFICATION_REFRESH_INTERVAL_MILLIS = 3 * 60 * 60 * 1000L
private const val HOME_NOTIFICATION_CACHE_PREFERENCE_KEY = "onlineHomeNotificationCache"

@Serializable
private data class CachedHomeNotifications(
    val versionName: String,
    val fetchedAt: Long,
    val response: OnlineHomeNotificationResponse,
)

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

sealed interface HomeNotificationAction {
    data class OpenUrl(
        val url: String,
        val inWebView: Boolean,
    ) : HomeNotificationAction

    data class Navigate(
        val destination: NavDestination,
    ) : HomeNotificationAction

    data class SetBoolean(
        val name: String,
        val value: Boolean,
    ) : HomeNotificationAction

    data class SetString(
        val name: String,
        val value: String,
    ) : HomeNotificationAction

    data class SetInt(
        val name: String,
        val value: Int,
    ) : HomeNotificationAction
}

@Serializable
data class OnlineHomeNotificationAccept(
    val text: String,
    val key: String,
    val value: JsonElement? = null,
) {
    @Transient
    val action: HomeNotificationAction? = when (key) {
        "open_url", "open_webview" -> (value as? JsonPrimitive)
            ?.contentOrNull
            ?.takeIf { it.startsWith("https://") }
            ?.let { HomeNotificationAction.OpenUrl(it, key == "open_webview") }
        "open_update_settings" -> if (value == null) HomeNotificationAction.Navigate(Account.SystemAndUpdateSettings()) else null
        "open_pin", "open_answer", "open_article" -> (value as? JsonPrimitive)?.contentOrNull?.toLongOrNull()?.let {
            HomeNotificationAction.Navigate(
                when (key) {
                    "open_pin" -> Pin(it)
                    "open_answer" -> Article(type = ArticleType.Answer, id = it)
                    else -> Article(type = ArticleType.Article, id = it)
                },
            )
        }
        "set_setting" -> (value as? JsonObject)?.let { setting ->
            val name = (setting["setting_name"] as? JsonPrimitive)?.contentOrNull?.takeIf { it.isNotBlank() }
            val settingValue = setting["value"] as? JsonPrimitive
            if (name == null) {
                null
            } else {
                when ((setting["value_type"] as? JsonPrimitive)?.contentOrNull) {
                    "boolean" -> settingValue?.booleanOrNull?.let { HomeNotificationAction.SetBoolean(name, it) }
                    "string" -> settingValue?.contentOrNull?.let { HomeNotificationAction.SetString(name, it) }
                    "int" -> settingValue?.intOrNull?.let { HomeNotificationAction.SetInt(name, it) }
                    else -> null
                }
            }
        }
        else -> null
    }
}

class OnlineHomeNotificationRepository(
    private val settings: SettingsStore,
) {
    private val refreshMutex = Mutex()

    suspend fun load(
        versionName: String,
        httpClient: HttpClient,
    ): List<OnlineHomeNotification> = refreshMutex.withLock {
        val now = Clock.System.now().toEpochMilliseconds()
        val cached = settings.getStringOrNull(HOME_NOTIFICATION_CACHE_PREFERENCE_KEY)?.let {
            runCatching { ZhihuJson.json.decodeFromString<CachedHomeNotifications>(it) }.getOrNull()
        }
        val response = if (
            cached != null &&
            cached.versionName == versionName &&
            now - cached.fetchedAt in 0 until HOME_NOTIFICATION_REFRESH_INTERVAL_MILLIS
        ) {
            cached.response
        } else {
            httpClient
                .get(ZHIHU_PLUS_PLUS_HOME_NOTIFICATIONS_URL) {
                    parameter("version", versionName)
                }.raiseForStatus()
                .body<OnlineHomeNotificationResponse>()
                .also {
                    settings.putString(
                        HOME_NOTIFICATION_CACHE_PREFERENCE_KEY,
                        ZhihuJson.json.encodeToString(CachedHomeNotifications(versionName, now, it)),
                    )
                }
        }
        val readUuids = settings.getStringSet(HOME_NOTIFICATION_READ_UUIDS_PREFERENCE_KEY, emptySet())
        response
            .notifications
            .asSequence()
            .filter {
                it.expiresAt > now &&
                    it.uuid.matches(homeNotificationUuidPattern) &&
                    it.title.isNotBlank() &&
                    it.dismiss.isNotBlank() &&
                    (it.accept == null || (it.accept.text.isNotBlank() && it.accept.action != null))
            }.distinctBy { it.uuid }
            .filterNot { it.uuid in readUuids }
            .toList()
    }

    fun markRead(notification: OnlineHomeNotification) {
        val readUuids = settings.getStringSet(HOME_NOTIFICATION_READ_UUIDS_PREFERENCE_KEY, emptySet())
        settings.putStringSet(HOME_NOTIFICATION_READ_UUIDS_PREFERENCE_KEY, readUuids + notification.uuid)
    }
}
