package com.github.zly2006.zhihu.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import com.github.zly2006.zhihu.shared.platform.rememberUserMessageSink

@Composable
actual fun rememberAccountSettingsPlatformRuntime(): AccountSettingsRuntime {
    val userMessages = rememberUserMessageSink()
    return remember(userMessages) {
        AccountSettingsRuntime(
            accountState = mutableStateOf(AccountSettingsAccountState()),
            // TODO: iOS 刷新用户信息
            refreshProfile = { },
            // TODO: iOS 登录
            requestLogin = { userMessages.showMessage("iOS 登录暂未实现") },
            // TODO: iOS 扫码登录
            requestQrLoginScan = { userMessages.showMessage("iOS 扫码登录暂未实现") },
            // TODO: iOS 登出
            logout = { },
            appVersionInfo = { "iOS" },
            // TODO: iOS 主 Tab 切换
            selectMainTab = { },
        )
    }
}
