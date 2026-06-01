/*
 * Zhihu++ - Free & Ad-Free Zhihu client for Android.
 * Copyright (C) 2024-2026, zly2006 <i@zly2006.me>
 *
 * Licensed under AGPL-3.0-only.
 */

package com.github.zly2006.zhihu.ui.miuix.subscreens

import android.content.Context
import android.content.Intent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import com.github.zly2006.zhihu.ui.miuix.components.MiuixIconsEmbedded
import androidx.compose.material.icons.filled.ArrowOutward
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.foundation.clickable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.withLink
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.core.content.edit
import androidx.core.net.toUri
import com.github.zly2006.zhihu.R
import com.github.zly2006.zhihu.navigation.LocalNavigator
import com.github.zly2006.zhihu.theme.getMiuixAppBarColor
import com.github.zly2006.zhihu.theme.installerMiuixBlurEffect
import com.github.zly2006.zhihu.theme.rememberMiuixBlurBackdrop
import com.github.zly2006.zhihu.ui.PREFERENCE_NAME
import com.github.zly2006.zhihu.ui.miuix.components.MiuixExpandableArrowPreference
import com.github.zly2006.zhihu.updater.UpdateManager
import com.github.zly2006.zhihu.updater.UpdateManager.UpdateState
import com.github.zly2006.zhihu.util.ContinuousUsageReminderManager
import com.github.zly2006.zhihu.util.ContinuousUsageReminderPolicy
import com.github.zly2006.zhihu.util.luoTianYiUrlLauncher
import kotlinx.coroutines.launch
import top.yukonga.miuix.kmp.blur.layerBackdrop
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.DropdownItem
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.SmallTitle
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextField
import top.yukonga.miuix.kmp.basic.TopAppBar
import top.yukonga.miuix.kmp.preference.ArrowPreference
import top.yukonga.miuix.kmp.preference.SwitchPreference
import top.yukonga.miuix.kmp.preference.WindowSpinnerPreference
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.utils.overScrollVertical

