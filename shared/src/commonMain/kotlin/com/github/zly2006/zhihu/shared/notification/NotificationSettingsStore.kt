package com.github.zly2006.zhihu.shared.notification

import androidx.compose.runtime.Composable

interface NotificationSettingsStore {
    fun getSystemNotificationEnabled(type: NotificationType): Boolean

    fun setSystemNotificationEnabled(type: NotificationType, enabled: Boolean)

    fun getDisplayInAppEnabled(type: NotificationType): Boolean

    fun setDisplayInAppEnabled(type: NotificationType, enabled: Boolean)

    fun getAutoMarkAsReadEnabled(): Boolean

    fun setAutoMarkAsReadEnabled(enabled: Boolean)
}

@Composable
expect fun rememberNotificationSettingsStore(): NotificationSettingsStore
