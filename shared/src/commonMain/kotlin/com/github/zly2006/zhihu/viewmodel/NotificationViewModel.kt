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

package com.github.zly2006.zhihu.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import com.github.zly2006.zhihu.shared.data.NotificationItem
import com.github.zly2006.zhihu.shared.data.ZHIHU_ME_URL
import com.github.zly2006.zhihu.shared.data.ZHIHU_NOTIFICATION_READ_ALL_URLS
import com.github.zly2006.zhihu.shared.data.ZhihuJson
import com.github.zly2006.zhihu.shared.data.ZhihuMeNotifications
import com.github.zly2006.zhihu.shared.data.ZhihuPaging
import com.github.zly2006.zhihu.shared.notification.NotificationSettingsStore
import com.github.zly2006.zhihu.shared.notification.matchNotificationType
import com.github.zly2006.zhihu.shared.util.Log
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.jsonArray
import kotlin.coroutines.cancellation.CancellationException
import kotlin.reflect.typeOf

interface NotificationSettingsEnvironment {
    val notificationSettingsStore: NotificationSettingsStore
}

interface NotificationEnvironment :
    PaginationEnvironment,
    NotificationSettingsEnvironment

class NotificationViewModel :
    PaginationViewModel<NotificationItem>(
        dataType = typeOf<NotificationItem>(),
    ) {
    override val initialUrl = "https://www.zhihu.com/api/v4/notifications/v2/recent?limit=20"
    private val notificationSourceUrls = listOf(
        "https://www.zhihu.com/api/v4/notifications/v2/default?limit=20",
        "https://www.zhihu.com/api/v4/notifications/v2/follow?limit=20",
        "https://www.zhihu.com/api/v4/notifications/v2/vote_thank?limit=20",
    )
    private val notificationSourcePaging = mutableMapOf<String, ZhihuPaging?>()
    private val endedNotificationSources = mutableSetOf<String>()
    override val isEnd: Boolean
        get() = notificationSourcePaging.isNotEmpty() &&
            notificationSourceUrls.all { it in endedNotificationSources }

    // 未读消息数量
    var unreadCount: Int by mutableIntStateOf(0)
        private set

    @Suppress("HttpUrlsUsage")
    override suspend fun fetchFeeds(environment: PaginationEnvironment) {
        try {
            val notificationSettingsEnvironment = environment as? NotificationSettingsEnvironment
                ?: error("NotificationSettingsStore is required for notification pagination")
            val rawData = mutableListOf<JsonElement>()
            val decodedData = mutableListOf<NotificationItem>()
            val sourceFailures = mutableListOf<Exception>()
            var successfulSourceCount = 0
            val sourcesToFetch = if (notificationSourcePaging.isEmpty()) {
                notificationSourceUrls
            } else {
                notificationSourceUrls.filterNot { it in endedNotificationSources }
            }

            sourcesToFetch.forEach { sourceUrl ->
                try {
                    val url = notificationSourcePaging[sourceUrl]?.next ?: sourceUrl
                    val json = environment.fetchJson(url.replace("http://", "https://"), include) ?: return@forEach
                    successfulSourceCount++
                    val jsonArray = json["data"]?.jsonArray ?: JsonArray(emptyList())
                    rawData.addAll(jsonArray)
                    decodedData += jsonArray.mapNotNull {
                        try {
                            ZhihuJson.decodeJson<NotificationItem>(it)
                        } catch (e: Exception) {
                            if (shouldLogDecodeFailures) {
                                environment.logDecodeFailure(this::class.simpleName, it, e)
                            }
                            null
                        }
                    }

                    val paging = json["paging"]?.let { ZhihuJson.decodeJson<ZhihuPaging>(it) }
                    notificationSourcePaging[sourceUrl] = paging
                    if (paging == null || paging.isEnd) {
                        endedNotificationSources += sourceUrl
                    }
                } catch (e: Exception) {
                    if (e is CancellationException) throw e
                    sourceFailures += e
                    Log.e("NotificationViewModel", "Failed to fetch notification source: $sourceUrl", e)
                }
            }

            if (successfulSourceCount == 0 && sourceFailures.isNotEmpty()) {
                environment.handleFetchFailure(this::class.simpleName, sourceFailures.first())
                return
            }

            processResponse(
                environment,
                decodedData,
                buildJsonArray {
                    rawData.forEach { add(it) }
                },
            )

            // 获取未读消息数量
            unreadCount = try {
                environment
                    .fetchJson(ZHIHU_ME_URL, "")
                    ?.let { ZhihuJson.decodeJson<ZhihuMeNotifications>(it) }
                    ?.totalCount ?: 0
            } catch (_: Exception) {
                0
            }
            checkAndMarkAllAsRead(environment, notificationSettingsEnvironment)
        } finally {
            isLoading = false
        }
    }

    override fun refresh(environment: PaginationEnvironment) {
        notificationSourcePaging.clear()
        endedNotificationSources.clear()
        super.refresh(environment)
    }

    override fun processResponse(environment: PaginationEnvironment, data: List<NotificationItem>, rawData: JsonArray) {
        debugData.addAll(rawData)
        val existingIds = allData.mapTo(mutableSetOf()) { it.id }
        val merged = (allData + data.filter { existingIds.add(it.id) })
            .sortedByDescending { it.createTime }
        allData.clear()
        allData.addAll(merged)
    }

    /**
     * 检查是否需要显示通知
     */
    fun shouldShowNotification(settingsStore: NotificationSettingsStore, notification: NotificationItem): Boolean {
        val verb = notification.content.verb
        val type = matchNotificationType(verb)
        return if (type != null) {
            settingsStore.getDisplayInAppEnabled(type)
        } else {
            true
        }
    }

    /**
     * 检查如果所有消息都被屏蔽了，且unreadCount>=0，则主动调用一次readall
     */
    private suspend fun checkAndMarkAllAsRead(
        apiEnvironment: ZhihuApiEnvironment,
        settingsEnvironment: NotificationSettingsEnvironment,
    ) {
        if (unreadCount >= 0 && allData.isNotEmpty()) {
            val hasVisibleNotification = allData.any {
                shouldShowNotification(settingsEnvironment.notificationSettingsStore, it)
            }
            if (!hasVisibleNotification) {
                markAllAsRead(apiEnvironment)
            }
        }
        if (settingsEnvironment.notificationSettingsStore.getAutoMarkAsReadEnabled()) {
            markAllAsRead(apiEnvironment)
            unreadCount = 0
        }
    }

    /**
     * 标记所有消息为已读
     */
    suspend fun markAllAsRead(environment: ZhihuApiEnvironment) {
        ZHIHU_NOTIFICATION_READ_ALL_URLS.forEach { url ->
            environment.postSigned(url)
        }
    }
}
