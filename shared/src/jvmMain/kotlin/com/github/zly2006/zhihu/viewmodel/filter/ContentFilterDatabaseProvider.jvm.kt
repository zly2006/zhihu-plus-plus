package com.github.zly2006.zhihu.viewmodel.filter

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import java.io.File

@Composable
actual fun rememberBlockedFeedRecordDao(): BlockedFeedRecordDao = remember {
    val databasePath = File(System.getProperty("user.home"), ".zhihu-plus/content-filter.db")
    getContentFilterDatabase(databasePath).blockedFeedRecordDao()
}
