package com.github.zly2006.zhihu.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.runtime.Composable

@Composable
actual fun currentSystemInDarkTheme(): Boolean = isSystemInDarkTheme()

@Composable
actual fun platformDynamicColorScheme(darkTheme: Boolean): ColorScheme? = null

@Composable
actual fun PlatformSystemBarEffect(darkTheme: Boolean) = Unit
