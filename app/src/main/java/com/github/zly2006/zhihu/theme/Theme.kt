/*
 * Zhihu++ - Free & Ad-Free Zhihu client for Android.
 * Copyright (C) 2024-2026, zly2006 <i@zly2006.me>
 *
 * Licensed under AGPL-3.0-only.
 */

package com.github.zly2006.zhihu.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.movableContentOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import com.materialkolor.dynamicColorScheme
import top.yukonga.miuix.kmp.theme.ColorSchemeMode
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.theme.ThemeController

// ============================================================
// 顶层主题包装：根据 ThemeManager.getThemeStyle() 分流到 M3 或 miuix
//
// 这是 InstallerX-Revived 的核心模式：
//   1) 用 movableContentOf 包 content，切换主题时子树不会重建，
//      ViewModel/滚动位置/动画状态全部保留
//   2) 两个内部主题各自处理状态栏图标颜色（WindowCompat），因为
//      系统栏外观是 Window 层的事，不是 Compose 主题接口
//   3) Box 套底色 = 整个 app 的根背景；M3 和 miuix 的 surface 概念
//      不完全对齐，分别取各自的合适值
// ============================================================

@Composable
fun ZhihuTheme(
    content: @Composable () -> Unit,
) {
    val style = ThemeManager.getThemeStyle()

    // movableContentOf：切换 style 时 content 不重建。
    val preserved = remember {
        movableContentOf<@Composable () -> Unit> { it() }
    }

    when (style) {
        ThemeStyle.Material3 -> ZhihuMaterialTheme {
            preserved(content)
        }
        ThemeStyle.Miuix -> ZhihuMiuixTheme {
            preserved(content)
        }
    }
}

// ------------------------------------------------------------
// Material 3 实现 —— 等同于改造前的 ZhihuTheme，只是改名 & 抽出
// ------------------------------------------------------------

private val DarkColorScheme = darkColorScheme(
    primary = Purple80,
    secondary = PurpleGrey80,
    tertiary = Pink80,
)

private val LightColorScheme = lightColorScheme(
    primary = Purple40,
    secondary = PurpleGrey40,
    tertiary = Pink40,
    background = Color(0xFFFFFBFE),
    surface = Color(0xFFFFFBFE),
    onPrimary = Color.White,
    onSecondary = Color.White,
    onTertiary = Color.White,
    onBackground = Color(0xFF1C1B1F),
    onSurface = Color(0xFF1C1B1F),
)

@Composable
fun ZhihuMaterialTheme(
    content: @Composable () -> Unit,
) {
    val context = LocalContext.current
    val useDynamicColor = ThemeManager.getUseDynamicColor()
    val customBackgroundColor = ThemeManager.getBackgroundColor()
    val darkTheme = ThemeManager.isDarkTheme()

    val baseColorScheme = when {
        useDynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        !useDynamicColor -> {
            dynamicColorScheme(
                seedColor = ThemeManager.getCustomColor(),
                isDark = darkTheme,
                isAmoled = false,
            )
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    val colorScheme = baseColorScheme.copy(
        background = customBackgroundColor,
        surface = customBackgroundColor,
    )

    SyncSystemBarsAppearance(darkTheme = darkTheme)

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content,
    )
}

// ------------------------------------------------------------
// miuix 实现
// ------------------------------------------------------------

@Composable
fun ZhihuMiuixTheme(
    content: @Composable () -> Unit,
) {
    val darkTheme = ThemeManager.isDarkTheme()
    val useDynamicColor = ThemeManager.getUseDynamicColor()
    val customColor = ThemeManager.getCustomColor()
    val themeMode = ThemeManager.getThemeMode()

    SyncSystemBarsAppearance(darkTheme = darkTheme)

    val controller = remember(themeMode, darkTheme, useDynamicColor, customColor) {
        // 如果开了动态颜色（Android 12+），走 Monet；否则走自定义 seed
        if (useDynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            ThemeController(
                colorSchemeMode = when (themeMode) {
                    ThemeMode.SYSTEM -> ColorSchemeMode.MonetSystem
                    ThemeMode.LIGHT -> ColorSchemeMode.MonetLight
                    ThemeMode.DARK -> ColorSchemeMode.MonetDark
                },
                isDark = darkTheme,
            )
        } else {
            ThemeController(
                colorSchemeMode = when (themeMode) {
                    ThemeMode.SYSTEM -> ColorSchemeMode.System
                    ThemeMode.LIGHT -> ColorSchemeMode.Light
                    ThemeMode.DARK -> ColorSchemeMode.Dark
                },
                keyColor = customColor,
                isDark = darkTheme,
            )
        }
    }

    MiuixTheme(
        controller = controller,
    ) {
        // miuix 默认背景跟 M3 不一样，套一层 Box 给整个 app 一个 surface 底
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MiuixTheme.colorScheme.background),
        ) {
            content()
        }
    }
}

// ------------------------------------------------------------
// 共用：状态栏 / 导航栏 图标颜色同步
// ------------------------------------------------------------

@Composable
private fun SyncSystemBarsAppearance(darkTheme: Boolean) {
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window ?: return@SideEffect
            WindowCompat.getInsetsController(window, view).apply {
                isAppearanceLightStatusBars = !darkTheme
                isAppearanceLightNavigationBars = !darkTheme
            }
        }
    }
}
