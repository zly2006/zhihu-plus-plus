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
import androidx.lifecycle.viewModelScope
import com.github.zly2006.zhihu.shared.data.NotificationItem
import com.github.zly2006.zhihu.shared.data.ZhihuJson
import com.github.zly2006.zhihu.shared.data.ZhihuPaging
import com.github.zly2006.zhihu.shared.data.zhihuNotificationDefaultUrl
import com.github.zly2006.zhihu.shared.data.zhihuNotificationFollowUrl
import com.github.zly2006.zhihu.shared.data.zhihuNotificationRecentUrl
import com.github.zly2006.zhihu.shared.data.zhihuNotificationVoteThankUrl
import com.github.zly2006.zhihu.shared.notification.NotificationSettingsStore
import com.github.zly2006.zhihu.shared.notification.matchNotificationType
import com.github.zly2006.zhihu.shared.util.Log
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.launch
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
    override val initialUrl = zhihuNotificationRecentUrl()
    private val notificationSourceUrls = listOf(
        zhihuNotificationDefaultUrl(),
        zhihuNotificationFollowUrl(),
        zhihuNotificationVoteThankUrl(),
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
            val notificationSettingsEnvironment = environment.requireNotificationSettingsEnvironment()
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

            if (shouldReportNotificationFetchFailure(successfulSourceCount, sourceFailures.size)) {
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
            unreadCount = getUnreadCount(environment)
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
        val merged = mergeNotificationsByCreateTime(allData, data)
        allData.clear()
        allData.addAll(merged)
    }

    /**
     * 更新未读消息数量
     */
    private suspend fun getUnreadCount(environment: ZhihuApiEnvironment): Int {
        try {
            return environment.fetchUnreadNotificationCountSigned()
        } catch (_: Exception) {
            // 忽略错误
            return 0
        }
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
     * 标记消息为已读
     */
    @OptIn(DelicateCoroutinesApi::class)
    fun markAsRead(notificationId: String) {
        viewModelScope.launch {
        }
    }

    /**
     * 标记所有消息为已读
     */
    @OptIn(DelicateCoroutinesApi::class)
    suspend fun markAllAsRead(environment: ZhihuApiEnvironment) {
        environment.markAllNotificationsAsReadSigned()
    }
}

private fun PaginationEnvironment.requireNotificationSettingsEnvironment(): NotificationSettingsEnvironment =
    this as? NotificationSettingsEnvironment
        ?: error("NotificationSettingsStore is required for notification pagination")

internal fun mergeNotificationsByCreateTime(
    existing: List<NotificationItem>,
    incoming: List<NotificationItem>,
): List<NotificationItem> {
    val existingIds = existing.mapTo(mutableSetOf()) { it.id }
    return (existing + incoming.filter { existingIds.add(it.id) })
        .sortedByDescending { it.createTime }
}

internal fun shouldReportNotificationFetchFailure(
    successfulSourceCount: Int,
    failureCount: Int,
): Boolean = successfulSourceCount == 0 && failureCount > 0
