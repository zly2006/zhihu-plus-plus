package com.github.zly2006.zhihu.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.github.zly2006.zhihu.shared.platform.UserMessageSink

@Composable
actual fun rememberBlocklistSettingsPlatformRuntime(
    userMessages: UserMessageSink,
): BlocklistSettingsRuntime = remember(userMessages) {
    // TODO: iOS 屏蔽列表功能
    BlocklistSettingsRuntime(
        requestImport = { _ -> }, // TODO: iOS 导入规则
        exportRules = { "" }, // TODO: iOS 导出规则
    )
}
