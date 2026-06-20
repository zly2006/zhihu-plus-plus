/*
 * Zhihu++ - Free & Ad-Free Zhihu client for all platforms.
 * Copyright (C) 2024-2026, zly2006 <i@zly2006.me>
 *
 * Licensed under AGPL-3.0-only.
 */

package com.github.zly2006.zhihu.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import com.github.zly2006.zhihu.shared.theme.ThemeMode

data class ThemeSnapshot(
    val useDynamicColor: Boolean = true,
    val customColor: Int = 0xFF2196F3.toInt(),
    val backgroundColorLight: Int = 0xFFFFFFFF.toInt(),
    val backgroundColorDark: Int = 0xFF121212.toInt(),
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val themeStyle: ThemeStyle = ThemeStyle.Material3,
)

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

    var isDarkTheme: Boolean = false
        private set

    @Composable
    fun isDarkTheme(): Boolean {
        val isDark = resolveDarkTheme(themeMode.value, currentSystemInDarkTheme())
        isDarkTheme = isDark
        return isDark
    }

    fun load(snapshot: ThemeSnapshot) {
        useDynamicColor.value = snapshot.useDynamicColor
        customColorInt.intValue = snapshot.customColor
        backgroundColorLight.intValue = snapshot.backgroundColorLight
        backgroundColorDark.intValue = snapshot.backgroundColorDark
        themeMode.value = snapshot.themeMode
        themeStyle.value = snapshot.themeStyle
    }

    fun snapshot(): ThemeSnapshot = ThemeSnapshot(
        useDynamicColor = useDynamicColor.value,
        customColor = customColorInt.intValue,
        backgroundColorLight = backgroundColorLight.intValue,
        backgroundColorDark = backgroundColorDark.intValue,
        themeMode = themeMode.value,
        themeStyle = themeStyle.value,
    )

    fun setUseDynamicColor(useDynamic: Boolean) {
        useDynamicColor.value = useDynamic
    }

    fun setCustomColor(color: Color) {
        customColorInt.intValue = color.toArgb()
    }

    fun setBackgroundColor(color: Color, isDark: Boolean) {
        val colorInt = color.toArgb()
        if (isDark) {
            backgroundColorDark.intValue = colorInt
        } else {
            backgroundColorLight.intValue = colorInt
        }
    }

    fun setThemeMode(mode: ThemeMode) {
        themeMode.value = mode
    }

    /**
     * 新增：切换 UI 风格（state-only）。
     * 改完后顶层 ZhihuTheme 会自动 recompose 到对应主题，且因为 movableContentOf 包裹，
     * 子组件不会被销毁重建，ViewModel 和滚动位置都保留。
     * 持久化由调用方（androidMain AndroidThemeSettings / rememberThemeSettingsRuntime）负责。
     */
    fun setThemeStyle(style: ThemeStyle) {
        themeStyle.value = style
    }
}

fun resolveDarkTheme(
    themeMode: ThemeMode,
    systemDarkTheme: Boolean,
): Boolean = when (themeMode) {
    ThemeMode.LIGHT -> false
    ThemeMode.DARK -> true
    ThemeMode.SYSTEM -> systemDarkTheme
}
