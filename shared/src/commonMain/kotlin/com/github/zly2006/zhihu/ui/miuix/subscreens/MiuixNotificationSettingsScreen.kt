/*
 * Zhihu++ - Free & Ad-Free Zhihu client for Android.
 * Copyright (C) 2024-2026, zly2006 <i@zly2006.me>
 * Licensed under AGPL-3.0-only.
 */

package com.github.zly2006.zhihu.ui.miuix.subscreens

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
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
import com.github.zly2006.zhihu.shared.platform.rememberSettingsStore
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
import top.yukonga.miuix.kmp.basic.TopAppBar
import top.yukonga.miuix.kmp.blur.layerBackdrop
import top.yukonga.miuix.kmp.preference.SwitchPreference
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.utils.overScrollVertical

@Composable
fun MiuixNotificationSettingsScreen() {
    val navigator = LocalNavigator.current
    val settings = rememberSettingsStore()
    val store = rememberNotificationSettingsStore()
    val blurEnabled = remember { mutableStateOf(settings.getBoolean("blurEnabled", true)) }
    val backdrop = rememberMiuixBlurBackdrop(blurEnabled.value)
    val scrollBehavior = MiuixScrollBehavior()

    var systemNotificationSettings by remember {
        mutableStateOf(NotificationType.entries.associateWith { store.getSystemNotificationEnabled(it) })
    }
    var displayInAppSettings by remember {
        mutableStateOf(NotificationType.entries.associateWith { store.getDisplayInAppEnabled(it) })
    }
    var autoMarkAsRead by remember { mutableStateOf(store.getAutoMarkAsReadEnabled()) }
    var unreadBadgeEnabled by remember { mutableStateOf(store.getUnreadBadgeEnabled()) }

    Scaffold(
        topBar = {
            TopAppBar(
                modifier = Modifier.installerMiuixBlurEffect(backdrop),
                color = backdrop.getMiuixAppBarColor(),
                title = "通知设置",
                navigationIcon = {
                    IconButton(onClick = { navigator.onNavigateBack() }) {
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
            contentPadding = innerPadding,
        ) {
            item { Spacer(Modifier.size(12.dp)) }

            // ── 阅读行为 ──
            item { SmallTitle(text = "阅读行为") }
            item {
                Card(Modifier.padding(horizontal = 12.dp).padding(bottom = 12.dp)) {
                    SwitchPreference(
                        checked = autoMarkAsRead,
                        onCheckedChange = { checked ->
                            autoMarkAsRead = checked
                            store.setAutoMarkAsReadEnabled(checked)
                        },
                        title = "打开通知自动已读",
                        summary = "进入通知页后，自动把当前通知批次标记为已读",
                    )
                    SwitchPreference(
                        checked = unreadBadgeEnabled,
                        onCheckedChange = { checked ->
                            unreadBadgeEnabled = checked
                            store.setUnreadBadgeEnabled(checked)
                        },
                        title = "显示未读红点",
                    )
                }
            }

            // ── 系统通知 ──
            item { SmallTitle(text = "系统通知") }
            item {
                Card(Modifier.padding(horizontal = 12.dp).padding(bottom = 12.dp)) {
                    NotificationType.entries.forEach { type ->
                        SwitchPreference(
                            checked = systemNotificationSettings[type] ?: false,
                            onCheckedChange = { checked ->
                                systemNotificationSettings = systemNotificationSettings.toMutableMap().apply { put(type, checked) }
                                store.setSystemNotificationEnabled(type, checked)
                            },
                            title = type.displayName,
                        )
                    }
                }
            }

            // ── 应用内显示 ──
            item { SmallTitle(text = "应用内显示") }
            item {
                Card(Modifier.padding(horizontal = 12.dp).padding(bottom = 12.dp)) {
                    NotificationType.entries.forEach { type ->
                        SwitchPreference(
                            checked = displayInAppSettings[type] ?: true,
                            onCheckedChange = { checked ->
                                displayInAppSettings = displayInAppSettings.toMutableMap().apply { put(type, checked) }
                                store.setDisplayInAppEnabled(type, checked)
                            },
                            title = type.displayName,
                        )
                    }
                }
            }
        }
    }
}
