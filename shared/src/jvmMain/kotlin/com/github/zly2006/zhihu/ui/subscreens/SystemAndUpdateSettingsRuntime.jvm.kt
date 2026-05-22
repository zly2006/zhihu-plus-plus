package com.github.zly2006.zhihu.ui.subscreens

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import kotlinx.coroutines.flow.MutableStateFlow
import java.awt.Desktop
import java.net.URI

@Composable
actual fun rememberSystemUpdateRuntime(): SystemUpdateRuntime = remember {
    val state = MutableStateFlow<SystemUpdateState>(SystemUpdateState.NoUpdate)
    SystemUpdateRuntime(
        state = state,
        autoCheckEnabled = { false },
        setAutoCheckEnabled = {},
        checkForUpdate = { state.value = SystemUpdateState.Latest },
        skipVersion = { state.value = SystemUpdateState.Latest },
        resetToNoUpdate = { state.value = SystemUpdateState.NoUpdate },
        downloadUpdate = {
            state.value = SystemUpdateState.Error("桌面端不支持 APK 更新安装")
        },
        installDownloadedUpdate = {
            state.value = SystemUpdateState.Error("桌面端不支持 APK 更新安装")
        },
        setError = { message -> state.value = SystemUpdateState.Error(message) },
        supportsApkInstall = false,
    )
}

@Composable
actual fun rememberExternalUrlOpener(): (String) -> Unit = remember {
    { url ->
        runCatching {
            if (Desktop.isDesktopSupported()) {
                Desktop.getDesktop().browse(URI(url))
            }
        }
    }
}
