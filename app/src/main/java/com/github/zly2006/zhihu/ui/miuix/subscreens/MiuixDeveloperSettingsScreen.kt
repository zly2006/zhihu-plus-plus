/*
 * Zhihu++ - Free & Ad-Free Zhihu client for Android.
 * Copyright (C) 2024-2026, zly2006 <i@zly2006.me>
 * Licensed under AGPL-3.0-only.
 */

package com.github.zly2006.zhihu.ui.miuix.subscreens

import android.content.ClipData
import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.edit
import com.github.zly2006.zhihu.navigation.Account
import com.github.zly2006.zhihu.navigation.LocalNavigator
import com.github.zly2006.zhihu.navigation.SentenceSimilarityTest
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.Box
import com.github.zly2006.zhihu.theme.getMiuixAppBarColor
import com.github.zly2006.zhihu.theme.installerMiuixBlurEffect
import com.github.zly2006.zhihu.theme.rememberMiuixBlurBackdrop
import com.github.zly2006.zhihu.ui.PREFERENCE_NAME
import com.github.zly2006.zhihu.util.clipboardManager
import top.yukonga.miuix.kmp.blur.layerBackdrop
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.SmallTitle
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TopAppBar
import top.yukonga.miuix.kmp.preference.ArrowPreference
import top.yukonga.miuix.kmp.preference.SwitchPreference
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.utils.overScrollVertical

