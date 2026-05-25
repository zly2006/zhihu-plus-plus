package com.github.zly2006.zhihu.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import com.github.zly2006.zhihu.shared.platform.UserMessageSink
import com.github.zly2006.zhihu.viewmodel.filter.createBlocklistManager
import com.github.zly2006.zhihu.viewmodel.filter.getContentFilterDatabase
import kotlinx.coroutines.launch
import java.io.File
import javax.swing.JFileChooser
import javax.swing.filechooser.FileNameExtensionFilter

@Composable
actual fun rememberBlocklistSettingsPlatformRuntime(
    userMessages: UserMessageSink,
): BlocklistSettingsRuntime {
    val manager = remember {
        val databaseFile = blocklistDatabaseFile()
        databaseFile.parentFile?.mkdirs()
        val database = getContentFilterDatabase(databaseFile)
        database.createBlocklistManager()
    }
    val coroutineScope = rememberCoroutineScope()
    return remember(manager, userMessages) {
        BlocklistSettingsRuntime(
            userMessages = userMessages,
            loadKeywords = manager::getAllBlockedKeywords,
            loadUsers = manager::getAllBlockedUsers,
            loadTopics = manager::getAllBlockedTopics,
            loadStats = manager::getBlocklistStats,
            requestImport = { onImported ->
                val selectedFile = chooseBlocklistImportFile()
                if (selectedFile != null) {
                    coroutineScope.launch {
                        try {
                            val summary = manager.importAllBlocklistFromJsonText(selectedFile.readText())
                            onImported(summary)
                        } catch (e: Exception) {
                            e.printStackTrace()
                            userMessages.showShortMessage("导入失败: ${e.message}")
                        }
                    }
                }
            },
            exportRules = {
                val file = File(blocklistDatabaseFile().parentFile, "zhihupp_blocklist.json")
                file.writeText(manager.exportAllBlocklistToJsonText())
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

private fun blocklistDatabaseFile(): File {
    val home = System.getProperty("user.home")
    return File(home, ".zhihu-plus/content-filter.db")
}

private fun chooseBlocklistImportFile(): File? {
    val chooser = JFileChooser().apply {
        dialogTitle = "导入屏蔽规则"
        fileSelectionMode = JFileChooser.FILES_ONLY
        fileFilter = FileNameExtensionFilter("JSON 或文本文件", "json", "txt")
    }
    return if (chooser.showOpenDialog(null) == JFileChooser.APPROVE_OPTION) {
        chooser.selectedFile
    } else {
        null
    }
}
