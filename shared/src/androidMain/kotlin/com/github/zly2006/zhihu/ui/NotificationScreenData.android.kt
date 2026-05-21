package com.github.zly2006.zhihu.ui

import android.content.ClipData
import android.widget.Toast
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CopyAll
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import com.github.zly2006.zhihu.BuildConfig
import com.github.zly2006.zhihu.ui.components.DraggableRefreshButton
import com.github.zly2006.zhihu.util.clipboardManager
import com.github.zly2006.zhihu.viewmodel.NotificationViewModel
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json

@Composable
actual fun rememberNotificationScreenData(): NotificationScreenData {
    val context = LocalContext.current
    val viewModel = viewModel<NotificationViewModel>()
    val coroutineScope = rememberCoroutineScope()
    return NotificationScreenData(
        notifications = viewModel.allData.filter { viewModel.shouldShowNotification(context, it) },
        totalItemCount = viewModel.allData.size,
        unreadCount = viewModel.unreadCount,
        isLoading = viewModel.isLoading,
        isEnd = viewModel.isEnd,
        showDebugCopy = BuildConfig.DEBUG,
        refresh = { viewModel.refresh(context) },
        loadMore = { viewModel.loadMore(context) },
        markAsRead = { id -> viewModel.markAsRead(context, id) },
        markAllAsRead = {
            coroutineScope.launch {
                viewModel.markAllAsRead(context)
                Toast.makeText(context, "已全部标记为已读", Toast.LENGTH_SHORT).show()
            }
        },
        copyDebugData = {
            val debugData = Json.encodeToString(viewModel.debugData)
            context.clipboardManager.setPrimaryClip(ClipData.newPlainText("data", debugData))
            Toast.makeText(context, "已复制调试数据", Toast.LENGTH_SHORT).show()
        },
        showMessage = { message ->
            Toast.makeText(context, message, Toast.LENGTH_LONG).show()
        },
    )
}

@Composable
actual fun NotificationDebugCopyButton(
    visible: Boolean,
    onClick: () -> Unit,
) {
    if (!visible) {
        return
    }
    DraggableRefreshButton(
        onClick = onClick,
        preferenceName = "copyAll",
    ) {
        Icon(Icons.Default.CopyAll, contentDescription = "复制")
    }
}
