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

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Immutable
data class AppColors(
    val primary: Color,
    val onPrimary: Color,
    val primaryContainer: Color,
    val onPrimaryContainer: Color,
    val secondary: Color,
    val secondaryContainer: Color,
    val onSecondaryContainer: Color,
    val background: Color,
    val surface: Color,
    val surfaceContainer: Color,
    val surfaceContainerHigh: Color,
    val surfaceContainerLow: Color,
    val surfaceVariant: Color,
    val onSurface: Color,
    val onSurfaceVariant: Color,
    val outline: Color,
    val outlineVariant: Color,
    val error: Color,
    val onError: Color,
)

@Immutable
data class AppText(
    val headlineSmall: TextStyle,
    val titleLarge: TextStyle,
    val titleMedium: TextStyle,
    val titleSmall: TextStyle,
    val bodyLarge: TextStyle,
    val bodyMedium: TextStyle,
    val bodySmall: TextStyle,
    val labelLarge: TextStyle,
    val labelMedium: TextStyle,
    val labelSmall: TextStyle,
)

@Immutable
data class LiquidGlassSemanticTokens(
    val canvas: Color,
    val contentSurface: Color,
    val groupedSurface: Color,
    val segmentedContainer: Color,
    val segmentedSelectedSurface: Color,
    val chromeTint: Color,
    val chromePressedTint: Color,
    val separator: Color,
    val listRowMinHeight: Dp = 52.333.dp,
    val formGroupCornerRadius: Dp = 26.dp,
    val formHorizontalInset: Dp = 16.dp,
    val settingIconTileSize: Dp = 28.dp,
    val settingIconGlyphSize: Dp = 20.dp,
    val settingIconTitleStart: Dp = 60.dp,
    val settingIconBlue: Color = Color(0xFF007AFF),
    val settingIconTeal: Color = Color(0xFF30B0C7),
    val settingIconOrange: Color = Color(0xFFFF9500),
    val settingIconPurple: Color = Color(0xFFAF52DE),
    val settingIconRed: Color = Color(0xFFFF3B30),
    val settingIconNeutral: Color = Color(0xFF8E8E93),
    val modalCanvas: Color,
    val floatingControlMinHeight: Dp = 48.dp,
    val navigationHorizontalInset: Dp = 20.dp,
    val navigationVerticalGap: Dp = 8.dp,
    val groupedCornerRadius: Dp = 26.dp,
    val floatingCornerRadius: Dp = 28.dp,
)

/** 在弹窗/弹层子树内为 true，让分组卡片再抬升一级不透明层级。 */
val LocalLiquidGlassOverlay = staticCompositionLocalOf { false }

internal val LiquidGlassShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(26.dp),
    large = RoundedCornerShape(26.dp),
    extraLarge = RoundedCornerShape(32.dp),
)

private fun glassText(
    size: Int,
    height: Int,
    weight: FontWeight = FontWeight.Normal,
    tracking: Float = 0f,
) = TextStyle(
    fontFamily = FontFamily.Default,
    fontSize = size.sp,
    lineHeight = height.sp,
    fontWeight = weight,
    letterSpacing = tracking.sp,
)

internal val LiquidGlassTypography = Typography(
    displayLarge = glassText(48, 54, FontWeight.Bold, -0.6f),
    displayMedium = glassText(40, 46, FontWeight.Bold, -0.5f),
    displaySmall = glassText(34, 40, FontWeight.Bold, -0.4f),
    headlineLarge = glassText(34, 44, FontWeight.Bold, -0.2f),
    headlineMedium = glassText(28, 34, FontWeight.Bold, -0.3f),
    headlineSmall = glassText(24, 30, FontWeight.SemiBold, -0.2f),
    titleLarge = glassText(21, 29, FontWeight.SemiBold),
    titleMedium = glassText(17, 25, FontWeight.SemiBold),
    titleSmall = glassText(15, 21, FontWeight.SemiBold),
    bodyLarge = glassText(17, 26),
    bodyMedium = glassText(15, 23),
    bodySmall = glassText(13, 19),
    labelLarge = glassText(15, 20, FontWeight.SemiBold),
    labelMedium = glassText(12, 16, FontWeight.Medium),
    labelSmall = glassText(11, 15, FontWeight.Medium),
)

internal val LiquidGlassCompactNavigationTitle = glassText(17, 22, FontWeight.SemiBold)

