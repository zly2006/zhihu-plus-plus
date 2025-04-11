package com.github.zly2006.zhihu.v2.ui

import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
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
import com.github.zly2006.zhihu.*
import com.github.zly2006.zhihu.data.AccountData
import com.github.zly2006.zhihu.data.Person
import com.github.zly2006.zhihu.updater.UpdateManager
import com.github.zly2006.zhihu.updater.UpdateManager.UpdateState
import com.github.zly2006.zhihu.v2.ui.components.SwitchSettingItem
import io.ktor.client.call.*
import io.ktor.client.request.*
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.serialization.json.JsonObject

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun AccountSettingScreen(
    innerPadding: PaddingValues,
    onNavigate: (NavDestination) -> Unit,
) {
    val context = LocalContext.current as MainActivity
    val coroutineScope = rememberCoroutineScope()
    LaunchedEffect(Unit) {
        val data = AccountData.data
        if (data.login) {
            val httpClient = context.httpClient
            try {
                val response = httpClient.get("https://www.zhihu.com/api/v4/me") {
                    signFetchRequest(context)
                }.body<JsonObject>()
                val self = AccountData.decodeJson<Person>(response)
                AccountData.saveData(
                    context,
                    data.copy(self = self)
                )
            } catch (e: Exception) {
                e.printStackTrace()
                Toast.makeText(context, "获取用户信息失败", Toast.LENGTH_SHORT).show()
            }
        }
    }
    Column(
        modifier = Modifier
            .padding(horizontal = 16.dp)
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
                PREFERENCE_NAME,
                Context.MODE_PRIVATE
            )
        }
        var isDeveloper by remember { mutableStateOf(preferences.getBoolean("developer", false)) }
        LaunchedEffect(isDeveloper) {
            preferences.edit()
                .putBoolean("developer", isDeveloper)
                .apply()
        }
        Text(
            "版本号：${BuildConfig.VERSION_NAME} ${BuildConfig.BUILD_TYPE}, ${BuildConfig.GIT_HASH}",
            modifier = Modifier.Companion.clickable {
                clickTimes++
                if (clickTimes == 5) {
                    clickTimes = 0
                    isDeveloper = true
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
        AnimatedVisibility(
            visible = isDeveloper
        ) {
            FlowRow {
                Button(
                    onClick = {
                        AccountData.saveData(
                            context,
                            data.copy(username = data.username + "-test")
                        )
                    },
//                    modifier = Modifier.weight(1f)
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
//                    modifier = Modifier.weight(1f)
                ) {
                    Text("重新fetch数据测试")
                }
                Button(
                    onClick = {
                        coroutineScope.launch {
                            throw Exception("测试异常")
                        }
                    },
//                    modifier = Modifier.weight(1f)
                ) {
                    Text("抛出异常测试")
                }
                Text("当前padding: ${DisplayPadding(innerPadding)}")
                Text("statusBars: ${DisplayPadding(WindowInsets.statusBars.asPaddingValues())}")
                Text("contentWindowInsets: ${DisplayPadding(ScaffoldDefaults.contentWindowInsets.asPaddingValues())}")
            }
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
            Button(
                onClick = { onNavigate(CollectionScreen) },
                contentPadding = PaddingValues(horizontal = 8.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                )
            ) {
                Text("查看收藏夹")
            }
            var allowTelemetry by remember { mutableStateOf(preferences.getBoolean("allowTelemetry", true)) }
            SwitchSettingItem(
                title = "允许发送遥测统计数据",
                description = "允许发送遥测数据到开发者，数据仅供统计使用",
                checked = allowTelemetry,
                onCheckedChange = { 
                    allowTelemetry = it
                    preferences.edit().putBoolean("allowTelemetry", it).apply()
                }
            )
            
            val useWebview = remember { mutableStateOf(preferences.getBoolean("commentsUseWebview", true)) }
            SwitchSettingItem(
                title = "使用 WebView 显示评论",
                description = "使用 WebView 显示评论，可以显示图片和富文本链接",
                checked = useWebview.value,
                onCheckedChange = {
                    useWebview.value = it
                    preferences.edit().putBoolean("commentsUseWebview", it).apply()
                }
            )
            
            val pinWebview = remember { mutableStateOf(preferences.getBoolean("commentsPinWebview", false)) }
            SwitchSettingItem(
                title = "评论区 WebView 对象常驻",
                description = "直到关闭评论界面，否则 WebView 对象不会被销毁，这可以使滚动动画更流畅，但会占用更多内存，若您的设备性能较差，建议关闭",
                checked = pinWebview.value,
                onCheckedChange = {
                    pinWebview.value = it
                    preferences.edit().putBoolean("commentsPinWebview", it).apply()
                }
            )
            
            val useHardwareAcceleration = remember { mutableStateOf(preferences.getBoolean("webviewHardwareAcceleration", true)) }
            SwitchSettingItem(
                title = "WebView 硬件加速",
                description = "启用 WebView 的硬件加速功能，可以提高渲染性能，但可能在某些设备上导致渲染问题",
                checked = useHardwareAcceleration.value,
                onCheckedChange = {
                    useHardwareAcceleration.value = it
                    preferences.edit().putBoolean("webviewHardwareAcceleration", it).apply()
                }
            )

            val isTitleAutoHide = remember { mutableStateOf(preferences.getBoolean("titleAutoHide", false)) }
            SwitchSettingItem(
                title = "标题栏自动隐藏",
                description = "自动隐藏标题栏，随着滚动自动隐藏，当再滚动到顶部时自动显示",
                checked = isTitleAutoHide.value,
                onCheckedChange = {
                    isTitleAutoHide.value = it
                    preferences.edit().putBoolean("titleAutoHide", it).apply()
                }
            )
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
        Row {
            Text(
                "关于",
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.padding(top = 16.dp)
            )
        }
        Row {
            Button(
                onClick = {
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/zly2006/zhihu-plus-plus"))
                    context.startActivity(intent)
                },
                modifier = Modifier.weight(1f)
            ) {
                Text("GitHub 项目地址")
            }
            Spacer(modifier = Modifier.width(16.dp))
            Button(
                onClick = {
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/zly2006/zhihu-plus-plus/blob/master/LICENSE.md"))
                    context.startActivity(intent)
                }
            ) {
                Text("开源协议")
            }
        }
        Text(
            "本软件仅供学习交流使用，应用内内容由知乎网站提供，著作权归其对应作者所有。",
            style = MaterialTheme.typography.bodySmall.copy(
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
            ),
        )
        AnimatedVisibility(
            visible = isDeveloper
        ) {
            SwitchSettingItem(
                title = "开发者模式",
                checked = isDeveloper,
                onCheckedChange = {
                    isDeveloper = it
                }
            )
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
        innerPadding = PaddingValues(16.dp),
        onNavigate = { }
    )
}
