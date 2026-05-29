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
        // TODO: iOS 自动检查更新
        autoCheckEnabled = { false },
        // TODO: iOS 设置自动检查更新
        setAutoCheckEnabled = { },
        // TODO: iOS 检查更新
        checkForUpdate = { },
        // TODO: iOS 跳过版本
        skipVersion = { },
        // TODO: iOS 重置更新状态
        resetToNoUpdate = { },
        // TODO: iOS 下载更新
        downloadUpdate = { },
        // TODO: iOS 安装更新
        installDownloadedUpdate = { },
        // TODO: iOS 设置错误状态
        setError = { },
        supportsApkInstall = false,
    )
}

@Composable
actual fun rememberExternalUrlOpener(): (String) -> Unit = remember {
    { url -> openIosUrl(url) }
}
