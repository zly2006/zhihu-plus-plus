package com.github.zly2006.zhihu.ui.subscreens

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.github.zly2006.zhihu.shared.desktop.DesktopAccountStore
import com.github.zly2006.zhihu.shared.desktop.signedWithResponse
import com.github.zly2006.zhihu.shared.platform.rememberSettingsStore
import com.github.zly2006.zhihu.util.ZhihuCredentialRefresher
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpMethod

private const val DEVELOPER_MODE_KEY = "developer"

@Composable
actual fun rememberDeveloperSettingsRuntime(): DeveloperSettingsRuntime {
    val settings = rememberSettingsStore()
    val store = remember { DesktopAccountStore() }
    return remember(settings, store) {
        DeveloperSettingsRuntime(
            isDeveloperModeEnabled = { settings.getBoolean(DEVELOPER_MODE_KEY, false) },
            setDeveloperModeEnabled = { enabled -> settings.putBoolean(DEVELOPER_MODE_KEY, enabled) },
            cookies = { store.load().cookies },
            networkStatus = { "网络状态：桌面端使用系统网络" },
            powerSaveModeText = { null },
            runtimeInfo = { DeveloperRuntimeInfo() },
            verifyLogin = { cookies ->
                store.verifyAndSave(cookies.toMutableMap())
            },
            refreshToken = {
                val account = store.load()
                store.createHttpClient(account.cookies).use { client ->
                    ZhihuCredentialRefresher.refreshZhihuToken(
                        ZhihuCredentialRefresher.fetchRefreshToken(client),
                        client,
                    )
                }
            },
            saveCookies = { cookies ->
                val current = store.load()
                store.save(
                    current.copy(
                        login = true,
                        cookies = cookies.toMutableMap(),
                    ),
                )
            },
            signedGet = { url ->
                store.signedWithResponse(
                    url = url,
                    block = { method = HttpMethod.Get },
                ) { response ->
                    response.bodyAsText()
                }
            },
        )
    }
}
