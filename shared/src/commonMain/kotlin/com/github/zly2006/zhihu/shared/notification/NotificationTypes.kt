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

package com.github.zly2006.zhihu.shared.notification
import androidx.compose.runtime.Composable

interface NotificationSettingsStore {
    fun getSystemNotificationEnabled(type: NotificationType): Boolean

    fun setSystemNotificationEnabled(type: NotificationType, enabled: Boolean)

    fun getDisplayInAppEnabled(type: NotificationType): Boolean

    fun setDisplayInAppEnabled(type: NotificationType, enabled: Boolean)

    fun getAutoMarkAsReadEnabled(): Boolean

    fun setAutoMarkAsReadEnabled(enabled: Boolean)

    fun getUnreadBadgeEnabled(): Boolean

    fun setUnreadBadgeEnabled(enabled: Boolean)
}

@Composable
expect fun rememberNotificationSettingsStore(): NotificationSettingsStore

enum class NotificationType(
    val displayName: String,
    val defaultValue: Boolean,
    val regex: Regex,
) {
    LIKE_ANSWER("喜欢了你的回答", true, Regex("喜欢了你的回答")),
    LIKE_COMMENT("喜欢了你的评论", true, Regex("喜欢了.*你的评论")),
    REPLY_COMMENT("回复了你的评论", true, Regex("回复了.*你的评论")),
    INVITE_ANSWER("邀请你回答问题", false, Regex("\\s?(邀请你回答问题|的提问等你来答|邀请你回答)")),
}

fun matchNotificationType(verb: String): NotificationType? =
    NotificationType.entries.find { it.regex.matches(verb) }
