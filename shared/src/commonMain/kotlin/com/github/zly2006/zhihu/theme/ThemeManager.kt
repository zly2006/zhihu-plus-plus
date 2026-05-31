/*
 * Zhihu++ - Free & Ad-Free Zhihu client for Android.
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
)

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
    fun getBackgroundColor(): Color = if (isDarkTheme()) {
        Color(backgroundColorDark.intValue)
    } else {
        Color(backgroundColorLight.intValue)
    }

    @Composable
    fun getThemeMode(): ThemeMode = themeMode.value

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
    }

    fun snapshot(): ThemeSnapshot = ThemeSnapshot(
        useDynamicColor = useDynamicColor.value,
        customColor = customColorInt.intValue,
        backgroundColorLight = backgroundColorLight.intValue,
        backgroundColorDark = backgroundColorDark.intValue,
        themeMode = themeMode.value,
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
}

fun resolveDarkTheme(
    themeMode: ThemeMode,
    systemDarkTheme: Boolean,
): Boolean = when (themeMode) {
    ThemeMode.LIGHT -> false
    ThemeMode.DARK -> true
    ThemeMode.SYSTEM -> systemDarkTheme
}
