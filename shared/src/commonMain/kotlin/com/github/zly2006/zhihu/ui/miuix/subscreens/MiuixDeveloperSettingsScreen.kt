/*
 * Zhihu++ - Free & Ad-Free Zhihu client for Android.
 * Copyright (C) 2024-2026, zly2006 <i@zly2006.me>
 * Licensed under AGPL-3.0-only.
 */

package com.github.zly2006.zhihu.ui.miuix.subscreens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.github.zly2006.zhihu.navigation.Account
import com.github.zly2006.zhihu.navigation.LocalNavigator
import com.github.zly2006.zhihu.navigation.SentenceSimilarityTest
import com.github.zly2006.zhihu.shared.platform.rememberDeveloperDiagnostics
import com.github.zly2006.zhihu.shared.platform.rememberPlainTextClipboard
import com.github.zly2006.zhihu.shared.platform.rememberSettingsStore
import com.github.zly2006.zhihu.shared.platform.rememberUserMessageSink
import com.github.zly2006.zhihu.theme.getMiuixAppBarColor
import com.github.zly2006.zhihu.theme.installerMiuixBlurEffect
import com.github.zly2006.zhihu.theme.rememberMiuixBlurBackdrop
import com.github.zly2006.zhihu.ui.miuix.components.MiuixIconsEmbedded
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
    val diagnostics = rememberDeveloperDiagnostics()
    val copyPlainText = rememberPlainTextClipboard()
    val userMessages = rememberUserMessageSink()
    val blurEnabled = remember { mutableStateOf(settings.getBoolean("blurEnabled", true)) }
    val backdrop = rememberMiuixBlurBackdrop(blurEnabled.value)
    val scrollBehavior = MiuixScrollBehavior()

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
                    var useCustomNavHost by remember { mutableStateOf(settings.getBoolean("use_custom_nav_host", true)) }
                    SwitchPreference(title = "自定义导航宿主", summary = "用自定义 NavHost 替代系统导航", checked = useCustomNavHost, onCheckedChange = {
                        useCustomNavHost = it
                        settings.putBoolean("use_custom_nav_host", it)
                    })
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
                        summary = diagnostics.networkStatus,
                        onClick = {},
                    )
                    ArrowPreference(title = "句子相似度测试", summary = "NLP 模型测试", onClick = { navigator.onNavigate(SentenceSimilarityTest) })
                    ArrowPreference(title = "配色方案查看器", summary = "查看 M3 ColorScheme token", onClick = { navigator.onNavigate(Account.DeveloperSettings.ColorScheme) })

                    var showDebugOverlay by remember { mutableStateOf(settings.getBoolean("showDebugOverlay", false)) }
                    SwitchPreference(title = "调试悬浮窗", summary = "显示当前 Feed 详情", checked = showDebugOverlay, onCheckedChange = {
                        showDebugOverlay = it
                        settings.putBoolean("showDebugOverlay", it)
                    })
                }
            }

            // GitHub Token
            item { SmallTitle(text = "GitHub Token") }
            item {
                var githubToken by remember { mutableStateOf(settings.getString("githubToken", "") ?: "") }
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
                    var zse96Key by remember { mutableStateOf(settings.getString("zse96_key", "") ?: "") }
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
}
