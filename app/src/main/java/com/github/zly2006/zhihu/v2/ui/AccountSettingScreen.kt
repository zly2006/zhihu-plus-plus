package com.github.zly2006.zhihu.v2.ui

import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.github.zly2006.zhihu.BuildConfig
import com.github.zly2006.zhihu.LoginActivity
import com.github.zly2006.zhihu.data.AccountData
import com.github.zly2006.zhihu.data.Person
import com.github.zly2006.zhihu.signFetchRequest
import com.github.zly2006.zhihu.updater.UpdateManager
import com.github.zly2006.zhihu.updater.UpdateManager.UpdateState
import io.ktor.client.call.*
import io.ktor.client.request.*
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.serialization.json.JsonObject

@Composable
fun AccountSettingScreen(
    context: Context,
    innerPadding: PaddingValues
) {
    val coroutineScope = rememberCoroutineScope()
    LaunchedEffect(Unit) {
        val data = AccountData.data
        if (data.login) {
            val httpClient = AccountData.httpClient(context)
            val response = httpClient.get("https://www.zhihu.com/api/v4/me") {
                signFetchRequest(context)
            }.body<JsonObject>()
            val self = AccountData.decodeJson<Person>(response)
            AccountData.saveData(
                context,
                data.copy(self = self)
            )
        }
    }
    Column(
        modifier = Modifier
            .padding(16.dp)
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
    ) {
        @Composable
        fun DisplayPadding(padding: PaddingValues) = buildString {
            append("(")
            append(padding.calculateTopPadding())
            append(", ")
            append(padding.calculateRightPadding(LocalLayoutDirection.current))
            append(", ")
            append(padding.calculateBottomPadding())
            append(", ")
            append(padding.calculateLeftPadding(LocalLayoutDirection.current))
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
        val data by AccountData.asState()
        if (data.login) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                // 这里可以添加头像组件，暂时用 Box 代替
                AsyncImage(
                    model = data.self?.avatar_url,
                    contentDescription = "头像",
                    modifier = Modifier
                        .size(120.dp)
                        .background(MaterialTheme.colorScheme.primary, CircleShape)
                        .clip(CircleShape),
                )
                Spacer(modifier = Modifier.width(16.dp))
                Text(data.username, style = MaterialTheme.typography.headlineLarge)
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
                    } else if (connectivityManager.getNetworkCapabilities(activeNetwork)!!
                            .hasTransport(NetworkCapabilities.TRANSPORT_WIFI)) {
                        append(" (Wi-Fi)")
                    } else if (connectivityManager.getNetworkCapabilities(activeNetwork)!!
                            .hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)) {
                        append(" (以太网)")
                    } else if (connectivityManager.getNetworkCapabilities(activeNetwork)!!
                            .hasTransport(NetworkCapabilities.TRANSPORT_BLUETOOTH)) {
                        append(" (蓝牙)")
                    } else if (connectivityManager.getNetworkCapabilities(activeNetwork)!!
                            .hasTransport(NetworkCapabilities.TRANSPORT_VPN)) {
                        append(" (VPN)")
                    }
                } else {
                    append("未连接")
                }
            }
        }
        Text(networkStatus)
        if (isDeveloper) {
            Button(
                onClick = {
                    AccountData.saveData(
                        context,
                        data.copy(username = data.username + "-test")
                    )
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("刷新数据测试")
            }
            Button(
                onClick = {
                    coroutineScope.launch {
                        if (AccountData.verifyLogin(
                                context,
                                data.cookies
                            )
                        ) {
                            Toast.makeText(context, "登录成功", Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(context, "登录失败", Toast.LENGTH_SHORT).show()
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("重新fetch数据测试")
            }
            Text("当前padding: ${DisplayPadding(innerPadding)}")
            Text("statusBars: ${DisplayPadding(WindowInsets.statusBars.asPaddingValues())}")
            Text("contentWindowInsets: ${DisplayPadding(ScaffoldDefaults.contentWindowInsets.asPaddingValues())}")
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
                )
                Spacer(modifier = Modifier.width(16.dp))
                Text(data.username)
            }
            var allowTelemetry by remember { mutableStateOf(preferences.getBoolean("allowTelemetry", true)) }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column {
                    Text(
                        "允许发送遥测统计数据",
                        style = MaterialTheme.typography.bodyLarge,
                    )
                    Text(
                        "允许发送遥测数据到开发者，数据仅供统计使用",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                        ),
                    )
                }
                Switch(
                    checked = allowTelemetry,
                    onCheckedChange = { 
                        allowTelemetry = it
                        preferences.edit().putBoolean("allowTelemetry", it).apply()
                    }
                )
            }
        }
        val updateState by UpdateManager.updateState.collectAsState()
        LaunchedEffect(updateState) {
            val updateState = updateState
            if (updateState is UpdateState.UpdateAvailable) {
                Toast.makeText(
                    context,
                    "发现新版本 ${updateState.version}",
                    Toast.LENGTH_SHORT
                ).show()
            }
            if (updateState is UpdateState.Error) {
                Toast.makeText(
                    context,
                    "检查更新失败: ${updateState.message}",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
        Button(
            onClick = {
                GlobalScope.launch {
                    when (val updateState = updateState) {
                        is UpdateState.NoUpdate, is UpdateState.Error -> {
                            UpdateManager.checkForUpdate(context)
                        }
                        is UpdateState.UpdateAvailable -> {
                            UpdateManager.downloadUpdate(context)
                        }
                        is UpdateState.Downloaded -> {
                            UpdateManager.installUpdate(
                                context,
                                updateState.file
                            )
                        }
                        UpdateState.Checking, UpdateState.Downloading, UpdateState.Latest -> { /* NOOP */ }
                    }
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                when (val updateState = updateState) {
                    is UpdateState.NoUpdate -> "检查更新"
                    is UpdateState.Checking -> "检查中..."
                    is UpdateState.Latest -> "已经是最新版本"
                    is UpdateState.UpdateAvailable -> "下载更新 ${updateState.version}"
                    is UpdateState.Downloading -> "下载中..."
                    is UpdateState.Downloaded -> "安装更新"
                    is UpdateState.Error -> "检查更新失败，点击重试"
                }
            )
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
