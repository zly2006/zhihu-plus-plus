package com.github.zly2006.zhihu.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import com.github.zly2006.zhihu.shared.platform.UserMessageSink
import com.github.zly2006.zhihu.viewmodel.filter.BlocklistService
import com.github.zly2006.zhihu.viewmodel.filter.getContentFilterDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import javax.swing.JFileChooser
import javax.swing.filechooser.FileNameExtensionFilter

@Composable
actual fun rememberBlocklistSettingsPlatformRuntime(
    userMessages: UserMessageSink,
): BlocklistSettingsRuntime {
    val service = remember {
        val databaseFile = blocklistDatabaseFile()
        databaseFile.parentFile?.mkdirs()
        val database = getContentFilterDatabase(databaseFile)
        BlocklistService(
            keywordDao = database.blockedKeywordDao(),
            userDao = database.blockedUserDao(),
            topicDao = database.blockedTopicDao(),
        )
    }
    val coroutineScope = rememberCoroutineScope()
    return remember(service, userMessages) {
        BlocklistSettingsRuntime(
            userMessages = userMessages,
            loadKeywords = { withContext(Dispatchers.IO) { service.getAllBlockedKeywords() } },
            loadUsers = { withContext(Dispatchers.IO) { service.getAllBlockedUsers() } },
            loadTopics = { withContext(Dispatchers.IO) { service.getAllBlockedTopics() } },
            loadStats = { withContext(Dispatchers.IO) { service.getBlocklistStats() } },
            requestImport = { onImported ->
                val selectedFile = chooseBlocklistImportFile()
                if (selectedFile != null) {
                    coroutineScope.launch {
                        try {
                            val summary = withContext(Dispatchers.IO) {
                                service.importAllBlocklistFromJsonText(selectedFile.readText())
                            }
                            onImported(summary)
                        } catch (e: Exception) {
                            e.printStackTrace()
                            userMessages.showShortMessage("导入失败: ${e.message}")
                        }
                    }
                }
            },
            exportRules = {
                withContext(Dispatchers.IO) {
                    val file = File(blocklistDatabaseFile().parentFile, "zhihupp_blocklist.json")
                    file.writeText(service.encodeAllBlocklistToJson())
                    "已导出到 ${file.absolutePath}"
                }
            },
            addKeyword = { keyword, caseSensitive, isRegex ->
                withContext(Dispatchers.IO) {
                    service.addBlockedKeyword(keyword, caseSensitive, isRegex)
                }
            },
            deleteKeyword = { keywordId ->
                withContext(Dispatchers.IO) { service.removeBlockedKeyword(keywordId) }
            },
            clearKeywords = {
                withContext(Dispatchers.IO) { service.clearAllBlockedKeywords() }
            },
            addUser = { userId, userName ->
                withContext(Dispatchers.IO) { service.addBlockedUser(userId, userName) }
            },
            deleteUser = { userId ->
                withContext(Dispatchers.IO) { service.removeBlockedUser(userId) }
            },
            clearUsers = {
                withContext(Dispatchers.IO) { service.clearAllBlockedUsers() }
            },
            addTopic = { topicId, topicName ->
                withContext(Dispatchers.IO) { service.addBlockedTopic(topicId, topicName) }
            },
            deleteTopic = { topicId ->
                withContext(Dispatchers.IO) { service.removeBlockedTopic(topicId) }
            },
            clearTopics = {
                withContext(Dispatchers.IO) { service.clearAllBlockedTopics() }
            },
        )
    }
}

private fun blocklistDatabaseFile(): File {
    val home = System.getProperty("user.home")
    return File(home, ".zhihu-plus-plus/content-filter.db")
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
