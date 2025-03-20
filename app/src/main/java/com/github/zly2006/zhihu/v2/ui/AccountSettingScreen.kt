@file:Suppress("FunctionName")

package com.github.zly2006.zhihu.v2.ui

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.ScaffoldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.dp
import com.github.zly2006.zhihu.BuildConfig
import com.github.zly2006.zhihu.data.AccountData

@Composable
fun AccountSettingScreen(
    context: Context,
    innerPadding: PaddingValues
) {
    Column(
        modifier = Modifier.Companion.padding(16.dp)
    ) {
        @Composable
        fun displayPadding(padding: PaddingValues) = buildString {
            append("(")
            append(padding.calculateLeftPadding(LocalLayoutDirection.current))
            append(", ")
            append(padding.calculateTopPadding())
            append(", ")
            append(padding.calculateRightPadding(LocalLayoutDirection.current))
            append(", ")
            append(padding.calculateBottomPadding())
            append(", start=")
            append(padding.calculateStartPadding(LocalLayoutDirection.current))
            append(")")
        }
        Text("Account")
        var clickTimes by remember { mutableIntStateOf(0) }
        val preferences = remember {
            context.getSharedPreferences(
                "com.github.zly2006.zhihu_preferences",
                Context.MODE_PRIVATE
            )
        }
        val isDeveloper = preferences.getBoolean("developer", false)
        Text(
            "版本号：${BuildConfig.VERSION_NAME} ${BuildConfig.BUILD_TYPE}, ${BuildConfig.GIT_HASH}",
            modifier = Modifier.Companion.clickable {
                clickTimes++
                if (clickTimes == 5) {
                    clickTimes = 0
                    preferences.edit()
                        .putBoolean("developer", true)
                        .apply()
                    Toast.makeText(context, "You are now a developer", Toast.LENGTH_SHORT).show()
                }
            }
        )
        val data = remember { AccountData.getData(context) }
        Text(
            if (data.login) {
                "已登录，${data.username}"
            } else {
                "未登录"
            }
        )
        val networkStatus = remember {
            buildString {
                val connectivityManager = context.getSystemService(ConnectivityManager::class.java)
                val activeNetwork = connectivityManager.activeNetwork
                append("网络状态：")
                if (activeNetwork != null) {
                    append("已连接")
                    if (connectivityManager.getNetworkCapabilities(activeNetwork)!!
                            .hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)
                    ) {
                        append(" (移动数据)")
                    } else {
                        append(" (Wi-Fi)")
                    }
                } else {
                    append("未连接")
                }
            }
        }
        Text(networkStatus)
        if (isDeveloper) {
            Text("当前padding: ${displayPadding(innerPadding)}")
            Text("statusBars: ${displayPadding(WindowInsets.Companion.statusBars.asPaddingValues())}")
            Text("contentWindowInsets: ${displayPadding(ScaffoldDefaults.contentWindowInsets.asPaddingValues())}")
        }
    }
}
