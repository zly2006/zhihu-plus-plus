package com.github.zly2006.zhihu.shared.filter

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import com.github.zly2006.zhihu.viewmodel.filter.getContentFilterDatabase

@Composable
actual fun rememberContentFilterMaintenance(): ContentFilterMaintenance {
    val context = LocalContext.current.applicationContext
    return remember(context) { createContentFilterMaintenance(getContentFilterDatabase(context).contentFilterDao()) }
}
