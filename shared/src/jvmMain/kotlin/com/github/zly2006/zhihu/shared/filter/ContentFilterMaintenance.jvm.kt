package com.github.zly2006.zhihu.shared.filter

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.github.zly2006.zhihu.viewmodel.filter.getContentFilterDatabase
import java.io.File

@Composable
actual fun rememberContentFilterMaintenance(): ContentFilterMaintenance = remember {
    val databasePath = File(System.getProperty("user.home"), ".zhihu-plus/content-filter.db")
    createContentFilterMaintenance(getContentFilterDatabase(databasePath).contentFilterDao())
}
