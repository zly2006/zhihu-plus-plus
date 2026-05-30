package com.github.zly2006.zhihu.ui.subscreens

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import com.github.zly2006.zhihu.data.AccountData
import com.github.zly2006.zhihu.util.PowerSaveModeCompat
import com.github.zly2006.zhihu.util.ZhihuCredentialRefresher
import com.github.zly2006.zhihu.util.signFetchRequest

@Composable
actual fun rememberDeveloperSettingsRuntime(): DeveloperSettingsRuntime {
    val context = LocalContext.current
    val dataState by AccountData.asState()
    return remember(context, dataState) {
        DeveloperSettingsRuntime(
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
            signedGet = { url ->
                AccountData
                    .fetchGet(context, url) {
                        signFetchRequest()
                    }.toString()
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
