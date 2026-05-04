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

import android.content.Context
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
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
import androidx.core.content.edit
import com.github.zly2006.zhihu.data.RecommendationMode
import com.github.zly2006.zhihu.navigation.Account
import com.github.zly2006.zhihu.navigation.LocalNavigator
import com.github.zly2006.zhihu.ui.PREFERENCE_NAME
import com.github.zly2006.zhihu.ui.components.SettingItem
import com.github.zly2006.zhihu.ui.components.SettingItemGroup
import com.github.zly2006.zhihu.ui.components.SettingItemWithSwitch
import com.github.zly2006.zhihu.viewmodel.filter.ContentFilterManager
import com.github.zly2006.zhihu.viewmodel.filter.FilterStats
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.milliseconds

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun ContentFilterSettingsScreen(
    innerPadding: PaddingValues,
    setting: String = "",
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

    val scrollState = rememberScrollState()
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    LaunchedEffect(setting) {
        if (setting.isNotEmpty()) {
            delay(200.milliseconds)
            // 收缩 LargeTopAppBar（programmatic scroll 不触发 nestedScroll）
            scrollBehavior.state.heightOffset = scrollBehavior.state.heightOffsetLimit
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize().nestedScroll(scrollBehavior.nestedScrollConnection),
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
        topBar = {
            LargeTopAppBar(
                title = { Text("推荐系统与内容过滤") },
                navigationIcon = {
                    IconButton(
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
                .verticalScroll(scrollState)
                .testTag("contentFilterSettings:scroll")
                .padding(innerPadding)
                .padding(vertical = 16.dp),
        ) {
            SettingItemGroup {
                SettingItem(
                    title = { Text("推荐算法") },
                    settingKey = "recommendationMode",
                    highlightedKey = setting,
                    endAction = {
                        // Rec Mode
                        val currentRecommendationMode = remember {
                            mutableStateOf(
                                RecommendationMode.entries.find {
                                    it.key == preferences.getString("recommendationMode", RecommendationMode.MIXED.key)
                                } ?: RecommendationMode.MIXED,
                            )
                        }
                        var expanded by remember { mutableStateOf(false) }

                        ExposedDropdownMenuBox(
                            expanded = expanded,
                            onExpandedChange = { expanded = !expanded },
                            modifier = Modifier.width(256.dp),
                        ) {
                            OutlinedTextField(
                                value = currentRecommendationMode.value.displayName,
                                onValueChange = { },
                                readOnly = true,
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                                modifier = Modifier
                                    .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable, true)
                                    .testTag("contentFilterSettings:recommendationModeField"),
                            )
                            ExposedDropdownMenu(
                                expanded = expanded,
                                onDismissRequest = { expanded = false },
                            ) {
                                RecommendationMode.entries.forEach { mode ->
                                    DropdownMenuItem(
                                        text = {
                                            Column {
                                                Text(mode.displayName)
                                                Text(mode.description, style = MaterialTheme.typography.bodySmall)
                                            }
                                        },
                                        onClick = {
                                            currentRecommendationMode.value = mode
                                            preferences.edit { putString("recommendationMode", mode.key) }
                                            expanded = false
                                        },
                                    )
                                }
                            }
                        }
                    },
                )

                val isLoginForRecommendation = remember {
                    mutableStateOf(preferences.getBoolean("loginForRecommendation", true))
                }
                SettingItemWithSwitch(
                    modifier = Modifier.testTag("contentFilterSettings:loginForRecommendation"),
                    title = { Text("推荐内容时登录") },
                    description = { Text("获取推荐内容时携带登录凭证") },
                    checked = isLoginForRecommendation.value,
                    onCheckedChange = { checked ->
                        isLoginForRecommendation.value = checked
                        preferences.edit { putBoolean("loginForRecommendation", checked) }
                    },
                    settingKey = "loginForRecommendation",
                    highlightedKey = setting,
                )
            }

            val enableContentFilter = remember { mutableStateOf(preferences.getBoolean("enableContentFilter", true)) }
            SettingItemGroup {
                val enableQualityFilter = remember { mutableStateOf(preferences.getBoolean("enableQualityFilter", true)) }
                SettingItemWithSwitch(
                    title = { Text("启用质量过滤规则") },
                    description = { Text("根据赞同数、关注数等指标过滤低质量内容") },
                    checked = enableQualityFilter.value,
                    onCheckedChange = {
                        enableQualityFilter.value = it
                        preferences.edit { putBoolean("enableQualityFilter", it) }
                    },
                    settingKey = "enableQualityFilter",
                    highlightedKey = setting,
                )

                SettingItemWithSwitch(
                    modifier = Modifier.testTag("contentFilterSettings:enableContentFilter"),
                    title = { Text("启用智能内容过滤") },
                    description = { Text("自动过滤首页展示超过2次但用户未点击的内容，减少重复推荐") },
                    checked = enableContentFilter.value,
                    onCheckedChange = {
                        enableContentFilter.value = it
                        preferences.edit { putBoolean("enableContentFilter", it) }
                    },
                    settingKey = "enableContentFilter",
                    highlightedKey = setting,
                )

                val filterFollowedUserContent = remember { mutableStateOf(preferences.getBoolean("filterFollowedUserContent", false)) }
                SettingItemWithSwitch(
                    modifier = Modifier.testTag("contentFilterSettings:filterFollowedUserContent"),
                    title = { Text("过滤已关注用户内容") },
                    description = { Text("是否对已关注用户的内容也应用过滤规则。关闭此选项可确保关注用户的内容始终显示") },
                    checked = filterFollowedUserContent.value,
                    onCheckedChange = {
                        filterFollowedUserContent.value = it
                        preferences.edit { putBoolean("filterFollowedUserContent", it) }
                    },
                    enabled = enableContentFilter.value,
                    settingKey = "filterFollowedUserContent",
                    highlightedKey = setting,
                )
            }

            SettingItemGroup {
                val enableKeywordBlocking = remember { mutableStateOf(preferences.getBoolean("enableKeywordBlocking", true)) }
                SettingItemWithSwitch(
                    title = { Text("启用关键词屏蔽") },
                    description = { Text("屏蔽包含特定关键词的内容") },
                    checked = enableKeywordBlocking.value,
                    onCheckedChange = {
                        enableKeywordBlocking.value = it
                        preferences.edit { putBoolean("enableKeywordBlocking", it) }
                    },
                    settingKey = "enableKeywordBlocking",
                    highlightedKey = setting,
                )

                val enableUserBlocking = remember { mutableStateOf(preferences.getBoolean("enableUserBlocking", true)) }
                SettingItemWithSwitch(
                    title = { Text("启用用户屏蔽") },
                    description = { Text("屏蔽特定用户发布的内容") },
                    checked = enableUserBlocking.value,
                    onCheckedChange = {
                        enableUserBlocking.value = it
                        preferences.edit { putBoolean("enableUserBlocking", it) }
                    },
                    settingKey = "enableUserBlocking",
                    highlightedKey = setting,
                )

                val enableTopicBlocking = remember { mutableStateOf(preferences.getBoolean("enableTopicBlocking", true)) }
                SettingItemWithSwitch(
                    title = { Text("启用主题屏蔽") },
                    description = { Text("屏蔽包含特定主题的内容") },
                    checked = enableTopicBlocking.value,
                    onCheckedChange = {
                        enableTopicBlocking.value = it
                        preferences.edit { putBoolean("enableTopicBlocking", it) }
                    },
                    settingKey = "enableTopicBlocking",
                    highlightedKey = setting,
                )

                AnimatedVisibility(visible = enableTopicBlocking.value) {
                    val topicThreshold = remember { mutableStateOf(preferences.getInt("topicBlockingThreshold", 1)) }
                    var showThresholdDialog by remember { mutableStateOf(false) }

                    SettingItem(
                        title = { Text("主题屏蔽阈值") },
                        description = {
                            Text(
                                "当回答的问题包含 >= ${topicThreshold.value} 个被屏蔽主题时，屏蔽该内容",
                            )
                        },
                        endAction = {
                            Text(
                                topicThreshold.value.toString(),
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(horizontal = 16.dp),
                            )
                        },
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
                                TextButton(
                                    onClick = {
                                        val newThreshold = inputValue.toIntOrNull()
                                        if (newThreshold != null && newThreshold > 0) {
                                            topicThreshold.value = newThreshold
                                            preferences.edit { putInt("topicBlockingThreshold", newThreshold) }
                                            showThresholdDialog = false
                                        } else {
                                            Toast.makeText(context, "请输入大于0的整数", Toast.LENGTH_SHORT).show()
                                        }
                                    },
                                ) {
                                    Text("确定")
                                }
                            },
                            dismissButton = {
                                TextButton(onClick = { showThresholdDialog = false }) {
                                    Text("取消")
                                }
                            },
                        )
                    }
                }
            }

            SettingItemGroup {
                val blockZhihuAdPlatform = remember { mutableStateOf(preferences.getBoolean("blockZhihuAdPlatform", true)) }
                SettingItemWithSwitch(
                    title = { Text("屏蔽知乎广告平台内容") },
                    description = { Text("匹配并屏蔽包含 xg.zhihu.com 的推广内容") },
                    checked = blockZhihuAdPlatform.value,
                    onCheckedChange = {
                        blockZhihuAdPlatform.value = it
                        preferences.edit { putBoolean("blockZhihuAdPlatform", it) }
                    },
                    settingKey = "blockZhihuAdPlatform",
                    highlightedKey = setting,
                )

                val blockZhihuSchool = remember { mutableStateOf(preferences.getBoolean("blockZhihuSchool", true)) }
                SettingItemWithSwitch(
                    title = { Text("屏蔽知乎学堂内容") },
                    description = { Text("匹配并屏蔽包含 d.zhihu.com 或 data-edu-card-id 的内容") },
                    checked = blockZhihuSchool.value,
                    onCheckedChange = {
                        blockZhihuSchool.value = it
                        preferences.edit { putBoolean("blockZhihuSchool", it) }
                    },
                    settingKey = "blockZhihuSchool",
                    highlightedKey = setting,
                )

                val blockWeChatOfficialAccount = remember { mutableStateOf(preferences.getBoolean("blockWeChatOfficialAccount", true)) }
                SettingItemWithSwitch(
                    title = { Text("屏蔽微信公众号文章") },
                    description = { Text("匹配并屏蔽包含 mp.weixin.qq.com 的外链文章") },
                    checked = blockWeChatOfficialAccount.value,
                    onCheckedChange = {
                        blockWeChatOfficialAccount.value = it
                        preferences.edit { putBoolean("blockWeChatOfficialAccount", it) }
                    },
                    settingKey = "blockWeChatOfficialAccount",
                    highlightedKey = setting,
                )

                val blockPaidContent = remember { mutableStateOf(preferences.getBoolean("blockPaidContent", true)) }
                SettingItemWithSwitch(
                    title = { Text("屏蔽知乎严选付费内容") },
                    description = { Text("屏蔽知乎盐选会员专享的付费回答和文章") },
                    checked = blockPaidContent.value,
                    onCheckedChange = {
                        blockPaidContent.value = it
                        preferences.edit { putBoolean("blockPaidContent", it) }
                    },
                    settingKey = "blockPaidContent",
                    highlightedKey = setting,
                )

                val reverseBlock = remember { mutableStateOf(preferences.getBoolean("reverseBlock", false)) }
                SettingItemWithSwitch(
                    title = { Text("反向屏蔽（吃\uD83D\uDCA9模式）") },
                    description = { Text("开启后，首页将只保留广告和付费内容，屏蔽其余所有内容") },
                    checked = reverseBlock.value,
                    onCheckedChange = {
                        reverseBlock.value = it
                        preferences.edit { putBoolean("reverseBlock", it) }
                    },
                    settingKey = "reverseBlock",
                    highlightedKey = setting,
                )
            }

            SettingItemGroup {
                SettingItem(
                    modifier = Modifier.testTag("contentFilterSettings:blocklist"),
                    title = { Text("管理屏蔽列表") },
                    onClick = { navigator.onNavigate(Account.RecommendSettings.Blocklist) },
                    endAction = {
                        Icon(
                            Icons.AutoMirrored.Filled.KeyboardArrowRight,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    },
                )
            }

            SettingItemGroup {
                SettingItem(
                    modifier = Modifier.testTag("contentFilterSettings:blockedFeedHistory"),
                    title = { Text("屏蔽记录") },
                    onClick = { navigator.onNavigate(Account.RecommendSettings.BlockedFeedHistory) },
                    endAction = {
                        Icon(
                            Icons.AutoMirrored.Filled.KeyboardArrowRight,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    },
                )
            }

            // Filter Stats (Simplified)
            var filterStats by remember { mutableStateOf<FilterStats?>(null) }
            var showStatsDialog by remember { mutableStateOf(false) }

            LaunchedEffect(Unit) {
                try {
                    val contentFilterManager = ContentFilterManager.getInstance(context)
                    filterStats = contentFilterManager.getFilterStats()
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }

            SettingItemGroup {
                AnimatedVisibility(visible = enableContentFilter.value && filterStats != null) {
                    SettingItem(
                        title = { Text("过滤统计") },
                        description = {
                            Text(
                                "已累计过滤 ${filterStats?.filteredCount ?: 0} 条内容，点击查看详情",
                            )
                        },
                        onClick = { showStatsDialog = true },
                    )
                }
            }

            if (showStatsDialog && filterStats != null) {
                AlertDialog(
                    onDismissRequest = { showStatsDialog = false },
                    title = { Text("过滤统计详情") },
                    text = {
                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                            ) {
                                Text("总记录数: ${filterStats?.totalRecords}")
                                Text("过滤率: %.1f%%".format((filterStats?.filterRate ?: 0f) * 100))
                            }
                            Spacer(modifier = Modifier.height(16.dp))
                            Button(
                                onClick = {
                                    coroutineScope.launch {
                                        try {
                                            val contentFilterManager = ContentFilterManager.getInstance(context)
                                            contentFilterManager.cleanupOldData()
                                            filterStats = contentFilterManager.getFilterStats()
                                            Toast.makeText(context, "已清理过期数据", Toast.LENGTH_SHORT).show()
                                        } catch (e: Exception) {
                                            // ignore
                                        }
                                    }
                                },
                                modifier = Modifier.fillMaxWidth(),
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
                                            showStatsDialog = false
                                        } catch (e: Exception) {
                                            // ignore
                                        }
                                    }
                                },
                                modifier = Modifier.fillMaxWidth(),
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                            ) {
                                Text("重置所有数据")
                            }
                        }
                    },
                    confirmButton = {
                        TextButton(onClick = { showStatsDialog = false }) {
                            Text("关闭")
                        }
                    },
                )
            }
        }
    }
}
