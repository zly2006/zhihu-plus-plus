package com.github.zly2006.zhihu.viewmodel.filter

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext

@Composable
actual fun rememberBlockedFeedRecordDao(): BlockedFeedRecordDao {
    val context = LocalContext.current
    return remember(context) {
        getContentFilterDatabase(context).blockedFeedRecordDao()
    }
}
