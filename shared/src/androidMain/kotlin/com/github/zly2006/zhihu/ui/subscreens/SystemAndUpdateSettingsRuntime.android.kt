package com.github.zly2006.zhihu.ui.subscreens

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import androidx.core.net.toUri
import com.github.zly2006.zhihu.updater.UpdateManager
import com.github.zly2006.zhihu.updater.UpdateManager.UpdateState
import com.github.zly2006.zhihu.util.luoTianYiUrlLauncher
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

@Composable
actual fun rememberSystemUpdateRuntime(): SystemUpdateRuntime {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    return remember(context, scope) {
        SystemUpdateRuntime(
            state = UpdateManager.updateState.map { it.toSystemUpdateState() }.stateIn(
                scope,
                SharingStarted.Eagerly,
                UpdateManager.updateState.value.toSystemUpdateState(),
            ),
            autoCheckEnabled = { UpdateManager.isAutoCheckEnabled(context) },
            setAutoCheckEnabled = { enabled ->
                UpdateManager.setAutoCheckEnabled(context, enabled)
                if (!enabled) {
                    UpdateManager.updateState.value = UpdateState.NoUpdate
                }
            },
            checkForUpdate = { UpdateManager.checkForUpdate(context) },
            skipVersion = { version ->
                UpdateManager.skipVersion(context, version)
                UpdateManager.updateState.value = UpdateState.Latest
            },
            resetToNoUpdate = {
                UpdateManager.updateState.value = UpdateState.NoUpdate
            },
            downloadUpdate = { url -> UpdateManager.downloadUpdate(context, url) },
            installDownloadedUpdate = {
                val state = UpdateManager.updateState.value
                if (state is UpdateState.Downloaded) {
                    UpdateManager.installUpdate(context, state.file)
                }
            },
            setError = { message ->
                UpdateManager.updateState.value = UpdateState.Error(message)
            },
            supportsApkInstall = true,
        )
    }
}

@Composable
actual fun rememberExternalUrlOpener(): (String) -> Unit {
    val context = LocalContext.current
    return remember(context) {
        { url ->
            luoTianYiUrlLauncher(context, url.toUri())
        }
    }
}

private fun UpdateState.toSystemUpdateState(): SystemUpdateState = when (this) {
    UpdateState.NoUpdate -> SystemUpdateState.NoUpdate
    UpdateState.Checking -> SystemUpdateState.Checking
    UpdateState.Latest -> SystemUpdateState.Latest
    is UpdateState.UpdateAvailable -> SystemUpdateState.UpdateAvailable(
        version = version.toString(),
        isNightly = isNightly,
        releaseNotes = releaseNotes,
        downloadUrl = downloadUrl,
        cnDownloadUrl = cnDownloadUrl,
    )
    UpdateState.Downloading -> SystemUpdateState.Downloading
    is UpdateState.Downloaded -> SystemUpdateState.Downloaded
    is UpdateState.Error -> SystemUpdateState.Error(message)
}
