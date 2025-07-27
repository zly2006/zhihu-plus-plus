package com.github.zly2006.zhihu.ui

import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.util.Log
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.content.edit
import androidx.core.net.toUri
import coil3.compose.AsyncImage
import com.github.zly2006.zhihu.*
import com.github.zly2006.zhihu.data.AccountData
import com.github.zly2006.zhihu.data.Person
import com.github.zly2006.zhihu.data.RecommendationMode
import com.github.zly2006.zhihu.ui.components.SwitchSettingItem
import com.github.zly2006.zhihu.ui.components.QRCodeLogin
import com.github.zly2006.zhihu.updater.UpdateManager
import com.github.zly2006.zhihu.updater.UpdateManager.UpdateState
import com.github.zly2006.zhihu.viewmodel.filter.ContentFilterManager
import com.github.zly2006.zhihu.viewmodel.filter.FilterStats
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpHeaders
import io.ktor.http.Url
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.serialization.json.JsonObject
import org.jsoup.Jsoup

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class, DelicateCoroutinesApi::class)
@Composable
fun AccountSettingScreen(
    innerPadding: PaddingValues,
    onNavigate: (NavDestination) -> Unit,
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    // 手动设置Cookie弹窗状态
    var showCookieDialog by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        val data = AccountData.data
        if (data.login) {
            try {
                val httpClient = (context as MainActivity).httpClient
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
            preferences.edit {
                putBoolean("developer", isDeveloper)
            }
        }
        Text(
            "版本号：${BuildConfig.VERSION_NAME} ${BuildConfig.BUILD_TYPE}, ${BuildConfig.GIT_HASH}",
            modifier = Modifier.Companion.combinedClickable(
                onLongClick = {
                    // Copy version number
                    val versionInfo = "${BuildConfig.VERSION_NAME} ${BuildConfig.BUILD_TYPE}, ${BuildConfig.GIT_HASH}"

                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                    val clip = android.content.ClipData.newPlainText("version", versionInfo)
                    clipboard.setPrimaryClip(clip)
                    Toast.makeText(context, "已复制版本号", Toast.LENGTH_SHORT).show()
                },
                onClick = {
                    clickTimes++
                    if (clickTimes == 5) {
                        clickTimes = 0
                        isDeveloper = true
                        Toast.makeText(context, "You are now a developer", Toast.LENGTH_SHORT).show()
                    }
                }
            )
        )
        val data by AccountData.asState()
        if (data.login) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                // 这里可以添加头像组件，暂时用 Box 代替
                AsyncImage(
                    model = data.self?.avatarUrl,
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

            // 扫码登录功能 - 适用于已登录用户协助其他设备登录
            QRCodeLogin(
                modifier = Modifier.fillMaxWidth(),
                onScanResult = { qrContent ->
                    val url = Url(qrContent)
                    if (url.rawSegments.dropLast(1).lastOrNull() != "login") {
                        Toast.makeText(context, "二维码内容不正确", Toast.LENGTH_SHORT).show()
                        return@QRCodeLogin
                    }
                    Toast.makeText(context, "扫描成功，正在处理登录请求...", Toast.LENGTH_SHORT).show()
                    Intent(context, WebviewActivity::class.java).let {
                        it.setData(qrContent.toUri())
                        context.startActivity(it)
                    }
                }
            )

            Button(
                onClick = { onNavigate(Collections(AccountData.data.self!!.urlToken!!)) },
                contentPadding = PaddingValues(horizontal = 8.dp),
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                )
            ) {
                Text("查看收藏夹")
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
        // 为未登录用户也提供手动设置Cookie选项
        Button(
            onClick = { showCookieDialog = true },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.secondary,
                contentColor = MaterialTheme.colorScheme.onSecondary
            )
        ) {
            Text("手动设置Cookie")
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

            // TTS引擎信息显示
            val mainActivity = context as? MainActivity
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        "语音朗读引擎信息",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            "当前引擎",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                        Text(
                            when (mainActivity?.ttsEngine) {
                                MainActivity.TtsEngine.Pico -> "Pico TTS"
                                MainActivity.TtsEngine.Google -> "Google TTS"
                                else -> "未初始化"
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            "引擎状态",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                        Text(
                            if (mainActivity?.isSpeaking() == true) "正在朗读"
                            else if (mainActivity?.ttsEngine != MainActivity.TtsEngine.Uninitialized) "就绪"
                            else "未就绪",
                            style = MaterialTheme.typography.bodyMedium,
                            color = when {
                                mainActivity?.isSpeaking() == true -> MaterialTheme.colorScheme.tertiary
                                mainActivity?.ttsEngine != MainActivity.TtsEngine.Uninitialized -> MaterialTheme.colorScheme.primary
                                else -> MaterialTheme.colorScheme.error
                            }
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            "引擎列表",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                        Text(
                            (context as? MainActivity)?.textToSpeech?.engines?.joinToString { it.name } ?: "",
                            style = MaterialTheme.typography.bodyMedium,
                            color = when {
                                mainActivity?.isSpeaking() == true -> MaterialTheme.colorScheme.tertiary
                                mainActivity?.ttsEngine != MainActivity.TtsEngine.Uninitialized -> MaterialTheme.colorScheme.primary
                                else -> MaterialTheme.colorScheme.error
                            }
                        )
                    }
                }
            }

            var allowTelemetry by remember { mutableStateOf(preferences.getBoolean("allowTelemetry", true)) }
            SwitchSettingItem(
                title = "允许发送遥测统计数据",
                description = "允许发送遥测数据给开发者，数据仅供统计使用",
                checked = allowTelemetry,
                onCheckedChange = { 
                    allowTelemetry = it
                    preferences.edit { putBoolean("allowTelemetry", it) }
                }
            )
            
            val useWebview = remember { mutableStateOf(preferences.getBoolean("commentsUseWebview", true)) }
            SwitchSettingItem(
                title = "使用 WebView 显示评论",
                description = "使用 WebView 显示评论，可以显示图片和富文本链接",
                checked = useWebview.value,
                onCheckedChange = {
                    useWebview.value = it
                    preferences.edit { putBoolean("commentsUseWebview", it) }
                }
            )
            
            val pinWebview = remember { mutableStateOf(preferences.getBoolean("commentsPinWebview", false)) }
            SwitchSettingItem(
                title = "评论区 WebView 对象常驻",
                description = "直到关闭评论界面，否则 WebView 对象不会被销毁，这可以使滚动动画更流畅，但会占用更多内存，若您的设备性能较差，建议关闭",
                checked = pinWebview.value,
                onCheckedChange = {
                    pinWebview.value = it
                    preferences.edit { putBoolean("commentsPinWebview", it) }
                }
            )
            
            val useHardwareAcceleration = remember { mutableStateOf(preferences.getBoolean("webviewHardwareAcceleration", true)) }
            SwitchSettingItem(
                title = "WebView 硬件加速",
                description = "启用 WebView 的硬件加速功能，可以提高渲染性能，但可能在某些设备上导致渲染问题",
                checked = useHardwareAcceleration.value,
                onCheckedChange = {
                    useHardwareAcceleration.value = it
                    preferences.edit { putBoolean("webviewHardwareAcceleration", it) }
                }
            )

            val isTitleAutoHide = remember { mutableStateOf(preferences.getBoolean("titleAutoHide", false)) }
            SwitchSettingItem(
                title = "标题栏自动隐藏",
                description = "自动隐藏标题栏，随着滚动自动隐藏，当再滚动到顶部时自动显示",
                checked = isTitleAutoHide.value,
                onCheckedChange = {
                    isTitleAutoHide.value = it
                    preferences.edit { putBoolean("titleAutoHide", it) }
                }
            )

            val buttonSkipAnswer = remember { mutableStateOf(preferences.getBoolean("buttonSkipAnswer", true)) }
            SwitchSettingItem(
                title = "显示跳转下一个回答按钮",
                description = "在回答页面显示可拖拽的跳转按钮，快速跳转到下一个回答",
                checked = buttonSkipAnswer.value,
                onCheckedChange = {
                    buttonSkipAnswer.value = it
                    preferences.edit { putBoolean("buttonSkipAnswer", it) }
                }
            )

            Text(
                "内容过滤设置",
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.padding(top = 16.dp)
            )

            val enableContentFilter = remember { mutableStateOf(preferences.getBoolean("enableContentFilter", true)) }
            SwitchSettingItem(
                title = "启用智能内容过滤",
                description = "自动过滤首页展示超过2次但用户未点击的内容，减少重复推荐",
                checked = enableContentFilter.value,
                onCheckedChange = {
                    enableContentFilter.value = it
                    preferences.edit { putBoolean("enableContentFilter", it) }
                }
            )

            val filterFollowedUserContent = remember { mutableStateOf(preferences.getBoolean("filterFollowedUserContent", false)) }
            SwitchSettingItem(
                title = "过滤已关注用户内容",
                description = "是否对已关注用户的内容也应用过滤规则。关闭此选项可确保关注用户的内容始终显示",
                checked = filterFollowedUserContent.value,
                onCheckedChange = {
                    filterFollowedUserContent.value = it
                    preferences.edit { putBoolean("filterFollowedUserContent", it) }
                },
                enabled = enableContentFilter.value // 只有启用内容过滤时才能设置此选项
            )

            // 显示过滤统计信息
            var filterStats by remember { mutableStateOf<FilterStats?>(null) }
            LaunchedEffect(Unit) {
                try {
                    val contentFilterManager = ContentFilterManager.getInstance(context)
                    filterStats = contentFilterManager.getFilterStats()
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }

            AnimatedVisibility(visible = enableContentFilter.value && filterStats != null) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Text(
                            "过滤统计",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        filterStats?.let { stats ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column {
                                    Text(
                                        "总记录数",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        "${stats.totalRecords}",
                                        style = MaterialTheme.typography.bodyLarge
                                    )
                                }
                                Column {
                                    Text(
                                        "已过滤内容",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        "${stats.filteredCount}",
                                        style = MaterialTheme.typography.bodyLarge
                                    )
                                }
                                Column {
                                    Text(
                                        "过滤率",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        "${String.format("%.1f", stats.filterRate * 100)}%",
                                        style = MaterialTheme.typography.bodyLarge
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = {
                                    coroutineScope.launch {
                                        try {
                                            val contentFilterManager = ContentFilterManager.getInstance(context)
                                            contentFilterManager.cleanupOldData()
                                            filterStats = contentFilterManager.getFilterStats()
                                            Toast.makeText(context, "已清理过期数据", Toast.LENGTH_SHORT).show()
                                        } catch (e: Exception) {
                                            Toast.makeText(context, "清理失败: ${e.message}", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("清理过期数据")
                            }

                            Button(
                                onClick = {
                                    coroutineScope.launch {
                                        try {
                                            val contentFilterManager = ContentFilterManager.getInstance(context)
                                            contentFilterManager.clearAllData()
                                            filterStats = contentFilterManager.getFilterStats()
                                            Toast.makeText(context, "已重置所有数据", Toast.LENGTH_SHORT).show()
                                        } catch (e: Exception) {
                                            Toast.makeText(context, "重置失败: ${e.message}", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.error,
                                    contentColor = MaterialTheme.colorScheme.onError
                                )
                            ) {
                                Text("重置所有数据")
                            }
                        }
                    }
                }
            }

            Text(
                "推荐算法设置",
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.padding(top = 16.dp)
            )

            val currentRecommendationMode = remember {
                mutableStateOf(
                    RecommendationMode.entries.find {
                        it.key == preferences.getString("recommendationMode", RecommendationMode.WEB.key)
                    } ?: RecommendationMode.WEB
                )
            }

            var expanded by remember { mutableStateOf(false) }

            ExposedDropdownMenuBox(
                expanded = expanded,
                onExpandedChange = { expanded = !expanded },
                modifier = Modifier.fillMaxWidth()
            ) {
                OutlinedTextField(
                    value = currentRecommendationMode.value.displayName,
                    onValueChange = { },
                    readOnly = true,
                    label = { Text("推荐算法") },
                    trailingIcon = {
                        ExposedDropdownMenuDefaults.TrailingIcon(
                            expanded = expanded
                        )
                    },
                    modifier = Modifier
                        .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                        .fillMaxWidth()
                )
                ExposedDropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false }
                ) {
                    RecommendationMode.entries.forEach { mode ->
                        DropdownMenuItem(
                            text = {
                                Column {
                                    Text(mode.displayName)
                                    Text(
                                        mode.description,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            },
                            onClick = {
                                currentRecommendationMode.value = mode
                                preferences.edit {
                                    putString("recommendationMode", mode.key)
                                }
                                expanded = false
                                Toast.makeText(
                                    context,
                                    "已切换到${mode.displayName}",
                                    Toast.LENGTH_SHORT
                                ).show()
                            },
                            enabled = mode !in listOf(RecommendationMode.SIMILARITY) // 相似度推荐还未实现
                        )
                    }
                }
            }

            // 推荐内容登录设置
            val isLoginForRecommendation = remember {
                mutableStateOf(preferences.getBoolean("loginForRecommendation", true))
            }

            SwitchSettingItem(
                title = "推荐内容时登录",
                description = "获取推荐内容时是否使用登录状态，关闭后将以游客身份获取推荐",
                checked = isLoginForRecommendation.value,
                onCheckedChange = { checked ->
                    isLoginForRecommendation.value = checked
                    preferences.edit {
                        putBoolean("loginForRecommendation", checked)
                    }
                    Toast.makeText(
                        context,
                        if (checked) "已开启推荐内容时登录" else "已关闭推荐内容时登录",
                        Toast.LENGTH_SHORT
                    ).show()
                },
            )
        }
        // GitHub Token 设置
        Text(
            "GitHub 设置",
            style = MaterialTheme.typography.headlineSmall,
            modifier = Modifier.padding(top = 16.dp)
        )

        var githubToken by remember {
            mutableStateOf(preferences.getString("githubToken", "") ?: "")
        }
        var showGithubToken by remember { mutableStateOf(false) }

        OutlinedTextField(
            value = githubToken,
            onValueChange = {
                githubToken = it
                preferences.edit { putString("githubToken", it) }
            },
            label = { Text("GitHub Personal Access Token") },
            placeholder = { Text("输入你的 GitHub Token (可选)") },
            supportingText = {
                Text(
                    "用于访问 GitHub API 时解除限速，提高更新检查的稳定性。留空则使用匿名访问。",
                    style = MaterialTheme.typography.bodySmall
                )
            },
            visualTransformation = if (showGithubToken) VisualTransformation.None else PasswordVisualTransformation(),
            trailingIcon = {
                IconButton(onClick = { showGithubToken = !showGithubToken }) {
                    Icon(
                        imageVector = if (showGithubToken) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                        contentDescription = if (showGithubToken) "隐藏" else "显示"
                    )
                }
            },
            modifier = Modifier.fillMaxWidth()
        )

        val checkNightlyUpdates = remember { mutableStateOf(preferences.getBoolean("checkNightlyUpdates", false)) }
        SwitchSettingItem(
            title = "检查 Nightly 版本更新",
            description = "检查每日构建版本，包含最新功能但可能不够稳定。关闭则只检查正式发布版本。",
            checked = checkNightlyUpdates.value,
            onCheckedChange = {
                checkNightlyUpdates.value = it
                preferences.edit { putBoolean("checkNightlyUpdates", it) }
            }
        )

        val updateState by UpdateManager.updateState.collectAsState()
        LaunchedEffect(updateState) {
            val updateState = updateState
            if (updateState is UpdateState.UpdateAvailable) {
                val versionType = if (updateState.isNightly) "Nightly版本" else "正式版本"
                Toast.makeText(
                    context,
                    "发现新$versionType ${updateState.version}",
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
                    val intent = Intent(
                        Intent.ACTION_VIEW,
                        "https://github.com/zly2006/zhihu-plus-plus".toUri()
                    )
                    context.startActivity(intent)
                },
                modifier = Modifier.weight(1f)
            ) {
                Text("GitHub 项目地址")
            }
            Spacer(modifier = Modifier.width(16.dp))
            Button(
                onClick = {
                    val intent = Intent(
                        Intent.ACTION_VIEW,
                        "https://github.com/zly2006/zhihu-plus-plus/blob/master/LICENSE.md".toUri()
                    )
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

        // 手动设置Cookie弹窗
        if (showCookieDialog) {
            var cookieInputText by remember { mutableStateOf("") }
            var showCookieText by remember { mutableStateOf(false) }
            AlertDialog(
                onDismissRequest = {
                    showCookieDialog = false
                    cookieInputText = ""
                    showCookieText = false
                },
                title = { Text("手动设置Cookie") },
                text = {
                    Column {
                        Text(
                            "请输入完整的Cookie字符串，格式类似于document.cookie，使用 \"; \" 分割各个cookie项",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(bottom = 16.dp)
                        )
                        OutlinedTextField(
                            value = cookieInputText,
                            onValueChange = { cookieInputText = it },
                            label = { Text("Cookie字符串") },
                            placeholder = { Text("name1=value1; name2=value2; name3=value3") },
                            visualTransformation = if (showCookieText) VisualTransformation.None else PasswordVisualTransformation(),
                            trailingIcon = {
                                IconButton(onClick = { showCookieText = !showCookieText }) {
                                    Icon(
                                        imageVector = if (showCookieText) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                        contentDescription = if (showCookieText) "隐藏" else "显示"
                                    )
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            maxLines = 5
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            if (cookieInputText.isNotBlank()) {
                                try {
                                    // 解析cookie字符串
                                    val cookies = mutableMapOf<String, String>()
                                    cookieInputText.split("; ").forEach { cookieItem ->
                                        val parts = cookieItem.split("=", limit = 2)
                                        if (parts.size == 2) {
                                            cookies[parts[0].trim()] = parts[1].trim()
                                        }
                                    }

                                    if (cookies.isNotEmpty()) {
                                        // 保存cookie数据
                                        val currentData = AccountData.data
                                        AccountData.saveData(
                                            context,
                                            currentData.copy(
                                                cookies = cookies,
                                                login = true
                                            )
                                        )

                                        // 验证登录状态
                                        coroutineScope.launch {
                                            try {
                                                if (AccountData.verifyLogin(context, cookies)) {
                                                    Toast.makeText(context, "Cookie设置成功并验证登录状态", Toast.LENGTH_SHORT).show()
                                                } else {
                                                    Toast.makeText(context, "Cookie设置成功，但验证登录失败，请检查Cookie是否有效", Toast.LENGTH_LONG).show()
                                                }
                                            } catch (e: Exception) {
                                                Toast.makeText(context, "验证登录时发生错误：${e.message}", Toast.LENGTH_LONG).show()
                                            }
                                        }

                                        showCookieDialog = false
                                        cookieInputText = ""
                                        showCookieText = false
                                    } else {
                                        Toast.makeText(context, "未能解析有效的Cookie数据", Toast.LENGTH_SHORT).show()
                                    }
                                } catch (e: Exception) {
                                    Toast.makeText(context, "解析Cookie时发生错误：${e.message}", Toast.LENGTH_LONG).show()
                                }
                            } else {
                                Toast.makeText(context, "请输入Cookie字符串", Toast.LENGTH_SHORT).show()
                            }
                        }
                    ) {
                        Text("确认设置")
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = {
                            showCookieDialog = false
                            cookieInputText = ""
                            showCookieText = false
                        }
                    ) {
                        Text("取消")
                    }
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
