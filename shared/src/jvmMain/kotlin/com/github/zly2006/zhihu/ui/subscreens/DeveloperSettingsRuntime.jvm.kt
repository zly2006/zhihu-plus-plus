package com.github.zly2006.zhihu.ui.subscreens

import androidx.compose.runtime.Composable

@Composable
actual fun rememberDeveloperSettingsRuntime(): DeveloperSettingsRuntime = DeveloperSettingsRuntime(
    isDeveloperModeEnabled = { false },
    setDeveloperModeEnabled = {},
    cookies = { emptyMap() },
    networkStatus = { "网络状态：未知" },
    powerSaveModeText = { null },
    runtimeInfo = { DeveloperRuntimeInfo() },
    verifyLogin = { false },
    refreshToken = {},
    saveCookies = {},
    signedGetAndCopy = { "" },
    showShortMessage = {},
)
