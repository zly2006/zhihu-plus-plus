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
import com.github.zly2006.zhihu.account.ZhihuAccountStore
import com.github.zly2006.zhihu.account.defaultNativeAccountStore
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
actual fun rememberSystemUrlOpener(): SystemUrlOpener = rememberExternalUrlOpener()

@Composable
actual fun rememberZhihuWebUrlOpener(): ZhihuWebUrlOpener = rememberExternalUrlOpener()

@Composable
actual fun rememberImagePreviewOpener(): ImagePreviewOpener = rememberExternalUrlOpener()

@Composable
actual fun rememberImageSaver(): ImageSaver {
    val scope = rememberCoroutineScope()
    val userMessages = rememberUserMessageSink()
    val accountStore = defaultNativeAccountStore
    return remember(scope, userMessages, accountStore) {
        object : ImageSaver {
            override fun invoke(url: String) {
                scope.launch {
                    runCatching {
                        saveNativeImageToDownloads(accountStore, url)
                    }.onSuccess { filePath ->
                        userMessages.showShortMessage("已保存图片: $filePath")
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
                    copyNativePlainText(url)
                    userMessages.showShortMessage("已复制图片链接")
                }.onFailure { error ->
                    userMessages.showShortMessage("分享失败: ${error.message}")
                }
            }
        }
    }
}

@OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)
private suspend fun saveNativeImageToDownloads(
    accountStore: ZhihuAccountStore,
    imageUrl: String,
): String {
    val imageBytes = accountStore.client
        .httpClient()
        .get(imageUrl)
        .body<ByteArray>()
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
    val filePath = "$downloadsDirectory/image_${Clock.System.now().toEpochMilliseconds()}.$extension"
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
actual fun rememberPlainTextClipboard(): PlainTextClipboard = remember {
    object : PlainTextClipboard {
        override fun invoke(label: String, text: String) = copyNativePlainText(text)
    }
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
actual fun rememberAppPrivateDirectory(): Path = remember { Path(nativeAppPrivateDirectoryPath()) }

@Composable
actual fun rememberIsLiteVariant(): Boolean = false

actual val isBlocklistNlpSupported: Boolean = false

actual val isSentenceSimilaritySupported: Boolean = false

actual val isArticleHtmlExportSupported: Boolean = false

actual val isArticleImageExportSupported: Boolean = false
