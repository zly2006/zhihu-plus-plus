/*
 * Zhihu++ - Free & Ad-Free Zhihu client for Android.
 * Copyright (C) 2024-2026, zly2006 <i@zly2006.me>
 *
 * Licensed under AGPL-3.0-only.
 */

package com.github.zly2006.zhihu.theme

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.movableContentOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.github.zly2006.zhihu.shared.theme.ThemeMode
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

    CompositionLocalProvider(LocalThemeStyle provides style) {
        when (style) {
            ThemeStyle.Material3 -> ZhihuMaterialTheme {
                preserved(content)
            }
            ThemeStyle.Miuix -> ZhihuMiuixTheme {
                preserved(content)
            }
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
    val useDynamicColor = ThemeManager.getUseDynamicColor()
    val customBackgroundColor = ThemeManager.getBackgroundColor()
    val darkTheme = ThemeManager.isDarkTheme()
    val platformDynamicColorScheme = platformDynamicColorScheme(darkTheme)

    val baseColorScheme = when {
        useDynamicColor && platformDynamicColorScheme != null -> platformDynamicColorScheme
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

    PlatformSystemBarEffect(darkTheme)

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
    // 平台是否支持动态取色（Android 12+ 返回非 null），替代原先的 Build.VERSION 判断
    val monetAvailable = platformDynamicColorScheme(darkTheme) != null

    PlatformSystemBarEffect(darkTheme)

    val controller = remember(themeMode, darkTheme, useDynamicColor, customColor, monetAvailable) {
        // 如果开了动态颜色（平台支持 Monet），走 Monet；否则走自定义 seed
        if (useDynamicColor && monetAvailable) {
            ThemeController(
                colorSchemeMode = when (themeMode) {
                    ThemeMode.SYSTEM -> ColorSchemeMode.MonetSystem
                    ThemeMode.LIGHT -> ColorSchemeMode.MonetLight
                    ThemeMode.DARK -> ColorSchemeMode.MonetDark
                },
                isDark = darkTheme,
            )
        } else {
            // 标准模式：传 keyColor 让 miuix 用自定义主题色生成调色板（与 M3 端自定义色对齐）。
            ThemeController(
                colorSchemeMode = when (themeMode) {
                    ThemeMode.SYSTEM -> ColorSchemeMode.System
                    ThemeMode.LIGHT -> ColorSchemeMode.Light
                    ThemeMode.DARK -> ColorSchemeMode.Dark
                },
                keyColor = customColor,
            )
        }
    }

    MiuixTheme(
        controller = controller,
    ) {
        // miuix 默认背景跟 M3 不一样，套一层 Box 给整个 app 一个 surface 底。
        // 用自定义背景色（与 M3 端一致），让 overscroll 露出的底色也跟随主题设置。
        val bgColor = ThemeManager.getBackgroundColor()
        // miuix 页面里仍复用一批 M3 组件（overscroll、评论、答案预览卡、markdown 渲染等），
        // 它们取 MaterialTheme.colorScheme。若不显式提供，就会落到 M3 baseline 的紫/粉默认色，
        // 导致转场/overscroll 露底、预览卡等处发粉。这里把 miuix 调色板映射成一套 M3 colorScheme 兜底。
        val mc = MiuixTheme.colorScheme
        val m3Base = if (darkTheme) darkColorScheme() else lightColorScheme()
        val m3Scheme = m3Base.copy(
            primary = mc.primary,
            onPrimary = mc.onPrimary,
            primaryContainer = mc.primaryContainer,
            onPrimaryContainer = mc.onPrimaryContainer,
            secondary = mc.secondary,
            secondaryContainer = mc.secondaryContainer,
            onSecondaryContainer = mc.onSecondaryContainer,
            tertiaryContainer = mc.tertiaryContainer,
            onTertiaryContainer = mc.onTertiaryContainer,
            background = bgColor,
            onBackground = mc.onBackground,
            surface = bgColor,
            onSurface = mc.onSurface,
            surfaceVariant = mc.surfaceVariant,
            onSurfaceVariant = mc.onSurfaceSecondary,
            surfaceContainer = mc.surfaceContainer,
            surfaceContainerHigh = mc.surfaceContainerHigh,
            surfaceContainerHighest = mc.surfaceContainerHighest,
            outline = mc.outline,
            error = mc.error,
            errorContainer = mc.errorContainer,
            onErrorContainer = mc.onErrorContainer,
        )
        MaterialTheme(
            colorScheme = m3Scheme,
            typography = Typography,
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(bgColor),
            ) {
                content()
            }
        }
    }
}

@Composable
expect fun currentSystemInDarkTheme(): Boolean

@Composable
expect fun platformDynamicColorScheme(darkTheme: Boolean): ColorScheme?

@Composable
expect fun PlatformSystemBarEffect(darkTheme: Boolean)
