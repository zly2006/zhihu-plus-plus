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

import com.github.zly2006.zhihu.ui.noopSettingsStore
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import platform.Foundation.NSBundle
import platform.Foundation.NSDocumentDirectory
import platform.Foundation.NSFileManager
import platform.Foundation.NSTemporaryDirectory
import platform.Foundation.NSURL
import platform.Foundation.NSUserDomainMask
import platform.UIKit.UIApplication
import platform.UIKit.UIPasteboard
import platform.UIKit.UITraitCollection
import platform.UIKit.UIUserInterfaceStyle
import platform.UIKit.currentTraitCollection

internal actual val nativePlatformName: String = "iOS"

internal actual val nativeIsDesktop: Boolean = false

internal actual val nativeAppVersionName: String
    get() = NSBundle.mainBundle.objectForInfoDictionaryKey("CFBundleShortVersionString") as? String ?: "0.0.0"

private val iosQrLoginRequests = MutableStateFlow(0)

internal actual val nativeQrLoginRequestVersion: StateFlow<Int> = iosQrLoginRequests.asStateFlow()

@OptIn(ExperimentalForeignApi::class)
internal actual fun openNativeExternalUrl(url: String) {
    NSURL.URLWithString(url)?.let(UIApplication.sharedApplication::openURL)
}

internal actual fun copyNativePlainText(text: String) {
    UIPasteboard.generalPasteboard.string = text
}

internal actual fun requestNativeQrLogin() {
    iosQrLoginRequests.update { it + 1 }
}

@OptIn(ExperimentalForeignApi::class)
internal actual fun nativeAccountFilePath(): String = "${nativeAppPrivateDirectoryPath()}/account.json"

@OptIn(ExperimentalForeignApi::class)
internal actual fun nativeAppPrivateDirectoryPath(): String {
    val urls = NSFileManager.defaultManager.URLsForDirectory(NSDocumentDirectory, NSUserDomainMask)
    return (urls.firstOrNull() as? NSURL)?.path ?: NSTemporaryDirectory()
}

internal actual fun nativeDownloadsDirectoryPath(): String = "${nativeAppPrivateDirectoryPath()}/Downloads"

internal actual fun nativeChooseBlocklistImportFilePath(): String? = null

internal actual fun nativeBundledResourcePath(relativePath: String): String? =
    NSBundle.mainBundle.resourcePath?.let { resourceDirectory -> "$resourceDirectory/$relativePath" }

internal actual fun nativeSettingsStore(relativePath: String): SettingsStore = noopSettingsStore()

@OptIn(ExperimentalForeignApi::class)
internal actual fun nativeSystemInDarkTheme(): Boolean =
    UITraitCollection.currentTraitCollection.userInterfaceStyle == UIUserInterfaceStyle.UIUserInterfaceStyleDark

internal actual fun showNativeUserMessage(message: String) {
    println(message)
}
