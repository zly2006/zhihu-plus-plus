package com.github.zly2006.zhihu.ui.subscreens

import android.content.ClipData
import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.core.content.edit
import com.github.zly2006.zhihu.Account
import com.github.zly2006.zhihu.LocalNavigator
import com.github.zly2006.zhihu.MainActivity
import com.github.zly2006.zhihu.SentenceSimilarityTest
import com.github.zly2006.zhihu.data.AccountData
import com.github.zly2006.zhihu.ui.PREFERENCE_NAME
import com.github.zly2006.zhihu.ui.components.SwitchSettingItem
import com.github.zly2006.zhihu.util.PowerSaveModeCompat
import com.github.zly2006.zhihu.util.ZhihuCredentialRefresher
import com.github.zly2006.zhihu.util.clipboardManager
import com.github.zly2006.zhihu.util.signFetchRequest
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun DeveloperSettingsScreen(
    onNavigateBack: () -> Unit,
) {
    val navigator = LocalNavigator.current
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val preferences = remember {
        context.getSharedPreferences(
            PREFERENCE_NAME,
            Context.MODE_PRIVATE,
        )
    }
    val dataState by AccountData.asState()
    val data = dataState

    var showCookieDialog by remember { mutableStateOf(false) }
    var showSignedRequestDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("开发者选项") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
                windowInsets = WindowInsets(0),
            )
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
        ) {
            SwitchSettingItem(
                title = "开发者模式",
                checked = preferences.getBoolean("developer", false),
                onCheckedChange = {
                    preferences.edit {
                        putBoolean("developer", it)
                    }
                    if (!it) {
                        onNavigateBack()
                    }
                },
            )
            SelectionContainer {
                Column {
                    val networkStatus = remember {
                        buildString {
                            val connectivityManager = context.getSystemService(ConnectivityManager::class.java)
                            val activeNetwork = connectivityManager.activeNetwork
                            append("网络状态：")
                            if (activeNetwork != null) {
                                append("已连接")
                                if (connectivityManager.getNetworkCapabilities(activeNetwork)!!.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)) {
                                    append(" (移动数据)")
                                } else if (connectivityManager.getNetworkCapabilities(activeNetwork)!!.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)) {
                                    append(" (Wi-Fi)")
                                } else if (connectivityManager.getNetworkCapabilities(activeNetwork)!!.hasTransport(NetworkCapabilities.TRANSPORT_VPN)) {
                                    append(" (VPN)")
                                }
                            } else {
                                append("未连接")
                            }
                        }
                    }
                    Text(networkStatus)

                    when (PowerSaveModeCompat.getPowerSaveMode(context)) {
                        PowerSaveModeCompat.POWER_SAVE -> Text("省电模式：已开启")
                        PowerSaveModeCompat.HUAWEI_POWER_SAVE -> Text("省电模式：华为傻逼模式已开启")
                        else -> {}
                    }

                    Spacer(Modifier.height(16.dp))
                }
            }
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = {
                    coroutineScope.launch {
                        if (AccountData.verifyLogin(context, data.cookies)) {
                            Toast.makeText(context, "登录成功", Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(context, "登录失败", Toast.LENGTH_SHORT).show()
                        }
                    }
                }) { Text("验证登录") }

                Button(onClick = {
                    coroutineScope.launch {
                        val httpClient = AccountData.httpClient(context)
                        ZhihuCredentialRefresher.refreshZhihuToken(ZhihuCredentialRefresher.fetchRefreshToken(httpClient), httpClient)
                        Toast.makeText(context, "刷新成功", Toast.LENGTH_SHORT).show()
                    }
                }) { Text("刷新Token") }

                Button(onClick = { showCookieDialog = true }) { Text("手动设置Cookie") }

                Button(onClick = { showSignedRequestDialog = true }) { Text("签名请求") }

                Button(onClick = {
                    navigator.onNavigate(SentenceSimilarityTest)
                }) { Text("句子相似度") }

                Button(onClick = {
                    navigator.onNavigate(Account.DeveloperSettings.ColorScheme)
                }) { Text("Color Scheme") }
            }

            // TTS引擎信息显示
            val mainActivity = context as? MainActivity
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainer,
                ),
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                ) {
                    Text(
                        "语音朗读引擎信息",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text(
                            "当前引擎",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        Text(
                            when (mainActivity?.ttsEngine) {
                                MainActivity.TtsEngine.Pico -> "Pico TTS"
                                MainActivity.TtsEngine.Google -> "Google TTS"
                                MainActivity.TtsEngine.Sherpa -> "Sherpa TTS"
                                else -> "未初始化"
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text(
                            "引擎状态",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        Text(
                            if (mainActivity?.isSpeaking() == true) {
                                "正在朗读"
                            } else if (mainActivity?.ttsEngine != MainActivity.TtsEngine.Uninitialized) {
                                "就绪"
                            } else {
                                "未就绪"
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            color = when {
                                mainActivity?.isSpeaking() == true -> MaterialTheme.colorScheme.tertiary
                                mainActivity?.ttsEngine != MainActivity.TtsEngine.Uninitialized -> MaterialTheme.colorScheme.primary
                                else -> MaterialTheme.colorScheme.error
                            },
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text(
                            "引擎列表",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        Text(
                            (context as? MainActivity)?.textToSpeech?.engines?.joinToString { it.name } ?: "",
                            style = MaterialTheme.typography.bodyMedium,
                            color = when {
                                mainActivity?.isSpeaking() == true -> MaterialTheme.colorScheme.tertiary
                                mainActivity?.ttsEngine != MainActivity.TtsEngine.Uninitialized -> MaterialTheme.colorScheme.primary
                                else -> MaterialTheme.colorScheme.error
                            },
                        )
                    }
                }
            }
        }
    }

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
                        modifier = Modifier.padding(bottom = 16.dp),
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
                                    contentDescription = if (showCookieText) "隐藏" else "显示",
                                )
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        maxLines = 5,
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
                                            login = true,
                                        ),
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
                    },
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
                    },
                ) {
                    Text("取消")
                }
            },
        )
    }

    if (showSignedRequestDialog) {
        var urlInput by remember { mutableStateOf("https://www.zhihu.com/api/v4/me") }
        var responseText by remember { mutableStateOf("") }
        var isLoading by remember { mutableStateOf(false) }

        AlertDialog(
            onDismissRequest = {
                showSignedRequestDialog = false
                urlInput = "https://www.zhihu.com/api/v4/me"
                responseText = ""
                isLoading = false
            },
            title = { Text("签名GET请求") },
            text = {
                Column {
                    Text(
                        "输入需要签名的GET请求URL，将自动添加签名头并发送请求",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 16.dp),
                    )
                    OutlinedTextField(
                        value = urlInput,
                        onValueChange = { urlInput = it },
                        label = { Text("请求URL") },
                        placeholder = { Text("https://www.zhihu.com/api/v4/me") },
                        modifier = Modifier.fillMaxWidth(),
                        maxLines = 3,
                        enabled = !isLoading,
                    )
                    if (responseText.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            "响应内容:",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary,
                        )
                        SelectionContainer {
                            Text(
                                responseText,
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.padding(top = 8.dp),
                                maxLines = 10,
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (urlInput.isNotBlank() && !isLoading) {
                            isLoading = true
                            coroutineScope.launch {
                                try {
                                    val httpClient = AccountData.httpClient(context)
                                    val response = httpClient.get(urlInput) {
                                        signFetchRequest(context)
                                    }
                                    val body = response.bodyAsText()
                                    responseText = body

                                    // 复制到剪贴板
                                    val clip = ClipData.newPlainText("Signed Request Response", body)
                                    context.clipboardManager.setPrimaryClip(clip)

                                    Toast.makeText(context, "响应已复制到剪贴板", Toast.LENGTH_SHORT).show()
                                } catch (e: Exception) {
                                    responseText = "错误: ${e.message}"
                                    Toast.makeText(context, "请求失败: ${e.message}", Toast.LENGTH_LONG).show()
                                } finally {
                                    isLoading = false
                                }
                            }
                        } else {
                            Toast.makeText(context, "请输入有效的URL", Toast.LENGTH_SHORT).show()
                        }
                    },
                    enabled = !isLoading,
                ) {
                    Text(if (isLoading) "请求中..." else "发送请求")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showSignedRequestDialog = false
                        urlInput = "https://www.zhihu.com/api/v4/me"
                        responseText = ""
                        isLoading = false
                    },
                    enabled = !isLoading,
                ) {
                    Text("关闭")
                }
            },
        )
    }
}
