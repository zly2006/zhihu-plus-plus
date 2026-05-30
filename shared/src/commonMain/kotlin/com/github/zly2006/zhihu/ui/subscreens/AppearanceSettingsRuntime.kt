package com.github.zly2006.zhihu.ui.subscreens

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.github.zly2006.zhihu.shared.theme.ThemeMode

data class AppearanceSettingsRuntime(
    val setThemeMode: (ThemeMode) -> Unit,
    val setUseDynamicColor: (Boolean) -> Unit,
    val setCustomColor: (Color) -> Unit,
    val setBackgroundColor: (Color, Boolean) -> Unit,
)

@Composable
fun rememberCommonAppearanceSettingsRuntime(): AppearanceSettingsRuntime {
    val themeSettings = rememberThemeSettingsRuntime()
    return AppearanceSettingsRuntime(
        setThemeMode = themeSettings.setThemeMode,
        setUseDynamicColor = themeSettings.setUseDynamicColor,
        setCustomColor = themeSettings.setCustomColor,
        setBackgroundColor = themeSettings.setBackgroundColor,
    )
}

data class ThemeSettingsRuntime(
    val setThemeMode: (ThemeMode) -> Unit,
    val setUseDynamicColor: (Boolean) -> Unit,
    val setCustomColor: (Color) -> Unit,
    val setBackgroundColor: (Color, Boolean) -> Unit,
)

@Composable
expect fun rememberThemeSettingsRuntime(): ThemeSettingsRuntime

@Composable
expect fun WebViewCustomFontSettings(
    customFontName: String?,
    onCustomFontNameChange: (String?) -> Unit,
)
