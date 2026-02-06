package com.github.zly2006.zhihu.theme

import android.content.Context
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.core.content.edit
import com.github.zly2006.zhihu.ui.PREFERENCE_NAME

enum class ThemeMode {
    LIGHT,
    DARK,
    SYSTEM,
}

object ThemeManager {
    private val useDynamicColor = mutableStateOf(true)
    private val customColorInt = mutableIntStateOf(0xFF2196F3.toInt())
    private val backgroundColorLight = mutableIntStateOf(0xFFFFFFFF.toInt())
    private val backgroundColorDark = mutableIntStateOf(0xFF121212.toInt())
    private val themeMode = mutableStateOf(ThemeMode.SYSTEM)

    @Composable
    fun getUseDynamicColor(): Boolean = useDynamicColor.value

    @Composable
    fun getCustomColor(): Color = Color(customColorInt.intValue)

    @Composable
    fun getBackgroundColor(): Color {
        return if (isDarkTheme()) {
            Color(backgroundColorDark.intValue)
        } else {
            Color(backgroundColorLight.intValue)
        }
    }

    @Composable
    fun getThemeMode(): ThemeMode = themeMode.value

    @Composable
    fun isDarkTheme(): Boolean = when (themeMode.value) {
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
    }

    fun initialize(context: Context) {
        val preferences = context.getSharedPreferences("com.github.zly2006.zhihu_preferences", Context.MODE_PRIVATE)
        useDynamicColor.value = preferences.getBoolean("useDynamicColor", true)
        customColorInt.intValue = preferences.getInt("customThemeColor", 0xFF2196F3.toInt())
        backgroundColorLight.intValue = preferences.getInt("backgroundColorLight", 0xFFFFFFFF.toInt())
        backgroundColorDark.intValue = preferences.getInt("backgroundColorDark", 0xFF121212.toInt())
        val themeModeValue = preferences.getString("themeMode", ThemeMode.SYSTEM.name) ?: ThemeMode.SYSTEM.name
        themeMode.value = try {
            ThemeMode.valueOf(themeModeValue)
        } catch (_: IllegalArgumentException) {
            ThemeMode.SYSTEM
        }
    }

    fun setUseDynamicColor(context: Context, useDynamic: Boolean) {
        useDynamicColor.value = useDynamic
        val preferences = context.getSharedPreferences("com.github.zly2006.zhihu_preferences", Context.MODE_PRIVATE)
        preferences.edit { putBoolean("useDynamicColor", useDynamic) }
    }

    fun setCustomColor(context: Context, color: Color) {
        customColorInt.intValue = color.toArgb()
        val preferences = context.getSharedPreferences("com.github.zly2006.zhihu_preferences", Context.MODE_PRIVATE)
        preferences.edit { putInt("customThemeColor", color.toArgb()) }
    }

    fun setBackgroundColor(context: Context, color: Color, isDark: Boolean) {
        val colorInt = color.toArgb()
        if (isDark) {
            backgroundColorDark.intValue = colorInt
        } else {
            backgroundColorLight.intValue = colorInt
        }
        val preferences = context.getSharedPreferences(PREFERENCE_NAME, Context.MODE_PRIVATE)
        val key = if (isDark) "backgroundColorDark" else "backgroundColorLight"
        preferences.edit { putInt(key, colorInt) }
    }

    fun setThemeMode(context: Context, mode: ThemeMode) {
        themeMode.value = mode
        val preferences = context.getSharedPreferences(PREFERENCE_NAME, Context.MODE_PRIVATE)
        preferences.edit { putString("themeMode", mode.name) }
    }
}
