package com.github.zly2006.zhihu.ui

import android.content.ClipData
import android.content.pm.ApplicationInfo
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import com.github.zly2006.zhihu.shared.notification.NotificationSettingsStore
import com.github.zly2006.zhihu.util.clipboardManager
import com.github.zly2006.zhihu.viewmodel.NotificationViewModel
import com.github.zly2006.zhihu.viewmodel.notificationPaginationEnvironment

@Composable
actual fun rememberNotificationScreenRuntime(
    viewModel: NotificationViewModel,
    settingsStore: NotificationSettingsStore,
): NotificationScreenRuntime {
    val context = LocalContext.current
    val environment = remember(context, settingsStore, viewModel) {
        viewModel.notificationPaginationEnvironment(context, settingsStore)
    }
    return NotificationScreenRuntime(
        environment = environment,
        showDebugCopy = (context.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0,
        copyDebugText = { label, text ->
            context.clipboardManager.setPrimaryClip(ClipData.newPlainText(label, text))
        },
    )
}
