/*
 * Zhihu++ - Free & Ad-Free Zhihu client for Android.
 * Copyright (C) 2024-2026, zly2006 <i@zly2006.me>
 *
 * Licensed under AGPL-3.0-only.
 */

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

    // ===== 新增：UI 风格切换 =====
    // 默认仍走 Material 3，用户主动开启 miuix
    private val themeStyle = mutableStateOf(ThemeStyle.Material3)

    @Composable
    fun getUseDynamicColor(): Boolean = useDynamicColor.value

    @Composable
    fun getCustomColor(): Color = Color(customColorInt.intValue)

    @Composable
    fun getBackgroundColor(): Color = if (isDarkTheme()) {
        Color(backgroundColorDark.intValue)
    } else {
        Color(backgroundColorLight.intValue)
    }

    @Composable
    fun getThemeMode(): ThemeMode = themeMode.value

    @Composable
    fun getThemeStyle(): ThemeStyle = themeStyle.value

    /**
     * 非 Composable 上下文里读 useMiuix 的便捷入口。
     * 注意：这是裸读 state.value，不会触发 recompose。Composable 里请用 [getThemeStyle]。
     */
    val useMiuix: Boolean
        get() = themeStyle.value == ThemeStyle.Miuix

    //
    var isDarkTheme: Boolean = false

    @Composable
    fun isDarkTheme(): Boolean {
        val isDark = when (themeMode.value) {
            ThemeMode.LIGHT -> false
            ThemeMode.DARK -> true
            ThemeMode.SYSTEM -> isSystemInDarkTheme()
        }
        isDarkTheme = isDark
        return isDark
    }

    fun initialize(context: Context) {
        val preferences = context.getSharedPreferences(PREFERENCE_NAME, Context.MODE_PRIVATE)
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
        // 新增：读取 UI 风格
        themeStyle.value = ThemeStyle.fromValueOrDefault(
            preferences.getString("themeStyle", ThemeStyle.Material3.name)
        )
    }

    fun setUseDynamicColor(context: Context, useDynamic: Boolean) {
        useDynamicColor.value = useDynamic
        val preferences = context.getSharedPreferences(PREFERENCE_NAME, Context.MODE_PRIVATE)
        preferences.edit { putBoolean("useDynamicColor", useDynamic) }
    }

    fun setCustomColor(context: Context, color: Color) {
        customColorInt.intValue = color.toArgb()
        val preferences = context.getSharedPreferences(PREFERENCE_NAME, Context.MODE_PRIVATE)
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

    /**
     * 新增：切换 UI 风格。
     * 改完后顶层 ZhihuTheme 会自动 recompose 到对应主题，且因为 movableContentOf 包裹，
     * 子组件不会被销毁重建，ViewModel 和滚动位置都保留。
     */
    fun setThemeStyle(context: Context, style: ThemeStyle) {
        themeStyle.value = style
        val preferences = context.getSharedPreferences(PREFERENCE_NAME, Context.MODE_PRIVATE)
        preferences.edit { putString("themeStyle", style.name) }
    }
}