@Composable
fun MiuixSystemAndUpdateSettingsScreen() {
    val context = LocalContext.current
    val navigator = LocalNavigator.current
    val preferences = remember { context.getSharedPreferences(PREFERENCE_NAME, Context.MODE_PRIVATE) }
    val blurEnabled = remember { mutableStateOf(preferences.getBoolean("blurEnabled", true)) }
    val backdrop = rememberMiuixBlurBackdrop(blurEnabled.value)
    val scrollBehavior = MiuixScrollBehavior()
    val updateState by UpdateManager.updateState.collectAsState()
    val coroutineScope = rememberCoroutineScope()
    val showUpdateBanner = updateState is UpdateState.UpdateAvailable ||
        updateState is UpdateState.Downloading || updateState is UpdateState.Downloaded

    var updateVersion: String by remember { mutableStateOf("") }
    var releaseNotes: String? by remember { mutableStateOf(null) }
    LaunchedEffect(updateState) {
        val state = updateState
        if (state is UpdateState.UpdateAvailable) {
            updateVersion = state.version.toString()
            releaseNotes = state.releaseNotes
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                modifier = Modifier.installerMiuixBlurEffect(backdrop),
                color = backdrop.getMiuixAppBarColor(),
                title = "系统与更新",
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
            modifier = Modifier.fillMaxSize()
                .then(if (backdrop != null) Modifier.layerBackdrop(backdrop) else Modifier)
                .overScrollVertical()
                .nestedScroll(scrollBehavior.nestedScrollConnection),
            contentPadding = PaddingValues(
                top = innerPadding.calculateTopPadding(),
                bottom = innerPadding.calculateBottomPadding() + 24.dp,
            ),
        ) {
            item { Spacer(Modifier.height(12.dp)) }

            // 更新横幅
            if (showUpdateBanner) {
                item {
                    Card(Modifier.padding(horizontal = 12.dp).padding(bottom = 12.dp)) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            if (updateVersion.isNotEmpty()) {
                                Text("新版本：\n$updateVersion", fontSize = 20.sp)
                            } else {
                                Text("检测到新版本", fontSize = 20.sp)
                            }

                            if (releaseNotes != null) {
                                Spacer(Modifier.height(12.dp))
                                Card {
                                    Column(modifier = Modifier.padding(12.dp)) {
                                        Text("更新内容", fontSize = 16.sp)
                                        Spacer(Modifier.height(8.dp))
                                        SelectionContainer {
                                            Text(
                                                buildAnnotatedString {
                                                    val prRegex = Regex("https://github.com/zly2006/zhihu-plus-plus/pull/(\\d+)")
                                                    var lastIndex = 0
                                                    prRegex.findAll(releaseNotes!!).forEach { matchResult ->
                                                        append(releaseNotes!!.substring(lastIndex, matchResult.range.first))
                                                        val prNumber = matchResult.groupValues[1]
                                                        withLink(LinkAnnotation.Url("https://github.com/zly2006/zhihu-plus-plus/pull/$prNumber")) {
                                                            withStyle(SpanStyle(color = MiuixTheme.colorScheme.primary)) {
                                                                append("#$prNumber")
                                                            }
                                                        }
                                                        lastIndex = matchResult.range.last + 1
                                                    }
                                                    append(releaseNotes!!.substring(lastIndex))
                                                },
                                                fontSize = 14.sp,
                                                color = MiuixTheme.colorScheme.onSurfaceSecondary,
                                            )
                                        }
                                    }
                                }
                            }

                            Spacer(Modifier.height(12.dp))
                            val cnDownloadUrl = (updateState as? UpdateState.UpdateAvailable)?.cnDownloadUrl
                            if (!cnDownloadUrl.isNullOrBlank()) {
                                Card(
                                    modifier = Modifier.fillMaxWidth().clickable {
                                        runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, cnDownloadUrl.toUri())) }
                                            .onFailure { UpdateManager.updateState.value = UpdateState.Error(it.message ?: "无法打开浏览器") }
                                    },
                                ) {
                                    Box(Modifier.fillMaxWidth().padding(14.dp), contentAlignment = Alignment.Center) {
                                        Text("使用国内网盘加速下载", fontSize = 14.sp)
                                    }
                                }
                                Spacer(Modifier.height(8.dp))
                                Text("使用国内网盘下载，不需要梯，网络稳定。", fontSize = 13.sp, color = MiuixTheme.colorScheme.onSurfaceSecondary)
                                Spacer(Modifier.height(8.dp))
                            }

                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Card(
                                    modifier = Modifier.weight(1f).clickable {
                                        val state = updateState
                                        if (state is UpdateState.UpdateAvailable) {
                                            UpdateManager.skipVersion(context, state.version.toString())
                                            UpdateManager.updateState.value = UpdateState.Latest
                                        }
                                    },
                                ) {
                                    Box(Modifier.fillMaxWidth().padding(12.dp), contentAlignment = Alignment.Center) {
                                        Text("跳过此版本", fontSize = 14.sp)
                                    }
                                }
                                Card(
                                    modifier = Modifier.weight(1f).clickable {
                                        coroutineScope.launch {
                                            when (val state = updateState) {
                                                is UpdateState.UpdateAvailable -> UpdateManager.downloadUpdate(context, state.downloadUrl)
                                                is UpdateState.Downloaded -> UpdateManager.installUpdate(context, state.file)
                                                else -> {}
                                            }
                                        }
                                    },
                                ) {
                                    Box(Modifier.fillMaxWidth().padding(12.dp), contentAlignment = Alignment.Center) {
                                        Text(
                                            when (updateState) {
                                                is UpdateState.UpdateAvailable -> "下载更新"
                                                is UpdateState.Downloading -> "下载中..."
                                                is UpdateState.Downloaded -> "安装更新"
                                                else -> "下载更新"
                                            },
                                            fontSize = 14.sp,
                                            color = MiuixTheme.colorScheme.primary,
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // GitHub Token
            item { SmallTitle(text = "GitHub") }
            item {
                var githubToken by remember { mutableStateOf(preferences.getString("githubToken", "") ?: "") }
                var showGithubToken by remember { mutableStateOf(false) }
                var showPassword by remember { mutableStateOf(false) }
                Card(Modifier.padding(horizontal = 12.dp).padding(bottom = 12.dp)) {
                    MiuixExpandableArrowPreference(
                        title = "GitHub Token",
                        summary = if (githubToken.isNotEmpty()) "${githubToken.take(8)}..." else "用于访问 GitHub API 时解除限速",
                        expanded = showGithubToken,
                        onExpandedChange = { showGithubToken = !showGithubToken },
                    ) {
                        TextField(
                            value = githubToken,
                            onValueChange = { githubToken = it; preferences.edit { putString("githubToken", it) } },
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp).padding(bottom = 12.dp),
                            singleLine = true,
                            label = "Token",
                            visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
                            trailingIcon = {
                                IconButton(onClick = { showPassword = !showPassword }) {
                                    Icon(
                                        if (showPassword) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                        contentDescription = if (showPassword) "隐藏 Token" else "显示 Token",
                                        tint = MiuixTheme.colorScheme.onSurfaceSecondary,
                                    )
                                }
                            },
                        )
                    }
                }
            }

            // 自动更新设置
            item { SmallTitle(text = "更新设置") }
            item {
                Card(Modifier.padding(horizontal = 12.dp).padding(bottom = 12.dp)) {
                    var autoCheckUpdates by remember { mutableStateOf(UpdateManager.isAutoCheckEnabled(context)) }
                    SwitchPreference(
                        title = "自动检查更新",
                        summary = "应用启动后后台检查新版本，并在首页显示更新提醒",
                        checked = autoCheckUpdates,
                        onCheckedChange = {
                            autoCheckUpdates = it
                            UpdateManager.setAutoCheckEnabled(context, it)
                            if (!it) UpdateManager.updateState.value = UpdateState.NoUpdate
                        },
                    )
                    var checkNightlyUpdates by remember { mutableStateOf(preferences.getBoolean("checkNightlyUpdates", false)) }
                    SwitchPreference(
                        title = "检查 Nightly 版本更新",
                        summary = "检查每日构建版本 (可能不稳定)",
                        checked = checkNightlyUpdates,
                        onCheckedChange = {
                            checkNightlyUpdates = it
                            preferences.edit { putBoolean("checkNightlyUpdates", it) }
                        },
                    )
                    var allowTelemetry by remember { mutableStateOf(preferences.getBoolean("allowTelemetry", true)) }
                    SwitchPreference(
                        title = "允许发送遥测统计数据",
                        summary = "仅用于统计使用人数，不包含个人隐私",
                        checked = allowTelemetry,
                        onCheckedChange = {
                            allowTelemetry = it
                            preferences.edit { putBoolean("allowTelemetry", it) }
                        },
                    )
                }
            }

            // 手动检查更新按钮
            if (!showUpdateBanner) {
                item {
                    Card(
                        modifier = Modifier.padding(horizontal = 12.dp).padding(bottom = 12.dp)
                            .clickable {
                                coroutineScope.launch {
                                    when (updateState) {
                                        is UpdateState.NoUpdate, is UpdateState.Error -> UpdateManager.checkForUpdate(context)
                                        UpdateState.Latest -> UpdateManager.updateState.value = UpdateState.NoUpdate
                                        else -> {}
                                    }
                                }
                            },
                    ) {
                        Box(Modifier.fillMaxWidth().padding(14.dp), contentAlignment = Alignment.Center) {
                            Text(
                                when (updateState) {
                                    is UpdateState.NoUpdate -> "检查更新"
                                    is UpdateState.Checking -> "检查中..."
                                    is UpdateState.Latest -> "已经是最新版本"
                                    is UpdateState.Error -> "检查更新失败，点击重试"
                                    else -> ""
                                },
                                fontSize = 15.sp,
                            )
                        }
                    }
                }
            }

            // 防沉迷
            item { SmallTitle(text = "防沉迷") }
            item {
                Card(Modifier.padding(horizontal = 12.dp).padding(bottom = 12.dp)) {
                    var reminderIntervalMinutes by remember {
                        mutableIntStateOf(
                            ContinuousUsageReminderPolicy.normalizeIntervalMinutes(
                                preferences.getInt(ContinuousUsageReminderManager.KEY_CONTINUOUS_USAGE_REMINDER_INTERVAL_MINUTES, 0),
                            ),
                        )
                    }
                    val reminderOptions = listOf(
                        0 to "关闭", 15 to "每 15 分钟", 30 to "每 30 分钟", 60 to "每 1 小时",
                    )
                    val items = reminderOptions.map { DropdownItem(title = it.second) }
                    val idx = reminderOptions.indexOfFirst { it.first == reminderIntervalMinutes }.coerceAtLeast(0)
                    WindowSpinnerPreference(
                        title = "防沉迷提醒",
                        summary = "连续浏览时定时提醒休息",
                        items = items,
                        selectedIndex = idx,
                        onSelectedIndexChange = { newIdx ->
                            val minutes = reminderOptions[newIdx].first
                            reminderIntervalMinutes = minutes
                            preferences.edit { putInt(ContinuousUsageReminderManager.KEY_CONTINUOUS_USAGE_REMINDER_INTERVAL_MINUTES, minutes) }
                        },
                    )
                }
            }

            // 交流 & 闲聊
            item { SmallTitle(text = "交流 & 闲聊") }
            item {
                Card(Modifier.padding(horizontal = 12.dp).padding(bottom = 12.dp)) {
                    ArrowPreference(
                        title = "Discord 频道",
                        summary = "请在 my-other-apps/zhihu-plus-plus 频道讨论",
                        startAction = { Icon(painterResource(R.drawable.ic_discord_24dp), null) },
                        onClick = { luoTianYiUrlLauncher(context, "https://discord.gg/YCPFZV5XSA".toUri()) },
                    )
                    ArrowPreference(
                        title = "Telegram 群组 (Hydrogen)",
                        summary = "另一个知乎客户端 Hydrogen 的群组，也可以在里面讨论知乎++哦",
                        startAction = { Icon(painterResource(R.drawable.ic_telegram_24dp), null) },
                        onClick = { luoTianYiUrlLauncher(context, "https://t.me/+_A1Yto6EpyIyODA1".toUri()) },
                    )
                    ArrowPreference(
                        title = "GitHub Issue",
                        summary = "欢迎提交 issue 讨论功能和反馈问题",
                        startAction = { Icon(painterResource(R.drawable.ic_github_24dp), null) },
                        onClick = { luoTianYiUrlLauncher(context, "https://github.com/zly2006/zhihu-plus-plus/issues".toUri()) },
                    )
                }
            }
        }
    }
}
