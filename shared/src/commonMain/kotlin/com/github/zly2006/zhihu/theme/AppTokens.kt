/*
 * Zhihu++ - Free & Ad-Free Zhihu client for Android.
 * Copyright (C) 2024-2026, zly2006 <i@zly2006.me>
 *
 * Licensed under AGPL-3.0-only.
 */

package com.github.zly2006.zhihu.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Immutable
data class AppColors(
    // 24 actually-used tokens (grep-verified), sorted by usage frequency
    val onSurfaceVariant: Color, // 107 uses → miuix: onSurfaceSecondary
    val primary: Color, // 40 uses
    val onSurface: Color, // 36 uses
    val surfaceVariant: Color, // 25 uses
    val surfaceContainer: Color, // 25 uses
    val onPrimaryContainer: Color, // 20 uses
    val primaryContainer: Color, // 17 uses
    val onSecondaryContainer: Color, // 17 uses
    val error: Color, // 16 uses
    val secondaryContainer: Color, // 13 uses
    val surfaceContainerHigh: Color, // 9 uses
    val outlineVariant: Color, // 8 uses → miuix: outline
    val onTertiaryContainer: Color, // 8 uses
    val onPrimary: Color, // 8 uses
    val surface: Color, // 7 uses
    val tertiaryContainer: Color, // 5 uses
    val surfaceBright: Color, // 4 uses → miuix: surface
    val outline: Color, // 4 uses
    val background: Color, // 4 uses
    val surfaceContainerLow: Color, // 3 uses → miuix: surface
    val surfaceContainerHighest: Color, // 3 uses
    val tertiary: Color, // 2 uses → miuix: primary (fallback)
    val onErrorContainer: Color, // 2 uses
    val errorContainer: Color, // 2 uses
    val secondary: Color, // 1 use
    val inversePrimary: Color, // 1 use → miuix: primary (fallback)
)

@Immutable
data class AppText(
    // 11 actually-used typography levels (grep-verified)
    val bodyMedium: TextStyle, // 49 uses → miuix: body1
    val bodySmall: TextStyle, // 48 uses → miuix: footnote1
    val titleMedium: TextStyle, // 26 uses → miuix: title2
    val bodyLarge: TextStyle, // 26 uses → miuix: body1
    val labelMedium: TextStyle, // 8 uses  → miuix: body2
    val titleSmall: TextStyle, // 7 uses  → miuix: title3
    val titleLarge: TextStyle, // 7 uses  → miuix: title1
    val labelLarge: TextStyle, // 5 uses  → miuix: body2
    val headlineSmall: TextStyle, // 4 uses  → miuix: title3
    val labelSmall: TextStyle, // 3 uses  → miuix: footnote2
    val headlineMedium: TextStyle, // 3 uses → miuix: title1
)

