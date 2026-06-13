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

package com.github.zly2006.zhihu.shared.notification

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.github.zly2006.zhihu.shared.desktop.desktopZhihuDataFile
import java.util.Properties

@Composable
actual fun rememberNotificationSettingsStore(): NotificationSettingsStore = remember { desktopNotificationSettingsStore() }

fun desktopNotificationSettingsStore(): NotificationSettingsStore = DesktopNotificationSettingsStore()

private class DesktopNotificationSettingsStore : NotificationSettingsStore {
    private val settingsFile = desktopZhihuDataFile("notification_settings.properties")
    private val properties = Properties()

    init {
        load()
    }

    private fun load() {
        if (settingsFile.isFile) settingsFile.inputStream().use(properties::load)
    }

    private fun save() {
        settingsFile.parentFile?.mkdirs()
        settingsFile.outputStream().use { output ->
            properties.store(output, "Zhihu++ desktop notification settings")
        }
    }

    override fun getSystemNotificationEnabled(type: NotificationType): Boolean =
        properties.getProperty("$KEY_SYSTEM_NOTIFICATION${type.name}")?.toBooleanStrictOrNull() ?: false

    override fun setSystemNotificationEnabled(type: NotificationType, enabled: Boolean) =
        setBoolean("$KEY_SYSTEM_NOTIFICATION${type.name}", enabled)

    override fun getDisplayInAppEnabled(type: NotificationType): Boolean =
        properties.getProperty("$KEY_DISPLAY_IN_APP${type.name}")?.toBooleanStrictOrNull() ?: type.defaultValue

    override fun setDisplayInAppEnabled(type: NotificationType, enabled: Boolean) =
        setBoolean("$KEY_DISPLAY_IN_APP${type.name}", enabled)

    override fun getAutoMarkAsReadEnabled(): Boolean =
        properties.getProperty(KEY_AUTO_MARK_AS_READ)?.toBooleanStrictOrNull() ?: true

    override fun setAutoMarkAsReadEnabled(enabled: Boolean) = setBoolean(KEY_AUTO_MARK_AS_READ, enabled)

    override fun getUnreadBadgeEnabled(): Boolean =
        properties.getProperty(KEY_UNREAD_BADGE)?.toBooleanStrictOrNull() ?: true

    override fun setUnreadBadgeEnabled(enabled: Boolean) = setBoolean(KEY_UNREAD_BADGE, enabled)

    private fun setBoolean(key: String, enabled: Boolean) {
        properties.setProperty(key, enabled.toString())
        save()
    }
}

private const val KEY_SYSTEM_NOTIFICATION = "system_notification_"
private const val KEY_DISPLAY_IN_APP = "display_in_app_"
private const val KEY_AUTO_MARK_AS_READ = "auto_mark_notifications_read"
private const val KEY_UNREAD_BADGE = "show_unread_badge"
