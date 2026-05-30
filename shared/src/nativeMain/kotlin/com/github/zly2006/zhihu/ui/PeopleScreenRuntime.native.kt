package com.github.zly2006.zhihu.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember

// TODO: iOS 用户页面完整实现
@Composable
actual fun rememberPeopleScreenRuntime(): PeopleScreenRuntime = remember {
    PeopleScreenRuntime(
        openWebUrl = { error("People web URL not available on iOS yet") },
        openImage = { error("People image not available on iOS yet") },
    )
}
