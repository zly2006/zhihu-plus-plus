package com.github.zly2006.zhihu.ui.subscreens

import android.content.ClipData
import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import com.github.zly2006.zhihu.data.AccountData
import com.github.zly2006.zhihu.shared.platform.androidUserMessageSink
import com.github.zly2006.zhihu.shared.platform.rememberSettingsStore
import com.github.zly2006.zhihu.util.PowerSaveModeCompat
import com.github.zly2006.zhihu.util.ZhihuCredentialRefresher
import com.github.zly2006.zhihu.util.clipboardManager
import com.github.zly2006.zhihu.util.signFetchRequest

private const val DEVELOPER_MODE_KEY = "developer"

@Composable
actual fun rememberDeveloperSettingsRuntime(): DeveloperSettingsRuntime {
    val context = LocalContext.current
    val dataState by AccountData.asState()
    val settings = rememberSettingsStore()
    return remember(context, dataState, settings) {
        val userMessages = androidUserMessageSink(context)
        DeveloperSettingsRuntime(
            isDeveloperModeEnabled = { settings.getBoolean(DEVELOPER_MODE_KEY, false) },
            setDeveloperModeEnabled = { enabled ->
                settings.putBoolean(DEVELOPER_MODE_KEY, enabled)
            },
            cookies = { dataState.cookies },
            networkStatus = { context.networkStatusText() },
            powerSaveModeText = {
                when (PowerSaveModeCompat.getPowerSaveMode(context)) {
                    PowerSaveModeCompat.POWER_SAVE -> "省电模式：已开启"
                    PowerSaveModeCompat.HUAWEI_POWER_SAVE -> "省电模式：华为傻逼模式已开启"
                    else -> null
                }
            },
            runtimeInfo = { (context as? DeveloperRuntimeInfoProvider)?.developerRuntimeInfo ?: DeveloperRuntimeInfo() },
            verifyLogin = { cookies ->
                AccountData.verifyLogin(context, cookies)
            },
            refreshToken = {
                val httpClient = AccountData.httpClient(context)
                ZhihuCredentialRefresher.refreshZhihuToken(
                    ZhihuCredentialRefresher.fetchRefreshToken(httpClient),
                    httpClient,
                )
            },
            saveCookies = { cookies ->
                AccountData.saveData(
                    context,
                    AccountData.data.copy(
                        cookies = cookies.toMutableMap(),
                        login = true,
                    ),
                )
            },
            signedGetAndCopy = { url ->
                val body = AccountData.fetchGet(context, url) {
                    signFetchRequest()
                }.toString()
                val clip = ClipData.newPlainText("Signed Request Response", body)
                context.clipboardManager.setPrimaryClip(clip)
                body
            },
            showShortMessage = { message ->
                userMessages.showShortMessage(message)
            },
        )
    }
}

private fun Context.networkStatusText(): String {
    val connectivityManager = getSystemService(ConnectivityManager::class.java)
    val activeNetwork = connectivityManager.activeNetwork
    return buildString {
        append("网络状态：")
        if (activeNetwork != null) {
            append("已连接")
            val capabilities = connectivityManager.getNetworkCapabilities(activeNetwork)
            when {
                capabilities?.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) == true -> append(" (移动数据)")
                capabilities?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) == true -> append(" (Wi-Fi)")
                capabilities?.hasTransport(NetworkCapabilities.TRANSPORT_VPN) == true -> append(" (VPN)")
            }
        } else {
            append("未连接")
        }
    }
}
