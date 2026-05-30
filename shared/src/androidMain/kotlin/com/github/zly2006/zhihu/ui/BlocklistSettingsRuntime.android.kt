package com.github.zly2006.zhihu.ui

import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.FileProvider
import com.github.zly2006.zhihu.shared.platform.UserMessageSink
import com.github.zly2006.zhihu.shared.util.Log
import com.github.zly2006.zhihu.viewmodel.filter.exportAllBlocklistToJson
import com.github.zly2006.zhihu.viewmodel.filter.getBlocklistManager
import com.github.zly2006.zhihu.viewmodel.filter.importAllBlocklistFromJson
import kotlinx.coroutines.launch

@Composable
actual fun rememberBlocklistSettingsPlatformRuntime(
    userMessages: UserMessageSink,
): BlocklistSettingsRuntime {
    val context = LocalContext.current
    val manager = remember(context) { getBlocklistManager(context) }
    val coroutineScope = rememberCoroutineScope()
    var importCallback by remember { mutableStateOf<((String) -> Unit)?>(null) }
    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
    ) { uri ->
        if (uri != null) {
            coroutineScope.launch {
                try {
                    val summary = manager.importAllBlocklistFromJson(context, uri)
                    importCallback?.invoke(summary)
                } catch (e: Exception) {
                    Log.e("BlocklistSettingsRuntime", "Failed to import blocklist", e)
                    userMessages.showShortMessage("导入失败: ${e.message}")
                }
            }
        }
    }
    return remember(context, manager, userMessages, importLauncher) {
        BlocklistSettingsRuntime(
            loadKeywords = manager::getAllBlockedKeywords,
            loadUsers = manager::getAllBlockedUsers,
            loadTopics = manager::getAllBlockedTopics,
            loadStats = manager::getBlocklistStats,
            requestImport = { onImported ->
                importCallback = onImported
                importLauncher.launch(arrayOf("application/json", "text/plain", "*/*"))
            },
            exportRules = {
                val file = manager.exportAllBlocklistToJson(context)
                val intent = Intent().apply {
                    action = Intent.ACTION_VIEW
                    setDataAndType(
                        FileProvider.getUriForFile(
                            context,
                            "${context.packageName}.provider",
                            file,
                        ),
                        "application/json",
                    )
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                context.startActivity(Intent.createChooser(intent, "查看屏蔽规则"))
                "已导出到 ${file.absolutePath}"
            },
            addKeyword = manager::addBlockedKeyword,
            deleteKeyword = manager::removeBlockedKeyword,
            clearKeywords = manager::clearAllBlockedKeywords,
            addUser = { userId, userName -> manager.addBlockedUser(userId, userName) },
            deleteUser = manager::removeBlockedUser,
            clearUsers = manager::clearAllBlockedUsers,
            addTopic = manager::addBlockedTopic,
            deleteTopic = manager::removeBlockedTopic,
            clearTopics = manager::clearAllBlockedTopics,
        )
    }
}
