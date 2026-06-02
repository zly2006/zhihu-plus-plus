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
import com.github.zly2006.zhihu.shared.data.fetchZhihuUnreadNotificationCount
import com.github.zly2006.zhihu.shared.data.markAllZhihuNotificationsAsRead
import com.github.zly2006.zhihu.shared.data.zhihuNotificationRecentUrl
import com.github.zly2006.zhihu.shared.notification.NotificationSettingsStore
import com.github.zly2006.zhihu.shared.notification.matchNotificationType
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.launch
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

    // 未读消息数量
    var unreadCount: Int by mutableIntStateOf(0)
        private set

    @Suppress("HttpUrlsUsage")
    override suspend fun fetchFeeds(environment: PaginationEnvironment) {
        val notificationSettingsEnvironment = environment.requireNotificationSettingsEnvironment()
        super.fetchFeeds(environment)
        if (lastPaging?.next?.startsWith("http://") == true) {
            lastPaging = lastPaging!!.copy(next = lastPaging!!.next.replace("http://", "https://"))
        }

        // 获取未读消息数量
        unreadCount = getUnreadCount(environment)
        checkAndMarkAllAsRead(environment, notificationSettingsEnvironment)
    }

    /**
     * 更新未读消息数量
     */
    private suspend fun getUnreadCount(environment: ZhihuApiEnvironment): Int {
        try {
            return fetchZhihuUnreadNotificationCount(environment.httpClient()) {
                environment.configureSignedRequest(this)
            }
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
        markAllZhihuNotificationsAsRead(environment.httpClient()) {
            environment.configureSignedRequest(this)
        }
    }
}

private fun PaginationEnvironment.requireNotificationSettingsEnvironment(): NotificationSettingsEnvironment =
    this as? NotificationSettingsEnvironment
        ?: error("NotificationSettingsStore is required for notification pagination")
