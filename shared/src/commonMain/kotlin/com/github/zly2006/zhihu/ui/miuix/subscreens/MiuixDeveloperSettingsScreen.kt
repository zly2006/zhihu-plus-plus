/*
 * Zhihu++ - Free & Ad-Free Zhihu client for Android.
 * Copyright (C) 2024-2026, zly2006 <i@zly2006.me>
 * Licensed under AGPL-3.0-only.
 */

package com.github.zly2006.zhihu.ui.miuix.subscreens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.github.zly2006.zhihu.navigation.Account
import com.github.zly2006.zhihu.navigation.LocalNavigator
import com.github.zly2006.zhihu.navigation.SentenceSimilarityTest
import com.github.zly2006.zhihu.shared.data.ZHIHU_ME_URL
import com.github.zly2006.zhihu.shared.platform.rememberDeveloperDiagnostics
import com.github.zly2006.zhihu.shared.platform.rememberPlainTextClipboard
import com.github.zly2006.zhihu.shared.platform.rememberSettingBoolean
import com.github.zly2006.zhihu.shared.platform.rememberSettingsStore
import com.github.zly2006.zhihu.shared.platform.rememberUserMessageSink
import com.github.zly2006.zhihu.theme.getMiuixAppBarColor
import com.github.zly2006.zhihu.theme.installerMiuixBlurEffect
import com.github.zly2006.zhihu.theme.rememberMiuixBlurBackdrop
import com.github.zly2006.zhihu.ui.TtsState
import com.github.zly2006.zhihu.ui.miuix.components.MiuixIconsEmbedded
import com.github.zly2006.zhihu.ui.subscreens.parseCookieString
import com.github.zly2006.zhihu.ui.subscreens.rememberDeveloperSettingsRuntime
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.SmallTitle
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TopAppBar
import top.yukonga.miuix.kmp.blur.layerBackdrop
import top.yukonga.miuix.kmp.preference.ArrowPreference
import top.yukonga.miuix.kmp.preference.SwitchPreference
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.utils.overScrollVertical

