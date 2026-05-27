/*
 * Zhihu++ - Free & Ad-Free Zhihu client for Android.
 * Copyright (C) 2024-2026, zly2006 <i@zly2006.me>
 *
 * Licensed under AGPL-3.0-only.
 */

package com.github.zly2006.zhihu.ui.miuix

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Back
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.github.zly2006.zhihu.navigation.LocalNavigator
import com.github.zly2006.zhihu.theme.ThemeManager
import com.github.zly2006.zhihu.theme.ThemeMode
import com.github.zly2006.zhihu.theme.ThemeStyle
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.DropdownItem
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.SmallTitle
import top.yukonga.miuix.kmp.basic.TopAppBar
import top.yukonga.miuix.kmp.preference.SwitchPreference
import top.yukonga.miuix.kmp.preference.WindowSpinnerPreference
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.utils.overScrollVertical

/**
 * miuix 风格的外观设置页 —— 完整版，对齐 InstallerX-Revived 的视觉规范。
 *
 * 布局规范（每个分组）：
 *   item { Spacer(12.dp) } 或上一组的 Card bottom padding
 *   item { SmallTitle("分组名") }
 *   item {
 *       Card(Modifier.padding(horizontal = 12.dp).padding(bottom = 12.dp)) {
 *           [一组 preference 项，组件间细分隔线由 miuix 自动绘制]
 *       }
 *   }
 *
 * TopAppBar 通过 [MiuixScrollBehavior] 实现 HyperOS 标志性的"大字标题折叠"效果。
 *
 * @param setting 高亮跳转用的设置 key（兼容 M3 端签名）
 * @param onExit  退出回调
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

    val scrollBehavior = MiuixScrollBehavior()

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
                            imageVector = MiuixIcons.Back,
                            contentDescription = "返回",
                            tint = MiuixTheme.colorScheme.onBackground,
                        )
                    }
                },
                scrollBehavior = scrollBehavior,
            )
        },
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .overScrollVertical()
                .nestedScroll(scrollBehavior.nestedScrollConnection),
            contentPadding = innerPadding,
        ) {
            // ─── 顶部留白 ────────────────────────────────────────────
            item { Spacer(Modifier.size(12.dp)) }

            // ─── 分组 1：界面风格 ─────────────────────────────────────
            item { SmallTitle(text = "界面风格") }
            item {
                Card(
                    modifier = Modifier
                        .padding(horizontal = 12.dp)
                        .padding(bottom = 12.dp),
                ) {
                    UiEngineSpinner(
                        currentStyle = themeStyle,
                        onStyleChange = { ThemeManager.setThemeStyle(context, it) },
                    )
                }
            }

            // ─── 分组 2：主题 ────────────────────────────────────────
            item { SmallTitle(text = "主题") }
            item {
                Card(
                    modifier = Modifier
                        .padding(horizontal = 12.dp)
                        .padding(bottom = 12.dp),
                ) {
                    ThemeModeSpinner(
                        currentThemeMode = themeMode,
                        onThemeModeChange = { ThemeManager.setThemeMode(context, it) },
                    )
                    SwitchPreference(
                        checked = useDynamicColor,
                        onCheckedChange = { ThemeManager.setUseDynamicColor(context, it) },
                        title = "动态颜色",
                        summary = "跟随系统取色（Android 12 及以上）",
                    )
                    // TODO: 自定义颜色 SuperArrow（在 useDynamicColor=false 时启用）
                    // TODO: 自定义背景色 SuperArrow
                }
            }

            // ─── 后续分组（TODO） ─────────────────────────────────────
            // item { SmallTitle(text = "字体") }
            // item {
            //     Card(modifier = Modifier.padding(horizontal = 12.dp).padding(bottom = 12.dp)) {
            //         FontSizeSpinner(...)
            //     }
            // }
            //
            // item { SmallTitle(text = "底部栏") }
            // item {
            //     Card(modifier = Modifier.padding(horizontal = 12.dp).padding(bottom = 12.dp)) {
            //         // 底部栏按钮选择
            //     }
            // }
        }
    }
}

// ────────────────────────────────────────────────────────────────────
// Spinner 小组件 — 按 InstallerX MiuixThemeModeWidget 的模式实现
// ────────────────────────────────────────────────────────────────────

@Composable
private fun UiEngineSpinner(
    currentStyle: ThemeStyle,
    onStyleChange: (ThemeStyle) -> Unit,
) {
    val options = remember {
        mapOf(
            ThemeStyle.Material3 to "Material 3",
            ThemeStyle.Miuix to "Miuix",
        )
    }
    val items = remember(options) {
        options.values.map { DropdownItem(title = it) }
    }
    val selectedIndex = remember(currentStyle, options) {
        options.keys.indexOf(currentStyle).coerceAtLeast(0)
    }

    WindowSpinnerPreference(
        title = "UI 引擎",
        summary = "切换应用整体的视觉风格",
        items = items,
        selectedIndex = selectedIndex,
        onSelectedIndexChange = { newIndex ->
            val newStyle = options.keys.elementAt(newIndex)
            if (newStyle != currentStyle) onStyleChange(newStyle)
        },
    )
}

@Composable
private fun ThemeModeSpinner(
    currentThemeMode: ThemeMode,
    onThemeModeChange: (ThemeMode) -> Unit,
) {
    val options = remember {
        mapOf(
            ThemeMode.LIGHT to "浅色主题",
            ThemeMode.DARK to "深色主题",
            ThemeMode.SYSTEM to "跟随系统主题",
        )
    }
    val items = remember(options) {
        options.values.map { DropdownItem(title = it) }
    }
    val selectedIndex = remember(currentThemeMode, options) {
        options.keys.indexOf(currentThemeMode).coerceAtLeast(0)
    }

    WindowSpinnerPreference(
        title = "主题模式",
        items = items,
        selectedIndex = selectedIndex,
        onSelectedIndexChange = { newIndex ->
            val newMode = options.keys.elementAt(newIndex)
            if (newMode != currentThemeMode) onThemeModeChange(newMode)
        },
    )
}
