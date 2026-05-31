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

@Composable
actual fun rememberNotificationSettingsStore(): NotificationSettingsStore = remember {
    object : NotificationSettingsStore {
        override fun getSystemNotificationEnabled(type: NotificationType): Boolean = false // TODO: iOS 获取系统通知开关

        override fun setSystemNotificationEnabled(type: NotificationType, enabled: Boolean) = Unit // TODO: iOS 设置系统通知开关

        override fun getDisplayInAppEnabled(type: NotificationType): Boolean = false // TODO: 获取App内通知开关

        override fun setDisplayInAppEnabled(type: NotificationType, enabled: Boolean) = Unit // TODO: iOS 设置App内通知开关

        override fun getAutoMarkAsReadEnabled(): Boolean = false // TODO: iOS 获取自动已读开关

        override fun setAutoMarkAsReadEnabled(enabled: Boolean) = Unit // TODO: iOS 设置自动已读开关
    }
} // TODO: iOS 通知设置存储完整实现
