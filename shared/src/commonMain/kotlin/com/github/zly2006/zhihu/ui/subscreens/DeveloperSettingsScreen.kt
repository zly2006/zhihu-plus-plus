/*
 * Zhihu++ - Free & Ad-Free Zhihu client for Android.
 * Copyright (C) 2024-2026, zly2006 <i@zly2006.me>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation (version 3 only).
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.github.zly2006.zhihu.ui.subscreens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.github.zly2006.zhihu.navigation.Account
import com.github.zly2006.zhihu.navigation.LocalNavigator
import com.github.zly2006.zhihu.navigation.SentenceSimilarityTest
import com.github.zly2006.zhihu.shared.data.ZHIHU_ME_URL
import com.github.zly2006.zhihu.shared.platform.rememberPlainTextClipboard
import com.github.zly2006.zhihu.shared.platform.rememberSettingsStore
import com.github.zly2006.zhihu.shared.platform.rememberUserMessageSink
import com.github.zly2006.zhihu.ui.TtsState
import com.github.zly2006.zhihu.ui.components.SettingItemOverall
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

const val DEVELOPER_SETTINGS_BACK_BUTTON_TAG = "developerSettings/backButton"
const val DEVELOPER_SETTINGS_MODE_TAG = "developerSettings/modeToggle"
const val DEVELOPER_SETTINGS_SENTENCE_SIMILARITY_TAG = "developerSettings/sentenceSimilarity"
const val DEVELOPER_SETTINGS_COLOR_SCHEME_TAG = "developerSettings/colorScheme"

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun DeveloperSettingsScreen() {
    val navigator = LocalNavigator.current
    val runtime = rememberDeveloperSettingsRuntime()
    val copyPlainText = rememberPlainTextClipboard()
    val userMessages = rememberUserMessageSink()
    val coroutineScope = rememberCoroutineScope()
    val settings = rememberSettingsStore()
    var developerModeEnabled by remember {
        mutableStateOf(settings.getBoolean("developer", false))
    }
    val continuousUsageDurationMs by produceState(
        initialValue = runtime.runtimeInfo().continuousUsageDurationMs,
        key1 = runtime,
    ) {
        while (true) {
            value = runtime.runtimeInfo().continuousUsageDurationMs
            delay(1_000L)
        }
    }

    var showCookieDialog by remember { mutableStateOf(false) }
    var showSignedRequestDialog by remember { mutableStateOf(false) }

    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    Scaffold(
        modifier = Modifier.fillMaxSize().nestedScroll(scrollBehavior.nestedScrollConnection),
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
        topBar = {
            LargeTopAppBar(
                title = { Text("开发者选项") },
                navigationIcon = {
                    IconButton(
                        modifier = Modifier.testTag(DEVELOPER_SETTINGS_BACK_BUTTON_TAG),
                        onClick = navigator.onNavigateBack,
                        colors = IconButtonDefaults.iconButtonColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant,
                            contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        ),
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
                scrollBehavior = scrollBehavior,
                colors = TopAppBarDefaults.topAppBarColors().copy(
                    containerColor = MaterialTheme.colorScheme.surfaceContainer,
                    scrolledContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                ),
            )
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(innerPadding)
                .padding(16.dp),
        ) {
            SettingItemOverall(
                modifier = Modifier.testTag(DEVELOPER_SETTINGS_MODE_TAG),
                title = { Text("开发者模式") },
                checked = developerModeEnabled,
                onCheckedChange = {
                    developerModeEnabled = it
                    settings.putBoolean("developer", it)
                    if (!it) {
                        navigator.onNavigateBack()
                    }
                },
            )
            SelectionContainer {
                Column {
                    Text(runtime.networkStatus())
                    runtime.powerSaveModeText()?.let { Text(it) }
                    Text("连续使用时长：${formatContinuousUsageDuration(continuousUsageDurationMs)}")

                    Spacer(Modifier.height(16.dp))
                }
            }
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = {
                    coroutineScope.launch {
                        if (runtime.verifyLogin(runtime.cookies())) {
                            userMessages.showShortMessage("登录成功")
                        } else {
                            userMessages.showShortMessage("登录失败")
                        }
                    }
                }) { Text("验证登录") }

                Button(onClick = {
                    coroutineScope.launch {
                        runtime.refreshToken()
                        userMessages.showShortMessage("刷新成功")
                    }
                }) { Text("刷新Token") }

                Button(onClick = { showCookieDialog = true }) { Text("手动设置Cookie") }

                Button(onClick = { showSignedRequestDialog = true }) { Text("签名请求") }

                Button(
                    modifier = Modifier.testTag(DEVELOPER_SETTINGS_SENTENCE_SIMILARITY_TAG),
                    onClick = {
                        navigator.onNavigate(SentenceSimilarityTest)
                    },
                ) { Text("句子相似度") }

                Button(
                    modifier = Modifier.testTag(DEVELOPER_SETTINGS_COLOR_SCHEME_TAG),
                    onClick = {
                        navigator.onNavigate(Account.DeveloperSettings.ColorScheme)
                    },
                ) { Text("Color Scheme") }
            }

            // TTS引擎信息显示
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
                            runtime.runtimeInfo().currentTtsEngineLabel,
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
                            if (runtime.runtimeInfo().ttsState.isSpeaking) {
                                "正在朗读"
                            } else if (runtime.runtimeInfo().ttsState != TtsState.Uninitialized) {
                                "就绪"
                            } else {
                                "未就绪"
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            color = when {
                                runtime.runtimeInfo().ttsState.isSpeaking -> MaterialTheme.colorScheme.tertiary
                                runtime.runtimeInfo().ttsState != TtsState.Uninitialized -> MaterialTheme.colorScheme.primary
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
                            runtime.runtimeInfo().availableTtsEngineLabels.joinToString(),
                            style = MaterialTheme.typography.bodyMedium,
                            color = when {
                                runtime.runtimeInfo().ttsState.isSpeaking -> MaterialTheme.colorScheme.tertiary
                                runtime.runtimeInfo().ttsState != TtsState.Uninitialized -> MaterialTheme.colorScheme.primary
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
                                    runtime.saveCookies(cookies)

                                    // 验证登录状态
                                    coroutineScope.launch {
                                        try {
                                            if (runtime.verifyLogin(cookies)) {
                                                userMessages.showShortMessage("Cookie设置成功并验证登录状态")
                                            } else {
                                                userMessages.showShortMessage("Cookie设置成功，但验证登录失败，请检查Cookie是否有效")
                                            }
                                        } catch (e: Exception) {
                                            userMessages.showShortMessage("验证登录时发生错误：${e.message}")
                                        }
                                    }

                                    showCookieDialog = false
                                    cookieInputText = ""
                                    showCookieText = false
                                } else {
                                    userMessages.showShortMessage("未能解析有效的Cookie数据")
                                }
                            } catch (e: Exception) {
                                userMessages.showShortMessage("解析Cookie时发生错误：${e.message}")
                            }
                        } else {
                            userMessages.showShortMessage("请输入Cookie字符串")
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
        var urlInput by remember { mutableStateOf(ZHIHU_ME_URL) }
        var responseText by remember { mutableStateOf("") }
        var isLoading by remember { mutableStateOf(false) }

        AlertDialog(
            onDismissRequest = {
                showSignedRequestDialog = false
                urlInput = ZHIHU_ME_URL
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
                        placeholder = { Text(ZHIHU_ME_URL) },
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
                                    val body = runtime.signedGet(urlInput)
                                    copyPlainText("Signed Request Response", body)
                                    responseText = body
                                    userMessages.showShortMessage("响应已复制到剪贴板")
                                } catch (e: Exception) {
                                    responseText = "错误: ${e.message}"
                                    userMessages.showShortMessage("请求失败: ${e.message}")
                                } finally {
                                    isLoading = false
                                }
                            }
                        } else {
                            userMessages.showShortMessage("请输入有效的URL")
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
                        urlInput = ZHIHU_ME_URL
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

private fun formatContinuousUsageDuration(durationMs: Long): String {
    val safeDurationMs = durationMs.coerceAtLeast(0L)
    val totalSeconds = safeDurationMs / 1_000L
    val hours = totalSeconds / 3_600L
    val minutes = (totalSeconds % 3_600L) / 60L
    val seconds = totalSeconds % 60L
    return when {
        hours > 0 -> "${hours}小时${minutes}分${seconds}秒"
        minutes > 0 -> "${minutes}分${seconds}秒"
        else -> "${seconds}秒"
    }
}
