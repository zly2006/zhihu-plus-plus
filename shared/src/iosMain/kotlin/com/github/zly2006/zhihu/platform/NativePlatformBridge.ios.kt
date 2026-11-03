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
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.uikit.LocalUIViewController
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.io.RawSource
import kotlinx.io.files.Path
import kotlinx.io.files.SystemFileSystem
import platform.Foundation.NSDocumentDirectory
import platform.Foundation.NSFileManager
import platform.Foundation.NSTemporaryDirectory
import platform.Foundation.NSURL
import platform.Foundation.NSUserDomainMask
import platform.UIKit.UIApplication
import platform.UIKit.UIDocumentPickerDelegateProtocol
import platform.UIKit.UIDocumentPickerViewController
import platform.UIKit.UIPasteboard
import platform.UniformTypeIdentifiers.UTType
import platform.UniformTypeIdentifiers.UTTypeData
import platform.darwin.NSObject

internal actual val nativeIsDesktop: Boolean = false

actual val platformName: String = "iOS"

actual val isAigcVoteSupported: Boolean = false

@Composable
@OptIn(ExperimentalForeignApi::class)
actual fun rememberExternalUrlOpener(): ExternalUrlOpener = remember {
    object : ExternalUrlOpener {
        override fun invoke(url: String) {
            NSURL.URLWithString(url)?.let(UIApplication.sharedApplication::openURL)
        }
    }
}

@Composable
actual fun rememberWebViewUrlOpener(): WebViewUrlOpener = remember {
    object : WebViewUrlOpener {
        override fun invoke(url: String) {
            NSURL.URLWithString(url)?.let(UIApplication.sharedApplication::openURL)
        }
    }
}

@Composable
actual fun rememberImageGalleryOpener(): ImageGalleryOpener {
    val openExternalUrl = rememberExternalUrlOpener()
    return remember(openExternalUrl) {
        object : ImageGalleryOpener {
            override fun invoke(urls: List<String>, initialIndex: Int) {
                if (urls.isNotEmpty()) {
                    openExternalUrl(urls[initialIndex.coerceIn(0, urls.lastIndex)])
                }
            }
        }
    }
}

internal actual fun copyNativePlainText(text: String) {
    UIPasteboard.generalPasteboard.string = text
}

@OptIn(ExperimentalForeignApi::class)
internal actual fun nativeAccountFilePath(): String = "${nativeAppPrivateDirectoryPath()}/account.json"

@OptIn(ExperimentalForeignApi::class)
internal actual fun nativeAppPrivateDirectoryPath(): String {
    val urls = NSFileManager.defaultManager.URLsForDirectory(NSDocumentDirectory, NSUserDomainMask)
    return (urls.firstOrNull() as? NSURL)?.path ?: NSTemporaryDirectory()
}

internal actual fun nativeDownloadsDirectoryPath(): String = "${nativeAppPrivateDirectoryPath()}/Downloads"

@Composable
@OptIn(ExperimentalForeignApi::class)
internal actual fun rememberFilePicker(
    title: String,
    mimeTypes: List<String>,
    extensions: List<String>,
    onPicked: (() -> Pair<String, RawSource>) -> Unit,
): () -> Unit {
    val host = LocalUIViewController.current
    val currentOnPicked by rememberUpdatedState(onPicked)
    var activePicker by remember { mutableStateOf<UIDocumentPickerViewController?>(null) }
    val delegate = remember {
        object : NSObject(), UIDocumentPickerDelegateProtocol {
            override fun documentPicker(controller: UIDocumentPickerViewController, didPickDocumentsAtURLs: List<*>) {
                activePicker = null
                val url = didPickDocumentsAtURLs.firstOrNull() as? NSURL ?: return
                currentOnPicked {
                    val file = Path(checkNotNull(url.path))
                    val source = SystemFileSystem.source(file)
                    // Copy mode gives an app-owned temporary file, never the provider's original.
                    (url.lastPathComponent ?: "所选文件") to object : RawSource by source {
                        override fun close() {
                            try {
                                source.close()
                            } finally {
                                runCatching { SystemFileSystem.delete(file, mustExist = false) }
                            }
                        }
                    }
                }
            }

            override fun documentPickerWasCancelled(controller: UIDocumentPickerViewController) {
                activePicker = null
            }
        }
    }
    DisposableEffect(host) {
        onDispose {
            activePicker?.delegate = null
            activePicker?.dismissViewControllerAnimated(false, completion = null)
            activePicker = null
        }
    }
    return {
        if (activePicker == null) {
            val types = extensions
                .mapNotNull { UTType.typeWithFilenameExtension(it) }
                .ifEmpty { mimeTypes.mapNotNull { UTType.typeWithMIMEType(it) } }
                .ifEmpty { listOf(UTTypeData) }
            val picker = UIDocumentPickerViewController(forOpeningContentTypes = types, asCopy = true)
            picker.title = title
            picker.allowsMultipleSelection = false
            picker.shouldShowFileExtensions = true
            picker.delegate = delegate
            activePicker = picker
            host.presentViewController(picker, animated = true, completion = null)
        }
    }
}

@Composable
actual fun rememberUserMessageSink(): UserMessageSink = remember {
    object : UserMessageSink {
        override fun showShortMessage(message: String) = println(message)
    }
}

internal actual val platformBottomBarItemLimit: Int? = 5
