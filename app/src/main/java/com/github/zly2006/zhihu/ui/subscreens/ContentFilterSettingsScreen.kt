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
import com.github.zly2006.zhihu.R
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
                title = { Text(context.getString(R.string.recommend_and_filter)) },
                navigationIcon = {
                    IconButton(
                        onClick = navigator.onNavigateBack,
                        colors = IconButtonDefaults.iconButtonColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant,
                            contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        ),
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = context.getString(R.string.back))
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
                    title = { Text(context.getString(R.string.recommendation_algorithm)) },
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
                                value = context.getString(currentRecommendationMode.value.displayNameResId),
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
                                                Text(context.getString(mode.displayNameResId))
                                                Text(context.getString(mode.descriptionResId), style = MaterialTheme.typography.bodySmall)
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
                    title = { Text(context.getString(R.string.login_for_recommendation)) },
                    description = { Text(context.getString(R.string.login_for_recommendation_desc)) },
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
                    title = { Text(context.getString(R.string.enable_quality_filter)) },
                    description = { Text(context.getString(R.string.enable_quality_filter_desc)) },
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
                    title = { Text(context.getString(R.string.enable_smart_content_filter)) },
                    description = { Text(context.getString(R.string.enable_smart_content_filter_desc)) },
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
                    title = { Text(context.getString(R.string.filter_followed_user_content)) },
                    description = { Text(context.getString(R.string.filter_followed_user_content_desc)) },
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
                    title = { Text(context.getString(R.string.enable_keyword_blocking)) },
                    description = { Text(context.getString(R.string.enable_keyword_blocking_desc)) },
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
                    title = { Text(context.getString(R.string.enable_user_blocking)) },
                    description = { Text(context.getString(R.string.enable_user_blocking_desc)) },
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
                    title = { Text(context.getString(R.string.enable_topic_blocking)) },
                    description = { Text(context.getString(R.string.enable_topic_blocking_desc)) },
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
                        title = { Text(context.getString(R.string.topic_blocking_threshold)) },
                        description = {
                            Text(
                                context.getString(R.string.topic_blocking_threshold_desc, topicThreshold.value),
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
                            title = { Text(context.getString(R.string.set_topic_blocking_threshold)) },
                            text = {
                                Column {
                                    Text(context.getString(R.string.topic_blocking_threshold_dialog_desc))
                                    Spacer(modifier = Modifier.height(16.dp))
                                    OutlinedTextField(
                                        value = inputValue,
                                        onValueChange = { inputValue = it },
                                        label = { Text(context.getString(R.string.threshold)) },
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
                                            Toast.makeText(context, context.getString(R.string.positive_integer_required), Toast.LENGTH_SHORT).show()
                                        }
                                    },
                                ) {
                                    Text(context.getString(R.string.ok))
                                }
                            },
                            dismissButton = {
                                TextButton(onClick = { showThresholdDialog = false }) {
                                    Text(context.getString(R.string.cancel))
                                }
                            },
                        )
                    }
                }
            }

            SettingItemGroup {
                val blockZhihuAdPlatform = remember { mutableStateOf(preferences.getBoolean("blockZhihuAdPlatform", true)) }
                SettingItemWithSwitch(
                    title = { Text(context.getString(R.string.block_zhihu_ad_platform)) },
                    description = { Text(context.getString(R.string.block_zhihu_ad_platform_desc)) },
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
                    title = { Text(context.getString(R.string.block_zhihu_school)) },
                    description = { Text(context.getString(R.string.block_zhihu_school_desc)) },
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
                    title = { Text(context.getString(R.string.block_wechat_official_account)) },
                    description = { Text(context.getString(R.string.block_wechat_official_account_desc)) },
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
                    title = { Text(context.getString(R.string.block_paid_content)) },
                    description = { Text(context.getString(R.string.block_paid_content_desc)) },
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
                    title = { Text(context.getString(R.string.reverse_block)) },
                    description = { Text(context.getString(R.string.reverse_block_desc)) },
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
                    title = { Text(context.getString(R.string.manage_blocklist)) },
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
                    title = { Text(context.getString(R.string.blocked_feed_history)) },
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
                        title = { Text(context.getString(R.string.filter_stats)) },
                        description = {
                            Text(
                                context.getString(R.string.filter_stats_summary, filterStats?.filteredCount ?: 0),
                            )
                        },
                        onClick = { showStatsDialog = true },
                    )
                }
            }

            if (showStatsDialog && filterStats != null) {
                AlertDialog(
                    onDismissRequest = { showStatsDialog = false },
                    title = { Text(context.getString(R.string.filter_stats_details)) },
                    text = {
                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                            ) {
                                Text(context.getString(R.string.total_records, filterStats?.totalRecords ?: 0))
                                Text(context.getString(R.string.filter_rate, (filterStats?.filterRate ?: 0f) * 100))
                            }
                            Spacer(modifier = Modifier.height(16.dp))
                            Button(
                                onClick = {
                                    coroutineScope.launch {
                                        try {
                                            val contentFilterManager = ContentFilterManager.getInstance(context)
                                            contentFilterManager.cleanupOldData()
                                            filterStats = contentFilterManager.getFilterStats()
                                            Toast.makeText(context, context.getString(R.string.old_data_cleaned), Toast.LENGTH_SHORT).show()
                                        } catch (e: Exception) {
                                            // ignore
                                        }
                                    }
                                },
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                Text(context.getString(R.string.cleanup_old_data))
                            }
                            Button(
                                onClick = {
                                    coroutineScope.launch {
                                        try {
                                            val contentFilterManager = ContentFilterManager.getInstance(context)
                                            contentFilterManager.clearAllData()
                                            filterStats = contentFilterManager.getFilterStats()
                                            Toast.makeText(context, context.getString(R.string.all_data_reset), Toast.LENGTH_SHORT).show()
                                            showStatsDialog = false
                                        } catch (e: Exception) {
                                            // ignore
                                        }
                                    }
                                },
                                modifier = Modifier.fillMaxWidth(),
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                            ) {
                                Text(context.getString(R.string.reset_all_data))
                            }
                        }
                    },
                    confirmButton = {
                        TextButton(onClick = { showStatsDialog = false }) {
                            Text(context.getString(R.string.close))
                        }
                    },
                )
            }
        }
    }
}
