package com.github.zly2006.zhihu.ui

import android.content.ClipData
import android.content.pm.ApplicationInfo
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import com.github.zly2006.zhihu.shared.notification.rememberNotificationSettingsStore
import com.github.zly2006.zhihu.shared.platform.UserMessageDuration
import com.github.zly2006.zhihu.shared.platform.rememberUserMessageSink
import com.github.zly2006.zhihu.util.clipboardManager
import com.github.zly2006.zhihu.viewmodel.NotificationViewModel
import com.github.zly2006.zhihu.viewmodel.notificationPaginationEnvironment
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json

@Composable
actual fun rememberNotificationScreenData(): NotificationScreenData {
    val context = LocalContext.current
    val settingsStore = rememberNotificationSettingsStore()
    val viewModel = viewModel<NotificationViewModel>()
    val environment = remember(context, settingsStore, viewModel) {
        viewModel.notificationPaginationEnvironment(context, settingsStore)
    }
    val coroutineScope = rememberCoroutineScope()
    val userMessages = rememberUserMessageSink()
    return NotificationScreenData(
        notifications = viewModel.allData.filter { viewModel.shouldShowNotification(settingsStore, it) },
        totalItemCount = viewModel.allData.size,
        unreadCount = viewModel.unreadCount,
        isLoading = viewModel.isLoading,
        isEnd = viewModel.isEnd,
        showDebugCopy = (context.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0,
        refresh = { viewModel.refresh(environment) },
        loadMore = { viewModel.loadMore(environment) },
        markAsRead = { id -> viewModel.markAsRead(id) },
        markAllAsRead = {
            coroutineScope.launch {
                viewModel.markAllAsRead(environment)
                userMessages.showMessage("已全部标记为已读")
            }
        },
        copyDebugData = {
            val debugData = Json.encodeToString(viewModel.debugData)
            context.clipboardManager.setPrimaryClip(ClipData.newPlainText("data", debugData))
            userMessages.showMessage("已复制调试数据")
        },
        showMessage = { message ->
            userMessages.showMessage(message, UserMessageDuration.Long)
        },
    )
}
