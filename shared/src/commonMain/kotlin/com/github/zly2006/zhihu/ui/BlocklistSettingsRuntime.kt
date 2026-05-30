package com.github.zly2006.zhihu.ui

import androidx.compose.runtime.Composable
import com.github.zly2006.zhihu.shared.platform.UserMessageSink

data class BlocklistSettingsRuntime(
    val requestImport: (((String) -> Unit) -> Unit),
    val exportRules: suspend () -> String,
)

@Composable
expect fun rememberBlocklistSettingsPlatformRuntime(
    userMessages: UserMessageSink,
): BlocklistSettingsRuntime
