package com.github.zly2006.zhihu.ui.subscreens

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import com.github.zly2006.zhihu.shared.platform.rememberSettingsStore
import com.github.zly2006.zhihu.shared.theme.ThemeMode
import com.github.zly2006.zhihu.theme.ThemeManager

data class ThemeSettingsRuntime(
    val setThemeMode: (ThemeMode) -> Unit,
    val setUseDynamicColor: (Boolean) -> Unit,
    val setCustomColor: (Color) -> Unit,
    val setBackgroundColor: (Color, Boolean) -> Unit,
)

@Composable
fun rememberThemeSettingsRuntime(): ThemeSettingsRuntime {
    val settings = rememberSettingsStore()
    return remember(settings) {
        ThemeSettingsRuntime(
            setThemeMode = { mode ->
                ThemeManager.setThemeMode(mode)
                settings.putString("themeMode", mode.name)
            },
            setUseDynamicColor = { enabled ->
                ThemeManager.setUseDynamicColor(enabled)
                settings.putBoolean("useDynamicColor", enabled)
            },
            setCustomColor = { color ->
                ThemeManager.setCustomColor(color)
                settings.putInt("customThemeColor", color.toArgb())
            },
            setBackgroundColor = { color, isDark ->
                ThemeManager.setBackgroundColor(color, isDark)
                settings.putInt(if (isDark) "backgroundColorDark" else "backgroundColorLight", color.toArgb())
            },
        )
    }
}

@Composable
expect fun WebViewCustomFontSettings(
    customFontName: String?,
    onCustomFontNameChange: (String?) -> Unit,
)
