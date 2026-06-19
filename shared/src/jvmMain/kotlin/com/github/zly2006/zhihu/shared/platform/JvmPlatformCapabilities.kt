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
import com.github.zly2006.zhihu.shared.desktop.DesktopPropertiesFile
import com.github.zly2006.zhihu.shared.desktop.copyDesktopPlainText
import com.github.zly2006.zhihu.shared.desktop.openDesktopExternalUrl

@Composable
actual fun rememberSettingsStore(): SettingsStore = remember { desktopSettingsStore() }

fun desktopSettingsStore(): SettingsStore {
    val propertiesFile = DesktopPropertiesFile("settings.properties", "Zhihu++ desktop settings")
    val properties = propertiesFile.properties

    return SettingsStore(
        getBoolean = { key, defaultValue ->
            properties.getProperty(key)?.toBooleanStrictOrNull() ?: defaultValue
        },
        putBoolean = { key, value ->
            properties.setProperty(key, value.toString())
            propertiesFile.save()
        },
        getString = { key, defaultValue ->
            properties.getProperty(key) ?: defaultValue
        },
        putString = { key, value ->
            properties.setProperty(key, value)
            propertiesFile.save()
        },
        getStringOrNull = { key ->
            properties.getProperty(key)
        },
        putStringSet = { key, value ->
            properties.setProperty(key, value.joinToString("\u001F"))
            propertiesFile.save()
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
            propertiesFile.save()
        },
        getLong = { key, defaultValue ->
            properties.getProperty(key)?.toLongOrNull() ?: defaultValue
        },
        putLong = { key, value ->
            properties.setProperty(key, value.toString())
            propertiesFile.save()
        },
        getFloat = { key, defaultValue ->
            properties.getProperty(key)?.toFloatOrNull() ?: defaultValue
        },
        putFloat = { key, value ->
            properties.setProperty(key, value.toString())
            propertiesFile.save()
        },
        remove = { key ->
            properties.remove(key)
            propertiesFile.save()
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
actual fun rememberPlainTextClipboard(): (label: String, text: String) -> Unit =
    remember { { _, text -> runCatching { copyDesktopPlainText(text) } } }

@Composable
actual fun rememberUserMessageSink(): UserMessageSink = remember {
    UserMessageSink(
        showShortMessage = { message ->
            println(message)
            runCatching {
                ProcessBuilder("terminal-notifier", "-message", message, "-sound", "default")
                    .start()
            }
        },
    )
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
