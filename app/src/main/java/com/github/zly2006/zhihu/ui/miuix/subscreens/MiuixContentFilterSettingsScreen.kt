/*
 * Zhihu++ - Free & Ad-Free Zhihu client for Android.
 * Copyright (C) 2024-2026, zly2006 <i@zly2006.me>
 *
 * Licensed under AGPL-3.0-only.
 */

package com.github.zly2006.zhihu.ui.miuix.subscreens

import android.content.Context
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.Alignment
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.TextButton
import top.yukonga.miuix.kmp.window.WindowBottomSheet
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.edit
import com.github.zly2006.zhihu.data.RecommendationMode
import com.github.zly2006.zhihu.navigation.Account
import com.github.zly2006.zhihu.navigation.LocalNavigator
import com.github.zly2006.zhihu.theme.getMiuixAppBarColor
import com.github.zly2006.zhihu.theme.installerMiuixBlurEffect
import com.github.zly2006.zhihu.theme.rememberMiuixBlurBackdrop
import com.github.zly2006.zhihu.ui.PREFERENCE_NAME
import com.github.zly2006.zhihu.viewmodel.filter.ContentFilterManager
import com.github.zly2006.zhihu.viewmodel.filter.FilterStats
import kotlinx.coroutines.launch
import top.yukonga.miuix.kmp.blur.layerBackdrop
import top.yukonga.miuix.kmp.basic.Button
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.DropdownItem
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.SmallTitle
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TopAppBar
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Back
import top.yukonga.miuix.kmp.preference.ArrowPreference
import top.yukonga.miuix.kmp.preference.SwitchPreference
import top.yukonga.miuix.kmp.preference.WindowSpinnerPreference
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.utils.overScrollVertical

