package com.github.zly2006.zhihu.shared.notification

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember

// TODO: iOS 通知设置存储完整实现
@Composable
actual fun rememberNotificationSettingsStore(): NotificationSettingsStore = remember {
    object : NotificationSettingsStore {
        override fun getSystemNotificationEnabled(type: NotificationType): Boolean = false

        override fun setSystemNotificationEnabled(type: NotificationType, enabled: Boolean) = Unit

        override fun getDisplayInAppEnabled(type: NotificationType): Boolean = false

        override fun setDisplayInAppEnabled(type: NotificationType, enabled: Boolean) = Unit

        override fun getAutoMarkAsReadEnabled(): Boolean = false

        override fun setAutoMarkAsReadEnabled(enabled: Boolean) = Unit
    }
}
