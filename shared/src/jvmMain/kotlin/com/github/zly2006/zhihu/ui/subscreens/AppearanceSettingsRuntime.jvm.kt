package com.github.zly2006.zhihu.ui.subscreens

import androidx.compose.runtime.Composable
import com.github.zly2006.zhihu.theme.ThemeManager

@Composable
actual fun rememberThemeSettingsRuntime(): ThemeSettingsRuntime = ThemeSettingsRuntime(
    setThemeMode = ThemeManager::setThemeMode,
    setUseDynamicColor = ThemeManager::setUseDynamicColor,
    setCustomColor = ThemeManager::setCustomColor,
    setBackgroundColor = ThemeManager::setBackgroundColor,
)

@Composable
actual fun WebViewCustomFontSettings(
    customFontName: String?,
    onCustomFontNameChange: (String?) -> Unit,
) = Unit // TODO: desktop WebView custom font settings
