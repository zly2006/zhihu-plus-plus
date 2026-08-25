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
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import com.github.zly2006.zhihu.desktop.DesktopPropertiesFile
import com.github.zly2006.zhihu.desktop.copyDesktopPlainText
import com.github.zly2006.zhihu.desktop.defaultDesktopAccountStore
import com.github.zly2006.zhihu.desktop.desktopZhihuDataDir
import com.github.zly2006.zhihu.desktop.openDesktopExternalUrl
import com.github.zly2006.zhihu.desktop.saveImageToDownloads
import kotlinx.coroutines.launch
import kotlinx.io.files.Path

@Composable
actual fun rememberSettingsStore(): SettingsStore = remember { desktopSettingsStore() }

actual fun Modifier.exportTestTagsForUiAutomation(): Modifier = this

@Composable
actual fun rememberAppPrivateDirectory(): Path = remember { Path(desktopZhihuDataDir().absolutePath) }

fun desktopSettingsStore(): SettingsStore {
    val propertiesFile = DesktopPropertiesFile("settings.properties", "Zhihu++ desktop settings")
    val properties = propertiesFile.properties

    return object : SettingsStore {
        override fun getBoolean(key: String, defaultValue: Boolean) = properties.getProperty(key)?.toBooleanStrictOrNull() ?: defaultValue

        override fun putBoolean(key: String, value: Boolean) = write(key, value.toString())

        override fun getString(key: String, defaultValue: String) = properties.getProperty(key) ?: defaultValue

        override fun putString(key: String, value: String) = write(key, value)

        override fun getStringOrNull(key: String) = properties.getProperty(key)

        override fun putStringSet(key: String, value: Set<String>) = write(key, value.joinToString("\u001F"))

        override fun getStringSet(key: String, defaultValue: Set<String>) = properties
            .getProperty(key)
            ?.split("\u001F")
            ?.filter(String::isNotEmpty)
            ?.toSet() ?: defaultValue

        override fun getInt(key: String, defaultValue: Int) = properties.getProperty(key)?.toIntOrNull() ?: defaultValue

        override fun putInt(key: String, value: Int) = write(key, value.toString())

        override fun getLong(key: String, defaultValue: Long) = properties.getProperty(key)?.toLongOrNull() ?: defaultValue

        override fun putLong(key: String, value: Long) = write(key, value.toString())

        override fun getFloat(key: String, defaultValue: Float) = properties.getProperty(key)?.toFloatOrNull() ?: defaultValue

        override fun putFloat(key: String, value: Float) = write(key, value.toString())

        override fun remove(key: String) {
            properties.remove(key)
            propertiesFile.save()
        }

        private fun write(key: String, value: String) {
            properties.setProperty(key, value)
            propertiesFile.save()
        }
    }
}

@Composable
actual fun rememberExternalUrlOpener(): ExternalUrlOpener = remember {
    object : ExternalUrlOpener {
        override fun invoke(url: String) {
            openDesktopExternalUrl(url)
        }
    }
}

@Composable
actual fun rememberSystemUrlOpener(): SystemUrlOpener = remember {
    object : SystemUrlOpener {
        override fun invoke(url: String) {
            openDesktopExternalUrl(url)
        }
    }
}

@Composable
actual fun rememberZhihuWebUrlOpener(): ZhihuWebUrlOpener = remember {
    object : ZhihuWebUrlOpener {
        override fun invoke(url: String) {
            openDesktopExternalUrl(url)
        }
    }
}

@Composable
actual fun rememberImagePreviewOpener(): ImagePreviewOpener = remember {
    object : ImagePreviewOpener {
        override fun invoke(url: String) {
            openDesktopExternalUrl(url)
        }
    }
}

@Composable
actual fun rememberImageGalleryOpener(): ImageGalleryOpener = remember {
    object : ImageGalleryOpener {
        override fun invoke(urls: List<String>, initialIndex: Int) {
            if (urls.isNotEmpty()) {
                openDesktopExternalUrl(urls[initialIndex.coerceIn(0, urls.lastIndex)])
            }
        }
    }
}

@Composable
actual fun rememberImageSaver(): ImageSaver {
    val scope = rememberCoroutineScope()
    val userMessages = rememberUserMessageSink()
    val store = defaultDesktopAccountStore
    return remember(scope, userMessages, store) {
        object : ImageSaver {
            override fun invoke(url: String) {
                scope.launch {
                    runCatching {
                        store.saveImageToDownloads(url, "image")
                    }.onSuccess { file ->
                        userMessages.showShortMessage("已保存图片: ${file.absolutePath}")
                    }.onFailure { error ->
                        userMessages.showShortMessage("保存失败: ${error.message}")
                    }
                }
            }
        }
    }
}

@Composable
actual fun rememberImageSharer(): ImageSharer {
    val userMessages = rememberUserMessageSink()
    return remember(userMessages) {
        object : ImageSharer {
            override fun invoke(url: String) {
                runCatching {
                    copyDesktopPlainText(url)
                    userMessages.showShortMessage("已复制图片链接")
                }.onFailure { error ->
                    userMessages.showShortMessage("分享失败: ${error.message}")
                }
            }
        }
    }
}

@Composable
actual fun rememberPlainTextClipboard(): PlainTextClipboard =
    remember {
        object : PlainTextClipboard {
            override fun invoke(label: String, text: String) {
                runCatching { copyDesktopPlainText(text) }
            }
        }
    }

@Composable
actual fun rememberUserMessageSink(): UserMessageSink = remember {
    object : UserMessageSink {
        override fun showShortMessage(message: String) {
            println(message)
            runCatching {
                ProcessBuilder("terminal-notifier", "-message", message, "-sound", "default")
                    .start()
            }
        }
    }
}

@Composable
actual fun PlatformBackHandler(
    enabled: Boolean,
    onBack: () -> Unit,
) = Unit // TODO: desktop back handler

@Composable
actual fun PlatformPredictiveBackHandler(
    enabled: Boolean,
    onProgress: (Float) -> Unit,
    onCancel: () -> Unit,
    onBack: () -> Unit,
) = PlatformBackHandler(enabled = enabled, onBack = onBack)

@Composable
actual fun rememberIsLiteVariant(): Boolean = false

internal actual val platformBottomBarItemLimit: Int? = null

actual val platformName: String = "JVM"

actual val isAigcVoteSupported: Boolean = false

actual val isBlocklistNlpSupported: Boolean = false

actual val isSentenceSimilaritySupported: Boolean = false

actual val isArticleHtmlExportSupported: Boolean = true

actual val isArticleImageExportSupported: Boolean = true
