package com.github.zly2006.zhihu.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.github.zly2006.zhihu.shared.desktop.openDesktopExternalUrl

@Composable
actual fun rememberPeopleScreenRuntime(): PeopleScreenRuntime = remember {
    PeopleScreenRuntime(
        openWebUrl = { url ->
            openDesktopExternalUrl(url)
        },
        openImage = { url ->
            openDesktopExternalUrl(url)
        },
    )
}
