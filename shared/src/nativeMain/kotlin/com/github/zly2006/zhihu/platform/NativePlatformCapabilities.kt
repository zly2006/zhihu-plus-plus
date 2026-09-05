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

import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.InternalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.backhandler.BackHandler
import androidx.compose.ui.backhandler.LocalCompatNavigationEventDispatcherOwner
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.navigationevent.NavigationEventDispatcher
import androidx.navigationevent.NavigationEventDispatcherOwner
import androidx.navigationevent.NavigationEventInput
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

private class DesktopEscapeInput : NavigationEventInput() {
    fun dispatchBack() = dispatchOnBackCompleted()
}

@OptIn(InternalComposeUiApi::class)
@Composable
actual fun DesktopBackDispatcherHost(content: @Composable () -> Unit) {
    val dispatcher = remember { NavigationEventDispatcher() }
    val owner = remember(dispatcher) {
        object : NavigationEventDispatcherOwner {
            override val navigationEventDispatcher: NavigationEventDispatcher = dispatcher
        }
    }
    val escapeInput = remember { DesktopEscapeInput() }
    val focusRequester = remember { FocusRequester() }
    // Keep a root focus target so window key events still enter Compose when no child is focusable.
    LaunchedEffect(focusRequester) {
        focusRequester.requestFocus()
    }
    DisposableEffect(dispatcher, escapeInput) {
        dispatcher.addInput(escapeInput)
        onDispose { dispatcher.dispose() }
    }
    CompositionLocalProvider(LocalCompatNavigationEventDispatcherOwner provides owner) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .focusRequester(focusRequester)
                .focusable()
                .onPreviewKeyEvent { event ->
                    if (event.key == Key.Escape && event.type == KeyEventType.KeyUp) {
                        escapeInput.dispatchBack()
                        true
                    } else {
                        false
                    }
                },
        ) {
            content()
        }
    }
}

@Composable
actual fun rememberAppPrivateDirectory(): Path = remember { Path(nativeAppPrivateDirectoryPath()) }

@Composable
actual fun rememberIsLiteVariant(): Boolean = false

actual val isJvm: Boolean = false

actual val isNative: Boolean = true

actual val isBlocklistNlpSupported: Boolean = false

actual val isSentenceSimilaritySupported: Boolean = false

actual val isArticleHtmlExportSupported: Boolean = false

actual val isArticleImageExportSupported: Boolean = false
