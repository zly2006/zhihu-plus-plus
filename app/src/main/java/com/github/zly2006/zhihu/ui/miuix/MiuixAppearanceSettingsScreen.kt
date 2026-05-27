/*
 * Zhihu++ - Free & Ad-Free Zhihu client for Android.
 * Copyright (C) 2024-2026, zly2006 <i@zly2006.me>
 *
 * Licensed under AGPL-3.0-only.
 */

package com.github.zly2006.zhihu.ui.miuix

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.github.zly2006.zhihu.navigation.LocalNavigator
import com.github.zly2006.zhihu.theme.ThemeManager
import com.github.zly2006.zhihu.theme.ThemeMode
import com.github.zly2006.zhihu.theme.ThemeStyle
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.SmallTitle
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TopAppBar
import top.yukonga.miuix.kmp.preference.ArrowPreference
import top.yukonga.miuix.kmp.preference.SwitchPreference
import top.yukonga.miuix.kmp.theme.MiuixTheme

/**
 * miuix 风格的外观设置页。
 *
 * 跟 [com.github.zly2006.zhihu.ui.subscreens.AppearanceSettingsScreen] 语义对等，
 * 但 UI 用 miuix 风格。
 *
 * @param setting 跳过来时要滚到的设置项 key（暂未实现高亮，签名兼容用）
 * @param onExit  退出回调，跟 M3 版同签名
 */
@Composable
fun MiuixAppearanceSettingsScreen(
    @Suppress("UNUSED_PARAMETER") setting: String = "",
    onExit: () -> Unit = {},
) {
    val context = LocalContext.current
    val navigator = LocalNavigator.current

    val themeStyle = ThemeManager.getThemeStyle()
    val themeMode = ThemeManager.getThemeMode()
    val useDynamicColor = ThemeManager.getUseDynamicColor()

    var themeModePickerExpanded by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = "外观",
                navigationIcon = {
                    IconButton(
                        onClick = {
                            onExit()
                            navigator.onNavigateBack()
                        },
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "返回",
                            tint = MiuixTheme.colorScheme.onBackground,
                        )
                    }
                },
            )
        },
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = innerPadding,
        ) {
            // ===== UI 风格 =====
            item { SmallTitle(text = "界面风格") }
            item {
                SwitchPreference(
                    checked = themeStyle == ThemeStyle.Miuix,
                    onCheckedChange = { useMiuix ->
                        ThemeManager.setThemeStyle(
                            context,
                            if (useMiuix) ThemeStyle.Miuix else ThemeStyle.Material3,
                        )
                    },
                    title = "使用 miuix 风格",
                    summary = "类 HyperOS 视觉，可随时切回 Material 3",
                )
            }
            item { Spacer(Modifier.height(12.dp)) }

            // ===== 深浅色 =====
            item { SmallTitle(text = "深色模式") }
            item {
                ArrowPreference(
                    title = "模式",
                    summary = when (themeMode) {
                        ThemeMode.LIGHT -> "始终浅色"
                        ThemeMode.DARK -> "始终深色"
                        ThemeMode.SYSTEM -> "跟随系统"
                    },
                    onClick = { themeModePickerExpanded = !themeModePickerExpanded },
                )
            }
            if (themeModePickerExpanded) {
                items(ThemeMode.entries) { mode ->
                    ArrowPreference(
                        title = when (mode) {
                            ThemeMode.LIGHT -> "  · 始终浅色"
                            ThemeMode.DARK -> "  · 始终深色"
                            ThemeMode.SYSTEM -> "  · 跟随系统"
                        },
                        summary = if (mode == themeMode) "当前" else null,
                        onClick = {
                            ThemeManager.setThemeMode(context, mode)
                            themeModePickerExpanded = false
                        },
                    )
                }
            }
            item { Spacer(Modifier.height(12.dp)) }

            // ===== 动态颜色 =====
            item { SmallTitle(text = "颜色") }
            item {
                SwitchPreference(
                    checked = useDynamicColor,
                    onCheckedChange = { ThemeManager.setUseDynamicColor(context, it) },
                    title = "动态颜色",
                    summary = "跟随系统取色（Android 12 及以上）",
                )
            }

            // 占位提示
            item { Spacer(Modifier.height(24.dp)) }
            item {
                Text(
                    text = "TODO：把原 AppearanceSettingsScreen 里的字体大小、" +
                            "底部栏按钮选择、自定义背景色等选项补到这里。",
                    style = MiuixTheme.textStyles.paragraph,
                    color = MiuixTheme.colorScheme.onBackgroundVariant,
                    modifier = Modifier.padding(horizontal = 24.dp),
                )
            }
            item { Spacer(Modifier.height(24.dp)) }
        }
    }
}