@Composable
fun liquidGlassSemanticTokens(): LiquidGlassSemanticTokens {
    val scheme = MaterialTheme.colorScheme
    val dark = ThemeManager.isDarkTheme()
    return LiquidGlassSemanticTokens(
        canvas = scheme.background,
        contentSurface = if (LocalLiquidGlassOverlay.current) {
            if (dark) Color(0xFF2C2C2E) else Color.White
        } else {
            scheme.surfaceBright
        },
        modalCanvas = if (dark) Color(0xFF1C1C1E) else Color(0xFFF2F2F7),
        groupedSurface = scheme.surfaceContainerLow,
        segmentedContainer = if (dark) Color(0xFF24252A) else Color(0xFFE8EBF1),
        segmentedSelectedSurface = if (dark) Color(0xFF3A3B43) else Color.White,
        chromeTint = if (dark) Color.Black.copy(alpha = 0.50f) else Color.White.copy(alpha = 0.54f),
        chromePressedTint = if (dark) Color.White.copy(alpha = 0.14f) else Color.Black.copy(alpha = 0.09f),
        separator = scheme.outlineVariant,
    )
}

object AppTokens {
    val colors: AppColors
        @Composable @ReadOnlyComposable
        get() = when (LocalUiStyle.current) {
            UiStyle.Miuix -> {
                val mc = MiuixTheme.colorScheme
                AppColors(
                    primary = mc.primary,
                    onPrimary = mc.onPrimary,
                    primaryContainer = mc.primaryContainer,
                    onPrimaryContainer = mc.onPrimaryContainer,
                    secondary = mc.secondary,
                    secondaryContainer = mc.secondaryContainer,
                    onSecondaryContainer = mc.onSecondaryVariant,
                    background = mc.surface,
                    surface = mc.surface,
                    surfaceContainer = mc.surfaceContainer,
                    surfaceContainerHigh = mc.surfaceContainerHigh,
                    surfaceContainerLow = mc.surface,
                    surfaceVariant = mc.surfaceVariant,
                    onSurface = mc.onSurface,
                    onSurfaceVariant = mc.onSurfaceSecondary,
                    outline = mc.outline,
                    outlineVariant = mc.outline,
                    error = mc.error,
                    onError = mc.onError,
                )
            }
            UiStyle.Material3, UiStyle.LiquidGlass -> {
                val cs = MaterialTheme.colorScheme
                AppColors(
                    primary = cs.primary,
                    onPrimary = cs.onPrimary,
                    primaryContainer = cs.primaryContainer,
                    onPrimaryContainer = cs.onPrimaryContainer,
                    secondary = cs.secondary,
                    secondaryContainer = cs.secondaryContainer,
                    onSecondaryContainer = cs.onSecondaryContainer,
                    background = cs.background,
                    surface = cs.surface,
                    surfaceContainer = cs.surfaceContainer,
                    surfaceContainerHigh = cs.surfaceContainerHigh,
                    surfaceContainerLow = cs.surfaceContainerLow,
                    surfaceVariant = cs.surfaceVariant,
                    onSurface = cs.onSurface,
                    onSurfaceVariant = cs.onSurfaceVariant,
                    outline = cs.outline,
                    outlineVariant = cs.outlineVariant,
                    error = cs.error,
                    onError = cs.onError,
                )
            }
        }

    val text: AppText
        @Composable @ReadOnlyComposable
        get() = when (LocalUiStyle.current) {
            UiStyle.Miuix -> {
                val ts = MiuixTheme.textStyles
                AppText(
                    headlineSmall = ts.title3,
                    titleLarge = ts.title1,
                    titleMedium = ts.title2,
                    titleSmall = ts.title3,
                    bodyLarge = ts.body1,
                    bodyMedium = ts.body1,
                    bodySmall = ts.footnote1,
                    labelLarge = ts.body2,
                    labelMedium = ts.body2,
                    labelSmall = ts.footnote2,
                )
            }
            UiStyle.Material3, UiStyle.LiquidGlass -> {
                val ty = MaterialTheme.typography
                AppText(
                    headlineSmall = ty.headlineSmall,
                    titleLarge = ty.titleLarge,
                    titleMedium = ty.titleMedium,
                    titleSmall = ty.titleSmall,
                    bodyLarge = ty.bodyLarge,
                    bodyMedium = ty.bodyMedium,
                    bodySmall = ty.bodySmall,
                    labelLarge = ty.labelLarge,
                    labelMedium = ty.labelMedium,
                    labelSmall = ty.labelSmall,
                )
            }
        }
}
