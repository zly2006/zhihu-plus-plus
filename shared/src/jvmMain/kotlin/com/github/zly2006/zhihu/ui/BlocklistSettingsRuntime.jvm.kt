package com.github.zly2006.zhihu.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import com.github.zly2006.zhihu.shared.platform.UserMessageSink
import com.github.zly2006.zhihu.shared.util.Log
import com.github.zly2006.zhihu.viewmodel.filter.createBlocklistManager
import com.github.zly2006.zhihu.viewmodel.filter.desktopContentFilterDatabaseFile
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
            requestImport = { onImported ->
                val selectedFile = chooseBlocklistImportFile()
                if (selectedFile != null) {
                    coroutineScope.launch {
                        try {
                            val summary = manager.importAllBlocklistFromJsonText(selectedFile.readText())
                            onImported(summary)
                        } catch (e: Exception) {
                            Log.e("BlocklistSettingsRuntime", "Failed to import blocklist", e)
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
        )
    }
}

private fun blocklistDatabaseFile(): File = desktopContentFilterDatabaseFile()

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