@Composable
fun MiuixDeveloperSettingsScreen() {
    val context = LocalContext.current
    val navigator = LocalNavigator.current
    val preferences = remember { context.getSharedPreferences(PREFERENCE_NAME, Context.MODE_PRIVATE) }
    val blurEnabled = remember { mutableStateOf(preferences.getBoolean("blurEnabled", true)) }
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
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "返回", tint = MiuixTheme.colorScheme.onBackground)
                    }
                },
                scrollBehavior = scrollBehavior,
            )
        },
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize()
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
                    var useCustomNavHost by remember { mutableStateOf(preferences.getBoolean("use_custom_nav_host", true)) }
                    SwitchPreference(title = "自定义导航宿主", summary = "用自定义 NavHost 替代系统导航", checked = useCustomNavHost, onCheckedChange = { useCustomNavHost = it; preferences.edit { putBoolean("use_custom_nav_host", it) } })
                    var enablePredictiveBack by remember { mutableStateOf(preferences.getBoolean("enable_predictive_back", true)) }
                    SwitchPreference(title = "预测性返回手势", summary = "Android 14+", checked = enablePredictiveBack, onCheckedChange = { enablePredictiveBack = it; preferences.edit { putBoolean("enable_predictive_back", it) } })
                    var enableScrollEndHaptic by remember { mutableStateOf(preferences.getBoolean("enableScrollEndHaptic", true)) }
                    SwitchPreference(title = "滚动到底震动反馈", checked = enableScrollEndHaptic, onCheckedChange = { enableScrollEndHaptic = it; preferences.edit { putBoolean("enableScrollEndHaptic", it) } })
                    var enableSwipeReaction by remember { mutableStateOf(preferences.getBoolean("enableSwipeReaction", false)) }
                    SwitchPreference(title = "滑动反馈 (Like/Dislike)", summary = "左右滑动卡片触发喜欢/不喜欢", checked = enableSwipeReaction, onCheckedChange = { enableSwipeReaction = it; preferences.edit { putBoolean("enableSwipeReaction", it) } })
                    var openSourceLicenses by remember { mutableStateOf(preferences.getBoolean("open_source_licenses", true)) }
                    SwitchPreference(title = "开源许可页面", checked = openSourceLicenses, onCheckedChange = { openSourceLicenses = it; preferences.edit { putBoolean("open_source_licenses", it) } })
                    var showSearchHotSearch by remember { mutableStateOf(preferences.getBoolean("showSearchHotSearch", true)) }
                    SwitchPreference(title = "搜索页展示热搜", checked = showSearchHotSearch, onCheckedChange = { showSearchHotSearch = it; preferences.edit { putBoolean("showSearchHotSearch", it) } })
                    var showSearchHistory by remember { mutableStateOf(preferences.getBoolean("showSearchHistory", true)) }
                    SwitchPreference(title = "搜索页展示历史", checked = showSearchHistory, onCheckedChange = { showSearchHistory = it; preferences.edit { putBoolean("showSearchHistory", it) } })
                }
            }

            // Diagnostic tools
            item { SmallTitle(text = "诊断工具") }
            item {
                Card(Modifier.padding(horizontal = 12.dp).padding(bottom = 12.dp)) {
                    ArrowPreference(title = "App 信息", summary = context.packageName, onClick = {})
                    ArrowPreference(
                        title = "网络状态",
                        summary = run {
                            val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
                            val nc = cm?.getNetworkCapabilities(cm.activeNetwork)
                            when {
                                nc == null -> "无网络"
                                nc.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> "WiFi"
                                nc.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> "蜂窝网络"
                                else -> "其他"
                            }
                        },
                        onClick = {},
                    )
                    ArrowPreference(title = "句子相似度测试", summary = "NLP 模型测试", onClick = { navigator.onNavigate(SentenceSimilarityTest) })
                    ArrowPreference(title = "配色方案查看器", summary = "查看 M3 ColorScheme token", onClick = { navigator.onNavigate(Account.DeveloperSettings.ColorScheme) })

                    var showDebugOverlay by remember { mutableStateOf(preferences.getBoolean("showDebugOverlay", false)) }
                    SwitchPreference(title = "调试悬浮窗", summary = "显示当前 Feed 详情", checked = showDebugOverlay, onCheckedChange = { showDebugOverlay = it; preferences.edit { putBoolean("showDebugOverlay", it) } })
                }
            }

            // GitHub Token
            item { SmallTitle(text = "GitHub Token") }
            item {
                var githubToken by remember { mutableStateOf(preferences.getString("githubToken", "") ?: "") }
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
                            onValueChange = { githubToken = it; preferences.edit { putString("githubToken", it) } },
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
                    val clipboard = context.clipboardManager
                    var zse96Key by remember { mutableStateOf(preferences.getString("zse96_key", "") ?: "") }
                    var showZse96 by remember { mutableStateOf(false) }
                    ArrowPreference(
                        title = "ZSE-96 签名密钥",
                        summary = if (zse96Key.isNotEmpty()) "${zse96Key.take(8)}..." else "未设置",
                        onClick = { showZse96 = !showZse96 },
                    )
                    if (showZse96) {
                        OutlinedTextField(
                            value = zse96Key,
                            onValueChange = { zse96Key = it; preferences.edit { putString("zse96_key", it) } },
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                            singleLine = true,
                        )
                    }

                    var showDeviceInfo by remember { mutableStateOf(false) }
                    if (showDeviceInfo) {
                        val info = run {
                            val pkg = context.packageName
                            val pm = context.packageManager
                            try { "versionName=${pm.getPackageInfo(pkg, 0).versionName}, versionCode=${pm.getPackageInfo(pkg, 0).longVersionCode}" }
                            catch (_: Exception) { "unknown" }
                        }
                        Card(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
                            SelectionContainer {
                                Text(info, fontSize = 12.sp, modifier = Modifier.padding(12.dp))
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
                                    Text(clipboard.primaryClip?.getItemAt(0)?.text?.toString() ?: "(空)", color = MiuixTheme.colorScheme.onSurface)
                                }
                            },
                            confirmButton = { TextButton(onClick = { showClipboardDebug = false }) { Text("关闭") } },
                        )
                    }
                    ArrowPreference(title = "剪贴板调试", onClick = { showClipboardDebug = true })

                    var exportDevConfig by remember { mutableStateOf(false) }
                    if (exportDevConfig) {
                        exportDevConfig = false
                        val allPrefs = preferences.all.map { "${it.key}: ${it.value}" }.joinToString("\n")
                        clipboard.setPrimaryClip(ClipData.newPlainText("dev_config", allPrefs))
                        Toast.makeText(context, "已复制所有配置到剪贴板", Toast.LENGTH_SHORT).show()
                    }
                    ArrowPreference(title = "导出所有配置", summary = "复制 SharedPreferences 到剪贴板", onClick = { exportDevConfig = true })
                }
            }
        }
    }
}
