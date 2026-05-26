package com.github.zly2006.zhihu.shared.filter

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.github.zly2006.zhihu.viewmodel.filter.desktopContentFilterDatabaseFile
import com.github.zly2006.zhihu.viewmodel.filter.getContentFilterDatabase

@Composable
actual fun rememberContentFilterMaintenance(): ContentFilterMaintenance = remember {
    createContentFilterMaintenance(getContentFilterDatabase(desktopContentFilterDatabaseFile()).contentFilterDao())
}
