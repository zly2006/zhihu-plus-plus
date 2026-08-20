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
import com.github.zly2006.zhihu.ui.noopSettingsStore
import kotlinx.cinterop.ExperimentalForeignApi
import platform.Foundation.NSDocumentDirectory
import platform.Foundation.NSFileManager
import platform.Foundation.NSTemporaryDirectory
import platform.Foundation.NSURL
import platform.Foundation.NSUserDomainMask
import platform.UIKit.UIApplication
import platform.UIKit.UIPasteboard

internal actual val nativeIsDesktop: Boolean = false

@Composable
@OptIn(ExperimentalForeignApi::class)
actual fun rememberExternalUrlOpener(): (String) -> Unit = remember {
    { url -> NSURL.URLWithString(url)?.let(UIApplication.sharedApplication::openURL) }
}

internal actual fun copyNativePlainText(text: String) {
    UIPasteboard.generalPasteboard.string = text
}

internal actual fun requestNativeQrLogin() = Unit

@OptIn(ExperimentalForeignApi::class)
internal actual fun nativeAccountFilePath(): String = "${nativeAppPrivateDirectoryPath()}/account.json"

@OptIn(ExperimentalForeignApi::class)
internal actual fun nativeAppPrivateDirectoryPath(): String {
    val urls = NSFileManager.defaultManager.URLsForDirectory(NSDocumentDirectory, NSUserDomainMask)
    return (urls.firstOrNull() as? NSURL)?.path ?: NSTemporaryDirectory()
}

internal actual fun nativeDownloadsDirectoryPath(): String = "${nativeAppPrivateDirectoryPath()}/Downloads"

internal actual fun nativeChooseBlocklistImportFilePath(): String? = null

internal actual fun nativeSettingsStore(relativePath: String): SettingsStore = noopSettingsStore()

@Composable
actual fun rememberUserMessageSink(): UserMessageSink = remember {
    UserMessageSink(showShortMessage = ::println)
}
