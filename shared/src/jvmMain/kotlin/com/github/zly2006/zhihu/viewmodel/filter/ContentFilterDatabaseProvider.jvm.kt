package com.github.zly2006.zhihu.viewmodel.filter

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.github.zly2006.zhihu.shared.desktop.desktopZhihuDataFile
import java.io.File

@Composable
actual fun rememberBlockedFeedRecordDao(): BlockedFeedRecordDao = remember {
    getContentFilterDatabase(desktopContentFilterDatabaseFile()).blockedFeedRecordDao()
}

fun desktopContentFilterDatabaseFile(): File =
    desktopZhihuDataFile("content-filter.db")
