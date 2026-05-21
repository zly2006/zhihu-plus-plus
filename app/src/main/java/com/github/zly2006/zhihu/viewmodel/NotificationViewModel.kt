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

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.viewModelScope
import com.github.zly2006.zhihu.data.AccountData
import com.github.zly2006.zhihu.shared.data.NotificationItem
import com.github.zly2006.zhihu.shared.data.fetchZhihuUnreadNotificationCount
import com.github.zly2006.zhihu.shared.data.markAllZhihuNotificationsAsRead
import com.github.zly2006.zhihu.shared.data.zhihuNotificationRecentUrl
import com.github.zly2006.zhihu.ui.NotificationPreferences
import com.github.zly2006.zhihu.util.signFetchRequest
import com.github.zly2006.zhihu.viewmodel.androidContext
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.launch
import kotlin.reflect.typeOf

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
        val context = environment.androidContext()
        super.fetchFeeds(environment)
        if (lastPaging?.next?.startsWith("http://") == true) {
            lastPaging = lastPaging!!.copy(next = lastPaging!!.next.replace("http://", "https://"))
        }

        // 获取未读消息数量
        unreadCount = getUnreadCount(context)
        checkAndMarkAllAsRead(context)
    }

    /**
     * 更新未读消息数量
     */
    private suspend fun getUnreadCount(context: Context): Int {
        try {
            return fetchZhihuUnreadNotificationCount(AccountData.httpClient(context)) {
                signFetchRequest()
            }
        } catch (_: Exception) {
            // 忽略错误
            return 0
        }
    }

    /**
     * 检查是否需要显示通知
     */
    fun shouldShowNotification(context: Context, notification: NotificationItem): Boolean {
        val verb = notification.content.verb
        val type = NotificationPreferences.matchNotificationType(verb)
        return if (type != null) {
            NotificationPreferences.getDisplayInAppEnabled(context, type)
        } else {
            true
        }
    }

    /**
     * 检查如果所有消息都被屏蔽了，且unreadCount>=0，则主动调用一次readall
     */
    private suspend fun checkAndMarkAllAsRead(context: Context) {
        if (unreadCount >= 0 && allData.isNotEmpty()) {
            val hasVisibleNotification = allData.any { shouldShowNotification(context, it) }
            if (!hasVisibleNotification) {
                markAllAsRead(context)
            }
        }
        if (NotificationPreferences.getAutoMarkAsReadEnabled(context)) {
            markAllAsRead(context)
            unreadCount = 0
        }
    }

    /**
     * 标记消息为已读
     */
    @OptIn(DelicateCoroutinesApi::class)
    fun markAsRead(context: Context, notificationId: String) {
        viewModelScope.launch {
        }
    }

    /**
     * 标记所有消息为已读
     */
    @OptIn(DelicateCoroutinesApi::class)
    suspend fun markAllAsRead(context: Context) {
        markAllZhihuNotificationsAsRead(AccountData.httpClient(context)) {
            signFetchRequest()
        }
    }
}
