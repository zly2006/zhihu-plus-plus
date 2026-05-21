package com.github.zly2006.zhihu.shared.notification

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.edit

@Composable
actual fun rememberNotificationSettingsStore(): NotificationSettingsStore {
    val context = LocalContext.current
    return remember(context) {
        AndroidNotificationSettingsStore(context.applicationContext)
    }
}

private class AndroidNotificationSettingsStore(
    context: Context,
) : NotificationSettingsStore {
    private val preferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)

    override fun getSystemNotificationEnabled(type: NotificationType): Boolean =
        preferences.getBoolean("$KEY_SYSTEM_NOTIFICATION${type.name}", false)

    override fun setSystemNotificationEnabled(type: NotificationType, enabled: Boolean) {
        preferences.edit { putBoolean("$KEY_SYSTEM_NOTIFICATION${type.name}", enabled) }
    }

    override fun getDisplayInAppEnabled(type: NotificationType): Boolean =
        preferences.getBoolean("$KEY_DISPLAY_IN_APP${type.name}", type.defaultValue)

    override fun setDisplayInAppEnabled(type: NotificationType, enabled: Boolean) {
        preferences.edit { putBoolean("$KEY_DISPLAY_IN_APP${type.name}", enabled) }
    }

    override fun getAutoMarkAsReadEnabled(): Boolean =
        preferences.getBoolean(KEY_AUTO_MARK_AS_READ, true)

    override fun setAutoMarkAsReadEnabled(enabled: Boolean) {
        preferences.edit { putBoolean(KEY_AUTO_MARK_AS_READ, enabled) }
    }
}

private const val PREF_NAME = "notification_settings"
private const val KEY_SYSTEM_NOTIFICATION = "system_notification_"
private const val KEY_DISPLAY_IN_APP = "display_in_app_"
private const val KEY_AUTO_MARK_AS_READ = "auto_mark_notifications_read"
