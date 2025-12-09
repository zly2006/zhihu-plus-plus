package com.github.zly2006.zhihu.theme

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.core.content.edit

object ThemeManager {
    private val _useDynamicColor = mutableStateOf(true)
    private val _customColorInt = mutableIntStateOf(0xFF2196F3.toInt())

    @Composable
    fun getUseDynamicColor(): Boolean = _useDynamicColor.value

    @Composable
    fun getCustomColorInt(): Int = _customColorInt.intValue

    @Composable
    fun getCustomColor(): Color = Color(_customColorInt.intValue)

    fun initialize(context: Context) {
        val preferences = context.getSharedPreferences("com.github.zly2006.zhihu_preferences", Context.MODE_PRIVATE)
        _useDynamicColor.value = preferences.getBoolean("useDynamicColor", true)
        _customColorInt.intValue = preferences.getInt("customThemeColor", 0xFF2196F3.toInt())
    }

    fun setUseDynamicColor(context: Context, useDynamic: Boolean) {
        _useDynamicColor.value = useDynamic
        val preferences = context.getSharedPreferences("com.github.zly2006.zhihu_preferences", Context.MODE_PRIVATE)
        preferences.edit { putBoolean("useDynamicColor", useDynamic) }
    }

    fun setCustomColor(context: Context, color: Color) {
        _customColorInt.intValue = color.toArgb()
        val preferences = context.getSharedPreferences("com.github.zly2006.zhihu_preferences", Context.MODE_PRIVATE)
        preferences.edit { putInt("customThemeColor", color.toArgb()) }
    }
}
