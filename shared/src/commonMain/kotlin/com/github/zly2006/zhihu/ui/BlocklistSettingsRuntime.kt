package com.github.zly2006.zhihu.ui

import androidx.compose.runtime.Composable
import com.github.zly2006.zhihu.shared.platform.UserMessageSink
import com.github.zly2006.zhihu.viewmodel.filter.BlockedKeyword
import com.github.zly2006.zhihu.viewmodel.filter.BlockedTopic
import com.github.zly2006.zhihu.viewmodel.filter.BlockedUser
import com.github.zly2006.zhihu.viewmodel.filter.BlocklistStats

data class BlocklistSettingsRuntime(
    val loadKeywords: suspend () -> List<BlockedKeyword>,
    val loadUsers: suspend () -> List<BlockedUser>,
    val loadTopics: suspend () -> List<BlockedTopic>,
    val loadStats: suspend () -> BlocklistStats,
    val requestImport: (((String) -> Unit) -> Unit),
    val exportRules: suspend () -> String,
    val addKeyword: suspend (String, Boolean, Boolean) -> Unit,
    val deleteKeyword: suspend (Long) -> Unit,
    val clearKeywords: suspend () -> Unit,
    val addUser: suspend (String, String) -> Unit,
    val deleteUser: suspend (String) -> Unit,
    val clearUsers: suspend () -> Unit,
    val addTopic: suspend (String, String) -> Unit,
    val deleteTopic: suspend (String) -> Unit,
    val clearTopics: suspend () -> Unit,
)

@Composable
expect fun rememberBlocklistSettingsPlatformRuntime(
    userMessages: UserMessageSink,
): BlocklistSettingsRuntime
