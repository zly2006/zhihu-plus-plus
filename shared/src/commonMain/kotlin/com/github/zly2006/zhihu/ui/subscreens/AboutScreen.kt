/*
 * Zhihu++ - Free & Ad-Free Zhihu client for Android.
 * Copyright (C) 2024-2026, zly2006 <i@zly2006.me>
 * Licensed under AGPL-3.0-only.
 */

package com.github.zly2006.zhihu.ui.subscreens

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.github.zly2006.zhihu.navigation.Account
import com.github.zly2006.zhihu.navigation.LocalNavigator
import com.github.zly2006.zhihu.shared.platform.rememberExternalUrlOpener
import com.github.zly2006.zhihu.ui.components.SettingItem
import com.github.zly2006.zhihu.ui.components.SettingItemGroup

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AboutScreen(innerPadding: PaddingValues = PaddingValues(0.dp)) {
    val navigator = LocalNavigator.current
    val openUrl = rememberExternalUrlOpener()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("关于") },
                navigationIcon = {
                    IconButton(onClick = { navigator.onNavigateBack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "返回")
                    }
                },
            )
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding).padding(innerPadding),
        ) {
            item {
                SettingItemGroup(title = "外部链接") {
                    SettingItem(
                        title = { Text("View Source") },
                        description = { Text("GitHub") },
                        onClick = { openUrl("https://github.com/zly2006/zhihu-plus-plus") },
                    )
                    SettingItem(
                        title = { Text("Join Group") },
                        description = { Text("Telegram") },
                        onClick = { openUrl("https://t.me/+_A1Yto6EpyIyODA1") },
                    )
                }
            }
            item {
                SettingItemGroup(title = "许可证") {
                    SettingItem(
                        title = { Text("License") },
                        description = { Text("AGPL-3.0") },
                        onClick = { openUrl("https://www.gnu.org/licenses/agpl-3.0.html") },
                    )
                    SettingItem(
                        title = { Text("Third Party Licenses") },
                        onClick = { navigator.onNavigate(Account.OpenSourceLicenses) },
                    )
                }
            }
            item {
                SettingItemGroup(title = "系统") {
                    SettingItem(
                        title = { Text("系统与更新") },
                        description = { Text("GitHub、更新设置等") },
                        icon = { Icon(Icons.Default.Settings, null) },
                        onClick = { navigator.onNavigate(Account.SystemAndUpdateSettings) },
                    )
                }
            }
        }
    }
}