object AppTokens {
    val colors: AppColors
        @Composable @ReadOnlyComposable
        get() = when (LocalThemeStyle.current) {
            ThemeStyle.Miuix -> AppColors(
                onSurfaceVariant = MiuixTheme.colorScheme.onSurfaceSecondary,
                primary = MiuixTheme.colorScheme.primary,
                onSurface = MiuixTheme.colorScheme.onSurface,
                surfaceVariant = MiuixTheme.colorScheme.surfaceVariant,
                surfaceContainer = MiuixTheme.colorScheme.surfaceContainer,
                onPrimaryContainer = MiuixTheme.colorScheme.onPrimaryContainer,
                primaryContainer = MiuixTheme.colorScheme.primaryContainer,
                onSecondaryContainer = MiuixTheme.colorScheme.onSecondaryContainer,
                error = MiuixTheme.colorScheme.error,
                secondaryContainer = MiuixTheme.colorScheme.secondaryContainer,
                surfaceContainerHigh = MiuixTheme.colorScheme.surfaceContainerHigh,
                outlineVariant = MiuixTheme.colorScheme.outline,
                onTertiaryContainer = MiuixTheme.colorScheme.onTertiaryContainer,
                onPrimary = MiuixTheme.colorScheme.onPrimary,
                surface = MiuixTheme.colorScheme.surface,
                tertiaryContainer = MiuixTheme.colorScheme.tertiaryContainer,
                surfaceBright = MiuixTheme.colorScheme.surface,
                outline = MiuixTheme.colorScheme.outline,
                background = MiuixTheme.colorScheme.background,
                surfaceContainerLow = MiuixTheme.colorScheme.surface,
                surfaceContainerHighest = MiuixTheme.colorScheme.surfaceContainerHighest,
                tertiary = MiuixTheme.colorScheme.primary,
                onErrorContainer = MiuixTheme.colorScheme.onErrorContainer,
                errorContainer = MiuixTheme.colorScheme.errorContainer,
                secondary = MiuixTheme.colorScheme.secondary,
                inversePrimary = MiuixTheme.colorScheme.primary,
            )
            ThemeStyle.Material3 -> AppColors(
                onSurfaceVariant = MaterialTheme.colorScheme.onSurfaceVariant,
                primary = MaterialTheme.colorScheme.primary,
                onSurface = MaterialTheme.colorScheme.onSurface,
                surfaceVariant = MaterialTheme.colorScheme.surfaceVariant,
                surfaceContainer = MaterialTheme.colorScheme.surfaceContainer,
                onPrimaryContainer = MaterialTheme.colorScheme.onPrimaryContainer,
                primaryContainer = MaterialTheme.colorScheme.primaryContainer,
                onSecondaryContainer = MaterialTheme.colorScheme.onSecondaryContainer,
                error = MaterialTheme.colorScheme.error,
                secondaryContainer = MaterialTheme.colorScheme.secondaryContainer,
                surfaceContainerHigh = MaterialTheme.colorScheme.surfaceContainerHigh,
                outlineVariant = MaterialTheme.colorScheme.outlineVariant,
                onTertiaryContainer = MaterialTheme.colorScheme.onTertiaryContainer,
                onPrimary = MaterialTheme.colorScheme.onPrimary,
                surface = MaterialTheme.colorScheme.surface,
                tertiaryContainer = MaterialTheme.colorScheme.tertiaryContainer,
                surfaceBright = MaterialTheme.colorScheme.surfaceBright,
                outline = MaterialTheme.colorScheme.outline,
                background = MaterialTheme.colorScheme.background,
                surfaceContainerLow = MaterialTheme.colorScheme.surfaceContainerLow,
                surfaceContainerHighest = MaterialTheme.colorScheme.surfaceContainerHighest,
                tertiary = MaterialTheme.colorScheme.tertiary,
                onErrorContainer = MaterialTheme.colorScheme.onErrorContainer,
                errorContainer = MaterialTheme.colorScheme.errorContainer,
                secondary = MaterialTheme.colorScheme.secondary,
                inversePrimary = MaterialTheme.colorScheme.inversePrimary,
            )
        }

    val text: AppText
        @Composable @ReadOnlyComposable
        get() = when (LocalThemeStyle.current) {
            ThemeStyle.Miuix -> AppText(
                bodyMedium = MiuixTheme.textStyles.body1,
                bodySmall = MiuixTheme.textStyles.footnote1,
                titleMedium = MiuixTheme.textStyles.title2,
                bodyLarge = MiuixTheme.textStyles.body1,
                labelMedium = MiuixTheme.textStyles.body2,
                titleSmall = MiuixTheme.textStyles.title3,
                titleLarge = MiuixTheme.textStyles.title1,
                labelLarge = MiuixTheme.textStyles.body2,
                headlineSmall = MiuixTheme.textStyles.title3,
                labelSmall = MiuixTheme.textStyles.footnote2,
                headlineMedium = MiuixTheme.textStyles.title1,
            )
            ThemeStyle.Material3 -> AppText(
                bodyMedium = MaterialTheme.typography.bodyMedium,
                bodySmall = MaterialTheme.typography.bodySmall,
                titleMedium = MaterialTheme.typography.titleMedium,
                bodyLarge = MaterialTheme.typography.bodyLarge,
                labelMedium = MaterialTheme.typography.labelMedium,
                titleSmall = MaterialTheme.typography.titleSmall,
                titleLarge = MaterialTheme.typography.titleLarge,
                labelLarge = MaterialTheme.typography.labelLarge,
                headlineSmall = MaterialTheme.typography.headlineSmall,
                labelSmall = MaterialTheme.typography.labelSmall,
                headlineMedium = MaterialTheme.typography.headlineMedium,
            )
        }
}
