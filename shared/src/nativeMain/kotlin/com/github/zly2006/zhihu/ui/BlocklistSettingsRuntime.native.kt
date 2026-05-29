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
        // TODO: iOS 加载关键词
        loadKeywords = { emptyList() },
        // TODO: iOS 加载用户
        loadUsers = { emptyList() },
        // TODO: iOS 加载话题
        loadTopics = { emptyList() },
        // TODO: iOS 加载统计数据
        loadStats = { error("Blocklist stats not available on iOS yet") },
        // TODO: iOS 导入规则
        requestImport = { _ -> },
        // TODO: iOS 导出规则
        exportRules = { "" },
        // TODO: iOS 添加关键词
        addKeyword = { _, _, _ -> },
        // TODO: iOS 删除关键词
        deleteKeyword = { },
        // TODO: iOS 清空关键词
        clearKeywords = { },
        // TODO: iOS 添加用户
        addUser = { _, _ -> },
        // TODO: iOS 删除用户
        deleteUser = { },
        // TODO: iOS 清空用户
        clearUsers = { },
        // TODO: iOS 添加话题
        addTopic = { _, _ -> },
        // TODO: iOS 删除话题
        deleteTopic = { },
        // TODO: iOS 清空话题
        clearTopics = { },
    )
}
