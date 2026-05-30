package com.github.zly2006.zhihu.ui.subscreens

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.github.zly2006.zhihu.shared.platform.rememberSettingsStore

private const val DEVELOPER_MODE_KEY = "developer"

@Composable
actual fun rememberDeveloperSettingsRuntime(): DeveloperSettingsRuntime {
    val settings = rememberSettingsStore()
    return remember(settings) {
        DeveloperSettingsRuntime(
            isDeveloperModeEnabled = { settings.getBoolean(DEVELOPER_MODE_KEY, false) },
            setDeveloperModeEnabled = { settings.putBoolean(DEVELOPER_MODE_KEY, it) },
            // TODO: iOS cookies 获取
            cookies = { emptyMap() },
            networkStatus = { "网络状态：iOS 端使用系统网络" },
            powerSaveModeText = { null },
            runtimeInfo = { DeveloperRuntimeInfo() },
            // TODO: iOS 登录验证
            verifyLogin = { false },
            // TODO: iOS token 刷新
            refreshToken = { },
            // TODO: iOS cookies 保存
            saveCookies = { },
            // TODO: iOS signed GET
            signedGet = { "iOS 暂不支持" },
        )
    }
}
