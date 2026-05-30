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
            cookies = { emptyMap() }, // TODO: iOS cookies 获取
            networkStatus = { "网络状态：iOS 端使用系统网络" },
            powerSaveModeText = { null },
            runtimeInfo = { DeveloperRuntimeInfo() },
            verifyLogin = { false }, // TODO: iOS 登录验证
            refreshToken = { }, // TODO: iOS token 刷新
            saveCookies = { }, // TODO: iOS cookies 保存
            signedGet = { "iOS 暂不支持" }, // TODO: iOS signed GET
        )
    }
}
