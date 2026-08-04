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

package com.github.zly2006.zhihu.ui

import com.github.zly2006.zhihu.data.MobileNotificationContent
import com.github.zly2006.zhihu.data.MobileNotificationHead
import com.github.zly2006.zhihu.data.MobileNotificationTimelineItem
import com.github.zly2006.zhihu.navigation.Notification
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class NotificationScreenTest {
    @Test
    fun conversationPrefersInboxLinkOverNotificationEntryLink() {
        val notification = MobileNotificationTimelineItem(
            content = MobileNotificationContent(
                title = "测试会话",
                targetLink = "https://www.zhihu.com/notifications/v3/timeline/entry/system",
            ),
            head = MobileNotificationHead(
                targetLink = "https://www.zhihu.com/inbox/peer-token",
            ),
        )

        val destination = assertIs<Notification.Message>(notification.navDestination())

        assertEquals("peer-token", destination.peerId)
        assertEquals("测试会话", destination.name)
    }
}
