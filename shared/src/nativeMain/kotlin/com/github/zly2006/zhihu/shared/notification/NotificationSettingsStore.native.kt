package com.github.zly2006.zhihu.shared.notification

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember

// TODO: iOS 通知设置存储完整实现
@Composable
actual fun rememberNotificationSettingsStore(): NotificationSettingsStore = remember {
    object : NotificationSettingsStore {
        // TODO: iOS 获取系统通知开关
        override fun getSystemNotificationEnabled(type: NotificationType): Boolean = false

        // TODO: iOS 设置系统通知开关
        override fun setSystemNotificationEnabled(type: NotificationType, enabled: Boolean) = Unit

        // TODO: 获取App内通知开关
        override fun getDisplayInAppEnabled(type: NotificationType): Boolean = false

        // TODO: iOS 设置App内通知开关
        override fun setDisplayInAppEnabled(type: NotificationType, enabled: Boolean) = Unit

        // TODO: iOS 获取自动已读开关
        override fun getAutoMarkAsReadEnabled(): Boolean = false

        // TODO: iOS 设置自动已读开关
        override fun setAutoMarkAsReadEnabled(enabled: Boolean) = Unit
    }
}
