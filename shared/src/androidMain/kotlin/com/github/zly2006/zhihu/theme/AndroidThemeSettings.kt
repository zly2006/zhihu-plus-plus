package com.github.zly2006.zhihu.theme

import android.content.Context
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
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
