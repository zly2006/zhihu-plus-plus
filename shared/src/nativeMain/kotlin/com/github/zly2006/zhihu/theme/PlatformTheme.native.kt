package com.github.zly2006.zhihu.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.runtime.Composable

// TODO: iOS 检测系统暗色模式
@Composable
actual fun currentSystemInDarkTheme(): Boolean = false

// TODO: iOS 动态色彩方案
@Composable
actual fun platformDynamicColorScheme(darkTheme: Boolean): ColorScheme? = null

// TODO: iOS 系统栏效果
@Composable
actual fun PlatformSystemBarEffect(darkTheme: Boolean) = Unit
