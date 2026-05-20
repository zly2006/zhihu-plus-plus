package com.github.zly2006.zhihu.theme

import android.content.Context
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.core.content.edit
import com.github.zly2006.zhihu.shared.theme.ThemeMode

private const val PREFERENCE_NAME = "com.github.zly2006.zhihu_preferences"

object AndroidThemeSettings {
    fun initialize(context: Context) {
        ThemeManager.load(readSnapshot(context))
    }

    fun setUseDynamicColor(context: Context, useDynamic: Boolean) {
        ThemeManager.setUseDynamicColor(useDynamic)
        context.preferences.edit { putBoolean("useDynamicColor", useDynamic) }
    }

    fun setCustomColor(context: Context, color: Color) {
        ThemeManager.setCustomColor(color)
        context.preferences.edit { putInt("customThemeColor", color.toArgb()) }
    }

    fun setBackgroundColor(context: Context, color: Color, isDark: Boolean) {
        ThemeManager.setBackgroundColor(color, isDark)
        val key = if (isDark) "backgroundColorDark" else "backgroundColorLight"
        context.preferences.edit { putInt(key, color.toArgb()) }
    }

    fun setThemeMode(context: Context, mode: ThemeMode) {
        ThemeManager.setThemeMode(mode)
        context.preferences.edit { putString("themeMode", mode.name) }
    }

    private fun readSnapshot(context: Context): ThemeSnapshot {
        val preferences = context.preferences
        val themeModeValue = preferences.getString("themeMode", ThemeMode.SYSTEM.name) ?: ThemeMode.SYSTEM.name
        val themeMode = try {
            ThemeMode.valueOf(themeModeValue)
        } catch (_: IllegalArgumentException) {
            ThemeMode.SYSTEM
        }
        return ThemeSnapshot(
            useDynamicColor = preferences.getBoolean("useDynamicColor", true),
            customColor = preferences.getInt("customThemeColor", 0xFF2196F3.toInt()),
            backgroundColorLight = preferences.getInt("backgroundColorLight", 0xFFFFFFFF.toInt()),
            backgroundColorDark = preferences.getInt("backgroundColorDark", 0xFF121212.toInt()),
            themeMode = themeMode,
        )
    }

    private val Context.preferences
        get() = getSharedPreferences(PREFERENCE_NAME, Context.MODE_PRIVATE)
}
