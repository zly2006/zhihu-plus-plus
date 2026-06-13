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

package com.github.zly2006.zhihu.ui.subscreens
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import com.github.zly2006.zhihu.shared.platform.rememberSettingsStore
import com.github.zly2006.zhihu.shared.theme.ThemeMode
import com.github.zly2006.zhihu.theme.ThemeManager
import com.github.zly2006.zhihu.theme.ThemeStyle
import com.github.zly2006.zhihu.ui.TtsState
import kotlinx.coroutines.flow.StateFlow

data class ThemeSettingsRuntime(
    val setThemeMode: (ThemeMode) -> Unit,
    val setUseDynamicColor: (Boolean) -> Unit,
    val setCustomColor: (Color) -> Unit,
    val setBackgroundColor: (Color, Boolean) -> Unit,
    val setThemeStyle: (ThemeStyle) -> Unit,
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
                // 用户主动设置自定义主题色时，联动关闭动态取色，否则自定义 seed 会被 Monet 静默屏蔽。
                ThemeManager.setUseDynamicColor(false)
                settings.putBoolean("useDynamicColor", false)
                ThemeManager.setCustomColor(color)
                settings.putInt("customThemeColor", color.toArgb())
            },
            setBackgroundColor = { color, isDark ->
                ThemeManager.setBackgroundColor(color, isDark)
                settings.putInt(if (isDark) "backgroundColorDark" else "backgroundColorLight", color.toArgb())
            },
            setThemeStyle = { style ->
                ThemeManager.setThemeStyle(style)
                settings.putString("themeStyle", style.name)
            },
        )
    }
}

@Composable
expect fun WebViewCustomFontSettings(
    customFontName: String?,
    onCustomFontNameChange: (String?) -> Unit,
)

data class SystemUpdateRuntime(
    val state: StateFlow<SystemUpdateState>,
    val autoCheckEnabled: () -> Boolean,
    val setAutoCheckEnabled: (Boolean) -> Unit,
    val checkForUpdate: suspend () -> Unit,
    val skipVersion: (String) -> Unit,
    val resetToNoUpdate: () -> Unit,
    val downloadUpdate: suspend (String) -> Unit,
    val installDownloadedUpdate: suspend () -> Unit,
    val setError: (String) -> Unit,
    val supportsApkInstall: Boolean,
)

sealed interface SystemUpdateState {
    data object NoUpdate : SystemUpdateState

    data object Checking : SystemUpdateState

    data object Latest : SystemUpdateState

    data class UpdateAvailable(
        val version: String,
        val isNightly: Boolean,
        val releaseNotes: String?,
        val downloadUrl: String,
        val cnDownloadUrl: String?,
    ) : SystemUpdateState

    data object Downloading : SystemUpdateState

    data object Downloaded : SystemUpdateState

    data class Error(
        val message: String,
    ) : SystemUpdateState
}

@Composable
expect fun rememberSystemUpdateRuntime(): SystemUpdateRuntime

data class DeveloperRuntimeInfo(
    val continuousUsageDurationMs: Long = 0L,
    val ttsState: TtsState = TtsState.Uninitialized,
    val currentTtsEngineLabel: String = "未初始化",
    val availableTtsEngineLabels: List<String> = emptyList(),
)

interface DeveloperRuntimeInfoProvider {
    val developerRuntimeInfo: DeveloperRuntimeInfo
}

data class DeveloperSettingsRuntime(
    val cookies: () -> Map<String, String>,
    val networkStatus: () -> String,
    val powerSaveModeText: () -> String?,
    val runtimeInfo: () -> DeveloperRuntimeInfo,
    val verifyLogin: suspend (Map<String, String>) -> Boolean,
    val refreshToken: suspend () -> Unit,
    val saveCookies: (Map<String, String>) -> Unit,
    val signedGet: suspend (String) -> String,
)

@Composable
expect fun rememberDeveloperSettingsRuntime(): DeveloperSettingsRuntime
