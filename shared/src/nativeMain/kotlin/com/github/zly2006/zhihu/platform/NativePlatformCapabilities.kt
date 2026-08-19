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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.backhandler.BackHandler
import androidx.compose.ui.platform.testTag
import com.github.zly2006.zhihu.account.IosAccountStore
import io.ktor.client.call.body
import io.ktor.client.request.get
import kotlinx.cinterop.BetaInteropApi
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.usePinned
import kotlinx.coroutines.launch
import kotlinx.io.files.Path
import platform.Foundation.NSData
import platform.Foundation.NSFileManager
import platform.Foundation.dataWithBytes
import kotlin.time.Clock

@Composable
actual fun rememberExternalUrlOpener(): (String) -> Unit = remember { ::openNativeExternalUrl }

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
    val accountStore = remember { IosAccountStore() }
    return remember(scope, userMessages, accountStore) {
        { imageUrl ->
            scope.launch {
                runCatching {
                    saveNativeImageToDownloads(accountStore, imageUrl, "image")
                }.onSuccess { filePath ->
                    userMessages.showShortMessage("已保存图片: $filePath")
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
                copyNativePlainText(imageUrl)
                userMessages.showShortMessage("已复制图片链接")
            }.onFailure { error ->
                userMessages.showShortMessage("分享失败: ${error.message}")
            }
        }
    }
}

@OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)
private suspend fun saveNativeImageToDownloads(
    accountStore: IosAccountStore,
    imageUrl: String,
    filePrefix: String,
): String {
    val imageBytes = accountStore.httpClient().get(imageUrl).body<ByteArray>()
    val extension = imageUrl
        .substringBefore('?')
        .substringAfterLast('/')
        .substringAfterLast('.', "")
        .takeIf { it.length in 2..5 } ?: "jpg"
    val downloadsDirectory = nativeDownloadsDirectoryPath()
    NSFileManager.defaultManager.createDirectoryAtPath(
        downloadsDirectory,
        withIntermediateDirectories = true,
        attributes = null,
        error = null,
    )
    val filePath = "$downloadsDirectory/${filePrefix}_${Clock.System.now().toEpochMilliseconds()}.$extension"
    val written = imageBytes.usePinned { pinned ->
        NSFileManager.defaultManager.createFileAtPath(
            filePath,
            contents = NSData.dataWithBytes(pinned.addressOf(0), imageBytes.size.toULong()),
            attributes = null,
        )
    }
    check(written) { "无法写入 $filePath" }
    return filePath
}

@Composable
actual fun rememberPlainTextClipboard(): (label: String, text: String) -> Unit = remember {
    { _, text -> copyNativePlainText(text) }
}

@OptIn(ExperimentalComposeUiApi::class)
@Suppress("DEPRECATION")
@Composable
actual fun PlatformBackHandler(enabled: Boolean, onBack: () -> Unit) =
    BackHandler(enabled = enabled, onBack = onBack)

@Composable
actual fun PlatformPredictiveBackHandler(
    enabled: Boolean,
    onProgress: (Float) -> Unit,
    onCancel: () -> Unit,
    onBack: () -> Unit,
) = PlatformBackHandler(enabled = enabled, onBack = onBack)

@Composable
actual fun rememberSettingsStore(): SettingsStore = remember { nativeSettingsStore("settings.properties") }

actual fun Modifier.exportTestTagsForUiAutomation(): Modifier = this

@Composable
actual fun Modifier.platformBackNavigationHost(navigationKey: Any?): Modifier =
    testTag("platform_back_navigation_host")

@Composable
actual fun rememberAppPrivateDirectory(): Path = remember { Path(nativeAppPrivateDirectoryPath()) }

@Composable
actual fun rememberIsLiteVariant(): Boolean = false

@Composable
actual fun rememberUserMessageSink(): UserMessageSink = remember {
    UserMessageSink(showShortMessage = ::showNativeUserMessage)
}
