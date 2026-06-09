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

package com.github.zly2006.zhihu.shared.platform
import androidx.compose.runtime.Composable

enum class UserMessageDuration {
    Short,
    Long,
}

data class UserMessageSink(
    val showShortMessage: (String) -> Unit,
    val showLongMessage: (String) -> Unit = showShortMessage,
) {
    fun showMessage(
        message: String,
        duration: UserMessageDuration = UserMessageDuration.Short,
    ) {
        when (duration) {
            UserMessageDuration.Short -> showShortMessage(message)
            UserMessageDuration.Long -> showLongMessage(message)
        }
    }
}

@Composable
expect fun rememberUserMessageSink(): UserMessageSink

data class SettingsStore(
    val getBoolean: (String, Boolean) -> Boolean,
    val putBoolean: (String, Boolean) -> Unit,
    val getString: (String, String) -> String,
    val putString: (String, String) -> Unit,
    val getStringOrNull: (String) -> String?,
    val putStringSet: (String, Set<String>) -> Unit,
    val getStringSet: (String, Set<String>) -> Set<String>,
    val getInt: (String, Int) -> Int,
    val putInt: (String, Int) -> Unit,
    val getLong: (String, Long) -> Long,
    val putLong: (String, Long) -> Unit,
    val getFloat: (String, Float) -> Float,
    val putFloat: (String, Float) -> Unit,
    val remove: (String) -> Unit,
    val observeKeyChanges: (onChanged: (String) -> Unit) -> () -> Unit = { {} },
)

@Composable
expect fun rememberSettingsStore(): SettingsStore

@Composable
expect fun rememberExternalUrlOpener(): (String) -> Unit

@Composable
expect fun rememberSystemUrlOpener(): (String) -> Unit

@Composable
expect fun rememberZhihuWebUrlOpener(): (String) -> Unit

@Composable
expect fun rememberImagePreviewOpener(): (String) -> Unit

@Composable
expect fun rememberImageGalleryOpener(): (List<String>, Int) -> Unit

@Composable
expect fun rememberPlainTextClipboard(): (label: String, text: String) -> Unit

/**
 * 开发者诊断信息：包名/版本/网络状态等只读快照，以及剪贴板读取、全量配置导出
 * 这些动作依赖各平台原生 API（Android ConnectivityManager/PackageManager/ClipboardManager/SharedPreferences.all）。
 */
data class DeveloperDiagnostics(
    val appInfo: String,
    val deviceInfo: String,
    val networkStatus: String,
    val readClipboardText: () -> String?,
    val exportAllSettings: () -> String,
)

@Composable
expect fun rememberDeveloperDiagnostics(): DeveloperDiagnostics

data class ScreenSizeDp(
    val width: Float,
    val height: Float,
)

@Composable
expect fun rememberScreenSizeDp(): ScreenSizeDp

@Composable
expect fun PlatformBackHandler(
    enabled: Boolean,
    onBack: () -> Unit,
)

@Composable
expect fun PlatformPredictiveBackHandler(
    enabled: Boolean,
    onProgress: (Float) -> Unit,
    onCancel: () -> Unit,
    onBack: () -> Unit,
)

@Composable
expect fun rememberIsLiteVariant(): Boolean
