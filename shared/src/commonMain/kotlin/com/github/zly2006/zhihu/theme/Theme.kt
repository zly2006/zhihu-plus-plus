/*
 * Zhihu++ - Free & Ad-Free Zhihu client for all platforms.
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

package com.github.zly2006.zhihu.theme

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.movableContentOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.github.zly2006.zhihu.platform.rememberSettingsStore
import com.materialkolor.dynamicColorScheme
import top.yukonga.miuix.kmp.theme.ColorSchemeMode
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.theme.ThemeController

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
fun ZhihuTheme(
    content: @Composable () -> Unit,
) {
    val settings = rememberSettingsStore()
    remember(settings) {
        ThemeManager.syncUiStyleIfUninitialized(
            settings.getString(UI_STYLE_PREFERENCE_KEY, UiStyle.Material3.prefValue),
        )
    }
    val effectiveStyle = ThemeManager.getUiStyle().resolveEffective()
    val darkTheme = ThemeManager.isDarkTheme()

    PlatformSystemBarEffect(darkTheme)

    // movableContentOf：切换 UI 风格时保持内容子树与导航栈、滚动位置、输入草稿不重建。
    val preserved = remember {
        movableContentOf<@Composable () -> Unit> { it() }
    }

    CompositionLocalProvider(LocalUiStyle provides effectiveStyle) {
        when (effectiveStyle) {
            UiStyle.Material3 -> ZhihuMaterialTheme(darkTheme = darkTheme) {
                preserved(content)
            }
            UiStyle.Miuix -> ZhihuMiuixTheme(darkTheme = darkTheme) {
                preserved(content)
            }
            UiStyle.LiquidGlass -> ZhihuLiquidGlassTheme(darkTheme = darkTheme) {
                preserved(content)
            }
        }
    }
}

@Composable
private fun ZhihuMaterialTheme(
    darkTheme: Boolean,
    content: @Composable () -> Unit,
) {
    val useDynamicColor = ThemeManager.getUseDynamicColor()
    val customBackgroundColor = ThemeManager.getBackgroundColor()
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

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        shapes = Shapes(),
        content = content,
    )
}

@Composable
private fun ZhihuMiuixTheme(
    darkTheme: Boolean,
    content: @Composable () -> Unit,
) {
    val useDynamicColor = ThemeManager.getUseDynamicColor()
    val customColor = ThemeManager.getCustomColor()
    val themeMode = ThemeManager.getThemeMode()
    val monetAvailable = platformDynamicColorScheme(darkTheme) != null

    val controller = remember(themeMode, darkTheme, useDynamicColor, customColor, monetAvailable) {
        val monetMode = when (themeMode) {
            ThemeMode.SYSTEM -> ColorSchemeMode.MonetSystem
            ThemeMode.LIGHT -> ColorSchemeMode.MonetLight
            ThemeMode.DARK -> ColorSchemeMode.MonetDark
        }
        when {
            useDynamicColor && monetAvailable -> ThemeController(
                colorSchemeMode = monetMode,
                isDark = darkTheme,
            )
            !useDynamicColor -> ThemeController(
                colorSchemeMode = monetMode,
                keyColor = customColor,
                isDark = darkTheme,
            )
            else -> ThemeController(
                colorSchemeMode = when (themeMode) {
                    ThemeMode.SYSTEM -> ColorSchemeMode.System
                    ThemeMode.LIGHT -> ColorSchemeMode.Light
                    ThemeMode.DARK -> ColorSchemeMode.Dark
                },
                isDark = darkTheme,
            )
        }
    }

    MiuixTheme(controller = controller) {
        val mc = MiuixTheme.colorScheme
        val m3Base = if (darkTheme) darkColorScheme() else lightColorScheme()
        // 为尚未特化的 M3 组件、Markdown 渲染器和第三方库提供与 Miuix 一致的 ColorScheme 兜底。
        val fallbackColorScheme = m3Base.copy(
            primary = mc.primary,
            onPrimary = mc.onPrimary,
            primaryContainer = mc.primaryContainer,
            onPrimaryContainer = mc.onPrimaryContainer,
            secondary = mc.secondary,
            onSecondary = mc.onSecondaryVariant,
            secondaryContainer = mc.secondaryContainer,
            onSecondaryContainer = mc.onSecondaryVariant,
            tertiary = mc.primary,
            onTertiary = mc.onPrimary,
            tertiaryContainer = mc.tertiaryContainer,
            onTertiaryContainer = mc.onTertiaryContainer,
            background = mc.surface,
            onBackground = mc.onSurface,
            surface = mc.surface,
            onSurface = mc.onSurface,
            surfaceBright = mc.surfaceContainer,
            surfaceDim = mc.surface,
            surfaceVariant = mc.surfaceVariant,
            onSurfaceVariant = mc.onSurfaceSecondary,
            surfaceContainer = mc.surfaceContainer,
            surfaceContainerLow = mc.surface,
            surfaceContainerLowest = mc.surface,
            surfaceContainerHigh = mc.surfaceContainerHigh,
            surfaceContainerHighest = mc.surfaceContainerHighest,
            surfaceTint = Color.Transparent,
            scrim = Color.Black.copy(alpha = 0.32f),
            inversePrimary = mc.primary,
            inverseSurface = mc.onSurface,
            inverseOnSurface = mc.surface,
            outline = mc.outline,
            outlineVariant = mc.outline,
            error = mc.error,
            onError = mc.onError,
            errorContainer = mc.errorContainer,
            onErrorContainer = mc.onErrorContainer,
        )
        MaterialTheme(
            colorScheme = fallbackColorScheme,
            typography = Typography,
            shapes = Shapes(),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(mc.surface),
            ) {
                content()
            }
        }
    }
}

@Composable
private fun ZhihuLiquidGlassTheme(
    darkTheme: Boolean,
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = liquidGlassColors(darkTheme),
        typography = LiquidGlassTypography,
        shapes = LiquidGlassShapes,
        content = content,
    )
}

private fun liquidGlassColors(dark: Boolean): ColorScheme {
    val background = if (dark) Color.Black else Color(0xFFF2F2F7)
    val surface = if (dark) Color(0xFF1C1C1E) else Color.White
    val ink = if (dark) Color.White else Color.Black
    val secondaryInk = if (dark) Color(0x99EBEBF5) else Color(0x992F2F35)
    val blue = if (dark) Color(0xFF0091FF) else Color(0xFF0088FF)
    val red = if (dark) Color(0xFFFF4245) else Color(0xFFFF383C)
    val separator = if (dark) Color(0x80545458) else Color(0x1F2F2F35)
    return (if (dark) darkColorScheme() else lightColorScheme()).copy(
        primary = blue,
        onPrimary = Color.White,
        primaryContainer = blue.copy(alpha = if (dark) 0.24f else 0.16f),
        onPrimaryContainer = ink,
        secondary = secondaryInk,
        onSecondary = surface,
        secondaryContainer = if (dark) Color(0xFF2C2C2E) else Color(0xFFE8E8ED),
        onSecondaryContainer = ink,
        tertiary = blue,
        onTertiary = Color.White,
        tertiaryContainer = blue.copy(alpha = if (dark) 0.20f else 0.12f),
        onTertiaryContainer = ink,
        error = red,
        onError = Color.White,
        errorContainer = red.copy(alpha = if (dark) 0.24f else 0.14f),
        onErrorContainer = red,
        inversePrimary = if (dark) Color(0xFF0088FF) else Color(0xFF0091FF),
        background = background,
        onBackground = ink,
        surface = background,
        onSurface = ink,
        surfaceBright = surface,
        surfaceDim = background,
        surfaceContainer = background,
        surfaceContainerLow = surface,
        surfaceContainerLowest = surface,
        surfaceContainerHigh = surface,
        surfaceContainerHighest = if (dark) Color(0xFF2C2C2E) else Color(0xFFE8E8ED),
        surfaceVariant = surface,
        onSurfaceVariant = secondaryInk,
        outline = secondaryInk.copy(alpha = 0.5f),
        outlineVariant = separator,
        surfaceTint = Color.Transparent,
    )
}

@Composable
expect fun currentSystemInDarkTheme(): Boolean

@Composable
expect fun platformDynamicColorScheme(darkTheme: Boolean): ColorScheme?

@Composable
expect fun PlatformSystemBarEffect(darkTheme: Boolean)
