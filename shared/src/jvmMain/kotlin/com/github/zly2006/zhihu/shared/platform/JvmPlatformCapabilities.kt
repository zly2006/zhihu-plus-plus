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

package com.github.zly2006.zhihu.shared.platform
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import com.github.zly2006.zhihu.shared.desktop.DesktopAccountStore
import com.github.zly2006.zhihu.shared.desktop.copyDesktopPlainText
import com.github.zly2006.zhihu.shared.desktop.desktopZhihuDataFile
import com.github.zly2006.zhihu.shared.desktop.openDesktopExternalUrl
import com.github.zly2006.zhihu.shared.desktop.saveImageToDownloads
import java.util.Properties
import kotlinx.coroutines.launch

@Composable
actual fun rememberSettingsStore(): SettingsStore = remember { desktopSettingsStore() }

private val desktopSettingsChangeListeners = mutableSetOf<(String) -> Unit>()

private fun notifyDesktopSettingChanged(key: String) {
    desktopSettingsChangeListeners.toList().forEach { it(key) }
}

fun desktopSettingsStore(): SettingsStore {
    val settingsFile = desktopZhihuDataFile("settings.properties")
    val properties = Properties()

    fun load() {
        properties.clear()
        if (settingsFile.isFile) settingsFile.inputStream().use(properties::load)
    }

    fun save() {
        settingsFile.parentFile?.mkdirs()
        settingsFile.outputStream().use { output ->
            properties.store(output, "Zhihu++ desktop settings")
        }
    }

    load()

    return SettingsStore(
        getBoolean = { key, defaultValue ->
            properties.getProperty(key)?.toBooleanStrictOrNull() ?: defaultValue
        },
        putBoolean = { key, value ->
            properties.setProperty(key, value.toString())
            save()
            notifyDesktopSettingChanged(key)
        },
        getString = { key, defaultValue ->
            properties.getProperty(key) ?: defaultValue
        },
        putString = { key, value ->
            properties.setProperty(key, value)
            save()
            notifyDesktopSettingChanged(key)
        },
        getStringOrNull = { key ->
            properties.getProperty(key)
        },
        putStringSet = { key, value ->
            properties.setProperty(key, value.joinToString("\u001F"))
            save()
            notifyDesktopSettingChanged(key)
        },
        getStringSet = { key, defaultValue ->
            properties
                .getProperty(key)
                ?.split("\u001F")
                ?.filter { it.isNotEmpty() }
                ?.toSet() ?: defaultValue
        },
        getInt = { key, defaultValue ->
            properties.getProperty(key)?.toIntOrNull() ?: defaultValue
        },
        putInt = { key, value ->
            properties.setProperty(key, value.toString())
            save()
            notifyDesktopSettingChanged(key)
        },
        getLong = { key, defaultValue ->
            properties.getProperty(key)?.toLongOrNull() ?: defaultValue
        },
        putLong = { key, value ->
            properties.setProperty(key, value.toString())
            save()
            notifyDesktopSettingChanged(key)
        },
        getFloat = { key, defaultValue ->
            properties.getProperty(key)?.toFloatOrNull() ?: defaultValue
        },
        putFloat = { key, value ->
            properties.setProperty(key, value.toString())
            save()
            notifyDesktopSettingChanged(key)
        },
        remove = { key ->
            properties.remove(key)
            save()
            notifyDesktopSettingChanged(key)
        },
        observeKeyChanges = { onChanged ->
            val listener: (String) -> Unit = { key ->
                load()
                onChanged(key)
            }
            desktopSettingsChangeListeners += listener
            { desktopSettingsChangeListeners -= listener }
        },
    )
}

@Composable
actual fun rememberExternalUrlOpener(): (String) -> Unit = remember { { url -> openDesktopExternalUrl(url) } }

@Composable
actual fun rememberSystemUrlOpener(): (String) -> Unit = rememberExternalUrlOpener()

@Composable
actual fun rememberZhihuWebUrlOpener(): (String) -> Unit = rememberExternalUrlOpener()

@Composable
actual fun rememberImagePreviewOpener(): (String) -> Unit = rememberExternalUrlOpener()

@Composable
actual fun rememberImageGalleryOpener(): (List<String>, Int) -> Unit {
    val openExternalUrl = rememberExternalUrlOpener()
    return remember(openExternalUrl) {
        { urls, initialIndex ->
            if (urls.isNotEmpty()) {
                urls[initialIndex.coerceIn(0, urls.lastIndex)].let(openExternalUrl)
            }
        }
    }
}

@Composable
actual fun rememberImageSaver(): (String) -> Unit {
    val scope = rememberCoroutineScope()
    val userMessages = rememberUserMessageSink()
    val store = remember { DesktopAccountStore() }
    return remember(scope, userMessages, store) {
        { imageUrl ->
            scope.launch {
                runCatching {
                    store.saveImageToDownloads(imageUrl, "image")
                }.onSuccess { file ->
                    userMessages.showShortMessage("已保存图片: ${file.absolutePath}")
                }.onFailure { error ->
                    userMessages.showShortMessage("保存失败: ${error.message}")
                }
            }
        }
    }
}

@Composable
actual fun rememberImageSharer(): (String) -> Unit {
    val userMessages = rememberUserMessageSink()
    return remember(userMessages) {
        { imageUrl ->
            runCatching {
                copyDesktopPlainText(imageUrl)
                userMessages.showShortMessage("已复制图片链接")
            }.onFailure { error ->
                userMessages.showShortMessage("分享失败: ${error.message}")
            }
        }
    }
}

@Composable
actual fun rememberPlainTextClipboard(): (label: String, text: String) -> Unit =
    remember { { _, text -> runCatching { copyDesktopPlainText(text) } } }

@Composable
actual fun rememberDeveloperDiagnostics(): DeveloperDiagnostics = remember {
    DeveloperDiagnostics(
        appInfo = "desktop",
        deviceInfo = "${System.getProperty("os.name")} ${System.getProperty("os.version")}",
        networkStatus = "未知",
        readClipboardText = {
            runCatching {
                java.awt.Toolkit
                    .getDefaultToolkit()
                    .systemClipboard
                    .getData(java.awt.datatransfer.DataFlavor.stringFlavor) as? String
            }.getOrNull()
        },
        exportAllSettings = {
            runCatching {
                val file = desktopZhihuDataFile("settings.properties")
                if (file.isFile) {
                    Properties()
                        .apply { file.inputStream().use(::load) }
                        .entries
                        .joinToString("\n") { "${it.key}: ${it.value}" }
                } else {
                    "(空)"
                }
            }.getOrDefault("(空)")
        },
    )
}

@Composable
actual fun rememberUserMessageSink(): UserMessageSink = remember { UserMessageSink(::showDesktopMessage) }

private fun showDesktopMessage(message: String) {
    println(message)
    runCatching {
        ProcessBuilder("terminal-notifier", "-message", message, "-sound", "default")
            .start()
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
