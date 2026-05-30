package com.github.zly2006.zhihu.viewmodel.filter

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember

@Composable
actual fun rememberBlocklistManager(): BlocklistManager = remember {
    val databaseFile = desktopContentFilterDatabaseFile()
    databaseFile.parentFile?.mkdirs()
    getContentFilterDatabase(databaseFile).createBlocklistManager()
}
