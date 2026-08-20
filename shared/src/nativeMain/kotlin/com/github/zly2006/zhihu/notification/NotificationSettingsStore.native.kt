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

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.github.zly2006.zhihu.platform.SettingsStore
import com.github.zly2006.zhihu.platform.nativeSettingsStore

@Composable
actual fun rememberNotificationSettingsStore(): NotificationSettingsStore = remember {
    nativeNotificationSettingsStore()
}

internal fun nativeNotificationSettingsStore(): NotificationSettingsStore =
    NativeNotificationSettingsStore(nativeSettingsStore("notification_settings.properties"))

private class NativeNotificationSettingsStore(
    private val settings: SettingsStore,
) : NotificationSettingsStore {
    override fun getSystemNotificationEnabled(type: NotificationType): Boolean =
        settings.getBoolean("$KEY_SYSTEM_NOTIFICATION${type.name}", false)

    override fun setSystemNotificationEnabled(type: NotificationType, enabled: Boolean) =
        settings.putBoolean("$KEY_SYSTEM_NOTIFICATION${type.name}", enabled)

    override fun getDisplayInAppEnabled(type: NotificationType): Boolean =
        settings.getBoolean("$KEY_DISPLAY_IN_APP${type.name}", type.defaultValue)

    override fun setDisplayInAppEnabled(type: NotificationType, enabled: Boolean) =
        settings.putBoolean("$KEY_DISPLAY_IN_APP${type.name}", enabled)

    override fun getAutoMarkAsReadEnabled(): Boolean =
        settings.getBoolean(KEY_AUTO_MARK_AS_READ, false)

    override fun setAutoMarkAsReadEnabled(enabled: Boolean) =
        settings.putBoolean(KEY_AUTO_MARK_AS_READ, enabled)

    override fun getUnreadBadgeEnabled(): Boolean =
        settings.getBoolean(KEY_UNREAD_BADGE, true)

    override fun setUnreadBadgeEnabled(enabled: Boolean) =
        settings.putBoolean(KEY_UNREAD_BADGE, enabled)
}

private const val KEY_SYSTEM_NOTIFICATION = "system_notification_"
private const val KEY_DISPLAY_IN_APP = "display_in_app_"
private const val KEY_AUTO_MARK_AS_READ = "auto_mark_notifications_read"
private const val KEY_UNREAD_BADGE = "show_unread_badge"
