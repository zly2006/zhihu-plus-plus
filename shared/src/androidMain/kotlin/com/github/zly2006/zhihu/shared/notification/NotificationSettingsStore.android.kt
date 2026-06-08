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

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.edit

@Composable
actual fun rememberNotificationSettingsStore(): NotificationSettingsStore {
    val context = LocalContext.current
    return remember(context) { AndroidNotificationSettingsStore(context.applicationContext) }
}

class AndroidNotificationSettingsStore(
    context: Context,
) : NotificationSettingsStore {
    private val preferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)

    override fun getSystemNotificationEnabled(type: NotificationType): Boolean =
        preferences.getBoolean("$KEY_SYSTEM_NOTIFICATION${type.name}", false)

    override fun setSystemNotificationEnabled(type: NotificationType, enabled: Boolean) =
        preferences.edit { putBoolean("$KEY_SYSTEM_NOTIFICATION${type.name}", enabled) }

    override fun getDisplayInAppEnabled(type: NotificationType): Boolean =
        preferences.getBoolean("$KEY_DISPLAY_IN_APP${type.name}", type.defaultValue)

    override fun setDisplayInAppEnabled(type: NotificationType, enabled: Boolean) =
        preferences.edit { putBoolean("$KEY_DISPLAY_IN_APP${type.name}", enabled) }

    override fun getAutoMarkAsReadEnabled(): Boolean =
        preferences.getBoolean(KEY_AUTO_MARK_AS_READ, true)

    override fun setAutoMarkAsReadEnabled(enabled: Boolean) = preferences.edit { putBoolean(KEY_AUTO_MARK_AS_READ, enabled) }
}

private const val PREF_NAME = "notification_settings"
private const val KEY_SYSTEM_NOTIFICATION = "system_notification_"
private const val KEY_DISPLAY_IN_APP = "display_in_app_"
private const val KEY_AUTO_MARK_AS_READ = "auto_mark_notifications_read"
