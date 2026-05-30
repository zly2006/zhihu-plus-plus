package com.github.zly2006.zhihu.ui.subscreens

import androidx.compose.runtime.Composable
import kotlinx.coroutines.flow.StateFlow

data class SystemUpdateRuntime(
    val state: StateFlow<SystemUpdateState>,
    val autoCheckEnabled: () -> Boolean,
    val setAutoCheckEnabled: (Boolean) -> Unit,
    val checkForUpdate: suspend () -> Unit,
    val skipVersion: (String) -> Unit,
    val resetToNoUpdate: () -> Unit,
    val downloadUpdate: suspend (String) -> Unit,
    val installDownloadedUpdate: suspend () -> Unit,
    val setError: (String) -> Unit,
    val supportsApkInstall: Boolean,
)

sealed interface SystemUpdateState {
    data object NoUpdate : SystemUpdateState

    data object Checking : SystemUpdateState

    data object Latest : SystemUpdateState

    data class UpdateAvailable(
        val version: String,
        val isNightly: Boolean,
        val releaseNotes: String?,
        val downloadUrl: String,
        val cnDownloadUrl: String?,
    ) : SystemUpdateState

    data object Downloading : SystemUpdateState

    data object Downloaded : SystemUpdateState

    data class Error(
        val message: String,
    ) : SystemUpdateState
}

@Composable
expect fun rememberSystemUpdateRuntime(): SystemUpdateRuntime
