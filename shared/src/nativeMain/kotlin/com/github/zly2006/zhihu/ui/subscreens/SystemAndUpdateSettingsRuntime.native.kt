package com.github.zly2006.zhihu.ui.subscreens

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.github.zly2006.zhihu.ui.openIosUrl
import kotlinx.coroutines.flow.MutableStateFlow

// TODO: iOS 更新检查实现
@Composable
actual fun rememberSystemUpdateRuntime(): SystemUpdateRuntime = remember {
    SystemUpdateRuntime(
        state = MutableStateFlow(SystemUpdateState.NoUpdate),
        autoCheckEnabled = { false },
        setAutoCheckEnabled = { },
        checkForUpdate = { },
        skipVersion = { },
        resetToNoUpdate = { },
        downloadUpdate = { },
        installDownloadedUpdate = { },
        setError = { },
        supportsApkInstall = false,
    )
}

@Composable
actual fun rememberExternalUrlOpener(): (String) -> Unit = remember {
    { url -> openIosUrl(url) }
}
