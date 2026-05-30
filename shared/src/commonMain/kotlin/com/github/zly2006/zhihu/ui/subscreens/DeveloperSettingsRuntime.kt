package com.github.zly2006.zhihu.ui.subscreens

import androidx.compose.runtime.Composable

data class DeveloperSettingsRuntime(
    val isDeveloperModeEnabled: () -> Boolean,
    val setDeveloperModeEnabled: (Boolean) -> Unit,
    val cookies: () -> Map<String, String>,
    val networkStatus: () -> String,
    val powerSaveModeText: () -> String?,
    val runtimeInfo: () -> DeveloperRuntimeInfo,
    val verifyLogin: suspend (Map<String, String>) -> Boolean,
    val refreshToken: suspend () -> Unit,
    val saveCookies: (Map<String, String>) -> Unit,
    val signedGetAndCopy: suspend (String) -> String,
)

@Composable
expect fun rememberDeveloperSettingsRuntime(): DeveloperSettingsRuntime