@Composable
fun MiuixDeveloperSettingsScreen() {
    val navigator = LocalNavigator.current
    val settings = rememberSettingsStore()
    val runtime = rememberDeveloperSettingsRuntime()
    val diagnostics = rememberDeveloperDiagnostics()
    val copyPlainText = rememberPlainTextClipboard()
    val userMessages = rememberUserMessageSink()
    val coroutineScope = rememberCoroutineScope()
    val blurEnabled = rememberSettingBoolean("blurEnabled", true, settings)
    val backdrop = rememberMiuixBlurBackdrop(blurEnabled)
    val scrollBehavior = MiuixScrollBehavior()
    var developerModeEnabled by remember { mutableStateOf(settings.getBoolean("developer", false)) }
    var showCookieDialog by remember { mutableStateOf(false) }
    var showSignedRequestDialog by remember { mutableStateOf(false) }
    val continuousUsageDurationMs by produceState(
        initialValue = runtime.runtimeInfo().continuousUsageDurationMs,
        key1 = runtime,
    ) {
        while (true) {
            value = runtime.runtimeInfo().continuousUsageDurationMs
            delay(1_000L)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                modifier = Modifier.installerMiuixBlurEffect(backdrop),
                color = backdrop.getMiuixAppBarColor(),
                title = "开发者选项",
                navigationIcon = {
                    IconButton(onClick = navigator.onNavigateBack) {
                        Icon(MiuixIconsEmbedded.Back, "返回", tint = MiuixTheme.colorScheme.onBackground)
                    }
                },
                scrollBehavior = scrollBehavior,
            )
        },
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .then(if (backdrop != null) Modifier.layerBackdrop(backdrop) else Modifier)
                .overScrollVertical()
                .nestedScroll(scrollBehavior.nestedScrollConnection),
            contentPadding = PaddingValues(top = innerPadding.calculateTopPadding(), bottom = innerPadding.calculateBottomPadding() + 24.dp),
        ) {
            item { Spacer(Modifier.height(12.dp)) }

            // Behavior switches
            item { SmallTitle(text = "行为开关") }
            item {
                Card(Modifier.padding(horizontal = 12.dp).padding(bottom = 12.dp)) {
                    SwitchPreference(
                        title = "开发者模式",
                        checked = developerModeEnabled,
                        onCheckedChange = {
                            developerModeEnabled = it
                            settings.putBoolean("developer", it)
                            if (!it) {
                                navigator.onNavigateBack()
                            }
                        },
                    )
                    var enablePredictiveBack by remember { mutableStateOf(settings.getBoolean("enable_predictive_back", true)) }
                    SwitchPreference(title = "预测性返回手势", summary = "Android 14+", checked = enablePredictiveBack, onCheckedChange = {
                        enablePredictiveBack = it
                        settings.putBoolean("enable_predictive_back", it)
                    })
                    var enableScrollEndHaptic by remember { mutableStateOf(settings.getBoolean("enableScrollEndHaptic", true)) }
                    SwitchPreference(title = "滚动到底震动反馈", checked = enableScrollEndHaptic, onCheckedChange = {
                        enableScrollEndHaptic = it
                        settings.putBoolean("enableScrollEndHaptic", it)
                    })
                    var enableSwipeReaction by remember { mutableStateOf(settings.getBoolean("enableSwipeReaction", false)) }
                    SwitchPreference(title = "滑动反馈 (Like/Dislike)", summary = "左右滑动卡片触发喜欢/不喜欢", checked = enableSwipeReaction, onCheckedChange = {
                        enableSwipeReaction = it
                        settings.putBoolean("enableSwipeReaction", it)
                    })
                    var openSourceLicenses by remember { mutableStateOf(settings.getBoolean("open_source_licenses", true)) }
                    SwitchPreference(title = "开源许可页面", checked = openSourceLicenses, onCheckedChange = {
                        openSourceLicenses = it
                        settings.putBoolean("open_source_licenses", it)
                    })
                    var showSearchHotSearch by remember { mutableStateOf(settings.getBoolean("showSearchHotSearch", true)) }
                    SwitchPreference(title = "搜索页展示热搜", checked = showSearchHotSearch, onCheckedChange = {
                        showSearchHotSearch = it
                        settings.putBoolean("showSearchHotSearch", it)
                    })
                    var showSearchHistory by remember { mutableStateOf(settings.getBoolean("showSearchHistory", true)) }
                    SwitchPreference(title = "搜索页展示历史", checked = showSearchHistory, onCheckedChange = {
                        showSearchHistory = it
                        settings.putBoolean("showSearchHistory", it)
                    })
                }
            }

            // Diagnostic tools
            item { SmallTitle(text = "诊断工具") }
            item {
                Card(Modifier.padding(horizontal = 12.dp).padding(bottom = 12.dp)) {
                    ArrowPreference(title = "App 信息", summary = diagnostics.appInfo, onClick = {})
                    ArrowPreference(
                        title = "网络状态",
                        summary = runtime.networkStatus(),
                        onClick = {},
                    )
                    runtime.powerSaveModeText()?.let { powerSaveMode ->
                        ArrowPreference(
                            title = "省电模式",
                            summary = powerSaveMode,
                            onClick = {},
                        )
                    }
                    ArrowPreference(
                        title = "连续使用时长",
                        summary = formatContinuousUsageDuration(continuousUsageDurationMs),
                        onClick = {},
                    )
                    ArrowPreference(
                        title = "验证登录",
                        onClick = {
                            coroutineScope.launch {
                                if (runtime.verifyLogin(runtime.cookies())) {
                                    userMessages.showShortMessage("登录成功")
                                } else {
                                    userMessages.showShortMessage("登录失败")
                                }
                            }
                        },
                    )
                    ArrowPreference(
                        title = "刷新 Token",
                        onClick = {
                            coroutineScope.launch {
                                runtime.refreshToken()
                                userMessages.showShortMessage("刷新成功")
                            }
                        },
                    )
                    ArrowPreference(title = "手动设置 Cookie", onClick = { showCookieDialog = true })
                    ArrowPreference(title = "签名请求", onClick = { showSignedRequestDialog = true })
                    ArrowPreference(title = "句子相似度测试", summary = "NLP 模型测试", onClick = { navigator.onNavigate(SentenceSimilarityTest) })
                    ArrowPreference(title = "配色方案查看器", summary = "查看 M3 ColorScheme token", onClick = { navigator.onNavigate(Account.DeveloperSettings.ColorScheme) })

                    var showDebugOverlay by remember { mutableStateOf(settings.getBoolean("showDebugOverlay", false)) }
                    SwitchPreference(title = "调试悬浮窗", summary = "显示当前 Feed 详情", checked = showDebugOverlay, onCheckedChange = {
                        showDebugOverlay = it
                        settings.putBoolean("showDebugOverlay", it)
                    })
                }
            }

            item { SmallTitle(text = "语音朗读引擎") }
            item {
                Card(Modifier.padding(horizontal = 12.dp).padding(bottom = 12.dp)) {
                    val runtimeInfo = runtime.runtimeInfo()
                    DeveloperInfoRow("当前引擎", runtimeInfo.currentTtsEngineLabel)
                    DeveloperInfoRow(
                        "引擎状态",
                        when {
                            runtimeInfo.ttsState.isSpeaking -> "正在朗读"
                            runtimeInfo.ttsState != TtsState.Uninitialized -> "就绪"
                            else -> "未就绪"
                        },
                    )
                    DeveloperInfoRow("引擎列表", runtimeInfo.availableTtsEngineLabels.joinToString().ifEmpty { "无" })
                }
            }

            // GitHub Token
            item { SmallTitle(text = "GitHub Token") }
            item {
                var githubToken by remember { mutableStateOf(settings.getString("githubToken", "")) }
                var showGithubToken by remember { mutableStateOf(false) }
                Card(Modifier.padding(horizontal = 12.dp).padding(bottom = 12.dp)) {
                    ArrowPreference(
                        title = "GitHub Token",
                        summary = if (githubToken.isNotEmpty()) "${githubToken.take(12)}..." else "未设置",
                        onClick = { showGithubToken = !showGithubToken },
                    )
                    if (showGithubToken) {
                        OutlinedTextField(
                            value = githubToken,
                            onValueChange = {
                                githubToken = it
                                settings.putString("githubToken", it)
                            },
                            visualTransformation = if (showGithubToken) VisualTransformation.None else PasswordVisualTransformation(),
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                            singleLine = true,
                        )
                    }
                }
            }

            // Devices
            item { SmallTitle(text = "设备标识") }
            item {
                Card(Modifier.padding(horizontal = 12.dp).padding(bottom = 12.dp)) {
                    var zse96Key by remember { mutableStateOf(settings.getString("zse96_key", "")) }
                    var showZse96 by remember { mutableStateOf(false) }
                    ArrowPreference(
                        title = "ZSE-96 签名密钥",
                        summary = if (zse96Key.isNotEmpty()) "${zse96Key.take(8)}..." else "未设置",
                        onClick = { showZse96 = !showZse96 },
                    )
                    if (showZse96) {
                        OutlinedTextField(
                            value = zse96Key,
                            onValueChange = {
                                zse96Key = it
                                settings.putString("zse96_key", it)
                            },
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                            singleLine = true,
                        )
                    }

                    var showDeviceInfo by remember { mutableStateOf(false) }
                    if (showDeviceInfo) {
                        Card(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
                            SelectionContainer {
                                Text(diagnostics.deviceInfo, fontSize = 12.sp, modifier = Modifier.padding(12.dp))
                            }
                        }
                    }
                    ArrowPreference(title = "设备信息", onClick = { showDeviceInfo = !showDeviceInfo })

                    var showClipboardDebug by remember { mutableStateOf(false) }
                    if (showClipboardDebug) {
                        AlertDialog(
                            onDismissRequest = { showClipboardDebug = false },
                            title = { Text("剪贴板调试") },
                            text = {
                                Column {
                                    Text("当前剪贴板内容：")
                                    Spacer(Modifier.height(8.dp))
                                    Text(diagnostics.readClipboardText() ?: "(空)", color = MiuixTheme.colorScheme.onSurface)
                                }
                            },
                            confirmButton = { TextButton(onClick = { showClipboardDebug = false }) { Text("关闭") } },
                        )
                    }
                    ArrowPreference(title = "剪贴板调试", onClick = { showClipboardDebug = true })

                    ArrowPreference(
                        title = "导出所有配置",
                        summary = "复制 SharedPreferences 到剪贴板",
                        onClick = {
                            copyPlainText("dev_config", diagnostics.exportAllSettings())
                            userMessages.showShortMessage("已复制所有配置到剪贴板")
                        },
                    )
                }
            }
        }
    }

    if (showCookieDialog) {
        var cookieInputText by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = {
                showCookieDialog = false
                cookieInputText = ""
            },
            title = { Text("手动设置 Cookie") },
            text = {
                Column {
                    Text("请输入完整 Cookie 字符串，使用 \";\" 分割各个 cookie 项。")
                    Spacer(Modifier.height(12.dp))
                    OutlinedTextField(
                        value = cookieInputText,
                        onValueChange = { cookieInputText = it },
                        label = { Text("Cookie 字符串") },
                        visualTransformation = PasswordVisualTransformation(),
                        modifier = Modifier.fillMaxWidth(),
                        maxLines = 5,
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val cookies = parseCookieString(cookieInputText)
                        if (cookies.isEmpty()) {
                            userMessages.showShortMessage("未能解析有效的 Cookie 数据")
                            return@TextButton
                        }
                        runtime.saveCookies(cookies)
                        coroutineScope.launch {
                            if (runtime.verifyLogin(cookies)) {
                                userMessages.showShortMessage("Cookie 设置成功并验证登录状态")
                            } else {
                                userMessages.showShortMessage("Cookie 设置成功，但验证登录失败")
                            }
                        }
                        showCookieDialog = false
                        cookieInputText = ""
                    },
                ) { Text("确认设置") }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showCookieDialog = false
                        cookieInputText = ""
                    },
                ) { Text("取消") }
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
            title = { Text("签名 GET 请求") },
            text = {
                Column {
                    Text("输入需要签名的 GET 请求 URL，将自动添加签名头并发送请求。")
                    Spacer(Modifier.height(12.dp))
                    OutlinedTextField(
                        value = urlInput,
                        onValueChange = { urlInput = it },
                        label = { Text("请求 URL") },
                        modifier = Modifier.fillMaxWidth(),
                        maxLines = 3,
                        enabled = !isLoading,
                    )
                    if (responseText.isNotEmpty()) {
                        Spacer(Modifier.height(12.dp))
                        SelectionContainer {
                            Text(responseText, fontSize = 12.sp)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (urlInput.isBlank() || isLoading) {
                            userMessages.showShortMessage("请输入有效的 URL")
                            return@TextButton
                        }
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
                    },
                    enabled = !isLoading,
                ) { Text(if (isLoading) "请求中..." else "发送请求") }
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
                ) { Text("关闭") }
            },
        )
    }
}

@Composable
private fun DeveloperInfoRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            label,
            modifier = Modifier.weight(1f),
            color = MiuixTheme.colorScheme.onBackground,
        )
        Text(
            value,
            modifier = Modifier.weight(1.4f).padding(start = 16.dp),
            color = MiuixTheme.colorScheme.primary,
            textAlign = TextAlign.End,
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