@Composable
fun MiuixContentFilterSettingsScreen(
    setting: String = "",
) {
    val navigator = LocalNavigator.current
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val preferences = remember { context.getSharedPreferences(PREFERENCE_NAME, Context.MODE_PRIVATE) }
    val blurEnabled = remember { mutableStateOf(preferences.getBoolean("blurEnabled", true)) }
    val backdrop = rememberMiuixBlurBackdrop(blurEnabled.value)
    val scrollBehavior = MiuixScrollBehavior()
    var filterStats by remember { mutableStateOf<FilterStats?>(null) }
    val showStatsSheet = remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        try { filterStats = ContentFilterManager.getInstance(context).getFilterStats() } catch (_: Exception) {}
    }

    Scaffold(
        topBar = {
            TopAppBar(
                modifier = Modifier.installerMiuixBlurEffect(backdrop),
                color = backdrop.getMiuixAppBarColor(),
                title = "推荐系统与内容过滤",
                navigationIcon = {
                    IconButton(onClick = navigator.onNavigateBack) {
                        Icon(MiuixIcons.Back, "返回", tint = MiuixTheme.colorScheme.onBackground)
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

            // ── 推荐算法 ──
            item { SmallTitle(text = "推荐算法") }
            item {
                Card(Modifier.padding(horizontal = 12.dp).padding(bottom = 12.dp)) {
                    RecommendationModeSpinner(preferences)
                    val isLoginForRecommendation = remember { mutableStateOf(preferences.getBoolean("loginForRecommendation", true)) }
                    SwitchPreference(
                        title = "推荐内容时登录",
                        summary = "获取推荐内容时携带登录凭证",
                        checked = isLoginForRecommendation.value,
                        onCheckedChange = {
                            isLoginForRecommendation.value = it
                            preferences.edit { putBoolean("loginForRecommendation", it) }
                        },
                    )
                }
            }

            // ── 内容过滤 ──
            item { SmallTitle(text = "内容过滤") }
            item {
                Card(Modifier.padding(horizontal = 12.dp).padding(bottom = 12.dp)) {
                    val enableQualityFilter = remember { mutableStateOf(preferences.getBoolean("enableQualityFilter", true)) }
                    SwitchPreference(
                        title = "启用质量过滤规则",
                        summary = "根据赞同数、关注数等指标过滤低质量内容",
                        checked = enableQualityFilter.value,
                        onCheckedChange = {
                            enableQualityFilter.value = it
                            preferences.edit { putBoolean("enableQualityFilter", it) }
                        },
                    )

                    val enableContentFilter = remember { mutableStateOf(preferences.getBoolean("enableContentFilter", true)) }
                    SwitchPreference(
                        title = "启用智能内容过滤",
                        summary = "自动过滤首页展示超过2次但用户未点击的内容，减少重复推荐",
                        checked = enableContentFilter.value,
                        onCheckedChange = {
                            enableContentFilter.value = it
                            preferences.edit { putBoolean("enableContentFilter", it) }
                        },
                    )

                    val filterFollowedUserContent = remember { mutableStateOf(preferences.getBoolean("filterFollowedUserContent", false)) }
                    SwitchPreference(
                        title = "过滤已关注用户内容",
                        summary = "是否对已关注用户的内容也应用过滤规则",
                        checked = filterFollowedUserContent.value,
                        onCheckedChange = {
                            filterFollowedUserContent.value = it
                            preferences.edit { putBoolean("filterFollowedUserContent", it) }
                        },
                    )
                }
            }

            // ── 关键词 / 用户 / 主题屏蔽 ──
            item { SmallTitle(text = "屏蔽设置") }
            item {
                Card(Modifier.padding(horizontal = 12.dp).padding(bottom = 12.dp)) {
                    val enableKeywordBlocking = remember { mutableStateOf(preferences.getBoolean("enableKeywordBlocking", true)) }
                    SwitchPreference(
                        title = "启用关键词屏蔽",
                        summary = "屏蔽包含特定关键词的内容",
                        checked = enableKeywordBlocking.value,
                        onCheckedChange = {
                            enableKeywordBlocking.value = it
                            preferences.edit { putBoolean("enableKeywordBlocking", it) }
                        },
                    )

                    val enableUserBlocking = remember { mutableStateOf(preferences.getBoolean("enableUserBlocking", true)) }
                    SwitchPreference(
                        title = "启用用户屏蔽",
                        summary = "屏蔽特定用户发布的内容",
                        checked = enableUserBlocking.value,
                        onCheckedChange = {
                            enableUserBlocking.value = it
                            preferences.edit { putBoolean("enableUserBlocking", it) }
                        },
                    )

                    val enableTopicBlocking = remember { mutableStateOf(preferences.getBoolean("enableTopicBlocking", true)) }
                    SwitchPreference(
                        title = "启用主题屏蔽",
                        summary = "屏蔽包含特定主题的内容",
                        checked = enableTopicBlocking.value,
                        onCheckedChange = {
                            enableTopicBlocking.value = it
                            preferences.edit { putBoolean("enableTopicBlocking", it) }
                        },
                    )

                    if (enableTopicBlocking.value) {
                        val topicThreshold = remember { mutableStateOf(preferences.getInt("topicBlockingThreshold", 1)) }
                        var showThresholdDialog by remember { mutableStateOf(false) }

                        ArrowPreference(
                            title = "主题屏蔽阈值",
                            summary = "当回答的问题包含 >= ${topicThreshold.value} 个被屏蔽主题时屏蔽该内容",
                            onClick = { showThresholdDialog = true },
                        )

                        if (showThresholdDialog) {
                            var inputValue by remember { mutableStateOf(topicThreshold.value.toString()) }
                            AlertDialog(
                                onDismissRequest = { showThresholdDialog = false },
                                title = { Text("设置主题屏蔽阈值") },
                                text = {
                                    Column {
                                        Text("当内容包含的被屏蔽主题数量达到或超过此阈值时，该内容将被屏蔽。")
                                        Spacer(modifier = Modifier.height(16.dp))
                                        OutlinedTextField(
                                            value = inputValue,
                                            onValueChange = { inputValue = it },
                                            label = { Text("阈值") },
                                            singleLine = true,
                                        )
                                    }
                                },
                                confirmButton = {
                                    TextButton(onClick = {
                                        val newThreshold = inputValue.toIntOrNull()
                                        if (newThreshold != null && newThreshold > 0) {
                                            topicThreshold.value = newThreshold
                                            preferences.edit { putInt("topicBlockingThreshold", newThreshold) }
                                            showThresholdDialog = false
                                        } else {
                                            Toast.makeText(context, "请输入大于0的整数", Toast.LENGTH_SHORT).show()
                                        }
                                    }) { Text("确定") }
                                },
                                dismissButton = {
                                    TextButton(onClick = { showThresholdDialog = false }) { Text("取消") }
                                },
                            )
                        }
                    }
                }
            }

            // ── 内容类型屏蔽 ──
            item { SmallTitle(text = "内容类型屏蔽") }
            item {
                Card(Modifier.padding(horizontal = 12.dp).padding(bottom = 12.dp)) {
                    val blockZhihuAdPlatform = remember { mutableStateOf(preferences.getBoolean("blockZhihuAdPlatform", true)) }
                    SwitchPreference(
                        title = "屏蔽知乎广告平台内容",
                        summary = "匹配并屏蔽包含 xg.zhihu.com 的推广内容",
                        checked = blockZhihuAdPlatform.value,
                        onCheckedChange = {
                            blockZhihuAdPlatform.value = it
                            preferences.edit { putBoolean("blockZhihuAdPlatform", it) }
                        },
                    )

                    val blockZhihuSchool = remember { mutableStateOf(preferences.getBoolean("blockZhihuSchool", true)) }
                    SwitchPreference(
                        title = "屏蔽知乎学堂内容",
                        summary = "匹配并屏蔽包含 d.zhihu.com 或 data-edu-card-id 的内容",
                        checked = blockZhihuSchool.value,
                        onCheckedChange = {
                            blockZhihuSchool.value = it
                            preferences.edit { putBoolean("blockZhihuSchool", it) }
                        },
                    )

                    val blockWeChatOfficialAccount = remember { mutableStateOf(preferences.getBoolean("blockWeChatOfficialAccount", true)) }
                    SwitchPreference(
                        title = "屏蔽微信公众号文章",
                        summary = "匹配并屏蔽包含 mp.weixin.qq.com 的外链文章",
                        checked = blockWeChatOfficialAccount.value,
                        onCheckedChange = {
                            blockWeChatOfficialAccount.value = it
                            preferences.edit { putBoolean("blockWeChatOfficialAccount", it) }
                        },
                    )

                    val blockPaidContent = remember { mutableStateOf(preferences.getBoolean("blockPaidContent", true)) }
                    SwitchPreference(
                        title = "屏蔽知乎盐选付费内容",
                        summary = "屏蔽知乎盐选会员专享的付费回答和文章",
                        checked = blockPaidContent.value,
                        onCheckedChange = {
                            blockPaidContent.value = it
                            preferences.edit { putBoolean("blockPaidContent", it) }
                        },
                    )

                    val reverseBlock = remember { mutableStateOf(preferences.getBoolean("reverseBlock", false)) }
                    SwitchPreference(
                        title = "反向屏蔽（吃💩模式）",
                        summary = "开启后，首页将只保留广告和付费内容，屏蔽其余所有内容",
                        checked = reverseBlock.value,
                        onCheckedChange = {
                            reverseBlock.value = it
                            preferences.edit { putBoolean("reverseBlock", it) }
                        },
                    )
                }
            }

            // ── 管理 ──
            item { SmallTitle(text = "管理") }
            item {
                Card(Modifier.padding(horizontal = 12.dp).padding(bottom = 12.dp)) {
                    ArrowPreference(
                        title = "管理屏蔽列表",
                        onClick = { navigator.onNavigate(Account.RecommendSettings.Blocklist) },
                    )
                }
            }
            item {
                Card(Modifier.padding(horizontal = 12.dp).padding(bottom = 12.dp)) {
                    ArrowPreference(
                        title = "屏蔽记录",
                        onClick = { navigator.onNavigate(Account.RecommendSettings.BlockedFeedHistory) },
                    )
                }
            }

            // ── 过滤统计 ──
            val enableContentFilter = preferences.getBoolean("enableContentFilter", true)
            if (enableContentFilter && filterStats != null) {
                item {
                    Card(Modifier.padding(horizontal = 12.dp).padding(bottom = 12.dp)) {
                        ArrowPreference(
                            title = "过滤统计",
                            summary = "已累计过滤 ${filterStats?.filteredCount ?: 0} 条内容，点击查看详情",
                            onClick = { showStatsSheet.value = true },
                        )
                    }
                }
            }
        }
    }

    WindowBottomSheet(
        show = showStatsSheet.value,
        onDismissRequest = { showStatsSheet.value = false },
        title = "过滤统计详情",
    ) {
        Column(modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text("总记录数: ${filterStats?.totalRecords ?: 0}")
                Text("过滤率: %.1f%%".format((filterStats?.filterRate ?: 0f) * 100))
            }
            Spacer(modifier = Modifier.height(20.dp))
            Button(
                onClick = {
                    coroutineScope.launch {
                        try {
                            ContentFilterManager.getInstance(context).cleanupOldData()
                            filterStats = ContentFilterManager.getInstance(context).getFilterStats()
                            Toast.makeText(context, "已清理过期数据", Toast.LENGTH_SHORT).show()
                        } catch (_: Exception) {}
                    }
                },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("清理过期数据")
            }
            Spacer(modifier = Modifier.height(8.dp))
            Button(
                onClick = {
                    coroutineScope.launch {
                        try {
                            ContentFilterManager.getInstance(context).clearAllData()
                            filterStats = ContentFilterManager.getInstance(context).getFilterStats()
                            Toast.makeText(context, "已重置所有数据", Toast.LENGTH_SHORT).show()
                            showStatsSheet.value = false
                        } catch (_: Exception) {}
                    }
                },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("重置所有数据")
            }
        }
    }
}

@Composable
private fun RecommendationModeSpinner(preferences: android.content.SharedPreferences) {
    val currentMode = remember {
        mutableStateOf(
            RecommendationMode.entries.find { it.key == preferences.getString("recommendationMode", RecommendationMode.MIXED.key) }
                ?: RecommendationMode.MIXED,
        )
    }
    val items = remember { RecommendationMode.entries.map { DropdownItem(title = it.displayName) } }
    val idx = remember(currentMode.value) { RecommendationMode.entries.indexOf(currentMode.value).coerceAtLeast(0) }

    WindowSpinnerPreference(
        title = "推荐算法",
        items = items,
        selectedIndex = idx,
        onSelectedIndexChange = { newIdx ->
            val mode = RecommendationMode.entries[newIdx]
            currentMode.value = mode
            preferences.edit { putString("recommendationMode", mode.key) }
        },
    )
}
