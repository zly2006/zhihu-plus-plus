package com.github.zly2006.zhihu.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.github.zly2006.zhihu.shared.platform.UserMessageSink

// TODO: iOS 屏蔽列表功能
@Composable
actual fun rememberBlocklistSettingsPlatformRuntime(
    userMessages: UserMessageSink,
): BlocklistSettingsRuntime = remember(userMessages) {
    BlocklistSettingsRuntime(
        userMessages = userMessages,
        loadKeywords = { emptyList() },
        loadUsers = { emptyList() },
        loadTopics = { emptyList() },
        loadStats = { error("Blocklist stats not available on iOS yet") },
        requestImport = { _ -> },
        exportRules = { "" },
        addKeyword = { _, _, _ -> },
        deleteKeyword = { },
        clearKeywords = { },
        addUser = { _, _ -> },
        deleteUser = { },
        clearUsers = { },
        addTopic = { _, _ -> },
        deleteTopic = { },
        clearTopics = { },
    )
}
