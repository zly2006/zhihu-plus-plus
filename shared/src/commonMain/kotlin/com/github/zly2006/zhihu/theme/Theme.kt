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

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import com.github.zly2006.zhihu.platform.rememberSettingsStore
import com.github.zly2006.zhihu.ui.subscreens.UI_STYLE_PREFERENCE_KEY
import com.materialkolor.dynamicColorScheme

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
    val liquidGlass = remember { isLiquidGlassSupported && settings.getString(UI_STYLE_PREFERENCE_KEY, "material") == "liquid_glass" }
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

    val colorScheme = if (liquidGlass) {
        liquidGlassColors(darkTheme)
    } else {
        baseColorScheme.copy(
            background = customBackgroundColor,
            surface = customBackgroundColor,
        )
    }

    PlatformSystemBarEffect(darkTheme)

    CompositionLocalProvider(LocalLiquidGlass provides liquidGlass) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = if (liquidGlass) LiquidGlassTypography else Typography,
            shapes = if (liquidGlass) LiquidGlassShapes else androidx.compose.material3.Shapes(),
            content = content,
        )
    }
}

@Composable
expect fun currentSystemInDarkTheme(): Boolean

@Composable
expect fun platformDynamicColorScheme(darkTheme: Boolean): ColorScheme?

@Composable
expect fun PlatformSystemBarEffect(darkTheme: Boolean)

private fun liquidGlassColors(dark: Boolean): ColorScheme {
    val background = if (dark) Color(0xFF101114) else Color(0xFFF2F3F7)
    val surface = if (dark) Color(0xFF202126) else Color.White
    val ink = if (dark) Color(0xFFF4F4F7) else Color(0xFF191A20)
    val secondaryInk = if (dark) Color(0xFFB5B6C0) else Color(0xFF666872)
    return (if (dark) darkColorScheme() else lightColorScheme()).copy(
        primary = if (dark) Color(0xFF72ADFF) else Color(0xFF0065DD),
        onPrimary = if (dark) Color(0xFF002B60) else Color.White,
        primaryContainer = if (dark) Color(0xFF203552) else Color(0xFFE1EDFF),
        onPrimaryContainer = ink,
        secondary = secondaryInk,
        onSecondary = surface,
        secondaryContainer = if (dark) Color(0xFF343740) else Color(0xFFE8EBF1),
        onSecondaryContainer = ink,
        tertiary = if (dark) Color(0xFF93BDEA) else Color(0xFF38648F),
        onTertiary = if (dark) Color(0xFF17344F) else Color.White,
        tertiaryContainer = if (dark) Color(0xFF253748) else Color(0xFFE3EEF9),
        onTertiaryContainer = ink,
        inversePrimary = if (dark) Color(0xFF0065DD) else Color(0xFF72ADFF),
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
        surfaceContainerHighest = if (dark) Color(0xFF303239) else Color(0xFFE8EAF0),
        surfaceVariant = if (dark) Color(0xFF24252A) else Color(0xFFF8F9FC),
        onSurfaceVariant = secondaryInk,
        outline = secondaryInk.copy(alpha = 0.5f),
        outlineVariant = secondaryInk.copy(alpha = 0.16f),
        surfaceTint = Color.Transparent,
    )
}
