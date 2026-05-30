package com.github.zly2006.zhihu.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.github.zly2006.zhihu.shared.desktop.DesktopAccountStore
import com.github.zly2006.zhihu.shared.notification.NotificationSettingsStore
import com.github.zly2006.zhihu.shared.platform.rememberUserMessageSink
import com.github.zly2006.zhihu.viewmodel.DesktopPaginationEnvironment
import com.github.zly2006.zhihu.viewmodel.NotificationViewModel

@Composable
actual fun rememberNotificationScreenRuntime(
    viewModel: NotificationViewModel,
    settingsStore: NotificationSettingsStore,
): NotificationScreenRuntime {
    val userMessages = rememberUserMessageSink()
    val store = remember { DesktopAccountStore() }
    val environment = remember(store, settingsStore, userMessages) {
        DesktopPaginationEnvironment(
            store = store,
            notificationSettingsStore = settingsStore,
            showFetchFailureMessage = userMessages::showMessage,
        )
    }
    return NotificationScreenRuntime(
        environment = environment,
        showDebugCopy = true,
    )
}
