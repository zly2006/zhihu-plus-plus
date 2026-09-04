/*
 * Zhihu++ - Free & Ad-Free Zhihu client for all platforms.
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

package com.github.zly2006.zhihu.platform

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.github.zly2006.zhihu.ui.rememberObservedSetting
import kotlinx.io.files.Path

internal expect val platformBottomBarItemLimit: Int?

expect val platformName: String

expect val isJvm: Boolean

expect val isNative: Boolean

expect val isAigcVoteSupported: Boolean

expect val isBlocklistNlpSupported: Boolean

expect val isSentenceSimilaritySupported: Boolean

expect val isArticleHtmlExportSupported: Boolean

expect val isArticleImageExportSupported: Boolean

enum class UserMessageDuration {
    Short,
    Long,
}

interface UserMessageSink {
    fun showShortMessage(message: String)

    fun showLongMessage(message: String) = showShortMessage(message)

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

interface SettingsStore {
    fun getBoolean(key: String, defaultValue: Boolean): Boolean

    fun putBoolean(key: String, value: Boolean)

    fun getString(key: String, defaultValue: String): String

    fun putString(key: String, value: String)

    fun getStringOrNull(key: String): String?

    fun putStringSet(key: String, value: Set<String>)

    fun getStringSet(key: String, defaultValue: Set<String>): Set<String>

    fun getInt(key: String, defaultValue: Int): Int

    fun putInt(key: String, value: Int)

    fun getLong(key: String, defaultValue: Long): Long

    fun putLong(key: String, value: Long)

    fun getFloat(key: String, defaultValue: Float): Float

    fun putFloat(key: String, value: Float)

    fun remove(key: String)

    fun observeKeyChanges(onChanged: (String) -> Unit): AutoCloseable = AutoCloseable { }
}

interface SystemUrlOpener {
    operator fun invoke(url: String)
}

interface ZhihuWebUrlOpener {
    operator fun invoke(url: String)
}

interface ImagePreviewOpener {
    operator fun invoke(url: String)
}

interface ExternalUrlOpener :
    SystemUrlOpener,
    ZhihuWebUrlOpener,
    ImagePreviewOpener

interface ImageGalleryOpener {
    operator fun invoke(urls: List<String>, initialIndex: Int)
}

interface ImageSaver {
    operator fun invoke(url: String)
}

interface ImageSharer {
    operator fun invoke(url: String)
}

interface PlainTextClipboard {
    operator fun invoke(label: String, text: String)
}

@Composable
expect fun rememberSettingsStore(): SettingsStore

expect fun Modifier.exportTestTagsForUiAutomation(): Modifier

@Composable
expect fun rememberAppPrivateDirectory(): Path

/*
 * 按类型包装 rememberObservedSetting：调用点只写一次 key 和默认值，且直接拿到值而不是 MutableState。
 * 观察机制本身只有 rememberObservedSetting 一份实现。
 */

@Composable
fun rememberSettingBoolean(
    key: String,
    defaultValue: Boolean,
    settings: SettingsStore = rememberSettingsStore(),
): Boolean = rememberObservedSetting(settings, key) { getBoolean(key, defaultValue) }.value

@Composable
fun rememberSettingString(
    key: String,
    defaultValue: String,
    settings: SettingsStore = rememberSettingsStore(),
): String = rememberObservedSetting(settings, key) { getString(key, defaultValue) }.value

@Composable
fun rememberSettingInt(
    key: String,
    defaultValue: Int,
    settings: SettingsStore = rememberSettingsStore(),
): Int = rememberObservedSetting(settings, key) { getInt(key, defaultValue) }.value

@Composable
expect fun rememberExternalUrlOpener(): ExternalUrlOpener

@Composable
expect fun rememberSystemUrlOpener(): SystemUrlOpener

@Composable
expect fun rememberZhihuWebUrlOpener(): ZhihuWebUrlOpener

@Composable
expect fun rememberImagePreviewOpener(): ImagePreviewOpener

@Composable
expect fun rememberImageGalleryOpener(): ImageGalleryOpener

@Composable
expect fun rememberImageSaver(): ImageSaver

@Composable
expect fun rememberImageSharer(): ImageSharer

@Composable
expect fun rememberPlainTextClipboard(): PlainTextClipboard

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
