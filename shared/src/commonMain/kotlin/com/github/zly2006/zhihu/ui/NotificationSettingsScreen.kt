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

package com.github.zly2006.zhihu.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.unit.dp
import com.github.zly2006.zhihu.navigation.LocalNavigator
import com.github.zly2006.zhihu.shared.notification.NotificationType
import com.github.zly2006.zhihu.shared.notification.rememberNotificationSettingsStore
import com.github.zly2006.zhihu.ui.components.SettingItemGroup
import com.github.zly2006.zhihu.ui.components.SettingItemWithSwitch
import com.github.zly2006.zhihu.shared.notification.matchNotificationType as sharedMatchNotificationType

object NotificationPreferences {
    fun matchNotificationType(verb: String): NotificationType? = sharedMatchNotificationType(verb)
}

/**
 * 通知设置页。
 *
 * 页面分为阅读行为、系统通知和应用内显示三组：自动已读控制进入通知页后的处理方式，系统通知控制是否向 OS 发通知，
 * 应用内显示控制通知中心是否展示某类消息。这里使用 [NotificationSettingsStore]，不要和普通偏好设置 key 混用。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationSettingsScreen() {
    val navigator = LocalNavigator.current
    val settingsStore = rememberNotificationSettingsStore()
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    var systemNotificationSettings by remember {
        mutableStateOf(
            NotificationType.entries.associateWith {
                settingsStore.getSystemNotificationEnabled(it)
            },
        )
    }

    var displayInAppSettings by remember {
        mutableStateOf(
            NotificationType.entries.associateWith {
                settingsStore.getDisplayInAppEnabled(it)
            },
        )
    }
    var autoMarkAsRead by remember { mutableStateOf(settingsStore.getAutoMarkAsReadEnabled()) }

    Scaffold(
        modifier = Modifier.fillMaxSize().nestedScroll(scrollBehavior.nestedScrollConnection),
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
        topBar = {
            LargeTopAppBar(
                title = { Text("通知设置") },
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
                .verticalScroll(rememberScrollState())
                .padding(innerPadding)
                .padding(vertical = 16.dp),
        ) {
            SettingItemGroup(
                title = "阅读行为",
                footer = { Text("进入通知页后，自动把当前通知批次标记为已读") },
            ) {
                SettingItemWithSwitch(
                    title = { Text("打开通知自动已读") },
                    checked = autoMarkAsRead,
                    onCheckedChange = { checked ->
                        autoMarkAsRead = checked
                        settingsStore.setAutoMarkAsReadEnabled(checked)
                    },
                )
            }

            SettingItemGroup(title = "系统通知") {
                NotificationType.entries.forEach { type ->
                    SettingItemWithSwitch(
                        title = { Text(type.displayName) },
                        checked = systemNotificationSettings[type] ?: false,
                        onCheckedChange = { checked ->
                            systemNotificationSettings = systemNotificationSettings.toMutableMap().apply {
                                put(type, checked)
                            }
                            settingsStore.setSystemNotificationEnabled(type, checked)
                        },
                    )
                }
            }

            SettingItemGroup(
                title = "应用内显示",
                footer = { Text("选择在通知页面显示哪些通知") },
            ) {
                NotificationType.entries.forEach { type ->
                    SettingItemWithSwitch(
                        title = { Text(type.displayName) },
                        checked = displayInAppSettings[type] ?: true,
                        onCheckedChange = { checked ->
                            displayInAppSettings = displayInAppSettings.toMutableMap().apply {
                                put(type, checked)
                            }
                            settingsStore.setDisplayInAppEnabled(type, checked)
                        },
                    )
                }
            }
        }
    }
}
