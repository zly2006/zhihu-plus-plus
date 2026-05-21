package com.github.zly2006.zhihu.ui

import androidx.compose.runtime.Composable
import com.github.zly2006.zhihu.shared.platform.rememberUserMessageSink

@Composable
actual fun rememberNotificationScreenData(): NotificationScreenData {
    val userMessages = rememberUserMessageSink()
    return NotificationScreenData(
        notifications = emptyList(),
        totalItemCount = 0,
        unreadCount = 0,
        isLoading = false,
        isEnd = true,
        showDebugCopy = false,
        refresh = {},
        loadMore = {},
        markAsRead = {},
        markAllAsRead = {},
        copyDebugData = {},
        showMessage = { message -> userMessages.showMessage(message) },
    )
}

@Composable
actual fun NotificationDebugCopyButton(
    visible: Boolean,
    onClick: () -> Unit,
) {
}
