package com.github.zly2006.zhihu.ui

import androidx.compose.runtime.Composable

@Composable
actual fun rememberNotificationScreenData(): NotificationScreenData = NotificationScreenData(
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
    showMessage = {},
)

@Composable
actual fun NotificationDebugCopyButton(
    visible: Boolean,
    onClick: () -> Unit,
) {
}
