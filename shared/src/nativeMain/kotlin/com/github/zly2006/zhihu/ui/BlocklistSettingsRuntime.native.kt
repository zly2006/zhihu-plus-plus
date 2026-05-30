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
        loadKeywords = { emptyList() }, // TODO: iOS 加载关键词
        loadUsers = { emptyList() }, // TODO: iOS 加载用户
        loadTopics = { emptyList() }, // TODO: iOS 加载话题
        loadStats = { error("Blocklist stats not available on iOS yet") }, // TODO: iOS 加载统计数据
        requestImport = { _ -> }, // TODO: iOS 导入规则
        exportRules = { "" }, // TODO: iOS 导出规则
        addKeyword = { _, _, _ -> }, // TODO: iOS 添加关键词
        deleteKeyword = { }, // TODO: iOS 删除关键词
        clearKeywords = { }, // TODO: iOS 清空关键词
        addUser = { _, _ -> }, // TODO: iOS 添加用户
        deleteUser = { }, // TODO: iOS 删除用户
        clearUsers = { }, // TODO: iOS 清空用户
        addTopic = { _, _ -> }, // TODO: iOS 添加话题
        deleteTopic = { }, // TODO: iOS 删除话题
        clearTopics = { }, // TODO: iOS 清空话题
    )
}
