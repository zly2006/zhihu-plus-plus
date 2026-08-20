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

import com.github.zly2006.zhihu.data.macosAppDataDirectoryPath
import com.github.zly2006.zhihu.data.macosBackgroundUiDebugDataDirectoryPath
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import platform.AppKit.NSAppearanceNameAqua
import platform.AppKit.NSAppearanceNameDarkAqua
import platform.AppKit.NSApplication
import platform.AppKit.NSModalResponseOK
import platform.AppKit.NSOpenPanel
import platform.AppKit.NSPasteboard
import platform.AppKit.NSPasteboardTypeString
import platform.AppKit.NSWorkspace
import platform.AppKit.effectiveAppearance
import platform.Foundation.NSBundle
import platform.Foundation.NSHomeDirectory
import platform.Foundation.NSURL

internal actual val nativePlatformName: String = "macOS Kotlin/Native"

internal actual val nativeIsDesktop: Boolean = true

internal actual val nativeAppVersionName: String
    get() = NSBundle.mainBundle.objectForInfoDictionaryKey("CFBundleShortVersionString") as? String ?: "0.0.0"

private val macosQrLoginRequests = MutableStateFlow(0)

internal actual val nativeQrLoginRequestVersion: StateFlow<Int> = macosQrLoginRequests.asStateFlow()

@OptIn(ExperimentalForeignApi::class)
internal actual fun openNativeExternalUrl(url: String) {
    NSURL.URLWithString(url)?.let(NSWorkspace.sharedWorkspace::openURL)
}

@OptIn(ExperimentalForeignApi::class)
internal actual fun copyNativePlainText(text: String) {
    NSPasteboard.generalPasteboard.apply {
        clearContents()
        setString(text, forType = NSPasteboardTypeString)
    }
}

internal actual fun requestNativeQrLogin() {
    macosQrLoginRequests.update { it + 1 }
}

internal actual fun nativeAccountFilePath(): String =
    macosBackgroundUiDebugDataDirectoryPath()?.let { "$it/account.json" }
        ?: "${NSHomeDirectory()}/.zhihu-plus-plus/account.json"

internal actual fun nativeAppPrivateDirectoryPath(): String =
    macosAppDataDirectoryPath()

internal actual fun nativeDownloadsDirectoryPath(): String =
    macosBackgroundUiDebugDataDirectoryPath()?.let { "$it/Downloads" }
        ?: "${NSHomeDirectory()}/Downloads"

@OptIn(ExperimentalForeignApi::class)
internal actual fun nativeChooseBlocklistImportFilePath(): String? {
    val panel = NSOpenPanel.openPanel()
    panel.title = "导入屏蔽规则"
    panel.canChooseFiles = true
    panel.canChooseDirectories = false
    panel.allowsMultipleSelection = false
    return if (panel.runModal() == NSModalResponseOK) panel.URL?.path else null
}

internal actual fun nativeBundledResourcePath(relativePath: String): String? =
    NSBundle.mainBundle.resourcePath?.let { resourceDirectory -> "$resourceDirectory/$relativePath" }

@OptIn(ExperimentalForeignApi::class)
internal actual fun nativeSystemInDarkTheme(): Boolean =
    NSApplication.sharedApplication
        .effectiveAppearance()
        .bestMatchFromAppearancesWithNames(listOf(NSAppearanceNameAqua, NSAppearanceNameDarkAqua)) == NSAppearanceNameDarkAqua

internal data class MacosUserMessage(
    val text: String,
    val duration: UserMessageDuration,
)

internal val macosUserMessages = Channel<MacosUserMessage>(capacity = Channel.UNLIMITED)

fun showMacosUserMessage(
    message: String,
    duration: UserMessageDuration = UserMessageDuration.Short,
) {
    println(message)
    check(macosUserMessages.trySend(MacosUserMessage(message, duration)).isSuccess) {
        "macOS user message queue is unavailable"
    }
}

internal actual fun showNativeUserMessage(
    message: String,
    duration: UserMessageDuration,
) = showMacosUserMessage(message, duration)
