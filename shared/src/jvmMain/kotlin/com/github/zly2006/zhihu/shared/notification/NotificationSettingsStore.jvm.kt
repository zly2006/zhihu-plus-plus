package com.github.zly2006.zhihu.shared.notification

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember

@Composable
actual fun rememberNotificationSettingsStore(): NotificationSettingsStore = remember {
    InMemoryNotificationSettingsStore()
}

private class InMemoryNotificationSettingsStore : NotificationSettingsStore {
    private val systemNotificationSettings = mutableMapOf<NotificationType, Boolean>()
    private val displayInAppSettings = mutableMapOf<NotificationType, Boolean>()
    private var autoMarkAsRead = true

    override fun getSystemNotificationEnabled(type: NotificationType): Boolean =
        systemNotificationSettings[type] ?: false

    override fun setSystemNotificationEnabled(type: NotificationType, enabled: Boolean) {
        systemNotificationSettings[type] = enabled
    }

    override fun getDisplayInAppEnabled(type: NotificationType): Boolean =
        displayInAppSettings[type] ?: type.defaultValue

    override fun setDisplayInAppEnabled(type: NotificationType, enabled: Boolean) {
        displayInAppSettings[type] = enabled
    }

    override fun getAutoMarkAsReadEnabled(): Boolean = autoMarkAsRead

    override fun setAutoMarkAsReadEnabled(enabled: Boolean) {
        autoMarkAsRead = enabled
    }
}
