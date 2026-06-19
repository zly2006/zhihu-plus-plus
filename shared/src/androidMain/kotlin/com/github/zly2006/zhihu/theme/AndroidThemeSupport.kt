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
import android.app.Activity
import android.content.Context
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import com.github.zly2006.zhihu.shared.platform.androidSettingsStore
import com.github.zly2006.zhihu.shared.theme.ThemeMode

object AndroidThemeSettings {
    fun initialize(context: Context) {
        ThemeManager.load(readSnapshot(context))
    }

    fun setUseDynamicColor(context: Context, useDynamic: Boolean) {
        ThemeManager.setUseDynamicColor(useDynamic)
        context.settings.putBoolean("useDynamicColor", useDynamic)
    }

    fun setCustomColor(context: Context, color: Color) {
        ThemeManager.setCustomColor(color)
        context.settings.putInt("customThemeColor", color.toArgb())
    }

    fun setBackgroundColor(context: Context, color: Color, isDark: Boolean) {
        ThemeManager.setBackgroundColor(color, isDark)
        val key = if (isDark) "backgroundColorDark" else "backgroundColorLight"
        context.settings.putInt(key, color.toArgb())
    }

    fun setThemeMode(context: Context, mode: ThemeMode) {
        ThemeManager.setThemeMode(mode)
        context.settings.putString("themeMode", mode.name)
    }

    private fun readSnapshot(context: Context): ThemeSnapshot {
        val settings = context.settings
        val themeModeValue = settings.getString("themeMode", ThemeMode.SYSTEM.name)
        val themeMode = try {
            ThemeMode.valueOf(themeModeValue)
        } catch (_: IllegalArgumentException) {
            ThemeMode.SYSTEM
        }
        return ThemeSnapshot(
            useDynamicColor = settings.getBoolean("useDynamicColor", true),
            customColor = settings.getInt("customThemeColor", 0xFF2196F3.toInt()),
            backgroundColorLight = settings.getInt("backgroundColorLight", 0xFFFFFFFF.toInt()),
            backgroundColorDark = settings.getInt("backgroundColorDark", 0xFF121212.toInt()),
            themeMode = themeMode,
        )
    }

    private val Context.settings
        get() = androidSettingsStore(this)
}

@Composable
actual fun currentSystemInDarkTheme(): Boolean = isSystemInDarkTheme()

@Composable
actual fun platformDynamicColorScheme(darkTheme: Boolean): ColorScheme? {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return null
    val context = LocalContext.current
    return if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
}

@Composable
actual fun PlatformSystemBarEffect(darkTheme: Boolean) {
    val view = LocalView.current
    if (view.isInEditMode) return
    SideEffect {
        val window = (view.context as? Activity)?.window ?: return@SideEffect
        WindowCompat.getInsetsController(window, view).apply {
            isAppearanceLightStatusBars = !darkTheme
            isAppearanceLightNavigationBars = !darkTheme
        }
    }
}
