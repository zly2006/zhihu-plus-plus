package com.github.zly2006.zhihu.ui.subscreens

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.github.zly2006.zhihu.shared.platform.rememberSettingsStore
import kotlinx.coroutines.flow.MutableStateFlow
import java.awt.Desktop
import java.net.URI

@Composable
actual fun rememberSystemUpdateRuntime(): SystemUpdateRuntime {
    val settings = rememberSettingsStore()
    val state = remember { MutableStateFlow<SystemUpdateState>(SystemUpdateState.NoUpdate) }
    return remember(settings, state) {
        SystemUpdateRuntime(
            state = state,
            autoCheckEnabled = { settings.getBoolean(PREF_AUTO_CHECK_UPDATES, true) },
            setAutoCheckEnabled = { enabled ->
                settings.putBoolean(PREF_AUTO_CHECK_UPDATES, enabled)
                if (!enabled) {
                    state.value = SystemUpdateState.NoUpdate
                }
            },
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

private const val PREF_AUTO_CHECK_UPDATES = "autoCheckUpdates"
