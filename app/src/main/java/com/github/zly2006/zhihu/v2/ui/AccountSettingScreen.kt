@file:Suppress("FunctionName")

package com.github.zly2006.zhihu.v2.ui

import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ScaffoldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.github.zly2006.zhihu.BuildConfig
import com.github.zly2006.zhihu.LegacyMainActivity
import com.github.zly2006.zhihu.LoginActivity
import com.github.zly2006.zhihu.data.AccountData

@Composable
fun AccountSettingScreen(
    context: Context,
    innerPadding: PaddingValues
) {
    Column(
        modifier = Modifier
            .padding(16.dp)
            .fillMaxWidth()
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
        if (data.login) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                // 这里可以添加头像组件，暂时用 Box 代替
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .background(MaterialTheme.colorScheme.primary, CircleShape)
                )
                Spacer(modifier = Modifier.width(16.dp))
                Text(data.username)
            }
            Button(
                onClick = {
                    AccountData.delete(context)
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("退出登录")
            }
        } else {
            Button(
                onClick = {
                    context.startActivity(Intent(context, LoginActivity::class.java))
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("登录")
            }
        }

        Button(
            onClick = {
                context.startActivity(Intent(context, LegacyMainActivity::class.java))
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("回到旧版UI")
        }
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
            Text("statusBars: ${displayPadding(WindowInsets.statusBars.asPaddingValues())}")
            Text("contentWindowInsets: ${displayPadding(ScaffoldDefaults.contentWindowInsets.asPaddingValues())}")
        }
        Column {
            Text(
                "账号信息设置",
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.padding(top = 16.dp)

            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    "用户名",
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.border(
                        0.5.dp,
                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                        shape = RectangleShape
                    )
                )
                Spacer(modifier = Modifier.width(16.dp))
                Text(data.username)
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun AccountSettingScreenPreview() {
    AccountSettingScreen(
        context = LocalContext.current,
        innerPadding = PaddingValues(16.dp)
    )
}
