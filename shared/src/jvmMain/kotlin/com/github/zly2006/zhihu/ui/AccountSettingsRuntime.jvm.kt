package com.github.zly2006.zhihu.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import com.github.zly2006.zhihu.navigation.TopLevelDestination
import com.github.zly2006.zhihu.shared.data.fetchVerifiedZhihuSession
import com.github.zly2006.zhihu.shared.desktop.DesktopAccountStore
import com.github.zly2006.zhihu.shared.desktop.DesktopLoginRequests
import com.github.zly2006.zhihu.shared.desktop.copyDesktopPlainText
import com.github.zly2006.zhihu.shared.desktop.openDesktopExternalUrl
import com.github.zly2006.zhihu.shared.platform.rememberSettingsStore
import com.github.zly2006.zhihu.shared.platform.rememberUserMessageSink
import com.github.zly2006.zhihu.ui.subscreens.SystemUpdateState
import kotlinx.coroutines.flow.MutableStateFlow

@Composable
actual fun rememberAccountSettingsPlatformRuntime(): AccountSettingsRuntime {
    val store = remember { DesktopAccountStore() }
    val accountState = remember {
        mutableStateOf(store.load().toAccountSettingsAccountState())
    }
    val settings = rememberSettingsStore()
    val userMessages = rememberUserMessageSink()
    return AccountSettingsRuntime(
        accountState = accountState,
        settings = settings,
        userMessages = userMessages,
        refreshProfile = {
            val account = store.load()
            val refreshed = store.createHttpClient(account.cookies).use { client ->
                fetchVerifiedZhihuSession(client, account.cookies, account.userAgent)
            }
            if (refreshed != null) {
                store.save(refreshed)
                accountState.value = refreshed.toAccountSettingsAccountState()
            } else {
                accountState.value = account.toAccountSettingsAccountState()
            }
        },
        requestLogin = {
            DesktopLoginRequests.requestLogin()
            accountState.value = store.load().toAccountSettingsAccountState()
        },
        requestQrLoginScan = {
            DesktopLoginRequests.requestLogin()
            accountState.value = store.load().toAccountSettingsAccountState()
        },
        logout = {
            store.clear()
            accountState.value = AccountSettingsAccountState()
        },
        appVersionInfo = { "desktop" },
        copyText = { _, text ->
            runCatching {
                copyDesktopPlainText(text)
            }
        },
        openExternalUrl = { url ->
            runCatching {
                openDesktopExternalUrl(url)
            }
        },
        selectMainTab = { _: TopLevelDestination -> },
        updateState = MutableStateFlow(SystemUpdateState.NoUpdate),
    )
}

private fun com.github.zly2006.zhihu.shared.account.ZhihuAccountSession.toAccountSettingsAccountState(): AccountSettingsAccountState =
    AccountSettingsAccountState(
        login = login,
        username = username,
        avatarUrl = profile?.avatarUrl,
        id = profile?.id ?: "",
        urlToken = profile?.urlToken,
    )
